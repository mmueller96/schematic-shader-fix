# Schematic Shader Fix

Unofficial client-side Fabric addon that fixes two rendering bugs of
[Litematica](https://github.com/sakura-ryoko/litematica) and
[MaLiLib](https://github.com/sakura-ryoko/malilib) while an
[Iris](https://github.com/IrisShaders/Iris) shader pack is active.

**This is not an official Litematica or MaLiLib project and is not affiliated with their authors.
Please don't report issues with this addon to them.**

## Fixed issues

1. **"Exploding" schematic blocks** – ghost blocks stretched into huge walls converging on a point.
   Iris compiles Litematica's terrain pipelines with its extended `TERRAIN` vertex format, but only
   extends a `BLOCK` buffer if it gets created while the level is being rendered. Litematica builds
   its chunk meshes on worker threads, so some meshes ended up in the smaller `BLOCK` format and were
   read with the wrong vertex stride. The addon makes every schematic block buffer use the extended
   format while shaders are on, and rebuilds the schematic meshes when shaders are toggled.
2. **Diagonal "X" lines on the schematic overlay** – MaLiLib assigns its debug line pipelines
   (plain GL lines in `POSITION_COLOR_LINE_WIDTH`) to the Iris `LINES` program. That shader expands
   lines using a per-vertex normal these pipelines don't have, which displaces the line endpoints.
   The addon maps them to the Iris `BASIC_COLOR` shader instead, which only needs position and color.

## Requirements

- Minecraft 26.3, Fabric Loader
- Litematica 0.29.1+, MaLiLib 0.30.2+
- Iris 1.11.6+ (with Sodium)

## Building

Requires JDK 25.

```
./gradlew build
```

The jar ends up in `build/libs/`.

## Note for Wayland users

If Minecraft 26.x falls back to Vulkan with
`OpenGL is not supported: Could not retrieve OpenGL functions`, add the JVM argument
`-Dorg.lwjgl.opengl.contextAPI=native`. Iris only works with OpenGL.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

MIT, see [LICENSE.txt](LICENSE.txt).

This addon contains no code from Litematica, MaLiLib or Iris and does not bundle them. Those mods
are licensed under LGPLv3 and are only required at runtime.
