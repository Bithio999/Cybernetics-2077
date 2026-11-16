# Blender hologram source

Open **[cyberware_hologram.blend](cyberware_hologram.blend)** in Blender 4.3 or newer. The anatomical collection, materials, editable curves, dense cortical surfaces, modifiers, inspection cameras and studio lights are included. No external model or Cyberpunk game assets are used.

The design reference is the user's supplied cyberware-menu video, specifically **00:21–01:05**. The overall silhouette follows Minecraft: 8×8×8 head, 8×4×12 torso, two 4×4×12 arms and two 4×4×12 legs. Thin sculptural surface detail fits that envelope. Blender uses Z-up and -Y forward; the exporter converts to Minecraft coordinates.

## Build or edit

```sh
# Rebuild the authored source scene and export it.
blender -b --threads 4 --python art/blender/build_hologram.py

# Export an edited .blend without regenerating its geometry.
blender -b art/blender/cyberware_hologram.blend --threads 4 \
  --python art/blender/export_hologram.py
```

`build_hologram.py` uses Blender mesh editing, normal recalculation, shaped surface grids, Bezier anatomy, bevels and cortical decimation. The `.blend` keeps editable source detail. The exporter applies a lower curve resolution to tiny nerves, seams and hex cells, evaluates modifiers, and writes the actual positions, split normals, UVs and material groups. It also updates [model-report.json](model-report.json) with the native asset's triangle count, bounds and SHA-256.

The mod loads the exported mesh and caches vertex buffers. **Blender is not required to play.** Its holographic appearance is rendered by Minecraft's custom shader; the Blender studio materials serve as editing aids.

## Layers and category reveal

| Role | Layer |
| --- | --- |
| 0 | Translucent Minecraft body envelope |
| 1 | Muscle and fascia |
| 2 | Bones and costal laminae |
| 3 | Neural cords and peripheral branches |
| 4 | Vascular system and heart |
| 5 | Folded cerebral cortex |
| 6 | Embedded optical apertures |
| 7 | Subdermal hexagonal laminae and contacts |
| 8 | Fine dermal seams |
| 9 | Embedded interface and optical housing |
| 10 | Shaped facial tissue |
| 11 | Shaped thoracic tissue |

Object custom properties `cyber_role` and `cyber_region` define the export grouping. Regions use the ten `CyberwareCategory` ordinals. The cortex is concealed in the overview and revealed by its category; bones and nerves are subdued until selected. Eyes remain recessed in dark housings. Surface lighting transitions from rose above to blue below.

## Native mesh format

`body.cyh` uses big-endian integers/floats:

- `CYH3`, version `1`, group count (`int32` each).
- Per group: ASCII name in Java UTF framing (`uint16` byte length), role (`uint8`), region (`uint8`), vertex count (`int32`), material RGBA (`4×float32`).
- Per triangle vertex: position XYZ, normal XYZ, UV (`8×float32`).

Vertex count is divisible by three. Runtime validation bounds allocations, rejects non-finite coordinates and malformed normals, and checks the model envelope. Static GPU buffers retain both front and back triangle ordering for transparent layers, so category rotation does not require rebuilding geometry each frame.
