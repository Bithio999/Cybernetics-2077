# Cyberware inventory artwork

`atlas.png` is the authoritative image-generated source for 63 item icons and the mod badge. The illustrations adapt clean pixel art inspired by the colored cyberware inventory-art direction of Cyberpunk 2077: distinct anatomical/electronic silhouettes, ivory shells, red conduits and restrained material highlights. See `reference-sources.json` for visual references and attribution. Reference images themselves are not bundled.

Run `python3 art/inventory-icons/export.py` with ImageMagick 7 installed. The manifest fixes the cell-to-registry mapping, so later additions cannot silently shift the atlas. Export crops each cell, removes near-transparent source artifacts, fits its silhouette without stretching, centers it and adds clear padding. Items use 32×32 RGBA; the badge uses 128×128. Alpha is binary to avoid colored edge fringes and translucent noise in generated item models. No texture filtering, renderer, dynamic lighting or per-frame work is added.

The exporter updates existing wiki icon copies as well as Minecraft item textures. Run it after any older art generator that overwrites these sprites. Keep the source atlas unchanged during export; in-game textures are the normalized production files.

The current [icon identity pass](../icon-identity/README.md) overrides selected historical sprites. Run its exporter last when rebuilding artwork.
