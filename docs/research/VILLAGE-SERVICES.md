# Village services: reference and design notes

The buildings are Minecraft adaptations, rather than replicas of Night City's city blocks. All textures, skins and furniture models in this update are original. No game screenshots or extracted game assets are distributed.

## References actually reviewed

- [The Ripperdoc walkthrough, PowerPyx](https://www.powerpyx.com/cyberpunk-2077-the-ripperdoc-walkthrough/) — the in-game clinic screenshot shows Viktor's work counter, diagnostic displays, worn tiled work area, red workshop lighting and overhead services. The implementation uses a treatment chair, hydraulic base/footrest, diagnostic displays, cabinets, task lights and a clinical work zone behind reception.
- [The Heist walkthrough, PowerPyx](https://www.powerpyx.com/cyberpunk-2077-the-heist-walkthrough/) — the entrance and bar screenshots, and the documented route from the entrance/bar to a private meeting booth, inform the Afterlife layout. The green-lit bar canopy, bottle storage, exposed industrial structure, descending entrance and separate fixer alcoves are the main visual cues.
- [Cyberpunk 2077 Update 2.0, CD Projekt Red](https://www.cyberpunk.net/en/news/49060/update-2-0) — cyberware tier/capacity framework. Clinics use the mod's existing capacity and body-system checks; purchasing an implant does not automatically equip it.

The Fandom pages for Afterlife and Viktor's Clinic returned HTTP 402 in this environment; they were not treated as inspected references. Reference screenshots stayed outside the repository.

## Building decisions

- **Small clinic:** 11 × 13 footprint; reception, waiting seats, one two-part treatment chair, diagnostic station and supply cabinets. Recessed white clinical walls sit between dark structural pilasters; a canopy joins the entrance lintel.
- **Large clinic:** 13 × 13, two storeys; a ground-floor treatment bay and two upstairs treatment bays. Same stock and pricing as a small clinic: players do not have to find a rare clinic for tier-five equipment.
- **Afterlife:** 13 × 13; a raised street vestibule descends into the bar, which sits two blocks below the entrance. Long bar, stools, ceiling-mounted trusses, bottle shelves and two private fixer booths. Its industrial shell deliberately contrasts with village houses.
- A continuous foundation is included in each template. During jigsaw generation, a bounded support pass fills air, fluids and non-sturdy terrain beneath the floor down to solid ground, up to 32 blocks. It never runs as a world tick.
- House-pool weights are small clinic **12**, large clinic **2**, Afterlife **3**, added to vanilla houses in plains, desert, savanna, snowy and taiga villages. These are weights at eligible house sockets, **not** per-village percentages or a guarantee. Footprint/collision checks still apply. Zombie-village pools are left alone.

## Block context and performance

`art/city/generate.py` authors templates and runs per-position audits: complete floors, connected occupied blocks, sealed walls/roofs, physical furniture supports, paired door halves and two-block-high walking routes to service areas. JSON audit summaries and fixture positions are checked in beside the generator.

In-game tests additionally resolve every palette entry, place every template in all four rotations and check every resulting block's survival condition. Native client screenshots cover interiors, exteriors and NPC menus. The checks complement visual inspection; they do not replace it.

Furniture uses static baked JSON geometry. There are no furniture block entities, tickers, constantly emitted particles or per-frame furniture renderers. Full-cube faces cull against neighbours. Most models have one to seven elements; the treatment chair has twelve and the vent grille nine. The service NPCs use the normal humanoid renderer and look-at goals, with no wandering/pathfinding loop. Each clinic has one persistent doctor; Afterlife has two persistent fixers.

Vanilla road templates reserve internal house lots inside 16 × 16 bounds. An early generation test rejected oversized footprints, so the larger clinic expands vertically. The Afterlife pool element reserves its full horizontal footprint above street level and carves its shallow, two-block basement during placement; its connectors stay at street level. This keeps the descending entrance compatible with village lot bounds. Three equivalent frontage connections give the generator room to align a usable apron with different road sockets.

Credit-chip supply barrels use ordinary, non-ticking vanilla barrel block entities. All 19 new decoration blocks have no block entities.
