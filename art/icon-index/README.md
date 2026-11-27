# Named and numbered icon reference

Run `python3 art/icon-index/build.py` with Matplotlib and ReportLab installed. This produces five labelled PNG reference figures, a five-page PDF, a self-contained searchable HTML gallery and CSV. The input artwork is the exact existing 32px item textures; no sprites are redrawn or changed. Nearest-neighbour scaling preserves the pixel shapes in the figures.

`numbers.json` is the stable texture-to-number mapping. Keep existing numbers when regenerating the reference; new entries receive new numbers. Names come from the current English localization, categories from the cyberware definitions. Tier variants that share an icon appear once; separate utility-tier textures appear separately. Every PNG in the mod's item-texture directory must be represented exactly once.

This is a document/visualization generator, not a game build dependency. Its outputs do not change the mod or require a new JAR.
