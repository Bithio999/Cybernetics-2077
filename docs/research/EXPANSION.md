# Cyberware expansion research

Reviewed the individual Gamestegy Cyberpunk 2077 **2.x** database pages below on 17 November 2026. These pages were accessible for this pass. The visible tier selectors and “How to obtain” sections establish starting tiers; the effects sections establish each implant’s role. No pre-2.0 rarity names or `+`/`++` upgrades are treated as separate numbered tiers.

| Implant | Starting tier | Reviewed page |
| --- | --- | --- |
| Ex-Disk | 3 | [Source](https://gamestegy.com/cyberpunk-2077/wiki/963/ex-disk) |
| Memory Boost | 2 | [Source](https://gamestegy.com/cyberpunk-2077/wiki/962/memory-boost) |
| BioDyne Berserk | 2 | [Source](https://gamestegy.com/cyberpunk-2077/wiki/3082/biodyne-berserk) |
| Kiroshi "Clairvoyant" Optics | 3 | [Source](https://gamestegy.com/cyberpunk-2077/wiki/3053/kiroshi-clairvoyant-optics) |
| Bionic Joints | 1 | [Source](https://gamestegy.com/cyberpunk-2077/wiki/1030/bionic-joints) |
| Epimorphic Skeleton | 4 | [Source](https://gamestegy.com/cyberpunk-2077/wiki/3071/epimorphic-skeleton) |
| NeoFiber | 3 | [Source](https://gamestegy.com/cyberpunk-2077/wiki/1052/neofiber) |
| Adrenaline Converter | 1 | [Source](https://gamestegy.com/cyberpunk-2077/wiki/3114/adrenaline-converter) |
| Blood Pump | 2 | [Source](https://gamestegy.com/cyberpunk-2077/wiki/3056/blood-pump) |
| Heal-On-Kill | 1 | [Source](https://gamestegy.com/cyberpunk-2077/wiki/949/heal-on-kill) |
| Subdermal Armor | 1 | [Source](https://gamestegy.com/cyberpunk-2077/wiki/957/subdermal-armor) |
| Optical Camo | 2 | [Source](https://gamestegy.com/cyberpunk-2077/wiki/958/optical-camo) |
| Reinforced Tendons | 2 | [Source](https://gamestegy.com/cyberpunk-2077/wiki/941/reinforced-tendons) |
| Fortified Ankles | 2 | [Source](https://gamestegy.com/cyberpunk-2077/wiki/1240/fortified-ankles) |
| Mantis Blades | 2 | [Source](https://gamestegy.com/cyberpunk-2077/wiki/994/mantis-blades) |

Ex-Disk, Clairvoyant and NeoFiber start at **3**, not 2. Epimorphic Skeleton starts at **4**. Memory Boost starts at **2**. Bionic Joints, Adrenaline Converter, Heal-On-Kill and Subdermal Armor have tier-1 versions. These distinctions are reflected in registration and tested; unavailable lower tiers are not invented.

The requested hands implant was withdrawn, so the expansion adds **14** families and **55** item variants. Mantis Blades’ original tier-1 registry ID remains solely as a compatibility alias whose item definition is now tier 2. It is hidden from Creative and has no tier-1 crafting recipe. Canonical Mantis Blades start at tier 2.

## Minecraft adaptations

- Cortex RAM and upload speed use the mod’s real RAM cap and server upload duration, including refunds and loadout changes.
- Berserk retains its melee-only mode, damage reduction, health floor, speed/critical bonuses and healing for neutralized enemies. Minecraft has no stamina system, so no stamina perk is advertised. The damage-floor exception for void and kill commands preserves normal administrative/world behavior.
- Clairvoyant enables the scanner and retains enemy highlights. Initial detection requires visible hostile mobs/combat opponents. It is not The Oracle’s general through-wall discovery. Marks can remain visible after an enemy moves behind cover.
- Armor uses vanilla attributes. Max-health changes use reversible transient modifiers. Mitigation is a server-side chance to reduce remaining damage.
- Blood Pump uses a two-charge healing item model, with percent-of-max-HP values suited to Minecraft. Charge recharge/cooldowns persist across logout and death. Heal-On-Kill and Memory Boost use enemy kills.
- Optical Camo supplies concealment, distant-mob targeting protection and a timed HUD state without changing potion effects. It is not invulnerability.
- Reinforced Tendons supplies one midair jump, and Fortified Ankles supplies a ground charge and release. Both use server-validated velocity and retain fall damage.

The exact Minecraft numbers are balance choices, listed in [the player guide](../IMPLANTS.md), rather than claimed as literal Cyberpunk damage or armor conversions. Icons are original 32px pixel art authored by [the reproducible generator](../../art/generate-expansion.py). There are no new crafting recipes in this expansion.
