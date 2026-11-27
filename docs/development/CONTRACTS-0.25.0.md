# Contract expansion and named-target balance — NeoForge 0.25.0

## Current rules

Each daily board includes Wanted, Cyberpsycho Sighting and Cyberware Recovery. Wanted and Sighting use the player's highest unlocked rank; Recovery uses one rank lower, at least rank 1. Street Cred gates, daily reservation, one active bounty, the introductory delivery, fixer networks, rating and loyalty remain part of the existing system.

New Wanted jobs pay 600 / 1,000 / 1,600 / 2,400 / 3,400 / 4,800 eddies and 1 / 1 / 2 / 2 / 3 / 3 Street Cred. Sightings add 25% to base money; Recoveries add 10%. Accepted jobs retain their saved quote. Existing dossiers without a kind are Wanted jobs.

Named targets receive two to six tier-matched non-Iconic combat implants rather than random utility implants. Their health multiplier over the chromed chassis is 2 + 0.35 × rank; damage is 1.35 + 0.12 × rank; armor adds 2 × rank, toughness adds 0.5 × rank, speed adds 8% and follow range adds 32 blocks. Ranged damage receives the same named-target factor, once. Attribute IDs are stable: reload neither stacks the chassis nor refills health. Sightings add one implant, up to six, multiply named health by 1.5 and damage by 1.2, and use a 20% speed bonus.

## Containment

The owner's or their tamed pet's final damage is checked after normal mitigation. A hit crossing 10% max health cancels its damage, lowers health to that threshold and records containment; the live mob becomes invulnerable to normal attacks and stops its AI. This also catches an overkill hit. Bypass-invulnerability sources retain native behavior. A death before containment fails the job. Contained state and attributes persist through entity save/load; payment/abandonment cleans up the encounter.

## Recovery

An owner-credited courier kill enters a separate pickup phase rather than completing the job. A single persistent vanilla BlockDisplay presents a sealed barrel cache at loaded surface ground beneath the courier. No terrain is changed or distant chunk loaded. Using the current owner-bound dossier within four blocks and clear sight secures a virtual payload. The ledger, not item copies, owns the objective and payment. Cache display entities validate owner/token/UUID on join; abandoning and claiming remove them, including stale entities on later chunk loads. A missing loaded cache can be rebuilt for the existing job; it does not respawn the courier. The scanner points to the cache until recovery, then the fixer. Survival/civilian optionals remain active through pickup; Ren's timer includes pickup.

## Validation

The existing suite plus five new GameTests covers overkill containment, follow-up protection, save/load, foreign kill failure, recovery ownership/range/native item use, duplicate claims, saved cache cleanup, stronger target attributes and idempotent chassis application. The native fixer review now completes all three jobs through native accept/claim packets, native damage and an actual contract-item use request, alongside the introduction delivery and smaller GUI check.

One older Sandevistan fixture previously unforced neighboring chunks unconditionally; the expanded batch layout exposed that it could remove GameTest framework tickets and leave later cases waiting to start. Its cleanup now preserves the pre-test forced-chunk set, matching the other chunk-sensitive fixtures. This is test-harness cleanup only.

[Structured results](../review/results/contracts-0.25.0.json). Forge 1.20.1 remains 0.20.0 and is not regenerated.
