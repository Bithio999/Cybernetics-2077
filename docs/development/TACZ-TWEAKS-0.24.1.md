# TaCZ Tweaks ricochet compatibility — NeoForge 0.24.1

## Reproduction

The reported version is **TaCZ Tweaks 3.0.0-alpha.12+1.21.1-neoforge**, for TaCZ 1.2.0. Installing that exact public artifact beside TaCZ 1.2.0-hotfix reproduces four failures in the previously passing 17-test gun suite: native rebound, reflected target damage, fractional reflected damage and the fast second-wall scenario. This establishes a real projectile-removal failure, in addition to the older client-only display issue.

Tweaks replaces the native collision sweep with its own ray tracer. For a non-piercing block, its `BulletHandler` calls the native block-impact handler while processing the interaction, then calls that handler again before returning. Cybernetics cancels the first impact and queues a reflected velocity; it marks the bullet as already bounced. Previously, the duplicate callback then bypassed cancellation and native TaCZ discarded the bullet before the queued reflection could complete.

## Correction

While a reflection is pending, repeated impact events stay cancelled. The existing post-collision hook completes the reflection once, clears the pending flag and moves the real bullet outside the wall. A later genuine impact remains native: this does not add extra bounces, resurrect discarded projectiles or replace the Tweaks ray tracer. Explosive exclusions, native ammunition, target damage and the one-bounce limit remain intact.

The investigation also found that the same TaCZ block query becomes a live collision/damage operation under Tweaks when its collision context contains a bullet. Geometry-only queries now use Minecraft's empty collision context for bullet inputs. They retain the native TaCZ block traversal but cannot dispatch Tweaks live-shot handlers. Non-bullet contexts retain their original shapes. The aiming preview remains an estimate rather than a simulation of arbitrary Tweaks datapack piercing rules.

## Integrated-server trace state

The first live-client run after the duplicate-callback fix reflected five of six rounds; one round received no collision callback and crossed an intact stone wall. Tweaks' `BulletRayTracer` keeps its active `level` and `result` in static fields shared by the render and integrated-server threads. A concurrent preview query can clear those fields during a real bullet sweep, after which Tweaks still cancels the native collision continuation.

A soft mixin serializes the complete Tweaks `rayTraceBlocks` entry point, including its cleanup, with a reentrant lock. Native TaCZ tracing without Tweaks is untouched. This preserves Tweaks' interactions while preventing the client query from overwriting the server's active state. The lock adds no polling, repeated trace or extra projectile.

## Verification

The expanded optional suite has 20 tests, including duplicate-event idempotence, repeated read-only geometry queries with a real bullet as the supplied context, and overlapping trace-context isolation. The isolation test holds one trace open while another thread attempts entry, verifies that the second cannot overwrite the active context, then verifies cleanup. Existing actual native projectile damage and fractional-flight cases run through the installed Tweaks ray tracer. Native client review fires AKM and Desert Eagle rounds through normal client trigger requests and checks ammunition consumption, reflected target damage, received server segments and rendered reflected pixels with the aiming preview closed.

[Structured results](../review/results/tacz-tweaks-0.24.1.json) record the completed checks. Reproduce with the local optional dependency flags described in [compatibility testing](../../compat/README.md). Third-party JARs and sources are kept outside the repository and are not bundled. Forge 1.20.1 remains at 0.20.0.

## References

- [TaCZ Tweaks release](https://modrinth.com/mod/tacz-tweaks/version/yHf0OCxW), including its published source JAR, used to inspect the exact alpha.12 implementation.
- [TaCZ Tweaks source repository](https://github.com/MUKSC/TaCZTweaks): `feature/raytracer/BulletHandler.kt`, `BulletRayTracer.kt`, and `mixin/feature/raytracer/EntityKineticBulletMixin.java`.
- [TaCZ Tweaks bullet interactions](https://github.com/MUKSC/TaCZTweaks/wiki/Bullet-Interactions).
