# Ballistic Coprocessor

[Wiki home](index.md) · [Cyberware](cyberware.md) · [Quickhacks](quickhacks.md) · [Village services](ripperdocs.md)

<img src="assets/icons/ballistic_coprocessor.png" alt="Ballistic Coprocessor icon" width="48" height="48" class="item-icon">

| System | Tiers | Capacity |
| --- | --- | ---: |
| Hands | 1–5 | 2 |

Arrows can **ricochet once** from a solid surface. The rebound follows the reflected angle. Draw a bow or aim a TaCZ gun to see the trajectory: **gold before** the bounce and **cyan after** it.

The preview covers up to **160 blocks** and stops at a second surface or an entity. It estimates the centre of the shot; enemy movement, spread and Smart Link guidance can change the actual path. Tridents do not ricochet. Enchantments, ammunition and arrow pickup remain intact.

| Tier | Speed retained | Arrow base-damage bonus after bounce |
| --- | --- | --- |
| 1 | 65% | +5% |
| 2 | 70% | +10% |
| 3 | 75% | +15% |
| 4 | 80% | +20% |
| 5 | 85% | +25% |

**Damage type:** Physical/projectile, using the arrow's normal damage rules. The bonus increases the arrow's base-damage coefficient; slower rebound speed also affects the final hit.

![TaCZ aiming preview after a reflected wall hit](../images/neo023-gun-ricochet.png)

With TaCZ, non-explosive bullets ricochet once with the same retained-speed and damage bonuses. The aiming preview uses the gun’s speed, global speed setting, gravity, friction and attachment properties. A short light streak shows the reflected flight, including rounds that immediately hit a target. Explosive rounds retain their native impact behavior. [Gun integration](compatibility.md).

![Desert Eagle round after bouncing into a mob](../images/ricochet-0231-desert-eagle.png)

[All cyberware](cyberware.md) · [Installing implants](installation.md) · [Street Cred requirements](street-cred.md)
