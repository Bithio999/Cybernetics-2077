# Iconic cyberware

[Cyberware](cyberware.md) · [Chromed mobs](enhanced-mobs.md) · [Ripperdocs](ripperdocs.md)

Iconics are special implants within the normal **Tier 1–5** system. They require a matching-tier **Iconic Requisition Chip** and cost more than ordinary cyberware. The first available Iconic is the **Tier 5 [Militech “Apogee” Sandevistan](militech-apogee.md)**.

## Requisition chips

<img src="assets/icons/iconic_requisition_tier_5.png" alt="Tier 5 Iconic Requisition Chip" width="48" height="48" class="item-icon">

Defeating a chromed mob has a separate chance to drop **one** chip in addition to its usual damaged part. The chance is **1% × average implant tier + 0.5% × additional installed implants**: from **1%** for a single Tier 1 implant to **7.5%** for six Tier 5 implants.

Chip tiers use the [same loadout-based tier distribution as salvage](enhanced-mobs.md#damaged-cybernetic-parts), rolled independently. Higher average tiers and more implants favor higher-tier chips. Normal mobs do not drop them; `doMobLoot=false` disables these drops, and Looting does not multiply them.

Keep chips in your main inventory or hotbar. Chips exist at tiers 1–5; **only Tier 5 has an Iconic to buy in this update**. Lower-tier chips can be kept for future additions. Chips cannot be combined into higher tiers.

## Purchasing

1. Find a ripperdoc stocking the Iconic you want. Owning a chip does not add it to stock.
2. Meet the displayed Street Cred requirement and bring the full Iconic price plus one matching-tier chip.
3. Choose **ACQUIRE**. The chip and eddies are consumed together, and the implant enters your inventory.
4. Use **Install** to equip it in its normal body-system socket.

Failed purchases consume nothing. A single chip can free its inventory slot for the implant. Damaged cybernetic parts cannot replace chips or discount Iconics. Buying one does not consume the doctor's daily offer, so another purchase needs another chip and payment.

| Iconic | Tier | Capacity | Street Cred | Eddies | Chip |
| --- | ---: | ---: | ---: | ---: | --- |
| [Militech “Apogee” Sandevistan](militech-apogee.md) | 5 | 44 | 40 | 75,000 | Tier 5 |

![Iconic offer in the ripperdoc menu](../images/apogee-ripperdoc-stock-0.18.0.png)
