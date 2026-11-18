# Additional cyberware

Install at the Cybernetics Bench and inspect effects in the inventory cyberware menu. Higher tiers improve the listed effects; each implant starts at its Cyberpunk 2077 starting tier and continues through **tier 5**. No `+` or `++` tiers are included. These additions have no crafting recipes yet; use Creative **Tools & Utilities** or `/give` for testing.

Only one version of each implant family can be installed. Different families can share a body system, within its socket count and your total capacity. Removing an implant removes its bonuses. Cooldowns and Blood Pump charges persist across logout and death; reinstalling an item does not reset them.

| System | Implant | Available tiers | Capacity | Role |
| --- | --- | --- | --- | --- |
| Frontal Cortex | Ex-Disk | 3–5 | 10 | More RAM and faster quickhack uploads |
| Frontal Cortex | Memory Boost | 2–5 | 18 | More RAM and RAM recovery on kills |
| Operating System | BioDyne Berserk | 2–5 | 20 | Timed melee combat mode |
| Face | Kiroshi "Clairvoyant" Optics | 3–5 | 8 | Scanning and retained enemy highlights |
| Skeleton | Bionic Joints | 1–5 | 4 | Armor |
| Skeleton | Epimorphic Skeleton | 4–5 | 32 | Armor and maximum health |
| Nervous System | NeoFiber | 3–5 | 14 | Chance to mitigate incoming damage |
| Nervous System | Adrenaline Converter | 1–5 | 6 | Speed when entering combat |
| Circulatory System | Blood Pump | 2–5 | 15 | Two-charge burst healing and regeneration |
| Circulatory System | Heal-On-Kill | 1–5 | 10 | Healing on enemy kills |
| Integumentary System | Subdermal Armor | 1–5 | 4 | Armor |
| Integumentary System | Optical Camo | 2–5 | 16 | Temporary concealment |
| Legs | Reinforced Tendons | 2–5 | 8 | Double jump |
| Legs | Fortified Ankles | 2–5 | 6 | Hold-and-release charged jump |

Hands cyberware is reserved for a later firearms update.

Example commands: `/give @s cybernetics2077:ex_disk_tier_3`, `/give @s cybernetics2077:blood_pump_tier_2`, `/give @s cybernetics2077:reinforced_tendons_tier_5`. Change the numbered suffix only within that family’s available range.

## Frontal Cortex

**Ex-Disk:** +3 / 4 / 5 maximum RAM and +15 / 20 / 25% upload speed at tiers 3 / 4 / 5. Upload ticks are divided by the speed multiplier and rounded to a whole tick. **Memory Boost:** +1 maximum RAM, and enemy kills recover 0.75 / 0.95 / 1.15 / 1.35 RAM at tiers 2 / 3 / 4 / 5. These benefits require an installed cyberdeck. RAM remains capped to the equipped loadout.

## BioDyne Berserk

Press **I** to activate. This occupies the single Operating System socket, so it replaces your cyberdeck.

| Tier | Duration | Damage reduction | Melee attack speed | Added crit chance | Crit damage bonus | Cooldown |
| --- | --- | --- | --- | --- | --- | --- |
| 2 | 8 s | 20% | 15% | 5% | 25% | 50 s |
| 3 | 9 s | 30% | 20% | 10% | 50% | 45 s |
| 4 | 10 s | 40% | 25% | 15% | 75% | 40 s |
| 5 | 11 s | 50% | 30% | 20% | 100% | 35 s |

Health cannot fall below **25% maximum HP** while active. Void damage and `/kill` remain lethal. Item use and ranged damage are blocked; melee weapons and Mantis Blades work. Mantis timing and animation speed follow the attack-speed boost. Critical damage bonuses multiply the normal critical hit, including critical Mantis contacts. When Berserk expires, heal **25% maximum HP per enemy killed during it**, capped to maximum health. Cooldowns begin on activation.

## Kiroshi "Clairvoyant" Optics

