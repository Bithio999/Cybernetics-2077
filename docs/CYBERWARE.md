# Cyberware reference

Install implants at a **Cybernetics Bench**. The tab beside your inventory views your equipment and manages quickhack chips. All three implant families and Short Circuit have **tiers 1–5**, shown in white, green, blue, purple and orange. Higher tiers improve their actual effects.

| Implant | System | Capacity, tier 1 → 5 |
| --- | --- | --- |
| Basic Kiroshi Optics | Face | 4 / 5 / 6 / 7 / 8 |
| Militech Paraline | Operating System | 8 / 12 / 16 / 20 / 24 |
| Mantis Blades | Arms | 12 / 15 / 18 / 21 / 24 |

[Cyberpunk 2077 cyberware catalogue](research/CYBERWARE-CATALOGUE.md) · [Upgrade recipes](#tier-upgrades)

## Basic Kiroshi Optics

Hold **V** to scan a living target with clear line of sight. Range is **32 / 40 / 48 / 56 / 64 blocks** at tiers 1–5. Release to close. Shows health, armor, attack damage, absorption and visible effects. Installing a loaded cyberdeck adds quickhack controls to this same overlay.

Tier 1 ID: `cybernetics2077:optical_scanner`. Tiers 2–5: `cybernetics2077:kiroshi_optics_tier_2` through `_5`.

| | | |
| --- | --- | --- |
| Iron ingot | Glass | Iron ingot |
| Redstone | Amethyst shard | Copper ingot |
| Iron ingot | Lapis lazuli | Iron ingot |

## Militech Paraline

| Tier | RAM | Program sockets | RAM / second | Queue limit | Upload time | Quickhack damage bonus |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 4 | 2 | 0.2 | 2 | 1.0 s | — |
| 2 | 6 | 3 | 0.3 | 2 | 0.9 s | +5% |
| 3 | 8 | 4 | 0.4 | 3 | 0.8 s | +10% |
| 4 | 10 | 6 | 0.5 | 3 | 0.7 s | +15% |
| 5 | 12 | 8 | 0.6 | 4 | 0.6 s | +20% |

Install the deck in the bench’s **OS** socket. Select the installed deck under **Operating System** in the inventory cyberware menu, then press **Manage Quickhacks**. The back arrow returns to the same Operating System page. The bench also has a **Cyberdeck** shortcut. Shift-click a chip to install it, or move it into a quickhack socket. Chips must be the same tier as the deck or lower. Only one copy of each program can be installed, including across different tiers. Currently Short Circuit is the available program; spare sockets are ready for future quickhacks. Removing the deck preserves its chips; removing the deck or program cancels pending uploads and refunds their reserved RAM.

Tier 1 ID: `cybernetics2077:cyberdeck_tier_1`. Tiers 2–5: `cybernetics2077:militech_paraline_tier_2` through `_5`.

![Tier-5 Paraline program sockets](images/tier-five-cyberdeck.png)

| | | |
| --- | --- | --- |
| Gold ingot | Copper ingot | Gold ingot |
| Redstone | Amethyst shard | Redstone |
| Iron ingot | Copper ingot | Iron ingot |

## Short Circuit

| Chip tier | RAM cost | Base electrical damage |
| --- | --- | --- |
| 1 | 2 | 8 |
| 2 | 3 | 11 |
| 3 | 4 | 14 |
| 4 | 5 | 18 |
| 5 | 6 | 22 |

The installed deck supplies the upload time and damage bonus above. A tier-5 chip in a tier-5 Paraline deals **26.4 electrical damage**, costs **6 RAM**, and uploads in **0.6 seconds**.

Install its chip in the cyberdeck. Hold **V**, aim at a living target, and **left-click** the Short Circuit entry or crosshair to upload. The deck’s queue limit applies to all pending uploads, with RAM reserved immediately. Progress appears above the target’s head. Electrical damage bypasses armor; it respects invulnerability and server PvP/team rules. A locked upload can finish after losing sight, within the installed optics’ range plus 16 blocks, in the same dimension. Removing or switching the deck or loaded program cancels affected uploads and refunds only their reserved RAM, capped to the current deck’s capacity.

IDs: `cybernetics2077:short_circuit_tier_1` through `_5`.

![Tier-5 Short Circuit in the scanner](images/tier-five-scanner.png)

| | | |
| --- | --- | --- |
| *(empty)* | Redstone | *(empty)* |
| Copper ingot | Lapis lazuli | Copper ingot |
| *(empty)* | Iron nugget | *(empty)* |

## Mantis Blades

Install in **Arms** and press **G** with both hands empty to deploy. Press **G** again to retract the pair. A shallow dorsal hatch opens on the back of each forearm, while the palm, wrist and sides stay opaque. Native skin and enabled sleeve UVs remain on the arm and hatch. The mount faces upward; the blade points forward past the hand.

**Left-click** alternates two slashes and a rising cut, with a **0.5-second** interval. **Hold right-click for at least 0.4 seconds**, then release for a charged lunge, with a **1.1-second** interval. Damage is physical.

| Tier | Slash 1 / 2 | Rising cut | Lunge |
| --- | --- | --- | --- |
| 1 | 8 | 10 | 14 |
| 2 | 10 | 12.5 | 17.5 |
| 3 | 12 | 15 | 21 |
| 4 | 14 | 17.5 | 24.5 |
| 5 | 16 | 20 | 28 |

Deployment takes **1.5 seconds**: the original Minecraft hand moves into position, the covers start opening, the folded support and blade rise together, the support/housing slides backward, then the blade pivots forward and the nested tip extends. Retraction pulls the tip in first and returns the same hand to its normal pose. Damage lands on the animated contact frame and requires aim, range and line of sight. Aim at a visible living target within **8 blocks** to leap toward it, including from midair. With no target, leap **6 blocks in the direction you are looking**. Both use normal collision and stop at solid obstacles. When the guided leap ends, its remaining velocity passes into normal movement with ordinary gravity and friction. Aimed leaps deal damage at contact; free leaps can hit an enemy at their endpoint. Selected items stay stored and inactive while the claws are out: their attack attributes, enchantments, use actions and durability do not contribute to Mantis attacks. Both hands must be empty to deploy. Retracting the claws restores ordinary item use. Keys can be rebound under **Options → Controls → Cybernetics 2077**.

Tier 1 ID: `cybernetics2077:mantis_claws_tier_1`. Tiers 2–5: `cybernetics2077:mantis_blades_tier_2` through `_5`.

Light attacks alternate hands with a quick cross-body cut and a rising third hit. The free hand stays in guard. The sliding blade compacts during wind-up and extends for contact. Holding a charged attack reels both arms back; releasing drives them forward. The ready pose has subtle breathing and balance motion.

[Watch deployment and attacks](media/mantis-claws.mp4) · [Combat close-up](media/mantis-attacks.mp4) · [Idle movement](media/mantis-idle.mp4)

![Side-mounted Mantis Blades](images/mantis-first-person.png)

| | | |
| --- | --- | --- |
| Iron sword | *(empty)* | Iron sword |
| Piston | Diamond | Piston |
| Copper ingot | Redstone | Copper ingot |

## Cybernetics Bench

Right-click to install or remove matching implants. Shift-clicking an implant out requires room in your inventory. Capacity and body-system restrictions apply. The **Cyberdeck** button becomes available when a deck is installed and your cursor is empty.

ID: `cybernetics2077:cybernetics_bench` · Creative tab: **Functional Blocks**. Implants and quickhack chips appear in **Tools & Utilities**.

| | | |
| --- | --- | --- |
| Iron ingot | Glass | Iron ingot |
| Redstone | Crafting table | Redstone |
| Iron ingot | *(empty)* | Iron ingot |

The tier-1 crafting recipes unlock in the recipe book after obtaining redstone. Breaking a bench drops the workstation; installed cyberware stays with your character. Installed equipment is retained on death.

## Tier upgrades

Remove the item at the Cybernetics Bench, then use a **vanilla smithing table**. Put **one redstone dust in the left template slot**, the previous tier item in the middle, and the material below in the right slot. Each recipe makes one item of the next tier and consumes its inputs.

| Upgrade | Right slot material |
| --- | --- |
| Tier 1 → 2 | Gold block |
| Tier 2 → 3 | Diamond |
| Tier 3 → 4 | Netherite ingot |
| Tier 4 → 5 | Nether star |

These recipes apply to optics, Paraline, Mantis Blades and Short Circuit chips. Loaded deck programs, custom names and other item components carry over. New recipes unlock after obtaining the preceding tier. Install the upgraded implant back at the bench; merely carrying it gives no effects.

Existing worlds keep their installed tier-1 implants and loaded chips. Their original IDs and gameplay values are retained; their displayed names change to the Cyberpunk names. Tier stats are Minecraft adaptations. Older tier-1 screenshots show the same interfaces with the earlier display names.
