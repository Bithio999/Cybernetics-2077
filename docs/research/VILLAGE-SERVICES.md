# Village services: reference and design notes

The buildings are Minecraft adaptations, rather than replicas of Night City's city blocks. All textures, skins and furniture models in this update are original. No game screenshots or extracted game assets are distributed.

## Primary Afterlife references

The 0.9.1 redesign was guided by the location artist's own portfolio and the original concept work, rather than relying only on walkthrough screenshots:

- [Imanol Delgado Salazar — The Afterlife, Cyberpunk 2077](https://www.artstation.com/artwork/WKzrBG). The artist identifies himself as the location owner responsible for bar modelling, environment construction, set dressing and technical passes. Reviewed the full 29-image environment set: the chamfered/coffin-like island bar, illuminated opal canopy, layered metal counters, large service pipes and clamps, industrial entrance portal and side display niches. The entrance view `afterlife-2` is especially useful for the slanted lime sign and pipe framing.
- [Michał Lisowski — Afterlife, part 1](https://www.artstation.com/artwork/lxlOEe). The two original bar/VIP concepts inform the continuous counter underlighting and angular lounge shapes.
- [Michał Lisowski — Afterlife, part 2](https://www.artstation.com/artwork/18WRLK). The crypt/VIP concepts inform the industrial enclosure and enclosed meeting areas.
- [Bogna Gawrońska — Afterlife soundproof capsule](https://www.artstation.com/artwork/nY81N1). The production design and variations show the intended private briefing capsule. The Minecraft version keeps compact, enclosed fixer alcoves with padded seating.

These images were downloaded for local visual inspection and **are not distributed** in the repository or mod. All materials, meshes, wordmark art and skins shipped here are original Minecraft assets. The 13 × 13 village-lot constraint means this is a village-scale interpretation of those architectural features, not a full-scale copy of Night City's location.

Additional references retained from the first implementation:

- [The Ripperdoc walkthrough, PowerPyx](https://www.powerpyx.com/cyberpunk-2077-the-ripperdoc-walkthrough/) — Viktor's counter, diagnostic displays, tiled workshop and overhead medical equipment.
- [The Heist walkthrough, PowerPyx](https://www.powerpyx.com/cyberpunk-2077-the-heist-walkthrough/) — entrance/bar/private-meeting route.
- [Cyberpunk 2077 Update 2.0, CD Projekt Red](https://www.cyberpunk.net/en/news/49060/update-2-0) — cyberware tier/capacity framework.

## Building decisions

- **Small clinic:** 11 × 13 footprint; reception, waiting seats, one two-part treatment chair, diagnostic station and supply cabinets. Recessed white clinical walls sit between dark structural pilasters; a canopy joins the entrance lintel.
- **Large clinic:** 13 × 13, two storeys; a ground-floor treatment bay and two upstairs treatment bays. Same stock and pricing as a small clinic: players do not have to find a rare clinic for tier-five equipment.
- **Afterlife:** 13 × 13; a deeply framed street vestibule descends into the bar, which sits two blocks below the entrance. Chamfered island bar, aligned counter-light channels, an opal canopy, ceiling-mounted trusses, bottle niches and two padded fixer booths. Its industrial shell deliberately contrasts with village houses.
- A continuous foundation is included in each template. During jigsaw generation, a bounded support pass fills air, fluids and non-sturdy terrain beneath the floor down to solid ground, up to 32 blocks. It never runs as a world tick.
- House-pool weights are small clinic **12**, large clinic **2**, Afterlife **3**, added to vanilla houses in plains, desert, savanna, snowy and taiga villages. These are weights at eligible house sockets, **not** per-village percentages or a guarantee. Footprint/collision checks still apply. Zombie-village pools are left alone.

## Block context and performance

`art/city/generate.py` authors templates and runs per-position audits: complete floors, connected occupied blocks, sealed walls/roofs, physical furniture supports, paired door halves and two-block-high walking routes to service areas. JSON audit summaries and fixture positions are checked in beside the generator.

In-game tests additionally resolve every palette entry, place every template in all four rotations and check every resulting block's survival condition. Native client screenshots cover interiors, exteriors and NPC menus. The checks complement visual inspection; they do not replace it.

Furniture uses static baked JSON geometry. There are no furniture block entities, tickers, constantly emitted particles or per-frame furniture renderers. Full-cube faces cull against neighbours. Most models have fewer than ten elements. The treatment chair, shaped bar corners and duct modules use a few more fixed elements; none requires a per-frame custom block renderer. The service NPCs use the normal humanoid renderer and look-at goals, with no wandering/pathfinding loop. Each clinic has one persistent doctor; Afterlife has two persistent fixers.

Vanilla road templates reserve internal house lots inside 16 × 16 bounds. An early generation test rejected oversized footprints, so the larger clinic expands vertically. The Afterlife pool element reserves its full horizontal footprint above street level and carves its shallow, two-block basement during placement; its connectors stay at street level. This keeps the descending entrance compatible with village lot bounds. Three equivalent frontage connections give the generator room to align a usable apron with different road sockets.

Credit-chip supply barrels use ordinary, non-ticking vanilla barrel block entities. All 25 decoration blocks have no block entities.

## 0.9.1 checks for the reported problems

- **Exterior depth:** inset steel bays, continuous clamped duct runs, deep portal cheeks, a stepped parapet and supported mechanical roof housings replace the flat glass-fronted shell.
- **Clinic lights:** isolated wall light cubes are removed. Four-lens surgical task lights hang from continuous rods over the treatment seat/footrest, at a checked working distance. General room lighting comes from ceiling-mounted neutral diffusers.
- **Light alignment:** counter bands are geometry at a shared height, including the corner pieces. Canopy panels share matching edge geometry. NeoForge baked face-light data makes the signs, light channels and lenses emissive without a block renderer or ticker. Strip blocks use separate side and longitudinal top/bottom textures, avoiding a perpendicular stripe on each cube.
- **Walls:** render-geometry checks verify a closing plane in every exterior wall/roof cell. This catches the old case where a thin ceiling truss occupied a wall cell but did not actually seal it. A deliberate regression injection confirms the check rejects that case.
- **Items:** every furniture item has fitted GUI, both first-person hands, both third-person hands, ground and fixed/frame transforms. Native checks measure the baked vertices after those transforms; native screenshots show the icons and held furniture.
- **Residents:** four original skins use distinct faces, ages, hair and clothing, with raised outer layers. Vale and Ren use classic arms; Iris and Mara use slim arms. The same body/skin choice is used in-world and in native inventory-renderer previews.
- **Currency:** the same saved credit-chip item now deposits 100 eddies instead of 1,000; implant prices are unchanged. Full wallets reject the deposit without consuming a chip.
