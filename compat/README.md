# Port and gun compatibility

## Bounty review (NeoForge 0.22.0)

Add `-PcompatBountyReview` with `-PcompatClientReview` to exercise daily fixer offers, the native bounty accept/claim flow, real target deployment and scanning, the payment debrief, original delivery and a 640×480 GUI. It changes only the disposable review save and closes the client on completion. [Validation scope](../docs/development/BOUNTIES-0.22.0.md).


## NeoForge 0.21.1

Gun cadence and burst spacing now follow the slowed world clock rather than inheriting the owner's Sandevistan acceleration. TaCZ bullets do the same; native reload duration remains normal. Chromed mobs draw from current non-Iconic implant families; this does not enable player-only controls on mob AI. [Validation and implementation](../docs/development/GUN-CLOCK-0.21.1.md).


Active development targets **1.21.1 NeoForge (0.20.1)**. The **1.20.1 Forge release remains 0.20.0** and is updated only when explicitly requested. The root-source generator is not an automatic promise of port compatibility.

The previous 0.20.0 port validation below covers two independent platforms:

| Build | Runtime | Tested optional gun mod |
| --- | --- | --- |
| Root project | Minecraft 1.21.1, NeoForge 21.1.248, Java 21 | TaCZ 1.2.0-hotfix |
| `forge-1.20.1` | Minecraft 1.20.1, Forge 47.4.29, Java 17 | TaCZ 1.1.8-hotfix2 |

[Player-facing effects and controls](../docs/wiki/compatibility.md) · [Forge build notes](../forge-1.20.1/README.md)

## Integration design

The optional bridge registers TaCZ's published events only when TaCZ is installed. Cached reflection avoids distributing or requiring TaCZ classes in the standalone build. Pseudo mixins hook the native final firing interval, burst spacing and camera recoil; absent targets are skipped without TaCZ. Client/server interval hooks preserve attachment, heat and native network timestamp handling.

Smart guidance and one-shot ricochets operate on real `EntityKineticBullet` projectiles. Explosive rounds do not bounce. Full-precision trajectory packets retain TaCZ's projectile velocities beyond the vanilla motion-packet clamp. The receiver validates entity ID, UUID and bullet type. Guidance checks ownership, optics/lock, allies/PvP, distance and solid terrain. The gun damage bridge applies weapon critical bonuses once and preserves TaCZ's headshot factor. Electric discharge requires confirmed HP/absorption loss.

Default key modifiers avoid the gun controls without resetting existing user settings. Complex machine/physics namespaces are exempt from block-ticker skipping in a Sandevistan bubble; configure `timeBubbleExcludedBlockNamespaces` for additional machinery.

## NeoForge 0.20.1 follow-up

Basic optics now preserve locks through equipment ticks. A ninth TaCZ GameTest covers lock persistence and cleanup after removing the optics. The native AK47 review verifies a positive ADS lock, smooth scanner/HUD handover, preservation of TaCZ's crosshair cancellation and a blocked trigger pull without ammunition loss. The Forge port and its release stay unchanged.

## Pack manifest and scope

[forge-1.20.1-pack.json](forge-1.20.1-pack.json) records the 159 supplied public CurseForge filenames/project/file IDs. Personal paths and launcher account details from the original export were removed. Mod JARs, downloaded third-party code and pack saves are not redistributed.

All 159 pinned files were obtained and their loader metadata checked. A production Forge server was tested first without Cybernetics 2077, then with the reobfuscated release JAR and the same generated world/configs. Both reached `Done`. A production client containing all 159 listed files also completed a real multiplayer login and opened the cyberware menu. This exposed a JEI overlap, corrected by reporting the menu’s full-screen bounds; the native review checks that JEI’s item overlay stays hidden there. **157 pack files** were loaded in this server check: Essential and Respawn Recap were excluded because they are client-side components. They can remain in a player's client instance.

The baseline already reports third-party recipe/advancement, vehicle-model and physics warnings. The client also reports an invalid resource-pack metadata warning for Infiniters; its warning screen allows continuing to the menu. These also occur without Cybernetics 2077. TaCZ: Creatified and Cybernetic System were included in the server check. Cybernetic System keeps its own implant/progression system.

