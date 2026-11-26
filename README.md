# Cybernetics 2077

A standalone **Minecraft 1.21.1 NeoForge / 1.20.1 Forge** mod inspired by Cyberpunk 2077. Install cyberware, load quickhacks and find ripperdocs and fixers in villages.

## Start here

**[Open the wiki →](docs/wiki/index.md)**

Individual item pages, tier stats, controls, acquisition and village guides.

| Minecraft | Loader | Java | Version / Download |
| --- | --- | --- | --- |
| 1.21.1 | NeoForge 21.1.248+ | 21 | 0.21.0 · [NeoForge JAR](https://github.com/Bithio999/Cybernetics-2077/raw/refs/heads/main/releases/Cybernetics2077-NeoForge-1.21.1-0.21.0.jar) |
| 1.20.1 | Forge 47.4+ (tested 47.4.29) | 17 | 0.20.0 · [Forge JAR](https://github.com/Bithio999/Cybernetics-2077/raw/refs/heads/main/releases/Cybernetics2077-Forge-1.20.1-0.20.0.jar) |

Development currently focuses on **1.21.1 NeoForge**. The Forge 1.20.1 build stays at its last release until an update is requested.

Install the matching build on both server and clients. Replace the previous JAR. [TaCZ integration and modpack compatibility](docs/wiki/compatibility.md).

| Looking for… | Open |
| --- | --- |
| First steps and your first implant | [Getting started](docs/wiki/getting-started.md) |
| Implant effects and tier stats | [Cyberware](docs/wiki/cyberware.md) |
| Iconic purchases, Apogee and David’s Sandevistan | [Iconics](docs/wiki/iconics.md) |
| Decks, RAM and program slots | [Cyberdecks](docs/wiki/cyberdecks.md) |
| Quickhack costs and damage types | [Quickhacks](docs/wiki/quickhacks.md) |
| Daily shops, currency and contracts | [Village services](docs/wiki/ripperdocs.md) |
| Rare enhanced mobs and implant discounts | [Chromed mobs and salvage](docs/wiki/enhanced-mobs.md) |
| Keybindings or a problem | [Controls](docs/wiki/controls.md) · [Common questions](docs/wiki/faq.md) |

Existing worlds are supported; new village buildings require new chunks.

## Media downloader & trimmer

**[Media Workshop](tools/media-workshop/README.md)** converts YouTube/Twitch links to MP4 or MP3 and includes a preview, trimmer and batch queue. Runs independently of Minecraft. **[Download the program ZIP](releases/tools/Media-Workshop-1.7.0.zip)**.

<details>
<summary>Source code, building and documentation maintenance</summary>

Active NeoForge development: Java 21, `./gradlew test build`, then `python3 scripts/verify-release.py`. Build and validate Forge separately only for an explicitly requested port update. [Port and compatibility testing](compat/README.md).

[Developer notes](docs/development/README.md) · [Validation](docs/VALIDATION.md) · [Research](docs/research/CYBERWARE-CATALOGUE.md) · [Preview archive](docs/PREVIEWS.md)

</details>

Cyberpunk 2077 belongs to CD PROJEKT. This is an independent fan project. Mod art is original; [Rajdhani font license](src/main/resources/assets/cybernetics2077/font/rajdhani-license.txt).
