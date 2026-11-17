# Cyberware implementation references

## Animation and combat

The user's trimmed **All 52 Mantis Blade Variants — Cyberpunk 2077 Update 2.5** MP4 was reviewed frame by frame. At approximately **8–16 seconds**, the physical variant deploys beside the forearm, holds an open-hand guard, makes a cross-body cut while the opposite arm guards, and returns to ready. The electrical variant later in the clip repeats the motion. Tier 1 uses the physical mechanism, without that variant's electrical effects.

The Minecraft rig keeps the shoulder attached to the body. The upper arm and forearm share an elbow pivot; the forearm skin hatch, mechanism, hand and blade move together. Alternating cuts use anticipation, contact, follow-through and eased recovery, sampled at render partial ticks. First- and third-person views use the same joints. The default Minecraft swing is suppressed while the claws are deployed.

- [Gamestegy: Mantis Blades build, updated for 2.5](https://gamestegy.com/post/cyberpunk-2077/386/the-wolverine-mantis-blades-build): reviewed the guide and its embedded [combat MP4](https://cdn.gamestegy.com/posts-videos/mantis-blades-build-combat-showcase.mp4). Shows strong-attack leaps used from both the ground and the air. Its Relic/perk-enhanced range and area attacks are separate from the base implant; they were not assigned to tier 1.
- [Gamestegy: Mantis Blades](https://gamestegy.com/cyberpunk-2077/wiki/994/mantis-blades): arm-system placement and original item icon.
- [PowerPyx: Mantis Blades](https://www.powerpyx.com/cyberpunk-2077-mantis-blades-location/): reviewed article and screenshots for installation, drawing, slashes and held/released heavy leaps. This is a pre-2.0 guide, not a source for current acquisition or damage values.

The mod's heavy attack acquires a visible target within **8 blocks** and follows a short arcing leap using Minecraft collision movement, including when released in midair. Damage occurs at contact. Minecraft controls remain **G** to draw, left-click to slash, and held/released right-click to charge. Those controls and the range are Minecraft adaptations.

## Scanner

Reviewed the user's **Blackwall Gateway Quickhack** MP4: the RAM cells, cyan chip-shaped quickhack rows, amber tags, right-side data panel, target label and red upload chips above the target. The earlier Additional Additions scanner source was also compared. The implementation retains its staged native-HUD transition, with no Tensura dependencies or displays. RAM shows the installed deck's real four cells, rather than inventing additional capacity to match the reference screenshot.

- [CD PROJEKT RED: Update 2.0](https://www.cyberpunk.net/en/news/49060/update-2-0): revised cyberware/ripperdoc experience, netrunning RAM, upload timing and queue mechanics.
- [Gamestegy: Netrunner build](https://gamestegy.com/post/cyberpunk-2077/698/netrunner-build): deck/optics and quickhack references.
- [Gamestegy: Short Circuit](https://gamestegy.com/cyberpunk-2077/wiki/1028/short-circuit): reviewed tier descriptions and icon. The page initially selects Iconic; its displayed high-tier numbers were not treated as tier-one values.

The eye enables scanning; a loaded cyberdeck enables hacking. Server checks cover installed equipment, target visibility, range, PvP rules and RAM. The temporary bench replaces the installation service. The inventory viewer remains read-only.

## Icons and attribution

The four implant/program icons now use Cyberpunk 2077 artwork hosted by Gamestegy, credited to **CD PROJEKT RED**. Exact asset URLs and SHA-256 hashes are recorded in [the source manifest](../../art/cyberpunk-icons/sources.json). The preparation script crops transparent margins and adds a transparent square border without stretching the artwork. These icons are referenced game artwork, not original mod drawings. The mod's arm meshes, interface geometry, category glyphs and workstation textures are original.

Fandom and wiki.gg pages were inaccessible. YouTube itself was unavailable; the uploaded MP4 and accessible community-hosted MP4 were used. No blocked page or inaccessible video was treated as reviewed. Full reference videos are not redistributed in the mod.

## Balance

Tier-one Minecraft values: **4 RAM**, **2 program sockets**, **8 electrical damage**, **8/8/10 physical light damage**, **14 charged damage**. They are mod balance choices, not exact Cyberpunk statistics. No Relic, Overclock or broader perk system is included.
