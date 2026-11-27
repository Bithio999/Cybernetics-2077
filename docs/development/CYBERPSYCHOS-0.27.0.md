# Cyberpsycho entities — NeoForge 0.27.0

Sightings now deploy the registered `cybernetics2077:cyberpsycho` hostile humanoid. It uses a Minecraft player-shaped mesh with varied built-in skins, the existing chromed panel/glow layers, native held swords, navigation, attack swings, damage feedback and independent AI. It does not replace vanilla mobs or add natural spawns. A spawn egg is included in the creative tab.

## Capacity and dossiers

The human baseline is 150 capacity. Difficulty is `min(6, ceil((installed - 150) / 25))`; a loadout at or below 150 is rejected. Catalogue item costs determine installed capacity. The loadout planner checks socket validity, uniqueness, no Iconics and exact band membership. Cost totals are 162, 187, 215, 250, 270 and 278, using 14–17 distinct implants. The first two difficulties use Tier 3 implants, the next two Tier 4, and the last two Tier 5. Contract tier and implant tier are different measures.

The Street Cred bracket chooses which loadout/difficulty can be offered. The actual cost then determines the quoted contract rank. The menu, dossier and scanner show capacity against 150. Existing reward tables, guards, containment, ownership and one-time payment checks remain in force. Pending older sightings redeploy using the dedicated entity while preserving their token and quoted rewards; completed/contained jobs are preserved.

## Working implant package

All selected implants have explicit NPC behavior, rather than relying on player-only hooks:

- Native sword strikes use Dense Marrow, Stabber, Berserk critical bonuses and (at Tier 6) Handle Wrap's combat-entry window.
- Biodyne Berserk activates in combat with the player duration, recharge, attack-rate increase, reduction and 25% HP floor. Kills heal on expiry.
- Biomonitor triggers Blood Pump below half health. Pump uses two real charges, one-at-a-time recharge and six seconds of pulses. Biomonitor increases healing.
- Adrenaline activates on entering combat, respecting the 20-second rearm and five-second disengagement requirement.
- Reinforced Tendons permits cooldown-limited, line-of-sight gap-closing jumps through normal entity physics.
- Armor/toughness, Epimorphic max HP, Para Bellum, Neofiber and Pain Editor follow their equipped tier formulas.
- Microgenerator and Shock-N-Awe use bounded, line-of-sight electrical damage with native cooldowns and existing typed-secondary damage handling.
- Self-ICE intercepts the actual quickhack submission path and recharges; it is not unconditional hack immunity.
- Heal-on-Kill at lower ranks and Second Heart at higher ranks retain their healing/fatal-hit behavior. Second Heart does not stop `/kill` or void deaths.

Medical charges, OS timers, Self-ICE, Second Heart, proc cooldowns and equipment survive save/load. Attribute reapplication is idempotent and does not refill HP. Containment disables AI and all autonomous implant activity. Body loot uses existing salvage odds saturated at six implants; nonlethal containment grants no death loot.

## Validation

See [recorded results](../review/results/cyberpsychos-0.27.0.json), [capacity dossier](../images/psycho-027-offer.png) and [native entity/scanner](../images/psycho-027-scanner.png). Forge 1.20.1 remains unchanged.
