# Sandevistan trail corrections (0.15.1)

The previous six-stop palette used trail age both to choose the color and to reduce opacity. Blue and violet arrived when the mesh was almost invisible. The local-body proximity guard compared the camera with the saved feet position, so it did not reliably reject a saved head surrounding the camera. World ghosts were also drawn after live entities, allowing them to overlay the current player. The short activation silhouette deliberately occupied the current body.

## Color and length

World histories now hold up to 12 saved poses, sampled at least 3.5 ticks apart, with a 42-tick lifetime (2.1 seconds at normal world speed). Eleven color stops use smooth interpolation: orange, gold, yellow-green, green, mint, turquoise, cyan, blue, violet, purple and pink. The color transition is separate from the entry/fade envelope: older colors remain visible, then fade at the end. First-person hands use four subtler poses with a 12-tick lifetime.

The retained GPU capture/replay design remains. Rendering is limited to six actors, 48 world poses overall and three new captures per frame. Distant actors use at most six evenly distributed poses, retaining both ends of the spectrum. Expired or evicted buffers are closed. Longer histories trade some GPU memory/draw work for the requested longer trail; this is bounded, not free.

## Overlap prevention

- The local full-body history is never drawn in first person; hand/blade echoes handle that view.
- World echoes render after cutout terrain and before live entities, so current opaque players and equipment cover old poses.
- Each ghost excludes its owner's current interpolated body volume, with a soft boundary. The previous activation body overlay is removed.
- The ghost fragment shader fades/discards fragments near the camera. This also covers another player's saved head crossing the view.
- First-person echoes render before current opaque hands.

## Native regression review

Run `./gradlew runClient -I docs/review/client.init.gradle -PtrailReview` with the disposable Cybernetics Review save. The review first repeats normal installation/activation and tracked-player scenes, then exercises stationary first/third person, attacks, a camera inside another runner's saved head, direction reversal through local history and view changes. It asserts local-body suppression, live-body exclusion, a longer visible history and the 48-pose budget. The test-only tracked runner is not a second human/network-latency test.

Success writes `capture/trails-passed.txt`; the test also records active-segment frame timings. `-PcyberRecording` uses timestamp markers instead of screenshot readbacks. Clean before packaging so review classes never enter the release JAR.

Combat strength, tier progression, battery rules and network protocols are unchanged. All Dynalar tiers retain the same 50% relative slowing.
