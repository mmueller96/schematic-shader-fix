#!/usr/bin/env python3
"""Upload a release jar to Modrinth.

Usage: publish-modrinth.py <jar> <version number> <minecraft version> <changelog file>
Requires the environment variable MODRINTH_TOKEN (scopes: Read projects, Read versions,
Create versions). Skips the upload if the version number already exists on Modrinth.
"""

import json
import os
import sys
import urllib.error
import urllib.request
import uuid

API = "https://api.modrinth.com/v2"
USER_AGENT = "mmueller96/schematic-shader-fix (release)"
PROJECT = "schematic-shader-fix"

# Modrinth project IDs of Litematica, MaLiLib and Iris
DEPENDENCIES = ["bEpr0Arc", "GcWjdA9I", "YL57xq9U"]


def request(method, path, token, body=None, content_type=None):
	headers = {"User-Agent": USER_AGENT, "Authorization": token}

	if content_type:
		headers["Content-Type"] = content_type

	req = urllib.request.Request(f"{API}{path}", data=body, method=method, headers=headers)

	try:
		with urllib.request.urlopen(req, timeout=120) as response:
			return json.load(response)
	except urllib.error.HTTPError as error:
		sys.exit(f"Modrinth API {method} {path} failed: {error.code} {error.read().decode(errors='replace')}")


def multipart(data, file_name, file_bytes):
	boundary = uuid.uuid4().hex
	parts = [
		f'--{boundary}\r\nContent-Disposition: form-data; name="data"\r\n'
		f"Content-Type: application/json\r\n\r\n{json.dumps(data)}\r\n".encode(),
		f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{file_name}"\r\n'
		f"Content-Type: application/java-archive\r\n\r\n".encode(),
		file_bytes,
		f"\r\n--{boundary}--\r\n".encode(),
	]
	return b"".join(parts), f"multipart/form-data; boundary={boundary}"


def main():
	if len(sys.argv) != 5:
		sys.exit(__doc__)

	jar, version_number, minecraft_version, changelog_file = sys.argv[1:]
	token = os.environ.get("MODRINTH_TOKEN")

	if not token:
		sys.exit("MODRINTH_TOKEN is not set")

	project = request("GET", f"/project/{PROJECT}", token)
	versions = request("GET", f"/project/{project['id']}/version", token)

	if any(version["version_number"] == version_number for version in versions):
		print(f"Version {version_number} already exists on Modrinth, skipping")
		return

	with open(changelog_file, encoding="utf-8") as file:
		changelog = file.read()

	with open(jar, "rb") as file:
		jar_bytes = file.read()

	data = {
		"project_id": project["id"],
		"name": f"Schematic Shader Fix {version_number}",
		"version_number": version_number,
		"changelog": changelog,
		"dependencies": [{"project_id": dependency, "dependency_type": "required"} for dependency in DEPENDENCIES],
		"game_versions": [minecraft_version],
		"version_type": "beta" if "-" in version_number.split("+")[0] else "release",
		"loaders": ["fabric"],
		"featured": True,
		"file_parts": ["file"],
		"primary_file": "file",
	}

	body, content_type = multipart(data, os.path.basename(jar), jar_bytes)
	version = request("POST", "/version", token, body, content_type)
	print(f"Published https://modrinth.com/project/{PROJECT}/version/{version['id']}")


if __name__ == "__main__":
	main()
