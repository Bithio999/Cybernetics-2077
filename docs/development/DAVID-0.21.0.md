# David’s Sandevistan (NeoForge 0.21.0)

Original Tier 5 Iconic OS, appended to the implant enum and catalogue to preserve existing ordinal/offer identifiers. Its model supplies 95% relative slow, 10-second duration, 40-second recharge, critical/headshot bonuses and hostile-kill recovery. Second Wind uses a persisted one-second heal cooldown. It reuses the existing bounded local-time scheduler, captured-color trails, sounds and rendering; it adds no separate ticking entities or shaders.

The server filters the catalogue by each offer’s Street Cred requirement before daily stock selection. Purchase validates stock, Street Cred 50, the Tier 5 chip, inventory Apogee, eddies and output space before consuming inputs. The equipped OS is never removed implicitly. The full-inventory case supports the Apogee freeing an output slot; failed insertion refunds both inputs and payment.

Saved battery profiles retain legacy `sandyApogee` and add `sandyDavid`; missing fields preserve old saves. Equipped model identity is synchronized separately from saved battery identity, so swapping implants cannot refill a battery or change its inactive recharge profile. The HUD measures key-binding width to avoid modified bindings overlapping the model label.

Original 32px sprite source: `python3 art/david/generate.py`.

## Validation

- 99 JVM tests, including the model’s 95% relative pace and battery/combat values.
- 136 standalone NeoForge GameTests. New scenarios exercise atomic purchases including full inventories; relative David/Apogee pace and model swaps; hostile-kill healing, its cooldown, battery cap and passive-animal exclusion.
- 9 optional TaCZ GameTests, including David’s native headshot multiplier without replacing TaCZ’s base headshot value.
- Native client review with TaCZ: actual shop search, ACQUIRE packet, both consumed materials/payment, shift-click installation, DAVID model/HUD, field pace, deactivation and recharge. Apply `compat/validation.init.gradle` with `-PcompatClientReview -PcompatDavidReview`; use a disposable review save as described in `compat/README.md`.
- Clean release audit excludes all review fixtures. Wiki coverage and strict site build pass.

Forge 1.20.1 remains at 0.20.0, without regenerated sources or a new artifact. The external modpack was not rerun for this NeoForge-only addition. Runtime fixture JARs never ship in the release.
