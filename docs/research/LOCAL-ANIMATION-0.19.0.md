# Local block animation and piston continuity — 0.19.0

## Native reference

Reviewed the installed Minecraft 1.21.1 / NeoForge 21.1.248 sources for `SpriteContents`, `TextureAtlas`, `ShaderInstance`, `LevelRenderer`, `PistonMovingBlockEntity`, `PistonHeadRenderer`, and `BlockEntityRenderDispatcher`, plus the five native terrain/moving-block shaders. No global tick rate, sun/weather clock or texture-atlas ticker is changed. Installed sprite pixels and shader sources are read at runtime; no Minecraft texture assets or full copied shaders are shipped.

## Piston motion

The previous block-entity gate left `progressO` and `progress` unchanged on skipped ticks. Native rendering then repeatedly interpolated the same interval using each real tick's 0–1 partial, producing visible reversals. Moving pistons now bypass that gate. Their native ticker runs every real tick, and its 0.5-block progress increment is multiplied by local pace: 0.3125 for Dynalar and 0.09375 for Apogee. Native collision, pushing, sticky movement and completion logic remain. The slime impulse along the push axis is paced as well; transverse velocity is retained. Straight pushed lines resolve their matching base/source clock, up to the native push limit, without loading chunks. Other gated block renderers receive virtual partial ticks; their first gated tick runs immediately to avoid rewinding an old interpolation interval on activation.

## Animated texture path

On first use after resource reload, original frames for animated `minecraft:block/` sprites are cached in a texture array. Layers have independent mip chains, preventing adjacent-frame bleeding. Actual metadata order, repeated frames, frame durations and RGB interpolation are respected. Water/lava/fire/Nether portal sprites are prioritized. The vanilla test atlas contains 49 supported animated sprites. Storage is bounded to 64 sprites, 16,384 timeline entries, device layer limits, frame dimensions up to 128 and 16 MiB base frame pixels; oversized additions retain native rendering.

Small runtime shader variants wrap the installed five terrain/moving-block shaders. Their original lighting, tinting, alpha cutout and fog remain. Frame sampling changes only during world rendering and at recognized animated UVs; global atlas uploads, inventory and held-item rendering remain native. Unrecognized resource-pack shader layouts retain their own shader. Procedural effects such as the End portal shader, replacement chunk renderers and external shader packs are outside this implementation's tested scope.

A sparse four-block lattice integrates local texture lag. Tetrahedral interpolation uses four nodes per sample for continuous spatial timing, including a six-block field-edge blend. It preserves phase through movement, overlap and deactivation. After 60 normal-speed ticks, a 40-tick smooth fade restores the native atlas; clocks expire at 100 ticks. CPU state is capped at 32,768 nodes, with a fixed 131,072-slot GPU hash table and 16 probes. At most six fields within 192 blocks of the camera contribute. The table uploads once per native atlas tick, not once per fragment/frame. There are no per-frame chunk rebuilds, block searches, new world entities or particle emitters.

Raw texture uploads isolate and restore OpenGL unpack row/skip state, which native atlas uploads can leave configured for subrectangles. A first pixel regression also exposed that a 32-sprite alphabetical limit excluded water; the 64-sprite cache and priority order cover all 49 vanilla candidates. Reload/world changes release the old frames and clock data before rebuilding. Native reported texture storage is about 7.6 MB in the default atlas, including the float clock table and mipmaps; this is a storage figure, not a hardware performance claim.

## Verification

Four new JVM cases cover local rates/phase continuity, smooth restoration/expiry, bounded state and the exact signed/unsigned hash agreement at positive/negative world limits. Four server scenarios cover both models' extending/retracting native piston progress and interpolation, real bound-ticker updates, a pushed line crossing the bubble edge and normal resumption, and actual slime contact/launch velocity. Existing furnace, scheduler, fluid, repeater, combat and shop cases remain included.

The native client review compares actual shader framebuffer pixels against independently selected source frames for water, lava, fire, Nether portal and magma. In the successful capture, all five match with zero channel error; outside-field shader samples also exactly match native output. It also checks advancing client piston endpoints on consecutive real ticks and cache expiry after deactivation. The same build runs the prior native Dynalar playthrough before the new fixture. Screenshots come from a disposable integrated world at 1280×720 with software OpenGL, without working audio/session services; no hardware benchmark or authenticated two-computer test is claimed.

The expanded suite exposed a pre-existing clone fixture that registered only the source chunk in its private native scheduler. When test placement put the destination across a chunk boundary, vanilla correctly refused that destination tick. The fixture now registers both containers, retaining all remaining-delay assertions. Production scheduling code is unchanged by this correction.
