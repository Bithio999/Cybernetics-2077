# TaCZ HUD follow-up — NeoForge 0.20.1

Target: Minecraft 1.21.1, NeoForge 21.1.248, TaCZ 1.2.0-hotfix. The Forge port remains 0.20.0. Its source overrides and released JAR are not rebuilt or changed.

## Native behavior checked

References: [TaCZ source/API](https://github.com/MCModderAnchor/TACZ), the 1.21.1 branch and the tested runtime JAR.

- `LocalPlayerAim` and `AimKey` keep ADS in TaCZ's own player state. Holding a gun does not put vanilla `Player.isUsingItem()` into bow-use state. `HandsClient` now also accepts TaCZ's synchronized aiming state when showing Smart Link.
- TaCZ's `ClientSetupEvent` registers `tac_gun_hud_overlay`, `tac_heat_bar`, `tac_kill_amount_overlay` and `tac_interact_key_overlay`. The existing GUI-layer wrapper maps only these named layers into the scanner's existing fade groups. Unrelated mods' layers are not grouped.
- `RenderCrosshairEvent.onRenderCrosshair` draws TaCZ's replacement crosshair and hit marker inside a cancellable **Pre event**, before the ordinary layer renderer. A pseudo mixin wraps that draw in the same fade scope. When fully hidden, it preserves TaCZ's cancellation of the vanilla crosshair. This avoids an abrupt replacement or restoring a second crosshair.
- The ordinary third-person recoil branch already changes the same player rotation as first person, so its existing recoil modifiers do not need another application.
- Basic Kiroshi Optics are an `OpticsItem`, without an advanced `Augment` highlight profile. Previously, the null-profile branch cleared Smart Link every equipment tick. It now clears the target only when scanning optics are actually absent; advanced highlight cleanup remains intact.

## Review

Nine native TaCZ GameTests include a new acquire → equipment ticks → retain → remove optics → clear lock regression. The existing 133 standalone NeoForge GameTests also pass.

The integrated client uses the real AK47 builder and native aiming key. It checks a positive Smart Link target, captures the scanner's opening/open/restored states, attempts a real native trigger pull during scanning, checks `FORGE_EVENT_CANCEL`, and verifies the magazine is unchanged. A lowest-priority crosshair-event observer confirms that hidden TaCZ crosshairs still cancel the vanilla layer. These fixtures and their reflection helpers are excluded from release JARs.

The release verifier defaults to the current NeoForge build and reads each platform's version independently. Forge verification requires an explicit JAR argument; building or publishing the port remains an explicit workflow step.
