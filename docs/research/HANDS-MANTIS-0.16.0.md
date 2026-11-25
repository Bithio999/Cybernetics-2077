# Hands cyberware and Mantis variants — 0.16.0

## References consulted

The accessible current-2.x Gamestegy catalogue lists Hands implants and their obtainable tiers. These pages supplied names, starting tiers, capacities and the original effect themes:

| Reference | Starting tier | Capacity | Original theme |
| --- | ---: | ---: | --- |
| [Hands catalogue](https://gamestegy.com/cyberpunk-2077/wiki/hands) | — | — | Slot classification |
| [Smart Link](https://gamestegy.com/cyberpunk-2077/wiki/1063/smart-link) | 1 | 4 | Smart-weapon targeting |
| [Ballistic Coprocessor](https://gamestegy.com/cyberpunk-2077/wiki/1056/ballistic-coprocessor) | 1 | 2 | Power-weapon ricochets and trajectory information |
| [Microgenerator](https://gamestegy.com/cyberpunk-2077/wiki/964/microgenerator) | 1 | 9 | Reload-primed electroshock |
| [Handle Wrap](https://gamestegy.com/cyberpunk-2077/wiki/3055/handle-wrap) | 3 | 8 | Temporary critical chance after equipping/throwing a throwable |
| [Shock Absorber](https://gamestegy.com/cyberpunk-2077/wiki/3062/shock-absorber) | 1 | 12 | Recoil reduction |
| [Immovable Force](https://gamestegy.com/cyberpunk-2077/wiki/3076/immovable-force) | 2 | 35 | Iconic recoil/spread handling |

The four [Physical](https://gamestegy.com/cyberpunk-2077/wiki/994/mantis-blades), [Thermal](https://gamestegy.com/cyberpunk-2077/wiki/14483/thermal-mantis-blades), [Electrifying](https://gamestegy.com/cyberpunk-2077/wiki/14492/electrifying-mantis-blades) and [Toxic](https://gamestegy.com/cyberpunk-2077/wiki/14487/toxic-mantis-blades) pages establish tier-2 starts, damage identity and the 20% Bleed/Burn/Shock/Poison themes. Minecraft damage values and status durations below are this mod's adaptations. The existing user-approved framing and articulation remain the reference for movement; this update does not replace the accepted animation with new inferred choreography.

Clutch Padding was excluded after checking its Circulatory classification. No third-party game textures, models, icons or video frames are redistributed. Reference HTML stays in ignored `references-local/`.

## Adaptation decisions

- Smart Link gently steers existing arrows/tridents toward a server-validated scan target. It requires optics and line of sight, has bounded turn rates and expires after two seconds of flight. It cannot shoot through blocks or U-turn toward enemies behind a projectile. Landing stops guidance, protecting Loyalty return behavior. Lock range is further constrained by the installed optics.
- Ballistic Coprocessor reflects arrows off one solid surface. Bow-only prediction uses at most 40 local segments and one bounce. It models blocks and tier gravity, not moving entities, random shot spread or Smart Link steering. Rebounds keep 65–85% speed and add 5–25% to the arrow's base-damage coefficient. Final arrow damage still depends on speed. Tridents are excluded to preserve sticking/return behavior.
- Microgenerator uses successful weapon hits instead of firearm reloads. One saved 7–11-second cooldown gates a radius-3 burst, at most four visible enemies, for 4–12 electrical damage. Main-target kills can trigger it; uninvolved passive creatures, allies and protected players are excluded. Secondary hits cannot recursively trigger another burst.
- Handle Wrap broadens drawing/throwing weapons to swords, axes, tridents and Mantis deployment. Its six-second +12–20% critical window has a saved ten-second rearm and shares the existing 50% cyberware critical cap. Slot switching cannot endlessly refresh it.
- Shock Absorber translates recoil control to knockback reduction during ranged charging, not general damage resistance.
- Immovable Force translates stable gun handling to grounded stationary charging and flatter arrow flight. No-gravity arrows are not pushed upward. Stationary charging is mirrored from synchronized state on the owning client and enforced on the server.
- All new implants are ordinary ripperdoc offers using existing daily rotation, Street Cred tier gates, installation capacity, salvage discounts and duplicate-family rejection. No new crafting recipes. Hands retains two sockets; Arms retains one.

## Damage and compatibility

Physical Mantis IDs and all old Augment ordinals remain unchanged. New offer families append after the old catalogue prefix, preserving saved daily-stock indexes. New blade items store their variant and synchronize it to local/remote render state and Sandevistan snapshots.

Elemental direct hits use dedicated damage types, still classified as melee in Dense Marrow and critical handling. Thermal is fire-tagged; Toxic is chemical rather than automatically bypassing armor. Status immunities retain Minecraft behavior. Slashes roll 20%; lunges guarantee the secondary status on surviving targets. Bleed refreshes without accumulating instances.

The discharge and bleed use secondary damage sources that bypass only the hit cooldown. They preserve the primary hit's `lastHurt` and existing invulnerability memory, request zero new post-hit immunity, and retain any longer protective interval raised during the secondary hit (such as Second Heart). This permits the extra hit without opening a normal weapon-spam bypass. No global hurt timer or armor bypass is introduced.

## Art and rendering

`art/mantis/build_variants.py` uses Blender 4.3 to build distinct hardware and append it to the accepted six-group base mesh. The three new `.blend` files contain their **additional hardware**, not duplicate complete base assemblies; the original base is in `mantis_blades.blend`.

| Variant | Additional shape language | Near triangles/arm | Far triangles/arm |
| --- | --- | ---: | ---: |
| Physical | Existing steel receiver and bare rails | 3,504 | 588 |
| Thermal | Ceramic shields, copper louvers, heating strips | 3,708 | 720 |
| Electrifying | Insulated rails, induction coils, electrode blocks | 4,016 | 880 |
| Toxic | Round reservoirs, seals, metering lines, serrated injectors | 3,900 | 948 |

All use the same six pivots and one atlas per variant. New atlases extend the baked base to 512×544 with a hardware swatch strip. Near/far meshes are cached on resource reload; the existing 16-block far-LOD switch and original arm/skin assembly remain. No extra geometry entities, per-frame file reads or Blender runtime dependency. The small original 32px inventory sprites use the existing pixel-art style. New shapes and materials distinguish the models as well as their colors.

[Player guides](../wiki/cyberware.md) · [Validation](../VALIDATION.md)
