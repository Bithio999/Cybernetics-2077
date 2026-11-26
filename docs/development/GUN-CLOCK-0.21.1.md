# Firearm clock and chromed loadouts (NeoForge 0.21.1)

## TaCZ

The previous final firing-interval and burst hooks divided native intervals by the owner's personal pace. That accelerated an active Sandevistan user's gun. They now use `Sandevistan.weaponPace`: the strongest applicable encounter field, with no personal acceleration. The same client and server hooks still operate on TaCZ's native final interval, preserving gunpack/attachment/heat calculations. No reload methods, reload timestamps or reload animations are scaled.

TaCZ bullets use this unaccelerated world clock through the existing native projectile tick budget and rendering interpolation. Ordinary arrows/tridents retain their previous owner-relative timing. The change preserves the existing field radius and player/team/PvP rules. Overlapping fields apply once; leaving a field resumes normal timing without catch-up ticks.

For a single field, real-time gun cadence and bullet simulation are 0.625 (Dynalar), 0.1875 (Apogee) or 0.0625 (David's). The owner still has 1.25 personal pace. Thus David's gun operates at 5% of the owner's pace; native reload duration remains unchanged.

## Chromed mobs

`MobCyberwarePool` derives equipment from the canonical ripperdoc catalogue, excluding Iconics and program chips. It groups tier variants by family and shuffles families once per roll, avoiding a selection bias toward families with more tiers. Available tiers respect each family's first tier. Subdermal Armor remains mandatory; the existing 1–6 count and tier/spawn rarity distributions remain in place. Single-socket systems cannot overflow and no family repeats.

Both loadout accounting and installation now accept all `CyberwareItem` types, so optics, arms and decks count toward chassis bonuses and salvage/Chip tier calculations. Existing saves keep their installed loadout; joining does not reroll or refill health. The existing mob AI and implant-specific mob effects are retained. This does not add automated player-only interfaces, deck queues or manual implant activations to mobs.

The pool is cached at first use after item registration. Generation is bounded by the catalogue family count, not a retry loop. It introduces no recurring registry scans or extra entity tick work. Validation rejects an Iconic before replacing the entity's inventory or marking it enhanced.

## Checks

- Existing 99 JVM tests and clean package audit.
- 138 standalone GameTests: two new scenarios cover 3,000 tier/count rolls, full non-Iconic catalogue coverage, socket/duplicate rules, rejection of both Iconics, mixed-type chassis accounting, and mixed loadout save/load.
- 10 optional TaCZ GameTests: native GunData firing intervals for all three models, unchanged empty/tactical reload data and accepted reload events; own native gun-bullet tick budgeting versus arrows and restoration outside the field; prior gun tests retained.
- Native integrated client review with TaCZ: baseline ADS/scanner/ammo checks, then a synchronized David activation and native client GunData query proving a x16 shot interval with unchanged reload timing.

The full standalone suite completes successfully but its review server was stopped during save shutdown after reporting all assertions passed. Release builds do not include the review fixtures. Forge 1.20.1 remains unmodified at 0.20.0.
