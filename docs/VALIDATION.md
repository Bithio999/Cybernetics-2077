# Validation

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Tests use NeoForge alone, without Tensura or other gameplay mods.

Release 0.1.0: all four JVM tests, all four native GameTests, the native client review and the packaging check passed on 16 November 2026.

## Automated checks

- Four JVM tests cover socket/category mapping, responsive hitboxes, overlap and full 32-bit stat synchronization through Minecraft's signed-short menu fields.
- Four native Minecraft GameTests cover category and quantity guards, capacity calculation, equipment serialization, actual player attachment persistence, menu synchronization, and rejection of inventory-editing packets.
- Native client review opens the inventory, clicks the actual cyberware button using Minecraft mouse input, selects sockets with mouse and keyboard, checks a server-side equipment update, and returns to the correct inventory. It exercises survival and creative, GUI scales 1/2/3, 4:3 and ultrawide windows.
- The packaging check verifies standalone dependencies, all shipped resources, shader JSON, the font license, and absence of test fixtures or old addon content.

The native client screenshots use software OpenGL. The armor meter shows a real iron chestplate's six armor points; the cyberware sockets are empty. A temporary test-only implant exercises item rendering and 70,000-point capacity synchronization. It is excluded from the release.

## Reproduce

```sh
./gradlew test build
python3 scripts/verify-release.py
```

Native checks are opt-in, so their helper classes and diagnostic item never enter normal builds:

```sh
./gradlew runGameTestServer -I docs/review/server.init.gradle
```

For client review, copy that disposable test server's `world` directory to the client run directory's `saves/Cybernetics Review`, then run:

```sh
./gradlew runClient -I docs/review/client.init.gradle
```

Both profiles accept `-PcyberReviewRoot=/absolute/path` (default: `/tmp/cybernetics2077-review`). The root holds `server`, `client` and `capture` directories. Client review only edits its specially named disposable save, clears its inventory, and exits automatically with `capture/passed.txt` or `failed.txt`. Use a desktop session or X display. First-launch accessibility setup is handled by the review harness.

After running a review profile, **clean before packaging**:

```sh
./gradlew clean test build
python3 scripts/verify-release.py
```

## Screenshots

| Equipment | Inventory entry |
| --- | --- |
| ![Cyberware menu](images/cyberware.png) | ![Inventory cyberware button](images/inventory.png) |

[Selected cortex socket](images/cortex.png) · [4:3 layout](images/four-three.png) · [Ultrawide layout](images/ultrawide.png) · [Creative inventory](images/creative.png)

The first release is a read-only equipment viewer. Implant acquisition, installation, capacity enforcement during installation, and implant gameplay effects are outside this release. Multiplayer uses native server-backed menus; an integrated client and headless server were tested, but a separate two-computer session was not.
