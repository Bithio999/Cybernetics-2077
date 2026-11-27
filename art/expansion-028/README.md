# Expansion pixel art

Original transparent implant/program atlas, exported into centered 32×32 sprites with a 24px content budget, binary alpha and at most 20 colors. The game and wiki share the same exported sprites. `manifest.json` keeps the cell order deterministic; `python3 art/expansion-028/export.py` re-exports with ImageMagick.

`skins/` contains the original texture source and deterministic UV packer for the standard 64×64 Minecraft cyberpsycho skin. Role-specific geometry is baked in `CyberpsychoModel`; no large per-entity texture or dynamic geometry generator is used. These are original mod assets, not extracted Cyberpunk textures.
