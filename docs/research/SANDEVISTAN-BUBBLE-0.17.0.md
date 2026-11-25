# Local time bubble and first-person body trails — 0.17.0

## Clock model

The existing 32-block Dynalar field keeps the same strength at tiers 2–5: active user movement/weapon pace is 1.25× normal, ordinary affected entities are 0.625× normal. The environmental field uses the strongest applicable source once. Environmental blocks have no personal acceleration or owner/team exemption because the block simulation is shared.

No server/world clock is replaced. Server tick rate, game time, sun movement, weather state progression, player resource timing and Sandevistan battery timing remain native. Environmental work runs at its local pace. Mounted entities retain ordinary vehicle behavior; empty boats/minecarts, items, XP, falling blocks and primed TNT join the existing mob/projectile simulation clock.

## Scheduled blocks and fluids

Native `LevelTicks` continues owning every scheduled entry, its priority, sub-order, chunk serialization and scheduling queries. A separate per-dimension SavedData file stores only remaining virtual delay, keyed by block/fluid registry ID and position. It is indexed by chunk so updates visit loaded containers rather than all historic unloaded positions.

Registration observes only successfully queued, nonduplicate entries. Active fields also discover previously queued work in nearby loaded containers without loading chunks. Each real tick consumes `localPace` units of remaining delay. If vanilla reaches an entry before its local delay has elapsed, the entry is requeued with its original priority/sub-order at the earliest possible normal-speed completion time. This reduces retries while still responding promptly if the field disappears. The callback runs once when ready; missed update calls are never accumulated for later replay.

Container unload/load hooks preserve remaining local delay alongside vanilla's saved chunk delays. SavedData round trips preserve fractional delay and rebuild the chunk index. Native area clearing removes matching metadata; area copying retains unfinished local delay and leaves pre-existing destination entries intact. The implementation does not hide pending work in a replacement queue or force chunks loaded.

Standard block-entity tickers use a bounded fractional gate per position on both server and client. Inactive gates are pruned. Random block/fluid ticks and ambient animation callbacks use proportional sampling, without accumulating catch-up work. Immediate neighbor notifications and player interactions remain native. Scheduled repeater/fluid timing and moving-piston tickers are paced; this is not a blanket delay on every neighbor query.

## Rain and splashes

Minecraft rain is mostly textured sheets. A local continuous clock per visible rain column changes their UV scrolling speed. It begins at the current native phase and retains phase when switching back to normal speed. A six-block edge blend covers the outer part of the 32-block field. Recent column clocks are retained briefly to avoid phase jumps while panning, then pruned. The vanilla column seed/speed pattern is reused with a cached RNG.

Only rain is changed; snow and sky clocks remain native. Rain splash spawning is proportionally thinned and WaterDropParticle physics/lifetimes use fractional ticks. Virtual interpolation keeps skipped particle ticks smooth. No extra particles or dynamic lights are added. Custom weather renderers that replace vanilla precipitation may keep their own animation; shared water/fire texture-atlas animation and mod-specific global machine clocks are not intercepted.

## First-person body echoes

Own world poses now render in first person once their saved root is at least 1.25 blocks from the current player root. Existing live-body exclusion and camera-proximity fading still apply. This allows looking back, circling or walking backward through the trail while avoiding the camera sitting inside a saved head/torso. Other observers and third-person views retain the same behavior. Saved camera anchoring, birth hues and history/draw limits remain unchanged.

## Checks

Five new JVM cases exercise complete local delays, field exit without catch-up, visual phase continuity, frame-subdivision independence and restored fractional timers. The first-person visibility test now checks clearance rather than blanket suppression.

Nine server cases cover local block/fluid delays, entry after a tick was already queued, field exit, SavedData NBT round trip and cancellation, range/overlap/random frequency, actual furnace tickers versus a control, copied scheduled delays, actual water flow, and actual repeater timing. Existing server gameplay coverage remains included. The furnace case verifies that world/sun clocks advance normally. Persistence coverage uses an NBT round trip; it is not a full process-restart or external-mod compatibility test.

The native client review verifies vanilla rain UV hooks, paced splashes, visible own full-body echoes in first person, third-person rendering, normal rain pace after deactivation and unchanged sun time. The existing stationary/overlap/camera-inside/reversal trail suite also runs. Native captures use a disposable integrated world and software OpenGL; they do not establish hardware performance, authenticated two-computer behavior, audio, or every external machine/weather implementation.
