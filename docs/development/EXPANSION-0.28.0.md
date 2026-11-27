# Catalogue and shared NPC systems — NeoForge 0.28.0

[Research / 103-family coverage](../research/expansion-028/README.md) · [Player wiki](../wiki/index.md)

## Delivered scope

The current non-arm catalogue excluding Canto is complete: 60 added Augment families, four added cyberdecks and two added Control programs. There are 415 clinic offers, including 216 new tiered offers. All starting tiers, capacities, prices, program limits, Iconic requirements and effects have player documentation. No crafting recipes were added. Cosmetic tattoos are deliberately cosmetic. Minecraft adaptations are explicitly described in the research notes and item tooltips.

Cyberpsycho dossiers now store a seed. A deterministic allocator produces socket-valid Berserker, Netrunner or Blitz bodies within real capacity bands, rather than fixed repeated loadouts. Contract ranks 1–5 cover 25-capacity bands above 150; rank 6 covers 276–325. Subdermal Armor is the only common protection baseline. Candidate families, secondary tiers, Operating Systems, weapons/Mantis variants and installed chips vary. Iconics cannot roll. The dossier retains the exact equipment and identity across redeployment.

`MobImplants` shares equipment-driven combat behavior between dedicated cyberpsychos and ordinary chromed mobs. It handles model-specific actives, medical charges, Second Heart, Self-ICE, jumps, Mantis combat, RAM and installed-program uploads. `ExpansionRuntime` supplies shared conditional stats, mitigation, RAM management, cooldown recovery and delayed wounds. Vanilla melee and bow goal recovery reads the real installed speed bonuses, and ranged mobs switch to a melee goal while Berserk is active. Previous duplicate ordinary/psycho event handlers were removed. Ordinary chassis bonuses remain; Epimorphic Skeleton is applied once through its owning layer.

NPC decks carry real container chips, have RAM reservation/recovery, sequential upload timing, legal tier/slot limits and range/line-of-sight delivery checks. Player targets receive an upload warning. They share the normal quickhack effects and counterplay boundary; no fake player or unavoidable instant cast is used. Weapon Glitch blocks new ranged firing before consumption; Cyberware Malfunction disables installed equipment. Jamming does not prevent cyberdeck uploads or freeze ordinary movement.

NPC Sandevistans join the existing relative-time field system. Player/NPC clocks, differing strengths, environmental clocks and projectile rules share the same implementation. Entity-ID/UUID snapshots synchronize equipment actives and model identity; bounded captured meshes render mob afterimages as well as player trails. The new models do not make tier upgrades strengthen the slowing percentage.

Smart Link now computes a capped constant-velocity intercept and uses stronger bounded spherical steering. Projectile speed is preserved; backwards targets cannot reverse a round. The original native bullet remains authoritative. TaCZ headshots, ammunition, attachments, explosive exclusions, ricochets and PvP/team checks remain in the native bridge. Conditional weapon bonuses and recovery implants use that bridge once.

Cyberpsycho rewards are 300% of standard base payment. The old civilian objective is replaced by +30% for taking no final HP damage before completing the objective. Shield-blocked/fully absorbed hits preserve it; death loses it. Existing accepted base payments remain quoted, and old active jobs migrate their optional objective and deterministic loadout without paying twice.

## Persistence and presentation

Original enum ordinals and the 199-offer prefix are preserved. New catalogue entries append. Existing installed cyberware remains recognizable. Mob runtime state persists with its actual cyberware, including medical cooldowns and uploaded jobs. Refresh/load does not refill HP or reroll implants. Compatibility checks cover save/load and modifier idempotence.

The new original pixel atlas exports centered 32px sprites with binary alpha and a limited palette. Three baked mechanical exoskeleton models follow native limb animation and use an original 64px Minecraft skin; optional installed blades reuse the optimized Blender mesh. Geometry is baked, nearby trail actors are capped and weak actor references are cleaned up. No additional ambient spawns or global tick-rate changes were introduced.

Titanium Bones provides a tier-limited reserve with synchronized native slots. Unequipping it preserves contents. Death follows keepInventory; ordinary drops clear the old reserve. Chrome Compressor occupies the OS socket and cannot be removed while its extra capacity is occupied. Behavioral Faceplate is not treated as scanner optics.

## Validation

See [machine-readable results](../review/results/expansion-0.28.0.json) and [validation index](../VALIDATION.md). Dedicated tests exercise 240 seeded plans across every band, actual NPC uploads/medical/jumps/time fields, RAM/chips, delayed damage, catalogue/save compatibility, carrying, conditional stats and optional rewards. Optional tests use TaCZ 1.2.0-hotfix with TaCZ Tweaks 3.0.0-alpha.12. Native client checks cover the rendered role models/icons, reserve synchronization, enemy hacking/time fields and fixer flow.

The integration does not implement Cyberpunk's device networks, tracing meter, perk tree or gun-class system. These are explicitly adapted rather than represented by nonfunctional text. Native vanilla/TaCZ ranged weapons are supported; unrelated third-party NPC gun APIs are not assumed. Tests cover specific scenarios, not every possible combination of all catalogue items.

Only NeoForge 1.21.1 is released. Forge sources/version/JAR remain unchanged.
