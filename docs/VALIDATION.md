# Validation

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Tests use NeoForge alone, without Tensura or other gameplay mods.

Release 0.3.1 restores the first-release hologram. All 11 JVM tests and the native client review pass. The client review covers the restored model, category close-ups, installation and scanning.

## Automated checks

- JVM tests cover category mapping, responsive hitboxes, overlap, 32-bit menu fields, smooth camera interpolation and staged scanner/native-HUD transitions.
- Ten native Minecraft GameTests cover category and quantity guards, capacity calculation, equipment serialization, actual player attachment persistence, menu synchronization, rejection of inventory-editing packets, real bench shift-click installation/removal, inventory-full protection, slot/category/capacity guards, bench distance/block validity, and scanner ownership/aim/range/line-of-sight.
- Native client review opens the inventory, clicks the actual cyberware button using Minecraft mouse input, selects sockets with mouse and keyboard, checks a server-side equipment update, and returns to the correct inventory. It exercises survival and creative, GUI scales 1/2/3, 4:3 and ultrawide windows. It then interacts with the actual bench block, installs the craftable eye using native inventory clicks, scans a husk, verifies server target data, closes the scanner, checks the installed-eye detail view, and removes the eye to confirm the scanner becomes unavailable.
- The packaging check verifies standalone dependencies, all shipped resources, shader JSON, the font license, and absence of test fixtures or old addon content.

The native client screenshots use software OpenGL. The initial armor meter shows a real iron chestplate's six armor points; later captures show the installed tier 1 eye. A temporary test-only implant exercises item rendering and 70,000-point capacity synchronization. It is excluded from the release.

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

Optional: encode the captured native animations (requires ffmpeg):

```sh
python3 docs/review/assemble-previews.py /tmp/cybernetics2077-review/capture
```

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

The inventory cyberware screen remains read-only; the bench is the only installation interface. There are no quickhacks or operating-system functions in 0.3.1. Multiplayer uses native server-backed menus and validates scans on the server. An integrated client and headless dedicated server were tested; a separate two-computer session was not.

The category zoom was guided by the user's supplied Cyberpunk video at 0:21–1:05. The scanner's original authored animation and UI drawing code were adapted from the earlier project, with its skill systems, resource displays and dependencies removed.

## Original hologram restoration

The model is Minecraft's native `PlayerModel`. Its diagnostic texture and hologram shader files are restored byte-for-byte from the first release (`a66c2a2`). The original overview framing and pointer tracking are retained, with an adapter for the later category close-ups.

The Blender mesh, its runtime loader, anatomy shaders and obsolete loader tests have been removed from the current tree. Packaging checks reject those assets/classes and verify the original diagnostic texture's digest. Git history and previous release JARs remain available.
