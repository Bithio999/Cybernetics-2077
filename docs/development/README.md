# Developer notes

[Player wiki](../wiki/index.md) · [Repository](../../README.md)

## Build the mod

Use Java 21:

```sh
./gradlew test build
python3 scripts/verify-release.py
```

Run `./gradlew runClient` for development. [Native/server review instructions](../VALIDATION.md) include disposable test-save setup. Research lives in [research](../research/CYBERWARE-CATALOGUE.md); model work in [art](../../art/mantis/README.md); older captures in the [preview archive](../PREVIEWS.md).

## Edit the wiki

The 40 player pages live in `docs/wiki/`, with navigation in `mkdocs.yml`. Update an item's own page when changing its behaviour. Keep engineering history and test results in the developer documentation.

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
