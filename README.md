# Cybernetics 2077

A standalone **Minecraft 1.21.1 / NeoForge** mod inspired by Cyberpunk 2077's cyberware interface.

![Cyberware equipment menu](docs/images/cyberware.png)

Open your inventory and press the **cyberware chip button** in its upper-right corner. Inspect sockets with the mouse, or use **Tab** and **Enter**. **Esc** returns to your inventory.

The interface includes a Minecraft diagnostic hologram, ten body-system categories, 21 equipment sockets, a capacity meter and your actual armor value. Hovering a socket highlights its body region. It supports survival and creative inventories and adapts to different window sizes and GUI scales.

**This first release builds the equipment viewer and saved equipment system. Implant items and installation mechanics are coming later; sockets start empty.**

## Install

Use [Cybernetics2077-0.1.0.jar](releases/Cybernetics2077-0.1.0.jar) with **Minecraft 1.21.1** and **NeoForge 21.1.248 or newer for 1.21.1**. Install on both the server and client for multiplayer.

No Tensura mod or other addon is required. Base cyberware capacity defaults to 100 and can be configured in the world's server configuration.

## Build

Requires **Java 21**. The Gradle wrapper downloads Gradle and the development dependencies.

```sh
./gradlew test build
```

Development client: `./gradlew runClient`. See [validation and review instructions](docs/VALIDATION.md) for the native Minecraft checks and screenshots.

## Credits

The interface artwork, diagnostic texture, shader and category glyphs are original mod assets. Rajdhani is distributed under the [SIL Open Font License](src/main/resources/assets/cybernetics2077/font/rajdhani-license.txt). Cyberpunk 2077 belongs to CD PROJEKT; this is an independent fan project, with no game textures included.
