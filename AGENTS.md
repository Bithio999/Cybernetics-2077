# Cybernetics 2077 workflow

- Active development targets **Minecraft 1.21.1 / NeoForge**, in the root project and `src/main`.
- Update, build or release the **1.20.1 Forge** port only when the user explicitly requests a port update. Do not automatically regenerate/release it with NeoForge changes.
- The published Forge build remains at its last requested version. Its generator reads shared sources, so a future port requires an explicit compatibility review; sharing source does not mean the port has been updated or validated.
- Keep the two platform versions/download links independent in the README, wiki and release checks.
- For normal work, run the relevant NeoForge tests and `python3 scripts/verify-release.py`. Forge checks are optional and explicit.
- Preserve standalone operation without TaCZ. Optional gun integration must respect native ammunition, attachments, headshots, fire modes, allies/PvP and explosive behavior.
- Keep engineering/test history outside the player wiki; the wiki describes current behavior and controls.
