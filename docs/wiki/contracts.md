# Fixer contracts

[Wiki home](index.md) · [The Afterlife](afterlife.md) · [Street Cred](street-cred.md)

Talk to **Mara Voss** or **Ren Calder** at the Afterlife. Their menu has **Bounties**, **Delivery** and **Debrief** tabs. Accepted jobs provide a readable contract shard and scanner directions, even before you install optics.

## Bounties

Available on **NeoForge 0.22.0 and later**. Forge 0.20.0 retains the introductory delivery only.

Each fixer has **three offers per player each Minecraft day**. The board includes a **Wanted** mark, a **Cyberpsycho Sighting** and a **Cyberware Recovery** job. Offers rotate at Overworld dawn. Reopening the menu does not reroll them. Street Cred unlocks stronger marks; some lower-difficulty work remains available.

| Required Street Cred | Difficulty | Target implant tier | Implants | Guards | Base payment | Base Street Cred |
| ---: | --- | ---: | ---: | ---: | ---: | ---: |
| 0 | Tier 1 | 1 | 2 | 0 | €$600 | 1 |
| 10 | Tier 2 | 2 | 3 | 0 | €$1,000 | 1 |
| 20 | Tier 3 | 3 | 4 | 1 | €$1,600 | 2 |
| 30 | Tier 4 | 4 | 5 | 1 | €$2,400 | 2 |
| 40 | Tier 5 | 5 | 6 | 2 | €$3,400 | 3 |
| 50 | Elite Tier 5 | 5 | 6 | 3 | €$4,800 | 3 |

The table shows standard Wanted jobs. New contracts use the reduced rewards; previously accepted dossiers keep their quoted payment.

The mark wears **real non-Iconic combat implants at the listed tier**, including armor and, at higher tiers, Blood Pump and Neofiber. Named targets have additional health, damage, armor, toughness and pursuit range. Guards are one tier lower, with up to three implants. The normal chromed-mob chassis and equipment effects apply. These are specific named targets, not a quota of random kills.

1. Choose an offer and **Accept Bounty**. You need one free inventory slot.
2. Hold the scan key to follow the lead. The encounter deploys on safe loaded ground as you approach, without changing blocks or force-loading distant chunks.
3. Complete the assigned objective below. For Wanted jobs, eliminate your assigned mark. Your kill credit—or your tamed pet's—must confirm the kill. Another player's kill does not pay your contract.
4. Return to the issuing fixer, or another fixer in the same network, and **Claim Payment**. The shard can also be used on the correct fixer when payment is ready.
5. Read your rating, payment breakdown and optional results in **Debrief**.

![Daily bounty dossiers](../images/contracts-025-board.png)

### Cyberpsycho Sighting

Fight the named cyberpsycho down to **10% maximum health**. Your hit—or your tamed pet's—automatically contains them, even if that hit would otherwise be fatal. A contained target stops fighting and is protected from normal follow-up damage. Return to the issuing fixer network to collect payment.

Cyberpsychos are **dedicated human mobs**, not chromed vanilla monsters. Their base cyberware capacity is **150**. Their installed implants exceed that limit; every additional **25 capacity** raises the contract tier, up to Tier 6. This is separate from the tiers of their individual implants.

| Contract tier | Overcapacity band | Installed capacity | Implant tier | Implants |
| ---: | --- | ---: | ---: | ---: |
| 1 | +1–25 | 162 / 150 | 3 | 14 |
| 2 | +26–50 | 187 / 150 | 3 | 15 |
| 3 | +51–75 | 215 / 150 | 4 | 15 |
| 4 | +76–100 | 250 / 150 | 4 | 16 |
| 5 | +101–125 | 270 / 150 | 5 | 16 |
| 6 | +126 or more | 278 / 150 | 5 | 17 |

Their combat implants work automatically: **Biodyne Berserk**, **Biomonitor/Blood Pump**, **Adrenaline Converter**, **Reinforced Tendons**, **Microgenerator** and **Shock-N-Awe**, plus melee, armor, toughness and mitigation bonuses. Self-ICE blocks an incoming quickhack before recharging. Higher contract tiers add Para Bellum, Epimorphic Skeleton, Pain Editor, Second Heart and Handle Wrap. Berserk's temporary 25% health floor must expire before containment is possible. No Iconic implants are equipped.

