# Changelog

Each section is used as the release notes of that version on GitHub and Modrinth.

## 1.0.0

First release for Minecraft 26.3.

- Fix schematic blocks stretching into huge "exploding" walls while an Iris shader pack is active.
  Schematic meshes are now always built in the vertex format Iris expects and get rebuilt when
  shaders are toggled.
- Fix diagonal "X" lines on the MaLiLib schematic overlay outlines while a shader pack is active.

Requires Litematica 0.29.1+, MaLiLib 0.30.2+ and Iris 1.11.6+.
