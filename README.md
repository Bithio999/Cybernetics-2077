# Cybernetics 2077

A standalone **Minecraft 1.21.1 / NeoForge** mod inspired by Cyberpunk 2077's cyberware interface.

![Equipped optical scanner and category view](docs/images/optics-detail.png)

## Cyberware

Open your inventory and press the **cyberware tab beside the inventory**. Select a socket to zoom into that body system and inspect its installed implant. Use **A / D** or the arrows in the category header to change systems. **Esc** returns to the full body; press it again to return to your inventory. **Tab / Enter** also navigate and select sockets.

Implant sockets in the inventory viewer are **read-only**. Select an installed **Operating System → Manage Quickhacks** to install or remove programs without visiting a bench. Its 21 sockets cover ten body systems. The hologram uses **your current Minecraft skin**, with the correct classic or slim body model. Hats, jackets, sleeves and trouser layers follow your **Skin Customization** settings. Skin colours remain recognizable beneath transparency, a cyan projection tint, visible scanlines, a moving scan sweep and a fine illuminated silhouette. Uniform scaling preserves Minecraft body proportions; the overview and category cameras center on the body and selected region respectively. Category close-ups remain available. Capacity and armor meters reflect your equipment.

All implants have a strength tier from **1–5**, shown in their tooltip and socket badge: white, green, blue, purple and orange.

| Equipment | Tiers | System | Effect, tier 1 → 5 |
| --- | --- | --- | --- |
| Basic Kiroshi Optics | 1–5 | Face | Scan range: 32 → 64 blocks. |
| Militech Paraline | 1–5 | Operating System | 4 → 12 RAM; 2 → 8 program sockets; faster recovery and uploads. |
| Mantis Blades | 2–5 | Arms | Physical slashes: 10 → 16; charged lunge: 17.5 → 28. |
| Short Circuit chip | 1–5 | Cyberdeck | 8 → 22 base electrical damage, plus the deck’s damage bonus. |

The original implant/chip families retain their earlier smithing upgrades; the new expansion implants have no recipes yet. Loaded chips and custom item data are preserved. [Tier stats and upgrade recipes](docs/CYBERWARE.md#tier-upgrades) · [Cyberpunk cyberware catalogue: 121 current entries](docs/research/CYBERWARE-CATALOGUE.md)

**14 more implant families** cover the cortex, operating system, face, skeleton, nerves, circulation, skin and legs. They include Berserk, Blood Pump, Optical Camo, retained enemy highlights, defensive implants, a double jump and a charged jump. Each starts at its researched Cyberpunk tier and continues through tier 5. [Full implant list, starting tiers and effects](docs/IMPLANTS.md).

Active controls: **I** for BioDyne Berserk, **H** for Blood Pump, **C** for Optical Camo. Leg abilities use **jump**. Bindings can be changed in Controls. New implants have no recipes yet; they are sold by ripperdocs and also available in Creative or through commands. Existing tier-1 Mantis Blades become tier 2 automatically.

The scanner shows health, armor, attack damage, absorption and visible effects. With a loaded cyberdeck, **left-click** to upload Short Circuit to the target; upload progress appears over its head. The overlay opens and closes with an animated transition from Minecraft’s HUD.

![Short Circuit scanner](docs/images/short-circuit-scanner.png)

Mantis Blades deploy from the top/back-of-hand face of the forearm through multiple moving skin and sleeve sections. As the covers open, the folded support and blade rise together; the support/housing slides backward before the blade pivots forward and the cutting tip extends. The palm and hand remain closed. The Blender model uses separate rigid pivots and near/far meshes, with cached geometry and textures. The original Minecraft hand transitions continuously into the ready stance, with visible hands held low, blade profiles along the outer edges and a wider weapon view, following the supplied reference video. Alternating cross-body cuts and a rising third strike use dynamic blade extension. Charged lunges pull both arms back before driving forward. Subtle idle movement keeps the ready pose alive, and the third-person guard leaves the chest clear.

[Watch the blade animations](docs/media/mantis-claws.mp4) · [Attacks and lunge](docs/media/mantis-attacks.mp4) · [Idle motion](docs/media/mantis-idle.mp4) · [Blender model and build notes](art/mantis/README.md) · [Equipment, controls and recipes](docs/CYBERWARE.md)

![Dorsal-mounted arm blades](docs/images/mantis-first-person.png)

## Village clinics and the Afterlife

Find **Ripperdoc clinics** in new villages, with a rarer two-storey clinic. Talk to the doctor to **purchase cyberware** or **install it** through separate tabs. Supply-barrel credit chips fund your saved eddies wallet. The occasional **Afterlife** has a descending entrance, industrial bar and two fixer NPCs. Contracts are coming later.

[Buildings, purchasing and installation](docs/VILLAGE-SERVICES.md) · [Design references and block audits](docs/research/VILLAGE-SERVICES.md)

![Village services](docs/images/village-services.png)

## Install cyberware

Visit a ripperdoc and select **Install**, or craft and place a **Cybernetics Bench**, then right-click it. Move an implant from your inventory into a matching body-system socket, or **shift-click** to install it. Move it back to your inventory to remove it. Category and total capacity limits apply; merely carrying the scanner does not enable it. Equipment is saved with the player and retained on death. While Mantis Blades are out, selected main-hand and off-hand items stay in their inventory slots and act as empty hands. Retract the claws to use them again. Drawing the claws still requires both hands to be empty. Install a deck, then select **Operating System → Manage Quickhacks** in the cyberware menu to load or remove chips. The bench’s **Cyberdeck** shortcut also remains available. Its programs stay with the physical deck when it is removed.

![Cybernetics Bench installation menu](docs/images/bench.png)

[Recipes and item IDs](docs/CYBERWARE.md) · [In-game screenshots and clips](docs/PREVIEWS.md)

## Install the mod

Use [Cybernetics2077-0.9.0.jar](releases/Cybernetics2077-0.9.0.jar) with **Minecraft 1.21.1** and **NeoForge 21.1.248 or newer for 1.21.1**. Replace the old Cybernetics 2077 JAR rather than keeping both. Install on both the server and client for multiplayer.

No Tensura mod or other addon is required. Base cyberware capacity defaults to 100 and can be configured in the world's server configuration. Existing saves remain compatible.

## Build

Requires **Java 21**. The Gradle wrapper downloads Gradle and the development dependencies.

```sh
./gradlew test build
python3 scripts/verify-release.py
```

Development client: `./gradlew runClient`. See [validation and review instructions](docs/VALIDATION.md) for native Minecraft checks and more screenshots.

[Reference video download and frame-review helper](docs/REFERENCE-VIDEOS.md)

## Credits

Articulated arm and blade meshes, interface geometry, shaders, workstation textures and category glyphs are original mod assets. The four equipment icons are original 32×32 pixel art inspired by the reference silhouettes. See the [implementation references](docs/research/CYBERWARE.md). Rajdhani is distributed under the [SIL Open Font License](src/main/resources/assets/cybernetics2077/font/rajdhani-license.txt). Cyberpunk 2077 belongs to CD PROJEKT; this is an independent fan project.
