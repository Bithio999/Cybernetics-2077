# Developer notes

[Player wiki](../wiki/index.md) · [Repository](../../README.md)

## Build the mod

Work on **NeoForge 1.21.1 with Java 21**. Only update the Forge 1.20.1 port when explicitly requested:

```sh
./gradlew test build
python3 scripts/verify-release.py
```

For an explicit port update, use Java 17 and `./gradlew -p forge-1.20.1 test build`, then pass its JAR path to the release verifier. Otherwise leave its sources, version and artifact unchanged.

[Forge port and TaCZ validation](../../compat/README.md).

Run `./gradlew runClient` for development. [Native/server review instructions](../VALIDATION.md) include disposable test-save setup. Research lives in [research](../research/CYBERWARE-CATALOGUE.md); model work in [art](../../art/mantis/README.md); older captures in the [preview archive](../PREVIEWS.md).

[0.14.0 implant research and adaptation notes](../research/CYBERWARE-EXPANSION-0.14.0.md) · Original pixel sprites: `python3 art/expansion/build.py`.

[0.16.0 Hands and blade-variant research](../research/HANDS-MANTIS-0.16.0.md) · Original sprites/atlases: `python3 art/hands/build.py`.

[0.16.1 Mantis appearance and elemental rendering](../research/MANTIS-APPEARANCE-0.16.1.md).

[0.16.2 afterimage camera anchoring and pixel regressions](../research/SANDEVISTAN-CAMERA-0.16.2.md).

[0.17.0 local time scheduler, rain clocks and first-person trails](../research/SANDEVISTAN-BUBBLE-0.17.0.md).

[0.18.0 Iconic requisitions, Apogee research and model-specific timing](../research/ICONICS-APOGEE-0.18.0.md) · Original sprites: `python3 art/iconics/generate.py`.

[0.19.0 local animated textures and continuous piston movement](../research/LOCAL-ANIMATION-0.19.0.md).

[0.20.1 TaCZ HUD and basic-optics lock research](../research/TACZ-HUD-0.20.1.md).

## Edit the wiki

The 67 player pages live in `docs/wiki/`, with navigation in `mkdocs.yml`. Update an item's own page when changing its behaviour. Keep engineering history and test results in the developer documentation.

```sh
python3 -m venv .venv-docs
.venv-docs/bin/pip install -r docs/requirements.txt
.venv-docs/bin/python scripts/check-wiki.py
.venv-docs/bin/mkdocs serve --dev-addr 127.0.0.1:8765
```

Build with `mkdocs build --strict`. The GitHub Actions workflow validates the wiki and uploads its searchable website build. It publishes automatically when GitHub Pages is configured with **GitHub Actions** as its source. Otherwise, deployment is skipped and the repository wiki remains available.

The current integration received **403 Resource not accessible by integration** when attempting to enable Pages. A repository owner can enable it under **Settings → Pages → Build and deployment → Source: GitHub Actions**, then run the **Publish wiki** workflow. The future site URL is `https://bithio999.github.io/Cybernetics-2077/`. After it is live, the README's wiki link can point there.

Wiki pages use relative Markdown links so they work directly on GitHub as well as on the published site. Existing media stays in `docs/images` and `docs/media`; `docs/site_hooks.py` copies only referenced assets into the site during a build. Generated `site/` files are not committed.

## Inspect structures

With cheats enabled, stand at the intended street/floor height. These commands place structures and replace blocks:

```mcfunction
/place jigsaw cybernetics2077:village/clinic_small minecraft:building_entrance 1 ~ ~-1 ~
/place jigsaw cybernetics2077:village/clinic_large minecraft:building_entrance 1 ~ ~-1 ~
/place jigsaw cybernetics2077:village/afterlife minecraft:building_entrance 1 ~ ~-1 ~
```

Ripperdoc and Fixer spawn eggs are available in Creative. [Building design and audits](../research/VILLAGE-SERVICES.md).
