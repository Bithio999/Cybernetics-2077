# Bounty contracts (NeoForge 0.22.0)

`BountyRules` generates three immutable offers per fixer/player/day from stable identifiers. Street Cred 0/10/20/30/40/50 unlocks ranks 1–6; rank 6 uses six Tier 5 implants rather than inventing a Tier 6. Mara offers ranged marks and identification bonuses; Ren offers close-combat marks and timed takedowns. Accepted dossiers are independent of subsequent day/reputation changes. An accepted, failed or abandoned offer stays consumed for its day.

`BountyLedger` persists one authoritative job per player in Overworld SavedData, including offline owners. `StreetAccount` retains daily used offers, network loyalty, elite completions, the one-time chip flag and latest debrief through its existing copy-on-death attachment. Old saves default to empty bounty data. Snapshot getters copy NBT so client displays and item tooltips cannot mutate the server ledger.

## Encounters and credit

Accepted leads deploy only when the player approaches within 96 blocks. Bounded searches inspect already-loaded heightmaps, solid ground, fluid state, native spawn-floor safety, collision, vertical separation and world-border bounds. Marks stay at least 16 blocks from the owner; positions are separated. Deployment edits no blocks and creates no chunk tickets. Mobs use their existing vanilla AI and real installed non-Iconic equipment, with exact-tier loadouts. Helmets prevent the objective dying to daylight. No player-only implant controls are newly automated.

Persistent owner/token/role markers identify the specific target and guards. Unrelated kills never advance the job. Owner or tamed-pet kill credit confirms the mark; another player's kill records failure. The first real combat hit involving the owner's group starts Ren's clock, including pet attacks. Player/pet civilian kills and owner death lose their respective optional bonuses without failing the main objective. Scanner credit requires installed optics, the server-validated scan request and identification of the actual mark before engagement.

Target positions update at most once per second. Missing targets get a grace period and can redeploy on safe ground. Abandonment or payment removes loaded encounter survivors. Unloaded stale entities are rejected when they rejoin; no chunks are loaded to clean them up. Bounties start in the Overworld and do not start in Peaceful.

## Payment and menu

Claiming requires a nearby living fixer of the issuing network. The server consumes the job before paying. Duplicate claims, stale/copied shards, another player's menu and obsolete daily accept buttons cannot pay or silently select a replacement offer. Native button nonces change with the displayed offer set. Full-inventory acceptance is rejected before consuming an offer; shard reissue preserves the mission.

Specialty/civilian/survival optionals add 20/10/10% of base payment. Missing them never reduces the base. Three/two/one/zero completed optionals produce S/A/B/C. Every third completed job in one network adds 25% base payment; the first three rank-6 completions grant one Tier 5 requisition chip once. Full-inventory chip rewards drop at the player's feet. Normal Street Cred/wallet caps remain in force.

The menu uses stable widgets and separate Bounties, Delivery and Debrief tabs. A private, menu-ID-bound client payload refreshes dossiers at most once per second while the menu is open. No empty inventory grid or native title labels overlap the panels. The board adapts down to the 320×240 GUI used by a 640×480 window. The original delivery remains on its own tab; bounty navigation takes priority while both jobs exist.

## Verification

- 103 JVM tests, including deterministic daily offers, rank gates, profile separation, rewards and ratings.
- 147 standalone GameTests: nine new bounty scenarios cover atomic acceptance/reissue, specific-target credit, player/pet kills, reward uniqueness, optional failures, loyalty/elite rewards, persistence, daily/stale buttons, ownership, exact-tier group deployment, hazardous-floor rejection, block preservation, unloaded-chunk avoidance and stale-entity cleanup.
- 10 optional TaCZ GameTests retained.
- Native integrated client with TaCZ: real menu accept/claim packets, actual objective deployment, pre-combat optical scan, real player kill, S-rated €$8,400 / +5 SC payout, preserved delivery acceptance, and visible-widget bounds at 640×480. Curated screenshots are native captures, with tutorials/toasts disabled in the disposable review fixture.
- Clean release resource/class audit and strict wiki build.

Apply `compat/validation.init.gradle` with `-PcompatClientReview -PcompatBountyReview` to use the native review fixture. It modifies only a disposable save as documented in `compat/README.md`; no review fixtures ship in the release. Forge sources and its 0.20.0 artifact remain unchanged.
