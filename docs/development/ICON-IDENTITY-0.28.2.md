# Icon identity — 0.28.2

The earlier sheets repeatedly used ivory beveled casings, cyan centres and small metallic details. These became hard to distinguish when reduced to inventory size. This asset pass replaces 84 implant/program sprites with individually planned functional silhouettes and quieter pixel clusters. The other 45 item textures, including clearer existing equipment motifs and utility items, are unchanged byte-for-byte.

[Source sheets, briefs and exporter](../../art/icon-identity/README.md) · [Audited export](../../art/icon-identity/export-report.json)

Each updated sprite uses a 32px texture, at most 26px of content, three transparent pixels at every edge, binary alpha and no more than nine opaque colours. The nine-colour limit removes noisy material speckles. Front/side views and large gaps separate models without relying on recoloured lights. Examples include the pendulum, neural branches, distinct memory-board arrangements, telescopic lens, binoculars, half mask, different tendon hardware and separate deck contours. Quickhack chips intentionally retain a common program-family format with different central symbols.

The original source sheets are image-generated. The change addresses the visual repetition and density of the previous designs; it does not claim human-only authorship. Packing is deterministic and preserves aspect ratio. Assets are inspired by the anatomical and electronic inventory-art direction of Cyberpunk; reference images are not redistributed.

## Native comparisons

Each panel shows the previous texture at 16 GUI pixels, the actual updated item model at 16, and a 32px detail view. These are Minecraft framebuffer captures, rather than enlarged source-sheet illustrations.

- [Cortex](../images/icon-identity-0.28.2/icons-cortex.png)
- [Operating Systems](../images/icon-identity-0.28.2/icons-systems.png)
- [Optics and control](../images/icon-identity-0.28.2/icons-senses.png)
- [Structural and hands](../images/icon-identity-0.28.2/icons-structure.png)
- [Nerves and circulation](../images/icon-identity-0.28.2/icons-neural.png)
- [Circulation and armor](../images/icon-identity-0.28.2/icons-vital.png)
- [Defense and mobility](../images/icon-identity-0.28.2/icons-defense-mobility.png)
- [All 116 current item/program families](../images/icon-identity-0.28.2/icons-current-catalogue.png)

## Scope and validation

The native fixture validates all 84 registry/model references, compares them to baseline textures, renders the complete family catalogue and exercises the real creative tab with 457 canonical items, search filtering and restoration. The release audit verifies the game textures against the checked export hashes; the exporter verifies bounds, alpha, colour counts, unique sprites and wiki parity. Normal JVM tests and the strict wiki build also run.

Only NeoForge 1.21.1 is updated. No production Java, gameplay, implant stats, models, shaders, recipes or registry IDs change. The image sizes and runtime rendering path remain the same. Source sheets and review code do not ship in the JAR. Forge remains unchanged.
