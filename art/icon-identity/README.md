# Icon identity pass — 0.28.2

84 implant/program sprites have individual silhouette briefs in [manifest.json](manifest.json). Original image-generated source sheets are retained here; ImageMagick performs texture packing, palette limiting and padding only. This pass addresses repeated casings and high-frequency shading in the previous inventory illustrations.

The designs use large functional forms: a pendulum for Newton, an open neural relay for Axolotl, separate memory boards, a half-face plate, stepped telescopic optics, exposed tendons, distinct armor contours and different silhouettes for each cyberdeck. Related equipment shares materials and a family cue, rather than merely changing the light in the same box. The existing clearer Mantis, medical, skeletal, hand and program motifs remain where appropriate. Tier variants intentionally retain their family's icon; tier text/color already identifies strength.

Every changed sprite is 32×32 RGBA with a 26px content budget, at least three transparent border pixels, binary alpha and at most nine opaque colors. No filtering, dynamic rendering, shader, model, gameplay or equipment-stat change is involved. Source sheets are development assets and do not ship in the JAR.

## Export and verify

Run `python3 art/icon-identity/export.py` with Pillow and ImageMagick 7. This updates the game and wiki together and writes [export-report.json](export-report.json), including checked bounds, palette sizes and production hashes.

**Export this pass last.** Historical generators under `art/inventory-icons`, `art/expansion-028`, and the earlier family scripts overwrite their own original sprite sets. If those are rerun, follow them with the current identity exporter. Existing models/registry IDs continue to reference the same texture paths.

The native review uses `-PcompatClientReview -PcompatIconIdentityReview` with `compat/validation.init.gradle`. It compares each actual registered Tier 5 item against its original texture at 16 GUI pixels and displays a 32px detail view, then renders all 116 item/program families and exercises the native creative tab/search. Baseline textures can be prepared from commit `f185a8897fed6d455fdee0db826be01b7be4c9cf` into `/workspace/icon-identity-before`; the fixture and its comparison images do not ship in production.

Visual direction remains inspired by Cyberpunk 2077's anatomical/electronic inventory objects; see [reference sources](../inventory-icons/reference-sources.json). No game-extracted reference images ship here.
