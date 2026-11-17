# Cyberware implementation references

## Animation and combat

The user's trimmed **All 52 Mantis Blade Variants — Cyberpunk 2077 Update 2.5** MP4 was reviewed frame by frame. At approximately **8–16 seconds**, the physical variant deploys from the dorsal forearm, holds an open-hand guard, makes a cross-body cut while the opposite arm guards, and returns to ready. The electrical variant later in the clip repeats the motion. Tier 1 uses the physical mechanism, without that variant's electrical effects.

The Minecraft rig keeps the shoulder attached to the body. The upper arm and forearm share an elbow pivot; the forearm skin hatch, mechanism, hand and blade move together. Alternating cuts use anticipation, contact, follow-through and eased recovery, sampled at render partial ticks. First- and third-person views use the same joints. The default Minecraft swing is suppressed while the claws are deployed.

- [Gamestegy: Mantis Blades build, updated for 2.5](https://gamestegy.com/post/cyberpunk-2077/386/the-wolverine-mantis-blades-build): reviewed the guide and its embedded [combat MP4](https://cdn.gamestegy.com/posts-videos/mantis-blades-build-combat-showcase.mp4). Shows strong-attack leaps used from both the ground and the air. Its Relic/perk-enhanced range and area attacks are separate from the base implant; they were not assigned to tier 1.
- [Gamestegy: Mantis Blades](https://gamestegy.com/cyberpunk-2077/wiki/994/mantis-blades): arm-system placement and original item icon.
- [PowerPyx: Mantis Blades](https://www.powerpyx.com/cyberpunk-2077-mantis-blades-location/): reviewed article and screenshots for installation, drawing, slashes and held/released heavy leaps. This is a pre-2.0 guide, not a source for current acquisition or damage values.

The mod's heavy attack acquires a visible target within **8 blocks**; without one it leaps **6 blocks** in the look direction. Both use short arcing Minecraft collision movement, including when released in midair. Damage occurs at contact. Minecraft controls remain **G** to draw, left-click to slash, and held/released right-click to charge. Those controls and the range are Minecraft adaptations.

## Scanner

Reviewed the user's **Blackwall Gateway Quickhack** MP4: the RAM cells, cyan chip-shaped quickhack rows, amber tags, right-side data panel, target label and red upload chips above the target. The earlier Additional Additions scanner source was also compared. The implementation retains its staged native-HUD transition, with no Tensura dependencies or displays. RAM shows the installed deck's real four cells, rather than inventing additional capacity to match the reference screenshot.

- [CD PROJEKT RED: Update 2.0](https://www.cyberpunk.net/en/news/49060/update-2-0): revised cyberware/ripperdoc experience, netrunning RAM, upload timing and queue mechanics.
- [Gamestegy: Netrunner build](https://gamestegy.com/post/cyberpunk-2077/698/netrunner-build): deck/optics and quickhack references.
- [Gamestegy: Short Circuit](https://gamestegy.com/cyberpunk-2077/wiki/1028/short-circuit): reviewed tier descriptions and icon. The page initially selects Iconic; its displayed high-tier numbers were not treated as tier-one values.

The eye enables scanning; a loaded cyberdeck enables hacking. Server checks cover installed equipment, target visibility, range, PvP rules and RAM. The temporary bench replaces the installation service. The inventory viewer remains read-only.

## Icons and attribution

The production icons are now **original 32×32 pixel art**, with restrained colour clusters, dark outlines and no smoothing. The Kiroshi optic, Parallel deck, folded Mantis assembly and Short Circuit program keep recognizable silhouettes while fitting Minecraft. Earlier high-detail source artwork is no longer used. Research asset URLs remain in [the reference manifest](../../art/cyberpunk-icons/sources.json); downloaded icon PNGs have been removed from the current source tree.

## Mechanical presentation

The uploaded physical-blade deployment at **8.4–10.0 seconds** was inspected at ten frames per second with an enlarged forearm crop. The user's clarification fixes the anatomical interpretation: **the mount faces upward, while the blade points toward and past the hand**. Both the left and right mounts use the negative-Z dorsal face of the canonical arm, rather than mirrored lateral cutouts.

The forearm keeps its palm, side and wrist surfaces. Segmented dorsal and lateral skin covers expose an opaque inner arm, circuitry and long wire bundles. The proximal cover retracts, the offset middle plates peel sideways and the wrist collar advances; actuator struts keep the sections attached. It is not a hollow tube or a hole through the arm. The support hinge, carriage and distal blade tip are separate rigid parts. The tip slides into an opaque fixed sleeve, extending last and retracting first; neither the tip nor the arm stretches.

The handover is based on Minecraft 1.21.1's actual `ItemInHandRenderer.renderPlayerArm` transform, including swing/equip movement and the classic/slim arm origins. Translation and quaternion rotation blend continuously into the ready stance. A separate client presentation clock starts on the first displayed frame, so packet age cannot skip the starting pose. Reversal keeps the last displayed progress. Native baked cube vertices and UVs are clipped into arm/hatch patches, preserving the player's skin rather than redrawing or stretching it. The second empty hand enters from outside the view. Retraction returns the original main hand to its native pose.

A test-only render listener compares the new rest-hand matrix to the engine's actual `RenderArmEvent` matrix. A separate native check validates cube-local arm/sleeve bounds and UVs, ensuring that the ModelPart transform is not also baked into the vertices. This checks the real render path, in addition to mathematical continuity, dorsal-up/forward-blade and separate-tip channel tests. The test helpers are excluded from the release JAR.

The Operating System page opens the same native, server-backed chip editor as the bench; implant installation still requires the bench. Server guards retain deck identity, duplicate-program rejection and cursor safety. Leap completion follows the ordinary client position packet with a bounded timeout; damage still requires server-side reach and line of sight.

Fandom and wiki.gg pages were inaccessible. YouTube itself was unavailable; the uploaded MP4 and accessible community-hosted MP4 were used. No blocked page or inaccessible video was treated as reviewed. Full reference videos are not redistributed in the mod.

## Blender mechanism revision

The user's latest side-view image clarifies the two-link shape beyond the earlier dorsal orientation: the short support reaches **back and up**, and the long cutting assembly hinges there to point **forward**. The new original Blender model follows that shape instead of placing a straight blade along the forearm. Named mount/support/blade/tip pivots are exported in local coordinates and articulated by the game. Chamfers, inset panels, hinge rings, fasteners, rails and cables are authored in Blender; colour and ambient occlusion share one baked atlas. The editable source, generator and geometry budgets are in [art/mantis](../../art/mantis/README.md). No extracted Cyberpunk model or texture is shipped.

## Tier-one balance

Tier-one Minecraft values: **4 RAM**, **2 program sockets**, **8 electrical damage**, **8/8/10 physical light damage**, **14 charged damage**. They are mod balance choices, not exact Cyberpunk statistics. No Relic, Overclock or broader perk system is included.

The 0.5.3 revision keeps the same dorsal mechanism but narrows its housing and support. The uploaded reference’s separated forearm covers guide a wider, outward-spreading hatch. Nested sliding leaves retract for stowage and deploy last; this is a mechanical adaptation, rather than a claim that the game model has identical joints. The elbow and hand split belong to our rig; vanilla Minecraft does not prescribe those sections. The hatch opens fully before the support unfolds, and the mesh lifts through the opening.

The user clarified that “sleek” primarily refers to the housing, not a smaller overall weapon. Version 0.5.4 restores a large cutting assembly while retaining shallow covers, and changes the custom elbow split and hatch instead of treating the earlier forearm split as a Minecraft constraint. The outer player arm bounds remain unchanged.

For 0.5.5, the user’s side-view image and the uploaded physical-blade sequence at 8.7–10.3 seconds were compared again. The interpretation is the complete Mantis implant, not just its cutting edge. The replacement uses segmented, offset skin covers and exposed lateral cable bundles instead of long rectangular hatch doors. The mount, support, receiver and sliding sections grow together; cover pivots are independently animated from the wearer’s native skin geometry.

In 0.5.6 the user requested a thicker implant and shorter blade portion. The model therefore keeps its support length, increases housing and linkage cross sections, and reduces sliding-leaf length and extension. The segmented skin opening and its timing are retained.

The 0.5.7 correction distinguishes anatomical mounting from screen orientation: native hanging arms have their outward knuckle face on opposite X sides. The old mapping treated native front −Z as dorsal. A handed skin basis and a compensated wrist/arm roll now put the actual back-of-hand face on top, with the mechanism emerging from it. Geometry proportions stay as in 0.5.6.

## Mantis shared lift and housing slide (0.5.8)

The uploaded physical-blade sequence and the user’s side-profile reference guide this pass. Covers start opening, then the still-folded assembly rises as one unit. Only the implant’s support/housing carriage slides backward along fixed guides; the forearm mount stays anchored. The upper receiver then pivots forward and the nested cutting leaves extend. The shared lift holds the relative upper/lower angle at 180°, replacing the old continuously counter-rotating receiver. The sequence reverses for stowing. First-person hand spacing, height and weapon projection are tuned against the supplied video while preserving the world-camera FOV setting. No exact Cyberpunk FOV can be inferred from the cropped reference, so the 52° vertical weapon lens is a composition choice.

## First-person shoulder continuity (0.5.9)

Rechecked the supplied 24.73-second Mantis variants clip, including physical-blade idle holds around 10–11 and 13 seconds. The hands rest low, the forearms enter from below the screen, and the centre remains open outside attack/deployment motion. The 0.5.8 shoulder origin was too high and too far forward in screen space, exposing the proximal ends of the finite Minecraft arm meshes. The viewmodel now places the shoulders below the viewport and pitches the forearms forward. Neither the player model nor the world camera is moved. Projection tests sample the upper arm ends through deployment, light/charged strikes and charge holds, including the sleeve margin. The existing 52° vertical weapon lens is retained; this is composition matching rather than an exact measurement of Cyberpunk’s camera.

## First-person composition study (0.5.10)

The physical-blade idle holds around 10–11 and 13 seconds of the supplied video were reviewed separately from the deployment and attack poses. The 0.5.9 view cropped the hands, crowded the bottom with housing and converged the blade tips toward the centre. Simply straightening the arms made the blades too foreshortened. This pass instead fits hand, distal hinge and cutting-tip screen positions together, using the existing exported mesh dimensions. Approximate dominant-hand composition targets are 70% across / 86% down for the wrist, the hinge near the outer bottom edge, and the cutting tip around 76% across / 64% down; these are compositional guides, not a measurement of Cyberpunk’s lens. The off hand sits slightly higher and farther forward.

The first-person stance is independent of the third-person shoulder rotations. The dorsal surface rolls outward/up and the arms reach forward/up from low anchors, keeping blade profiles readable. The weapon lens widens from 52° to 68° vertically without changing the world-camera FOV setting. Native skin geometry and UVs are retained. Rather than extending or stretching the upper skin, a smooth camera-space anchor moves the arm down only when its finite shoulder cap would enter the view. The cap rectangle’s support against the lower frustum plane uses a smooth maximum with no frame-by-frame vertex allocation. Tests clip the complete cap polygon against all six homogeneous frustum planes through deployment, light/charged strikes and charge holds at 4:3, 16:9 and 21:9. A separate test projects actual exported blade vertices to guard the idle central aim lane.
