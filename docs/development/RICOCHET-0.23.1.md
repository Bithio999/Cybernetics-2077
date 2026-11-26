# Full-speed ricochet display — NeoForge 0.23.1

The 0.23.0 collision tests established server reflection and damage, but did not establish that the reflected flight was visible during normal gunfire.

## Reproduction

An integrated client now fires the actual native AKM (`tacz:ak47`) and Desert Eagle (`tacz:deagle`) through `IClientPlayerGunOperator.shoot()`. TaCZ sends its normal fire request, creates the rounds, consumes ammunition and ticks them in the real world. The test does not construct, slow, reposition or manually tick these projectiles.

With default TaCZ 1.2.0-hotfix settings, the AKM rounds travel about 25 blocks per physical tick and the Desert Eagle rounds about 17. Both struck a stone wall, reflected, and damaged a target on the reflected path. In the close-range scene, each native round was removed on its second tick. Client render observations showed a straight-flight frame beyond the wall, followed by a pose at the impact point; the delayed pose interpolation never displayed the completed outgoing leg before removal. This reproduces the misleading appearance of no ricochet.

## Correction

- Before the first server correction, client prediction uses TaCZ's read-only block collision query to stop a coprocessor-equipped player's non-explosive round at the wall.
- Timestamped pose packets that arrive before their entity are retained briefly, with bounded storage and UUID checks, rather than discarded.
- The first two completed reflected flight segments are sent separately from entity tracking. Actual native collision results supply their endpoints, including the final entity or second-wall hit. A brief depth-tested streak survives native entity removal. It never deals damage or creates another projectile.
- The ordinary native tracer is hidden while that same reflected segment is displayed, preventing duplicate flight paths. Native bullets retain their speed, ammunition, headshots, damage, explosive exclusions, single-bounce limit and time-bubble simulation.
- The aiming preview now includes TaCZ's global bullet-speed multiplier and uses its pass-through block settings.

The new trace buffers hold at most 128 segments and 128 short-lived cover entries. Pose packets waiting for an entity also have a 128-entry cap and expire after 40 client ticks. Buffers clear when the client world changes.

## Verification

The client fires three AKM shots with a Tier 5 coprocessor and three Desert Eagle shots with Tier 1. Assertions check native ammunition consumption, damage to the reflected target, six received segments ending at the real target, and a visible reflected path after native entity removal. Captured framebuffer pixels are checked along the reflected segment with the aiming preview closed, in addition to manual image inspection.

Optional gun GameTests also verify the native global speed in both preview profiles and a full-speed Tier 1 ricochet hitting a second wall after its owner disappears. Existing optional-gun and standalone tests remain applicable. See the [results](../review/results/ricochet-0.23.1.json).

Minecraft 1.21.1 / NeoForge only. Forge remains 0.20.0 and is not regenerated.
