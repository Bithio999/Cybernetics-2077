# Cyberware expansion research — 0.14.0

Public Cyberpunk 2077 2.x item catalogue consulted 18 November 2026. These are gameplay adaptations for Minecraft, not exact reproductions of Cyberpunk’s numerical stats. No copyrighted game icons or models are bundled.

| Family / source | First obtainable tier | Capacity |
| --- | ---: | ---: |
| [RAM Upgrade](https://gamestegy.com/cyberpunk-2077/wiki/3152/ram-upgrade) | 1 | 8 |
| [Self-ICE](https://gamestegy.com/cyberpunk-2077/wiki/3148/self-ice) | 3 | 5 |
| [Kiroshi “Doomsayer” Optics](https://gamestegy.com/cyberpunk-2077/wiki/3083/kiroshi-doomsayer-optics) | 1 | 6 |
| [Kiroshi “Stalker” Optics](https://gamestegy.com/cyberpunk-2077/wiki/14458/stalker) | 1 | 6 |
| [Dense Marrow](https://gamestegy.com/cyberpunk-2077/wiki/946/dense-marrow) | 2 | 16 |
| [Para Bellum](https://gamestegy.com/cyberpunk-2077/wiki/3115/para-bellum) | 3 | 25 |
| [Stabber](https://gamestegy.com/cyberpunk-2077/wiki/3042/stabber) | 2 | 12 |
| [Visual Cortex Support](https://gamestegy.com/cyberpunk-2077/wiki/1001/visual-cortex-support) | 1 | 16 |
| [Biomonitor](https://gamestegy.com/cyberpunk-2077/wiki/950/biomonitor) | 1 | 14 |
| [Second Heart](https://gamestegy.com/cyberpunk-2077/wiki/952/second-heart) | 4 | 30 |
| [Shock-n-Awe](https://gamestegy.com/cyberpunk-2077/wiki/953/shock-n-awe) | 1 | 25 |
| [Pain Editor](https://gamestegy.com/cyberpunk-2077/wiki/954/pain-editor) | 4 | 35 |
| [Lynx Paws](https://gamestegy.com/cyberpunk-2077/wiki/942/lynx-paws) | 2 | 5 |
| [Jenkins’ Tendons](https://gamestegy.com/cyberpunk-2077/wiki/14465/jenkins-tendons) | 2 | 6 |

## Adaptation decisions

- Every family starts at the researched tier and continues through tier 5. Capacity follows the item’s listed 2.x capacity. Existing Arms, Hands and Operating Systems receive no new families. 14 new families contain 56 tiered items.
- Dense Marrow uses hunger exhaustion instead of stamina. Stabber supports swords, Mantis Blades and thrown tridents. Visual Cortex Support is distance-based; critical probabilities combine independently with a 50% chance cap.
- Doomsayer detects creepers, primed TNT and fireballs instead of Cyberpunk mines/cameras. Stalker identifies forward hostile/combat/PvP targets through cover, but cannot bypass scanner line-of-sight checks when starting hacks.
- Biomonitor uses actual installed Blood Pump charges or safe healing/regen potions and amplifies all received healing. This broader healing bonus is a deliberate Minecraft adaptation. It never creates free potion effects.
- Second Heart works on native fatal damage, restores full HP, and retains its recharge across logout/death/reinstallation. Void and `/kill` remain effective.
- Self-ICE rejects the entire incoming program before damage/control/DOT application. One charge recharges in 32/26/20 seconds at tiers 3/4/5. Native team/PvP protections remain authoritative.
- Para Bellum multiplies armor; every armor-granting implant also grants flat toughness. These do not counter damage tagged to bypass armor. Pain Editor independently reduces incoming damage.
- Shock-n-Awe is bounded to eight enemies within four blocks and cannot recursively trigger retaliatory discharges.
- Lynx halves step audio volume but does not cancel Minecraft sculk vibrations. Jenkins accelerates only sprinting and tapers over five seconds, recovering at rest. Existing leg implants keep their jump mechanics.

## Save compatibility

New enum entries and shop offers are appended after the original entries, preserving canonical daily-stock offer indices. Defensive timers and sprint fatigue are serialized in the existing attachment; absent fields default safely. No new crafting recipes or recipe-book unlocks are introduced.
