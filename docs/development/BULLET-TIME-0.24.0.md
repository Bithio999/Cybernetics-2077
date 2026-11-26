# Continuous bullet flight and gun mechanisms — NeoForge 0.24.0

## Flight

Previously the projectile tick budget ran a whole native TaCZ step only when enough world time had accumulated. At David's 0.0625 pace, that left a round stationary for fifteen ticks, followed by a full-speed displacement. Interpolating poses could hide some jumps, but collisions still happened in those large steps.

Eligible non-explosive kinetic rounds now run their native tick every world tick. Only the movement-vector reads inside the collision sweep and position integration are shortened. Canonical velocity remains in native units, so native hit callbacks, knockback, ammunition, headshots and ricochet retention do not receive an extra speed multiplier. Gravity, drag and lifetime advance on fractional local time. Overlapping fields still apply once; crossing the boundary changes pace immediately without catch-up steps.

`FlightMath` uses a fractional power of TaCZ's discrete move → drag → gravity update. With native drag factor q and local duration t, A = (1 − q^t) / (1 − q), displacement = A·v + Y·g·(A − t)/(1 − q), and next velocity = q^t·v − Y·g·A. The q≈1 limit uses a series to avoid cancellation. Any subdivision totaling one native tick recovers the original displacement and forces. This follows the native discrete trajectory rather than substituting a different ballistic equation.

Explosives and unsupported drag values keep the existing whole-tick clock; their native fuses and impact behavior are not rewritten. Arrows, tridents and other entities keep their existing timing.

Server sweep endpoints are timestamped each world tick. The client interpolates from its displayed endpoint to the newest confirmed endpoint, using ordinary render partial ticks and segment orientation. It holds when no fresh endpoint has arrived rather than predicting a flight through an unconfirmed wall. Ricochet contact points remain waypoints. Early packets are bounded to 128 entries, expire after 40 ticks and validate entity UUIDs. State clears on world changes; removed entities release their state. This is not a latency compensation or rewind system.

Native tracer length uses local travel speed while damage continues to use canonical velocity. Managed tracers share the actual trajectory rather than a second muzzle-offset path. Existing post-removal reflected-path streaks remain in place.

## Firing mechanisms

TaCZ's `ObjectAnimationRunner.elapsedSinceLastUpdate` supplies local elapsed time to firing/cycling clips (`shoot`, `fire`, `bolt`, `pump`, `cycle`, `cock`). This slows authored keyframes, transitions and timed sound/particle events with the same weapon clock as firing intervals. Static and reload clips are excluded, and firing-clip scaling is suspended during a native reload. Draw, aiming and ordinary handling retain their clocks. The native global `GunTime` clock is unchanged.

This is animation timing, not a new gun animation set. A gunpack must provide a visible mechanical animation for that mechanism to move. Custom clip names outside these mechanical families and separately implemented third-party effects are not automatically retimed. The change does not rewrite manual-bolt gameplay timestamps or reload mechanics.

## Verification

See [structured results](../review/results/bullet-time-0.24.0.json). Native client tests use actual AKM and Desert Eagle trigger requests, not manually spawned substitutes, and confirm successive local-flight packets and interpolated render positions. A separate native runner check exercises the injected elapsed-time method: firing/bolt clips advance 0.0625 seconds per real second with David active; empty/tactical reload, draw and idle advance one second. Firing/bolt return to normal after deactivation.

The optional server tests also check an actual native round ricocheting and damaging a target during fractional flight. Pure math tests verify fractional composition for air/water-style drag and near-zero drag. Existing normal-speed ricochet framebuffer checks remain applicable.

Active development remains Minecraft 1.21.1 / NeoForge only. The Forge 1.20.1 release stays at 0.20.0.

To reproduce the native flight/animation check, use the disposable `Cybernetics Neo Review` client save with `-I compat/validation.init.gradle -PcompatClientReview -PcompatTemporalGunReview -PtaczJar=/path/to/tacz.jar -PcompatRunDir=/path/to/review-instance -PcompatOutput=/path/to/results runClient`. `passed.txt` and `trace.txt` record the assertions. The separate `-PcompatRicochetReview` flag runs the normal-speed real-trigger/framebuffer regression. Neither fixture is part of the release JAR.
