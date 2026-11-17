# Validation

Minecraft 1.21.1, NeoForge 21.1.248, Java 21. Tests use NeoForge alone, without Tensura or other gameplay mods.

Release 0.5.6 uses the local player’s skin in the hologram. All 38 JVM tests and the native client review pass. The client review covers live skins, category close-ups, the rotated inventory tab, implant and chip installation, scanner uploads, and first/third-person claw animations.

## Automated checks

- JVM tests cover category mapping, responsive hitboxes, overlap, 32-bit menu fields, smooth camera interpolation, equal horizontal/vertical pixel scale throughout every category transition, centered region targets for classic/slim models, staged scanner/native-HUD transitions, articulated deployment/strike continuity, contact-frame timing, alternating arms, and stroke scale under a 90-degree tab rotation.
- Nineteen native Minecraft GameTests cover category and quantity guards, capacity calculation, equipment serialization, actual player attachment persistence, menu synchronization, rejection of inventory-editing packets, real bench shift-click installation/removal, inventory-full protection, slot/category/capacity guards, bench distance/block validity, scanner ownership/aim/range/line-of-sight, deck/chip category guards, duplicate program rejection, removal and serialization of loaded decks, upload prerequisites and RAM reservation, cancellation/refunds, and blade installation/deployment/contact/terrain guards, eight-block leap acquisition/aim/visibility and cancellation on equipping an item, plus viewer chip-editor prerequisites and cursor guards, and delayed leap completion without premature cancellation.
- Native client review opens the inventory, clicks the actual cyberware button using Minecraft mouse input, selects sockets with mouse and keyboard, checks a server-side equipment update, and returns to the correct inventory. It exercises survival and creative, GUI scales 1/2/3, 4:3 and ultrawide windows. It then interacts with the actual bench block, installs the craftable eye using native inventory clicks, scans a husk, verifies server target data, closes the scanner, checks the installed-eye detail view, and removes the eye to confirm the scanner becomes unavailable. It then installs the deck, eye and claws through native bench transactions, opens the chip editor with a mouse click, installs Short Circuit, uploads it through the actual scanner attack input and verifies exactly 8 damage and RAM consumption. It exercises all three light contacts, the charged lunge, deploy/retract and the occupied-hand guard. A free leap also verifies six blocks of travel without a target. The viewer opens the actual chip menu, removes and reinstalls Short Circuit, checks persistence, and returns to the Operating System page. Ground and airborne release tests each move **5.15 blocks** and require exactly one **14-damage** contact; the air test verifies a real jump before release. A solid wall must prevent both travel and damage. Joint tests sample every combo for both arm widths and verify that the shoulder socket stays fixed and both segments share the elbow. Render captures include both first- and third-person animations and the target-mounted upload label.
- The packaging check verifies standalone dependencies, all shipped resources, shader JSON, new items/menus/combat classes, the font license, and absence of test fixtures or old addon content.

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

Both profiles accept `-PcyberReviewRoot=/absolute/path` (default: `/tmp/cybernetics2077-review`). The root holds `server`, `client` and `capture` directories. Client review only edits its specially named disposable save, clears its inventory, and exits automatically with `capture/passed.txt` plus `content-passed.txt`, or `failed.txt`. Pass `-PcyberContentOnly` to the client profile for just the equipment/combat review. Use a desktop session or X display. First-launch accessibility setup is handled by the review harness.

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

The inventory cyberware screen remains read-only; the bench is the only installation interface. Release 0.5.6 includes the tier 1 deck and Short Circuit; other quickhacks and operating systems are not implemented. Multiplayer uses native server-backed menus and validates scans on the server. An integrated client and headless dedicated server were tested; a separate two-computer session was not.

The category zoom was guided by the user's supplied Cyberpunk video at 0:21–1:05. The scanner's original authored animation and UI drawing code were adapted from the earlier project, with its skill systems, resource displays and dependencies removed.