The original modpack configs, scripts, resource packs and extra TaCZ gunpacks were not supplied. Testing used the listed JARs and generated defaults. A successful startup is not a full playthrough of every machine, vehicle and custom dimension.

## Validation

- Shared JVM suite: 98 tests on each build, including projectile math, blade geometry/poses, local clocks, stock and salvage.
- Existing NeoForge gameplay regression suite: all 133 GameTests pass without TaCZ.
- Eight native TaCZ GameTests pass on each platform: headshot/critical handling, real-bullet guidance and wall rejection, single non-explosive ricochet, blade/fire exclusion, equipment/program persistence, owner-relative gun timing, high-velocity packet round trip and confirmed-hit discharge.
- Native integrated client checks exercise login, the server-authorized cyberware screen, hologram shaders, Mantis rendering, scanning and Sandevistan with TaCZ installed.
- Release checks enforce platform metadata, Java class version, resource parity, valid JSON, Forge shader inputs/blending, refmap/JarJar presence and exclusion of review fixtures.

## Reproduce

Normal development builds and verifies **NeoForge only**:

```sh
./gradlew test build
python3 scripts/verify-release.py
```

Only for an explicit port update, build with `./gradlew -p forge-1.20.1 test build` and pass the Forge JAR path to `scripts/verify-release.py`. The default verifier checks the current NeoForge build.

Download the appropriate TaCZ version separately. The init script adds a locally held gun JAR and optional test fixtures; it does not download or bundle it:

```sh
# NeoForge, Java 21
./gradlew -I compat/validation.init.gradle -PcompatGameTests \
  -PtaczJar=/absolute/path/tacz-neoforge-1.21.1-1.2.0-hotfix.jar runGameTestServer

# Forge, Java 17; -I is relative to the selected project directory
./gradlew -p forge-1.20.1 -I ../compat/validation.init.gradle -PcompatGameTests \
  -PtaczJar=/absolute/path/tacz-1.20.1-1.1.8-hotfix2.jar runGameTestServer
```

For TaCZ Tweaks on NeoForge, add `-PtaczTweaksJar=/absolute/path/to/tacz-tweaks.jar -PkotlinJar=/absolute/path/to/kotlinforforge.jar -PyaclJar=/absolute/path/to/yacl.jar`. The 0.24.1 regression uses Tweaks **3.0.0-alpha.12+1.21.1-neoforge**, Kotlin for Forge **5.13.0** and YACL **3.8.2+1.21.1-neoforge**, with TaCZ **1.2.0-hotfix**. These libraries remain optional validation dependencies and are never bundled.

Add `-PjeiJar=/absolute/path/to/jei.jar` to include JEI in the native client overlay check.

Add `-PcompatGunReview` with the NeoForge client review to exercise a real AK47, native ADS, Smart Link indication, scanner/HUD transitions and a blocked trigger pull without ammunition loss.

For a native client smoke test, use a **disposable** save named `Cybernetics Forge Review` or `Cybernetics Neo Review` under the selected client's `saves` directory. Apply the same init script with `-PcompatClientReview`, `-PcompatRunDir=/absolute/path/to/test-instance`, `-PcompatOutput=/absolute/path/to/captures` and `runClient`. The fixture changes equipment and terrain in that save and closes the client on completion. It never ships in release builds. Do not run separate builds against the same project's generated files simultaneously.

After using fixtures, run `clean test build` without the init script before packaging.

## API references

- [TaCZ source and API](https://github.com/MCModderAnchor/TACZ), Forge 1.1.8-hotfix tag and NeoForge 1.21.1 branch; exact downloaded hotfix JARs used at runtime.
- [Forge capabilities](https://docs.minecraftforge.net/en/1.20.1/datastorage/capabilities/)
- [Forge SimpleImpl networking](https://docs.minecraftforge.net/en/1.20.1/networking/simpleimpl/)
- [Legacy Forge build plugin](https://github.com/neoforged/ModDevGradle)

No TaCZ code or assets are copied into Cybernetics 2077.
