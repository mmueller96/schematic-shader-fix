# Schematic Shader Fix

Client-side Fabric addon for Minecraft 26.3 that fixes three rendering bugs of Litematica and MaLiLib
while an Iris shader pack is active. See README.md for the user-facing description and
CONTRIBUTING.md for the contribution rules, which apply to you as well.

## Layout

- `src/main/java/io/github/mmueller96/schematicshaderfix/IrisHelper.java`: the actual fix logic
- `mixin/MixinBufferBuilderCache.java`: Litematica `BufferBuilderCache.getBuilder` creates block
  buffers through `IrisHelper.createBlockBufferBuilder`, so they always get the extended Iris
  `TERRAIN` vertex format while shaders are on (fixes stretched "exploding" schematic blocks)
- `mixin/MixinWorldRendererSchematic.java`: Litematica `WorldRendererSchematic.setupTerrain` reloads
  all schematic renderers when the shader state changes
- `mixin/MixinLitematicaIrisCompat.java`: redirects Litematica `IrisCompat.registerPipelines` calls of
  `IrisApi.assignPipeline`, so the translucent pipelines use `ShaderKey.TERRAIN_TRANSLUCENT` instead
  of `SODIUM_TERRAIN_TRANSLUCENT` (fixes invisible water and stained glass ghost blocks)
- `mixin/MixinMaLiLibIrisCompat.java`: redirects MaLiLib `IrisCompat.registerPipelines` calls of
  `IrisApi.assignPipeline`, so debug line pipelines use `ShaderKey.BASIC_COLOR` instead of the
  `LINES` program (fixes diagonal "X" lines on the overlay)
- `gradle.properties`: dependency versions as Modrinth version IDs, updated by
  `.github/scripts/check-updates.py`
- `src/main/resources/fabric.mod.json`: dependency version ranges

Minecraft 26.x is not obfuscated, all mixins use `remap = false` and plain Mojang names.
Rendering uses the renderpearl API (`com.mojang.renderpearl.api.*`).

## Build

Requires JDK 25: `./gradlew build`. The jar ends up in `build/libs/`.

## Updating to new dependency versions

When a mixin target or an Iris internal changed, inspect the new jars. Gradle downloads them to
`~/.gradle/caches/modules-2/files-2.1/maven.modrinth/<slug>/`. Use `unzip -l` to list classes and
`javap -p -cp <jar> <class>` to see method signatures. Things to check:

- Litematica: `fi.dy.masa.litematica.render.schematic.BufferBuilderCache` (`getBuilder`,
  `blockBufferBuilders`) and `WorldRendererSchematic` (`setupTerrain`, `loadRenderers`)
- Litematica: `fi.dy.masa.litematica.compat.iris.IrisCompat.registerPipelines`
- MaLiLib: `fi.dy.masa.malilib.compat.iris.IrisCompat.registerPipelines`
- Iris: `net.irisshaders.iris.vertices.ImmediateState` (`isRenderingLevel`, `skipExtension`),
  `net.irisshaders.iris.pipeline.IrisPipelines.assignPipeline`, `ShaderKey.BASIC_COLOR`,
  `ShaderKey.TERRAIN_TRANSLUCENT`

If upstream fixed one of the bugs itself, say so instead of patching around it.

## Rules

- Never copy code from Litematica, MaLiLib or Iris. They are LGPLv3, this project is MIT. Only
  reference their classes, fields and methods.
- Keep changes minimal and match the existing code style (tabs, braces on their own line, mixin
  members prefixed with `schematicshaderfix$`).
- Do not change LICENSE.txt or the license in fabric.mod.json.
- Never merge pull requests or push to the default branch.
- You cannot test in-game. Always state that an in-game test with a shader pack is still needed.
- When reviewing a pull request, check: the build, no copied LGPL code, the testing checklist of the
  pull request template, plausible version ranges, and that the changes stay within the scope of
  this addon.
