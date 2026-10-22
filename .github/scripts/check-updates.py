#!/usr/bin/env python3
"""Check Modrinth for new Litematica, MaLiLib and Iris releases.

Updates gradle.properties and the version ranges in fabric.mod.json in place, and writes:
  - update-summary.md   pull request body, if any dependency was updated
  - new-minecraft.md    issue body, if Litematica supports a newer Minecraft version
and sets the outputs `updated`, `title` and `new_minecraft` for GitHub Actions.
"""

import json
import os
import re
import sys
import urllib.parse
import urllib.request

API = "https://api.modrinth.com/v2"
USER_AGENT = "mmueller96/schematic-shader-fix (update check)"
PROPERTIES = "gradle.properties"
MOD_JSON = "src/main/resources/fabric.mod.json"

# Modrinth project slug -> gradle.properties key
DEPENDENCIES = {
	"litematica": "litematica_version",
	"malilib": "malilib_version",
	"iris": "iris_version",
}


def api_get(path, **params):
	query = urllib.parse.urlencode({key: json.dumps(value) for key, value in params.items()})
	request = urllib.request.Request(f"{API}{path}?{query}" if query else f"{API}{path}",
	                                 headers={"User-Agent": USER_AGENT})
	with urllib.request.urlopen(request, timeout=30) as response:
		return json.load(response)


def read_properties():
	with open(PROPERTIES, encoding="utf-8") as file:
		text = file.read()
	properties = dict(re.findall(r"^(\w+)\s*=\s*(.*?)\s*$", text, re.MULTILINE))
	return text, properties


def base_version(version_number):
	"""'1.11.7+26.3-fabric' -> (1, 11, 7)"""
	numbers = re.match(r"\d+(?:\.\d+)*", version_number)
	return tuple(int(part) for part in numbers.group(0).split(".")) if numbers else ()


def latest_release(slug, minecraft_version):
	versions = api_get(f"/project/{slug}/version", loaders=["fabric"], game_versions=[minecraft_version])
	releases = [version for version in versions if version["version_type"] == "release"]
	return max(releases, key=lambda version: version["date_published"], default=None)


def raise_upper_bound(mod_json, slug, version_number):
	"""Move the '<x.y' upper bound of a dependency past the new version if needed."""
	pattern = re.compile(rf'("{re.escape(slug)}":\s*"[^"]*<)(\d+)\.(\d+)(")')
	match = pattern.search(mod_json)
	new = base_version(version_number)

	if match is None or len(new) < 2:
		return mod_json

	bound = (int(match.group(2)), int(match.group(3)))

	if new[:2] < bound:
		return mod_json

	return pattern.sub(rf"\g<1>{new[0]}.{new[1] + 1}\g<4>", mod_json, count=1)


def newer_minecraft_versions(minecraft_version):
	"""Release Minecraft versions newer than ours that Litematica already supports."""
	releases = [tag["version"] for tag in api_get("/tag/game_version") if tag["version_type"] == "release"]

	if minecraft_version not in releases:
		return []

	newer = releases[:releases.index(minecraft_version)]  # the tag list is sorted newest first
	supported = set()

	for version in api_get("/project/litematica/version", loaders=["fabric"]):
		supported.update(version["game_versions"])

	return [version for version in newer if version in supported]


def set_output(name, value):
	output = os.environ.get("GITHUB_OUTPUT")

	if output:
		with open(output, "a", encoding="utf-8") as file:
			file.write(f"{name}={value}\n")
	else:
		print(f"{name}={value}")


def main():
	text, properties = read_properties()
	minecraft_version = properties["minecraft_version"]

	with open(MOD_JSON, encoding="utf-8") as file:
		mod_json = file.read()

	updates = []
	current_numbers = {}

	for slug, key in DEPENDENCIES.items():
		current_id = properties[key]
		current = api_get(f"/version/{current_id}")
		current_numbers[slug] = current["version_number"]
		latest = latest_release(slug, minecraft_version)

		if latest is None or latest["id"] == current_id or latest["date_published"] <= current["date_published"]:
			print(f"{slug}: {current['version_number']} is up to date")
			continue

		print(f"{slug}: {current['version_number']} -> {latest['version_number']}")
		updates.append((slug, current["version_number"], latest))
		text = re.sub(rf"^({key}\s*=\s*).*$", rf"\g<1>{latest['id']}", text, count=1, flags=re.MULTILINE)
		mod_json = raise_upper_bound(mod_json, slug, latest["version_number"])
		current_numbers[slug] = latest["version_number"]

	if updates:
		summary_line = ", ".join(f"{slug} {current_numbers[slug]}" for slug in DEPENDENCIES)
		text = re.sub(r"^# litematica .*$", f"# {summary_line}", text, count=1, flags=re.MULTILINE)

		with open(PROPERTIES, "w", encoding="utf-8") as file:
			file.write(text)

		with open(MOD_JSON, "w", encoding="utf-8") as file:
			file.write(mod_json)

		lines = [f"Automatic dependency update for Minecraft {minecraft_version}.", "",
		         "| Mod | Old | New |", "|---|---|---|"]

		for slug, old, latest in updates:
			url = f"https://modrinth.com/mod/{slug}/version/{latest['id']}"
			lines.append(f"| {slug} | {old} | [{latest['version_number']}]({url}) |")

		with open("update-summary.md", "w", encoding="utf-8") as file:
			file.write("\n".join(lines) + "\n")

		title = "Update " + ", ".join(f"{slug} to {latest['version_number']}" for slug, _, latest in updates)
		set_output("title", title)

	set_output("updated", "true" if updates else "false")

	newer = newer_minecraft_versions(minecraft_version)

	if newer:
		print(f"Litematica supports newer Minecraft versions: {', '.join(newer)}")

		with open("new-minecraft.md", "w", encoding="utf-8") as file:
			file.write(f"Litematica is available for Minecraft {', '.join(newer)}, "
			           f"this addon is built for {minecraft_version}.\n\n"
			           "A new branch for the new Minecraft version is needed. "
			           "Check first whether the shader bugs still exist there.\n")

	set_output("new_minecraft", newer[0] if newer else "")


if __name__ == "__main__":
	sys.exit(main())
