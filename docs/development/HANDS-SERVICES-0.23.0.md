# Hands integration and service menus — NeoForge 0.23.0

Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21. The Forge 1.20.1 source, generator and published 0.20.0 JAR are unchanged.

## TaCZ collision and rendering

The integration was checked against the native **TaCZ NeoForge 1.2.0-hotfix** JAR. Its `EntityKineticBullet.tick()` runs `onBulletTick()` before movement, drag, gravity and lifespan updates. Cancelling `AmmoHitBlockEvent` only cancels the impact callback. The ricochet now completes immediately after the native collision pass: it moves the existing projectile just outside the struck face, installs the reflected velocity, sends its pose and ends the remainder of that impact tick. The next tick continues native flight and damage. Only one reflection is allowed, and explosive rounds retain native behavior.

The former aiming preview only handled bows. Gun ADS now reads the native gun/attachment property cache and the gun's gravity and friction, then traces one reflection. The preview is limited to 96 segments and 160 blocks, with entity stops and no extra preview entities. It represents the centre trajectory; random spread, moving targets and subsequent Smart Link guidance can differ.

Smart Link keeps the original native bullet, ammunition, attachments, damage events and headshot rules. Timestamped server pose samples replace its independent client ballistic simulation while guided. Rendering interpolates those samples and bounds extrapolation. Managed tracers no longer apply TaCZ's separate first-person muzzle displacement to the curved flight path. The HUD uses small target-following corners, a name/range and a remaining-lock line. The scanner's existing display is unchanged.

## Implants and services

Immovable Force becomes Iconic at its existing tiers 2–5 and capacity 35. Existing item IDs and every saved catalogue index stay intact. It uses Iconic prices and matching-tier chips, rejects salvage purchases and leaves the chromed-mob equipment pool. Its stronger effects include movement-independent charging, reduced arrow gravity, gun recoil and gun spread. This is a Minecraft adaptation, not a claim that these percentages reproduce the original game's stats.

[Cyberpunk reference: Immovable Force](https://cyberpunk2077.wiki.fextralife.com/Immovable+Force) describes recoil and bullet-spread reduction and its cover-related effect. The mod adapts that stability role to vanilla ranged weapons and TaCZ. Native gun methods, rather than the web description, determine the collision/render integration.

The ripperdoc, fixer, installation bench and quickhack editor share compact flat surfaces, slimmer text/actions and restrained accent lines. The shop has six selectable rows and one purchase footer, replacing repeated purchase buttons. The full cyberware viewer is visually unchanged. Native server-owned inventory slots, item storage and purchase validation remain in use.

Street Cred adds ten capacity at each complete ten-level milestone, on top of the configured base. Both installation and the cyberware viewer synchronize the same current server limit using two short data lanes. Existing inventories and reputations need no migration.

## Regression scope

- Native TaCZ tests tick a real bullet into a stone wall, verify reflected flight outside the wall and verify a real gun round subsequently damages a mob. Explosive exclusion and single-bounce checks remain.
- Standalone service tests verify live capacity limits and placement before/after a reputation milestone, the configured base and saturation, Iconic classification/pricing and chromed-pool exclusion.
- The native client uses real ADS, native gun data, rendered trajectory and target brackets. A deliberately slower, zero-gravity native round allows sustained inspection of the guided pose/render path; it verifies one entity and bounded successive samples. This is a visual-path fixture, not a change to real gun speed or gravity.
- Actual default and smaller service menus are captured in-game. The native shop check changes pages and confirms that the purchase selection follows the visible page. Production builds omit all fixtures.

A full standalone run also exposed a shutdown deadlock: scheduled-tick cleanup could request the same chunk being unloaded through `TemporalEnvironment.pace`. Pace inspection now uses an already-loaded chunk and never loads a missing one. A regression queries a distant unloaded position and confirms it remains unloaded; server shutdown is checked after the test run.

The tests cover the pinned default TaCZ gun pack. They do not establish identical visuals or scripts for every third-party gun pack or shader combination.
