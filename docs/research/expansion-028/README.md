# Current catalogue research and Minecraft adaptations

Scope: the current Cyberpunk 2077 2.x catalogue, not the legacy 1.x implants. [Coverage manifest](coverage.json) maps all **103 non-arm families**, excluding the requested Canto Mk.6, to the implementation. Existing Mantis families and the original David's Sandevistan remain. This expansion adds **64 implant families** (60 Augments and four cyberdecks) and **Weapon Glitch / Cyberware Malfunction**, completing the Combat and Control quickhacks. Covert and Ultimate quickhacks remain excluded.

References consulted:

- [Gamestegy current cyberware catalogue](https://gamestegy.com/cyberpunk-2077/wiki/cyberware): current/legacy separation, categories, Iconic flags, capacities and individual effect descriptions embedded in the current catalogue. Its individual detail pages returned HTTP 403; the public catalogue itself was accessible.
- [Cyberpunk build creator's per-category definitions](https://github.com/Crispsy/Unofficial-Cyberpunk-2077-Build-Creator/tree/main/cyberware%20files): cross-check for names, tier availability and baseline effects. Starting tiers and Minecraft balance are explicitly stored in [new-implants.json](new-implants.json).
- [TaCZ source/API](https://github.com/MCModderAnchor/TACZ), exact NeoForge 1.2.0-hotfix runtime and TaCZ Tweaks 3.0.0-alpha.12 runtime: native event order, kinetic bullet ownership, firing interval, recoil, ammunition and hit processing. Integration remains optional.

## Adaptation decisions

This is Minecraft balance and functionality, not a claim of identical Cyberpunk numerical stats. Per-tier tooltips and wiki pages describe the actual implementation.

- Minecraft lacks Cyberpunk's stamina bar. Stamina effects use hunger/exhaustion, attack recovery or NPC leap recovery. Carry capacity becomes a persistent, tier-limited Titanium Bones reserve.
- Vanilla mobs do not have a trace meter or device networks. Arasaka supports Control quickhack cost/duration rather than nonexistent covert trace behavior. Faceplate suppresses mob pursuit after disengagement; it cannot deceive human players.
- TaCZ does not expose Cyberpunk's Power/Tech/Smart gun classes or charged-tech attacks. Feedback Circuit/Electromag Recycler use steady ADS hits or fully drawn bow hits. Hands implants work on native bullets; no replacement ammunition, second projectiles or fabricated firearm class is introduced.
- Berserk and time systems remain distinct models. Time-slow strength belongs to the model, never increases with item tier, and uses shared relative-time fields. Tiers improve charge/duration or secondary effects.
- Conditional defenses, armor toughness, RAM management, cooldown recovery, poison interactions and delayed wounds use shared LivingEntity mechanics. NPCs use autonomous counterparts for supported combat actives. Cosmetic tattoos remain cosmetic.
- New artwork is original pixel art, not extracted game assets. Cyberpsycho geometry uses three baked articulated exoskeletons with a shared original Minecraft-format texture; existing optimized blade meshes render installed Mantis variants.

Only Minecraft 1.21.1 NeoForge is updated. The Forge release remains unchanged.
