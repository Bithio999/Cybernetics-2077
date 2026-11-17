# Mantis Blades — Blender source

[Open the editable Blender model](mantis_blades.blend) · [In-game deployment and combat](../../docs/media/mantis-claws.mp4) · [Native mechanism close-up](../../docs/media/mantis-mechanism.mp4)

![Blender model study, not gameplay](blender-preview.png)

This is an original model authored in **Blender 4.3.2**. The user's side-view reference and uploaded physical-blade video guide the two-section silhouette: a lower support extends backward and upward from the dorsal forearm, and the cutting assembly pivots at its far end to point forward past the hand. The mount, support, blade receiver and sliding tip have separate named pivots. The tip telescopes; geometry is not stretched.

The source includes chamfered housings, inset panels, hinge rings, bolts, serrations, rails and cable bundles. Minecraft supplies the wearer's skin and the solid-backed forearm hatch separately. The source preview shows just the mechanical assembly; the linked videos show the actual in-game result.

## Rebuild

From the repository root, with Blender and its bundled Python/NumPy:

```sh
blender -b -t 4 --python art/mantis/build_mantis.py -- "$PWD"
```

The generator writes the editable `.blend`, a studio image, the packed colour/AO atlas, two runtime meshes and [mesh-report.json](mesh-report.json). It uses CPU Cycles for baking; Blender is **not** a mod dependency. Running it overwrites these generated assets. The model scene uses X across the arm, +Y toward the hand and +Z dorsal/up. Export maps these to Minecraft's canonical arm coordinates `(X, Y, -Z)` and reverses triangle winding. Ready-pose parent transforms are excluded from the export; Minecraft animates each local pivot.

`GAME_*` objects retain the source geometry. Hidden `RUNTIME_*` and `LOD_*` objects contain the exported near and far meshes. Runtime groups are `mount`, `support`, `blade`, and `tip`.

## Runtime budget

| Asset | Budget |
| --- | ---: |
| Near mesh, per blade | 5,122 triangles |
| Far mesh, per blade | 490 triangles |
| Far switch, third person | Beyond 16 blocks |
| Shared colour / ambient-occlusion atlas | 512 × 512 RGBA |
| Animated rigid groups | 4 |

Both LODs share the same texture and pivots. Far geometry removes small fasteners and other details before simplification, reducing the triangle count by **90.4%**. Baking preserves surface shading without adding material passes. Mesh files are decoded during resource reload into cached primitive arrays, with no runtime Blender processing, subdivision, physics rig or per-frame file reads. Minecraft's quad entity pipeline uses one degenerate fourth vertex for each exported triangle. The existing opaque forearm backing is independent of LOD.

The mesh reader validates counts, indices, finite coordinates and group names. JVM tests check UVs, normals, triangle budgets, near/far bounds and the two-hinge alignment through deployment. Native Minecraft review verifies deployment, retraction, slashes and ground/air/free leaps. These measures bound model cost; performance still depends on hardware, shaders and the number of visible players.
