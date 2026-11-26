# Forge 1.20.1 build

Cybernetics 2077 for **Minecraft 1.20.1 / Forge 47.4+ / Java 17**, tested with Forge 47.4.29. The NeoForge 1.21.1 build remains in the repository root.

```sh
# From the repository root, with a Java 17 toolchain available:
./gradlew -p forge-1.20.1 test build
```

Output: `forge-1.20.1/build/libs/Cybernetics2077-Forge-1.20.1-0.20.0.jar`. Use the reobfuscated JAR from `libs`, not the development JAR in `devlibs`.

`generate.py` adapts the canonical sources/resources into disposable `build/generated/port` output. Maintain shared gameplay in `../src/main`. Version-specific implementations and overrides live in `src/main/java`; the generator omits shared classes with the same relative path.

The adapters cover Forge capabilities, player cloning and persistence, SimpleChannel packets, item NBT, damage/tick events, rendering, shader vertex formats/blending, atlas animation metadata and the 1.20.1 data-pack layout. MixinExtras 0.5.0 is embedded through JarJar. Mod IDs, item IDs and gameplay values are shared between platforms.

Do not copy save directories between Minecraft versions. Existing 1.20.1 worlds can install this build; it is not a save downgrade utility.

[Compatibility testing](../compat/README.md) · [Player guide](../docs/wiki/getting-started.md)
