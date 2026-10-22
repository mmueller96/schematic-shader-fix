# Contributing

Contributions are welcome, especially updates for new Minecraft, Litematica, MaLiLib or Iris versions.

## License

This project is licensed under the [MIT License](LICENSE.txt). By submitting a pull request you agree
that your contribution is licensed under the same terms.

Please don't copy code from Litematica, MaLiLib or Iris into this project. They are licensed under
LGPLv3, and this addon only references their classes through mixins.

## Branches

There is one branch per Minecraft version (e.g. `26.3`). Open your pull request against the branch of
the Minecraft version it targets. For a new Minecraft version, open an issue first or base the
pull request on the latest branch and say so in the description.

## Updating to new mod versions

1. Update the versions in `gradle.properties`. They are Modrinth version IDs, which you find on the
   version page of each mod. A daily GitHub Actions job does this automatically and opens a pull
   request, so check for an open `dependencies` pull request first.
2. Adjust the version ranges in `src/main/resources/fabric.mod.json` if needed.
3. Build with `./gradlew build` (requires JDK 25). If a mixin target changed, fix the mixin.
4. **Test in-game with a shader pack**, see below.

## Testing

A successful build does not mean the fix still works. Before opening a pull request, please check
in-game with a shader pack active:

- Schematic blocks render in the correct place, with no stretched or "exploding" geometry.
- The schematic overlay outlines are straight lines, with no diagonal "X" lines.
- Toggling shaders off and on again still renders the schematic correctly.
- The log contains no mixin errors.

Mention the shader pack and all mod versions you tested with in the pull request, and add a
screenshot if possible.

## Releasing (maintainer)

Releases are built and published by the `Release` workflow when a tag is pushed.

1. In a pull request, raise `mod_version` in `gradle.properties` and add a `## <mod_version>` section
   to `CHANGELOG.md`. Use patch versions (1.0.1) for fixes and new version ranges, minor versions
   (1.1.0) for new fixes or features, and a `-beta.1` suffix for pre-releases.
2. Merge it, then tag the merge commit on the Minecraft version branch:
   `git tag v<mod_version>+<minecraft_version>` (e.g. `v1.0.1+26.3`) and `git push origin <tag>`.
3. The workflow checks that the tag matches `gradle.properties`, builds the jar, creates the GitHub
   release and uploads the version to Modrinth with the changelog section as release notes.

Dependency updates that only change `gradle.properties` don't need a release. Pull requests of the
update check that do need one get the `release-needed` label.

## Code style

Match the existing code: tabs for indentation, braces on their own line, mixin members prefixed with
`schematicshaderfix$`.
