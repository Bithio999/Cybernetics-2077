# Sandevistan captured colors (0.15.2)

## Reference observation

The user supplied an 85.13-second trimmed *Cyberpunk: Edgerunners* compilation. Sequential inspection of the early school fight, particularly around 9–10.75 seconds, shows an older cyan foreground pose retaining its tint while newly appearing poses behind the opponent progress through blue and violet. Later shots introduce muted warmer colors. The useful distinction is between successive colored snapshots and a snapshot being recolored throughout its lifetime.

The supplied video and extracted reference frames remain local review material; they are not redistributed in this repository. This is a visual observation of that clip, not a claim about Cyberpunk 2077's internal renderer or every shot in the anime.

## Change

The old renderer called `TrailStyle.color(age, ...)` for every draw. Each individual snapshot traversed orange, green, blue and pink as it aged. Separating alpha from that age-based hue calculation in 0.15.1 did not fix this behavior.

Each captured world/body, vanilla-hand or Mantis-hand pose now contains an immutable `PoseTint` with its birth time and RGB. Draws reuse that RGB and evaluate only the opacity envelope. New captures sample a separate per-runner activation clock. Body and hands share that clock; stopping the activation or leaving the level clears its origin. Stationary poses retain their hue while later movement can create poses farther along the palette.

The palette begins in mint/turquoise, progresses through cyan, blue and violet, then muted pink, amber and yellow-green before wrapping smoothly. New captures advance one palette stop per six normal world ticks; a full cycle takes 66 ticks. The hues and timing are an adaptation for Minecraft, not exact sampled anime color values or a reproduction of anime shot timing.

The existing 42-tick world lifetime, 12 saved world poses, 12-tick hand lifetime, alpha envelopes, geometry capture, live-body exclusion, camera fade and rendering budgets remain. No network packet, server gameplay rule, tier strength, battery behavior or saved-world format changes.

## Checks

`TrailStyleTest` now verifies that older cyan/blue/violet poses retain their RGB across later frames while newer poses differ; opacity changes preserve RGB; first-person/body hue timing agrees; activation phase does not depend on world age; and palette wrapping is continuous. Existing LOD, first-person suppression and fade tests remain.

Run the native trail review with:

```sh
./gradlew runClient -I docs/review/client.init.gradle -PtrailReview -PcyberReviewRoot=/path/to/disposable-review
```

The native scenes cover attacks, stationary views, camera overlap, reversal and switching first/third person. These rendering regressions complement the immutable-color tests; they do not establish pixel-identical anime reproduction or multiplayer color synchronization between independent observers. Clean before the release build to exclude review fixtures.