## Player skin validation

The hologram reads Minecraft's own `PlayerSkin` provider each frame, uses the matching classic/slim model layer, and draws each enabled skin layer. Skin pixels and overlays resolve against depth in an isolated render target; the completed avatar is then composited once at holographic opacity. This retains recognizable skin markings beneath cyan shading and prevents outer layers from being hidden by the base model. Scanlines and the silhouette glow are applied to the completed projection, with no geometry jitter.

Native review verifies the bound skin texture and actual four/three-pixel arm geometry while switching between Minecraft's built-in Steve and Alex skins. It toggles all six outer skin-layer flags and compares render-target pixels using a temporary high-contrast hat/jacket fixture: **102,498 pixels changed**. The fixture exists only in the test process, is released afterward, and is excluded from the public preview images and release JAR. The original player skin is restored and the hologram cache reset at the end. A separate alpha-preserving render-target check measures the classic head’s displayed width/height ratio at **0.99990** (square is 1.0), with the full-body silhouette centered at **0, 0 pixels** of offset. The standard screenshot helper forces alpha to 255, so this silhouette check reads the texture without discarding alpha.

The diagnostic placeholder texture is no longer shipped. Offline/unavailable custom skins use Minecraft's normal fallback. No additional skin service, credential or asset download mechanism is introduced. Hats, jackets, sleeves and trouser overlays are supported; armor, held items and capes are outside this skin preview.

## New content review

The tier 1 deck, chip and arms are installed using native server-backed menu transactions. Loading a chip changes the actual deck’s `minecraft:container` component. A dedicated-server test removes the loaded deck into the player's inventory, serializes and restores that returned stack, and verifies that its program survives.

The client casts Short Circuit against a 100-HP husk: its health falls to 92 after the upload, with RAM reserved on acceptance. Mantis light contacts and the charged lunge are checked against the same target. The client also verifies that drawing fails when a normal weapon is held, then changes to the Nether and checks that the installed eye, deck, program and arm state remain available. The upload display has screenshot checks for visible cyan target-name glyphs and red queue/progress pixels, in addition to visual inspection.

[Mantis combat clip](media/mantis-claws.mp4) · [Quickhack clip](media/short-circuit.mp4) · [Implementation references](research/CYBERWARE.md)

The current Mantis clips are trimmed from a native 960×540, 30-fps desktop recording, including connected third-person cuts and actual ground/air travel. Tick-captured frames are also retained locally by the review for repeatable inspection; the optional preview encoder can assemble those at 20 fps. No rendered concept animation is substituted for gameplay.

To trim a desktop recording against the same run’s capture timestamps:

```sh
python3 docs/review/trim-native-recording.py recording.mp4 /path/to/capture RECORDING_START_UNIX_TIME
```

For desktop recording, match the capture rectangle to the actual game window; a 1280×720 review window can otherwise be centered inside a larger virtual display. The current Mantis clips were recorded with both the desktop and game window at 960×540 and visually checked after trimming.

Release 0.5.6 uses network protocol 6. Update clients and server together. The release JAR is built after a clean, without the review profiles.

The 0.5.6 revision also checks ordered cover/hinge/carriage channels and reverse retraction, verifies the inventory tab overlaps its inventory edge, and captures the exposed mechanical cavity. All four item icons are 32×32 nearest-neighbour pixel art.

Lunges now acknowledge completion after the client’s normal position packet. The server permits a bounded delay for slow frames or latency, while validating target contact and terrain visibility. The six-block free leap also waits for actual movement rather than ending on elapsed time alone.

## Mantis handover and anatomy

The 0.5.6 review checks the rest-hand matrix against the actual vanilla `RenderArmEvent` transform before deployment. Classic/slim and left/right mathematical tests check continuous wrist movement, non-collapsing transforms, a dorsal mount facing up and a blade axis pointing forward. The distal tip has its own channel and extends after the support, then retracts before it. Native captures include the original empty hand, deployment, a zoomed dorsal view, tip retraction and redeployment. Native cube UVs preserve skin alignment, and the shallow recess has an opaque floor, palm, sides and wrist.