Hold **V** for the scanner. Scanning range is **48 / 56 / 64 blocks** at tiers 3 / 4 / 5. Visible enemies within **12 / 16 / 20 blocks** receive an outline that lasts **60 seconds** from the last scan. The outline remains after closing the scanner. Hostile mobs and combat opponents count as enemies; passive animals and allies are excluded. The initial mark requires line of sight. Up to 64 marks are retained.

## Skeleton and armor

**Bionic Joints:** +2 / 3 / 4 / 5 / 6 armor at tiers 1–5. **Epimorphic Skeleton:** +6 / 8 armor and +10 / 15% maximum HP at tiers 4 / 5. **Subdermal Armor:** +1 / 2 / 3 / 4 / 5 armor at tiers 1–5. Armor uses Minecraft’s normal armor calculations. Unequipping Epimorphic Skeleton clamps current HP to the resulting maximum.

## Nervous System

**NeoFiber:** 6 / 8 / 10% chance to mitigate a hit at tiers 3 / 4 / 5. A successful mitigation reduces that hit’s remaining damage by 56 / 58 / 60%. It does not protect against void damage or `/kill`.

**Adrenaline Converter:** +8 / 16 / 24 / 32 / 40% movement speed for 5 / 6 / 7 / 8 / 9 seconds at tiers 1–5 when entering combat. It has a 20-second rearm time and requires five seconds without combat before triggering for a new fight. Equipment changes remove the speed boost without changing your base movement attribute.

## Circulatory System

**Blood Pump:** press **H**. It has **two charges**. Tiers 2 / 3 / 4 / 5 immediately restore 20 / 25 / 30 / 35% maximum HP, then regenerate 1 / 1.5 / 2 / 2.5% maximum HP each second for **6 seconds**. One charge regenerates every 40 / 35 / 30 / 25 seconds. Reusing the pump refreshes its regeneration duration rather than stacking it. Using it at full health does not consume a charge.

**Heal-On-Kill:** enemy kills heal 2 / 3 / 4 / 5 / 6% maximum HP at tiers 1–5. Both on-kill implants reward hostile mobs and combat opponents, rather than passive animal farming. Healing cannot exceed maximum HP.

## Optical Camo

Press **C** to cloak for 5 / 6 / 7 / 8 seconds at tiers 2 / 3 / 4 / 5. Cooldown: 60 / 55 / 50 / 45 seconds from activation. Distant mobs drop you as their target and cannot acquire you while cloaked; enemies within **2.5 blocks** can still detect you. Players can still attack you. It conceals your body without applying or removing potion invisibility effects. The HUD shows remaining duration and cooldown.

## Legs

Only one leg implant can be installed.

**Reinforced Tendons:** press **jump again in midair** for one extra jump. Its upward impulse is 0.49 / 0.525 / 0.56 / 0.595 at tiers 2 / 3 / 4 / 5. It resets on landing, swimming or climbing; repeatedly pressing jump cannot generate unlimited boosts. Flying and riding disable it.

**Fortified Ankles:** **hold jump on the ground, then release**. Charge reaches its maximum after one second. Maximum upward impulse is 0.76 / 0.84 / 0.92 / 1.0 at tiers 2 / 3 / 4 / 5. A quick tap makes a normal jump. A jump cannot be charged in midair. Normal fall damage applies to both leg implants.

All keys can be rebound under **Options → Controls → Cybernetics 2077**. The active-implant HUD shows the current bindings.

## Existing Mantis Blades

Mantis Blades now start at **tier 2**, continuing through tier 5. Existing tier-1 blades automatically function as tier 2, including installed and stored items. Their components are retained. Tier 1 is no longer obtainable as a tier, and its crafting/upgrade recipes are removed. Canonical tier-2 blades have 10-damage slashes, a 12.5-damage rising cut and a 17.5-damage lunge.

[Existing implants, chips and controls](CYBERWARE.md) · [Research and starting-tier sources](research/EXPANSION.md)
