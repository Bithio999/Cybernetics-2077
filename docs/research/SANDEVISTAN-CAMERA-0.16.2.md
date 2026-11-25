# Sandevistan camera anchoring — 0.16.2

## Cause

World afterimages were replayed at `RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS`. At this stage, Minecraft 1.21.1's `LevelRenderer.renderLevel` has not yet multiplied the camera view into the global `RenderSystem` model-view stack. Terrain uses explicitly supplied matrices. `TemporalMesh` instead read that global matrix, so saved world positions were projected with the wrong camera orientation. A pose could be missing or appear to move incorrectly when the observer turned, in first or third person.

First-person arm snapshots had a separate problem: they stored camera-space vertices with a zero origin and replayed them directly in the current screen frame. That made saved arms follow the camera rather than retaining their captured world position/orientation.

## Correction

- World echoes use the render event's explicit view and projection matrices. They apply the saved world origin relative to the current interpolated camera position exactly once.
- Arm echoes retain a copy of the camera position and camera-to-world quaternion at birth. Replay converts their saved camera-space vertices into the world and then into the current camera view. The current hand projection is retained for the vanilla/custom weapon lens.
- The common pure `TrailTransform` helper computes `view × translation(savedOrigin − currentCamera) × birthCameraRotation`; world meshes omit the last rotation because their vertices already have world-oriented axes.
- GPU meshes still upload once. Each echo's matrices are calculated once and reused across its material batches. Birth hues, fade times, draw/history budgets, body-volume exclusions and camera-proximity fading remain in effect.

## Regression evidence

The pixel review places a saved remote pose in a known visible location, moves its live owner away, captures the result, clears the history and captures a background control. Clouds and the held-arm/HUD overlay are disabled for this review, and comparison is limited to the known ghost region. The initial broad comparison included moving arms/clouds and was not a valid ghost measurement; only the isolated comparisons are used in the results.

Restoring the shipped 0.16.1 renderer before the correction reproduced **zero visible changed pixels** at first-person yaw 0°, 45°, 90° and yaw 135° / pitch 25°. Other angles could show a misplaced or visible ghost, consistent with the user's angle-dependent report. The old render-counter checks could pass while the real saved pose was absent from the screen.

The corrected renderer is checked at those six first-person yaw/pitch combinations and both third-person camera modes. Three JVM regression cases exercise 24 compass/pitch views, turning away from a saved arm, and moving/turning past a known captured position. The established native trail suite additionally checks stationary overlap, camera proximity, reversal, view switching and fixed birth hues.

No combat timing, Sandevistan strength, tier scaling or server tick logic is changed. Native review uses an integrated test world and software OpenGL, not authenticated two-computer testing or a hardware benchmark. Audio and session-service connectivity are unavailable in this environment.

The final pixel fixture confirms the scripted remote actor has finished tracking/interpolating at its target before capture. Its active hold window was extended after a short teleport test missed a near pose at a chunk boundary; production tracking/synchronization was not modified. The final corrected run shows the known pose in all eight views. [Pixel comparison](../images/sandevistan-camera-comparison-0.16.2.png) · [Current trail capture](../images/sandevistan-trails-0.16.2.png).
