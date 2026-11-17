# Cyberware implementation references

## References reviewed

- [CD PROJEKT RED: Update 2.0](https://www.cyberpunk.net/en/news/49060/update-2-0). Describes the revised cyberware/ripperdoc experience, netrunning RAM, upload timing and queue mechanics. Used for the equipment/program/resource separation; Overclock and the wider perk system are outside this release.
- [PowerPyx: Mantis Blades location](https://www.powerpyx.com/cyberpunk-2077-mantis-blades-location/). Reviewed the article and its original screenshots, including the arm implant’s appearance and weapon-wheel description. It documents installation in **Cyberware → Arms**, followed by drawing the blades, plus fast slashes/combos and a held/released heavy leap. This is a pre-2.0 guide; its acquisition rules and damage numbers were not copied.
- The user's Cyberpunk 2077 cyberware screenshot and supplied menu video, especially the optical implant silhouette and the body-system installation layout.
- The user's clarification that Mantis Blades emerge from the **side** of the arm. The final model uses mirrored outside forearm hatches, lateral rails and hinges. The fist remains intact during deployment.

Fandom and wiki.gg pages were unavailable from this environment. No text from their blocked pages was treated as verified research.

## Minecraft adaptation

Tier-one numbers are chosen for Minecraft’s health scale: four RAM, two program sockets, 8 electrical damage, 8/8/10 physical light-combo damage and a 14-damage heavy lunge. These are mod balance values, not claims of exact Cyberpunk 2077 statistics.

The temporary bench plays the role of installation service. A separate native container edits chips held on the actual deck’s data component. The inventory tab remains read-only. The scanner cannot hack without both installed optics and a loaded deck; the server checks aim, visibility, range, PvP rules and RAM before accepting an upload.

The arm model uses the live Minecraft skin rather than a fixed skin-colour texture. The outer half of the forearm pivots open; the inner half and fist remain in place. A side carriage and cylindrical hinge unfold a tapered steel blade. Animation uses staged easing for anticipation, contact, follow-through and recovery, sampled with render partial ticks. First-person motion preserves room around the crosshair; third-person layers replace the vanilla arms and hide the obstructing chest-armor arm geometry while deployed.

All shipped icons and mesh artwork are original. Reference screenshots were inspected locally and are not redistributed in the JAR.