The contract menu, shard and scanner show their capacity. Their base payment remains **25% higher** than standard Wanted jobs. A target killed by another player or the environment before containment fails the job.

![Cyberpsycho sighting dossier](../images/psycho-027-offer.png)

![Cyberpsycho and capacity scan](../images/psycho-027-scanner.png)

### Cyberware Recovery

Defeat the assigned courier, then find the **sealed barrel cache** at their last location. Hold scan for the updated waypoint. With the contract shard in your hand, **use it within four blocks of the cache with clear sight** to secure the payload. Return to the fixer for payment.

Killing the courier alone does not complete this job. The payload is recorded on your contract, so it does not need a separate inventory slot. Other players cannot collect your cache. Reissue a lost shard at a fixer; the cache remains part of the same job. Abandoning the job removes its cache and remaining guards.

Recovery jobs offer **10% higher base payment** than the table and run one rank below your current maximum, with Tier 1 as the minimum. Ren's 90-second specialty timer includes retrieving the cache.

![Recovery cache beside the defeated courier](../images/contracts-025-cache.png)

### Fixer specialties and optional bonuses

| Optional objective | Reward |
| --- | --- |
| **Mara:** identify the mark through optical scanning before engaging its group | +20% base payment and +1 Street Cred |
| **Ren:** defeat the mark within 90 seconds of the first combat hit involving its group | +20% base payment and +1 Street Cred |
| Do not kill villagers, wandering traders or service NPCs during the hunt | +10% base payment |
| Do not die during the hunt | +10% base payment |

Optical identification requires an installed scanner; navigation alone does not identify a target. Missing an optional objective **never reduces the base payment**. Surviving, civilian protection and specialty results determine the rating: **S / A / B / C** for **3 / 2 / 1 / 0** successful optionals. Street Cred remains capped at 50.

Every **third completed bounty for the same fixer network** adds a **25% base-payment loyalty bonus**. Separately, your first **three Elite Tier 5 completions** award **one Tier 5 Iconic Requisition Chip**, once per player. If your inventory is full, the bonus chip drops at your feet. This grants purchase authorization, not an Iconic implant; eddies, stock, Street Cred and other purchase requirements still apply.



### Active jobs and recovery

You can hold **one bounty at a time**, alongside the introductory delivery. An active bounty takes priority in scanner navigation. Accepted jobs persist through dawn, logout and death. Death only loses the survival bonus.

Use **Reissue Shard** if the dossier is lost. To abandon a job, press **Abandon Job**, then confirm it. Accepted offers remain used for that day, including abandoned or failed jobs. Loaded abandoned targets are removed; unloaded ones are cleaned up when their chunk next loads.

Bounties start with an **Overworld fixer** and require non-Peaceful difficulty. If a target disappears without a confirmed kill, the job can redeploy when you approach safe ground again. If another player claims the kill, clear the failed dossier at a fixer before taking another job.

## First Impressions

**One completion per player · Reward: €$1,000 + 5 Street Cred**

1. Open the **Delivery** tab and accept the job. You need a free inventory slot.
2. Hover the shard for the clinic's coordinates. The diagnostic packet is digital; no separate cargo item is given.
3. Hold the scan key for directions. Contract navigation works before you own optics.
4. **Use the shard on a ripperdoc** to deliver it. Any ripperdoc accepts it if the original destination becomes unavailable.
5. The doctor recommends **[Basic Kiroshi Optics](kiroshi-optics.md)**; the reward covers the tier-1 implant's price.

If no clinic route is known, visit a clinic and return to the fixer. Choose **Reissue Contract** for a lost delivery shard. Completed deliveries cannot be repeated through another fixer or copied shard.

![Contract directions in the scanner](../images/contract-scanner-route.png)

[The Afterlife](afterlife.md) · [Street Cred and purchases](street-cred.md)
