# Mantis Blades — Blender source

[Open the editable Blender model](mantis_blades.blend) · [In-game deployment and combat](../../docs/media/mantis-claws.mp4) · [Native mechanism close-up](../../docs/media/mantis-mechanism.mp4)

![Blender model study, not gameplay](blender-preview.png)

This is an original model authored in **Blender 4.3.2**. The user's side-view reference and uploaded physical-blade video guide the two-section silhouette: a lower support extends backward and upward from the dorsal forearm, and the cutting assembly pivots at its far end to point forward past the hand. The mount, support, blade receiver, inner sliding leaf and distal leaf have separate named pivots. The leaves telescope inside each other; geometry is not stretched. The custom rig defines the elbow and hand sections; vanilla Minecraft supplies a single rigid arm. The larger opening and longer sliding leaves keep the deployed cutting assembly large, while the receiver and support covers stay slim.

The source includes chamfered housings, inset panels, hinge rings, bolts, serrations, rails and cable bundles. Minecraft supplies the wearer's skin and the solid-backed forearm hatch separately. The source preview shows just the mechanical assembly; the linked videos show the actual in-game result.

## Rebuild

From the repository root, with Blender and its bundled Python/NumPy:

```sh
blender -b -t 4 --python art/mantis/build_mantis.py -- "$PWD"
```

The generator writes the editable `.blend`, a studio image, the packed colour/AO atlas, two runtime meshes and [mesh-report.json](mesh-report.json). It uses CPU Cycles for baking; Blender is **not** a mod dependency. Running it overwrites these generated assets. The model scene uses X across the arm, +Y toward the hand and +Z dorsal/up. Export maps these to Minecraft's canonical arm coordinates `(X, Y, -Z)` and reverses triangle winding. Ready-pose parent transforms are excluded from the export; Minecraft animates each local pivot.

`GAME_*` objects retain the source geometry. Hidden `RUNTIME_*` and `LOD_*` objects contain the exported near and far meshes. Runtime groups are `mount`, `support`, `blade`, `tip`, and `edge`.

## Runtime budget

| Asset | Budget |
| --- | ---: |
| Near mesh, per blade | 3,086 triangles |
| Far mesh, per blade | 504 triangles |
| Far switch, third person | Beyond 16 blocks |
| Shared colour / ambient-occlusion atlas | 512 × 512 RGBA |
| Animated rigid groups | 5 |

Both LODs share the same texture and pivots. Far geometry removes small fasteners and other details before simplification, reducing the triangle count by **83.7%**. Baking preserves surface shading without adding material passes. Mesh files are decoded during resource reload into cached primitive arrays, with no runtime Blender processing, subdivision, physics rig or per-frame file reads. The blade uses a triangle material, with no duplicate fourth vertex or degenerate triangle per face. Relative to 0.5.2, the reduced mesh and triangle pipeline submit about **55% fewer vertices**. Skin patches are clipped once and reused. The existing opaque forearm backing is independent of LOD.

The mesh reader validates counts, indices, finite coordinates and group names. JVM tests check UVs, normals, triangle budgets, near/far bounds and the two-hinge alignment and actual exported-vertex clearance through deployment. The hatch opens before the support unfolds. The opening spans Y=.11–.52 and 88% of the arm width, keeping the hand, wrist and underside intact. The old opening covered 64% of the width and Y=.29–.48, so the opening area is now about 3.0× larger. Native Minecraft review verifies deployment, retraction, slashes and ground/air/free leaps. These measures bound model cost; performance still depends on hardware, shaders and the number of visible players.