The presentation clock begins at the first displayed frame instead of the server timestamp, preventing delayed packets from skipping the normal-hand starting pose. Reversal preserves the last displayed progress. Native checks also validate cube-local bounds and skin UVs for both arms/sleeves, catching accidental baked ModelPart transforms.

## Blender model checks

The 0.5.6 model is built and baked in Blender 4.3.2. Asset and articulation tests validate the exported groups, finite coordinates, triangle indices, normalized normals, atlas UVs, near/far geometry budgets, preserved LOD bounds, rejection of corrupt/truncated data, and hinge continuity throughout deployment. The deployed support goes backward/up; the blade counter-rotates forward. Both LODs use these same local pivots.

The native content review was repeated with the imported model and resource-reload preload path. It passed the existing handover, deployment/retraction, three light attacks, charged damage, ground/air/free leap and terrain checks. First- and third-person captures were visually inspected. The near mesh has 3,446 triangles per blade; the far mesh has 546 and is used beyond 16 blocks in third person. Meshes and the 512px atlas are cached; no Blender dependency ships in the mod. This is a bounded geometry budget, not a hardware FPS guarantee. [Editable source and rebuild instructions](../art/mantis/README.md).


## Deployment clearance and recording

The 0.5.6 clearance tests sample exported geometry for both arm widths and both mesh LODs, and intersect the near mesh's triangles with the moving dermal plates throughout deployment. Closed regions must tile the original skin; the covers move on separate tracks before the support unfolds. The hand and palm remain closed. The eight cover regions replace the old rectangular door layout.

The recording profile (`-PcyberRecording -PcyberContentOnly`) stores capture timestamps in memory and writes them after the run, without per-tick GPU readbacks or PNG encoding. A separate full-size diagnostic run retains screenshot/pixel checks. Recording uses a 960×540 framebuffer on this software-rendered cloud machine and exports at 30 fps. The profile records actual combat frame times in `capture/recording-frame-times.txt`; the MP4 frame-rate field alone is not evidence of smooth rendering. The trimming helper reads `recording-marks.json`; use marks from the same recording. Pass `--mantis-only --fps 30` to replace just the Mantis clips.

The model now uses actual triangle rendering, a shared atlas and cached skin patches. It submits about 50% fewer blade vertices than 0.5.2. These changes reduce CPU/geometry work; they do not guarantee a particular FPS on a modded client.

The final recording run measured **54.5 mean rendered FPS**, with **23.724 ms P95** and **27.337 ms P99** combat frame times at 960×540, six-chunk render distance and a 60 FPS cap using llvmpipe. [Raw frame-time summary](review/mantis-frame-times-0.5.6.txt). This describes preview pacing, not a performance comparison to the earlier 1280×720 run or a hardware guarantee.


## Whole implant and segmented covers

Version 0.5.6 keeps the full-size support layout, adds depth to the mount and linkage, increases receiver depth and width, and shortens the cutting section. The dimension test checks the new reach range and a minimum support/housing depth as well as the mount width. Eight skin regions use separate rigid tracks: one proximal cover, two offset middle plates, two carrier covers, the moving wrist collar and two lateral leaves. Diagonal cuts preserve native skin UVs. Each cover has an opaque inner backing and rim; the palm and hand remain closed. The underlying arm stays solid, with long red/black service bundles occupying the opened area.

Tests check that the closed dermal regions tile without gaps or overlap, that their transforms start at the native skin and stay continuous, and that the collars clear the mechanism. The mesh collision test intersects actual exported triangles against the moving skin planes throughout deployment for both arm widths. The model hoist waits until the covers clear. A test-only camera gives a fixed close-up of closing and opening; it is excluded from the release JAR.
