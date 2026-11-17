"""Blender 4.3: editable two-link Mantis mechanism, baked atlas and compact runtime LODs.
Run: blender -b --python art/mantis/build_mantis.py -- /absolute/repository
"""

import bpy, math, sys, json, struct
from pathlib import Path
from mathutils import Matrix, Vector
import numpy as np

ROOT = Path(sys.argv[sys.argv.index("--") + 1]).resolve()
ART = ROOT / "art/mantis"
ASSET = ROOT / "src/main/resources/assets/cybernetics2077"
OUT = ASSET / "meshes/mantis"
TEX = ASSET / "textures/entity"
ART.mkdir(exist_ok=True)
OUT.mkdir(parents=True, exist_ok=True)
bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)
scene = bpy.context.scene
scene.render.engine = "CYCLES"
scene.cycles.device = "CPU"
scene.cycles.samples = 16
scene.cycles.use_denoising = False
scene.render.bake.margin = 3
scene.render.bake.use_clear = False
MATS = {}
for name, color, metal in [
    ("gunmetal", (0.105, 0.125, 0.14, 1), 0.75),
    ("recess", (0.025, 0.035, 0.043, 1), 0.5),
    ("steel", (0.48, 0.56, 0.59, 1), 0.85),
    ("edge", (0.80, 0.83, 0.79, 1), 0.9),
    ("ceramic", (0.64, 0.68, 0.63, 1), 0.2),
    ("brass", (0.44, 0.33, 0.19, 1), 0.75),
    ("red", (0.32, 0.055, 0.045, 1), 0.1),
    ("cable", (0.045, 0.055, 0.06, 1), 0.1),
    ("pcb", (0.055, 0.15, 0.13, 1), 0.2),
    ("letter", (0.67, 0.72, 0.70, 1), 0.1),
]:
    m = bpy.data.materials.new(name)
    m.diffuse_color = color
    m.use_nodes = True
    bs = m.node_tree.nodes.get("Principled BSDF")
    bs.inputs["Base Color"].default_value = color
    bs.inputs["Metallic"].default_value = metal
    bs.inputs["Roughness"].default_value = 0.42
    MATS[name] = m
PARTS = {}


def empty(name, parent=None, loc=(0, 0, 0), angle=0):
    o = bpy.data.objects.new(name, None)
    scene.collection.objects.link(o)
    o.parent = parent
    o.location = loc
    o.rotation_euler.x = math.radians(angle)
    o.empty_display_size = 0.045
    return o


base = empty("DORSAL_MOUNT", loc=(0, 0.43, 0.17))
PARTS["mount"] = base
PARTS["support"] = empty("SUPPORT_PIVOT", base, angle=145)
PARTS["blade"] = empty("BLADE_PIVOT", PARTS["support"], loc=(0, 0.405, 0), angle=-137)
PARTS["tip"] = empty("TIP_SLIDE", PARTS["blade"], loc=(0, 0.44145, 0))
PARTS["edge"] = empty("EDGE_SLIDE", PARTS["tip"], loc=(0, 0.43335, 0))


def finish(o, name, part, mat="gunmetal", bevel=0, detail=False):
    o.name = name
    o.parent = PARTS[part]
    o["part"] = part
    o["detail"] = detail
    o.data.materials.append(MATS[mat])
    if bevel:
        o.data.materials.append(MATS["steel"])
        b = o.modifiers.new("Machined chamfer", "BEVEL")
        b.width = bevel
        b.segments = 1
        b.material = 1
        n = o.modifiers.new("Face weighted normals", "WEIGHTED_NORMAL")
        n.keep_sharp = True
    return o


def box(name, part, loc, size, mat="gunmetal", bevel=0.001, detail=False):
    bpy.ops.mesh.primitive_cube_add(size=1, location=(0, 0, 0))
    o = bpy.context.object
    o.data.transform(Matrix.Diagonal((*size, 1)))
    o.location = loc
    return finish(o, name, part, mat, bevel, detail)


def profile(name, part, points, thick, mat="gunmetal", bevel=0.001, detail=False):
    # Y/Z side profile extruded across X; the blade points along +Y, not out of the forearm.
    n = len(points)
    verts = [(x, y, z) for x in [-thick / 2, thick / 2] for y, z in points]
    faces = [tuple(range(n - 1, -1, -1)), tuple(range(n, 2 * n))] + [
        (i, (i + 1) % n, (i + 1) % n + n, i + n) for i in range(n)
    ]
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(verts, [], faces)
    mesh.update()
    o = bpy.data.objects.new(name, mesh)
    scene.collection.objects.link(o)
    return finish(o, name, part, mat, bevel, detail)


def cyl(name, part, loc, radius, depth, mat="steel", detail=False, vertices=12):
    bpy.ops.mesh.primitive_cylinder_add(
        vertices=vertices,
        radius=radius,
        depth=depth,
        location=(0, 0, 0),
        rotation=(0, math.pi / 2, 0),
    )
    o = bpy.context.object
    o.location = loc
    return finish(o, name, part, mat, 0.0007, detail)


def bolt(name, part, loc, r=0.008):
    cyl(name + " seat", part, loc, r * 1.23, 0.003, "recess", True)
    for sign in [-1, 1]:
        p = (loc[0] + sign * 0.0022, loc[1], loc[2])
        cyl(name + " hex head", part, p, r, 0.0024, "steel", True, 8)
        box(
            name + " driver slot",
            part,
            (p[0] + sign * 0.0014, p[1], p[2]),
            (0.0005, r * 0.95, 0.0018),
            "recess",
            0,
            True,
        )


def cable(name, locs, mat):
    curve = bpy.data.curves.new(name, "CURVE")
    curve.dimensions = "3D"
    curve.resolution_u = 3
    curve.bevel_depth = 0.0033
    curve.bevel_resolution = 0
    spline = curve.splines.new("BEZIER")
    spline.bezier_points.add(len(locs) - 1)
    for b, loc in zip(spline.bezier_points, locs):
        b.co = loc
        b.handle_left_type = "AUTO"
        b.handle_right_type = "AUTO"
    o = bpy.data.objects.new(name, curve)
    scene.collection.objects.link(o)
    o.parent = PARTS["mount"]
    o["part"] = "mount"
    o["detail"] = True
    o.data.materials.append(MATS[mat])


# Mount: framed circuit tray, small sealed floor and a wrist-side collar.
box(
    "Dorsal chassis",
    "mount",
    (0, -0.014, -0.026),
    (0.136, 0.182, 0.021),
    "gunmetal",
    0.002,
)
box(
    "Recessed board", "mount", (0, -0.014, -0.013), (0.109, 0.149, 0.004), "pcb", 0.0005
)
for x in [-0.061, 0.061]:
    box(
        "Ceramic guide cover",
        "mount",
        (x, -0.015, -0.005),
        (0.013, 0.174, 0.017),
        "ceramic",
        0.0015,
    )
for y in [-0.089, 0.063]:
    box("End sill", "mount", (0, y, -0.008), (0.127, 0.012, 0.020), "steel", 0.0015)
for k in range(3):
    y = -0.064 + k * 0.048
    box(
        "Controller package",
        "mount",
        (0, y, -0.006),
        (0.028, 0.029, 0.010),
        "recess",
        0.001,
        True,
    )
    for sign in [-1, 1]:
        for j in range(3):
            box(
                "Controller pins",
                "mount",
                (sign * 0.018, y + (j - 1) * 0.008, -0.005),
                (0.011, 0.002, 0.002),
                "brass",
                0,
                True,
            )
for k in range(4):
    x = (k - 1.5) * 0.024
    cable(
        "Braided service line",
        [
            (x, -0.087, -0.003),
            (x + 0.007, -0.035, 0.006),
            (x - 0.003, 0.025, 0.004),
            (x, 0.065, -0.005),
        ],
        ["red", "cable", "ceramic", "red"][k],
    )
for x in [-0.058, 0.058]:
    for y in [-0.076, 0.052]:
        bolt("Chassis fastener", "mount", (x, y, -0.002), 0.0045)
cyl("Proximal hinge barrel", "mount", (0, 0, 0.007), 0.030, 0.119, "gunmetal")
cyl("Proximal hinge spindle", "mount", (0, 0, 0.007), 0.021, 0.130, "steel")
# Lower section: two load-bearing cheek plates lead BACK and UP to the second hinge.
lower = [
    (0, -0.027),
    (0.055, -0.035),
    (0.27, -0.023),
    (0.34, -0.013),
    (0.34, 0.019),
    (0.265, 0.029),
    (0.045, 0.026),
    (0, 0.020),
]
for sign in [-1, 1]:
    o = profile("Lower support cheek", "support", lower, 0.016, "gunmetal", 0.0018)
    o.location.x = sign * 0.039
    box(
        "Inlaid support spine",
        "support",
        (sign * 0.049, 0.164, 0.006),
        (0.003, 0.241, 0.012),
        "steel",
        0.0005,
    )
    box(
        "Support recessed channel",
        "support",
        (sign * 0.0495, 0.16, -0.006),
        (0.0015, 0.195, 0.006),
        "recess",
        0,
        True,
    )
    for y in [0.052, 0.278]:
        bolt("Linkage fastener", "support", (sign * 0.051, y, 0.006), 0.006)
box(
    "Support bridge", "support", (0, 0.073, 0), (0.071, 0.025, 0.035), "gunmetal", 0.002
)
box(
    "Piston housing",
    "support",
    (0, 0.155, -0.005),
    (0.022, 0.130, 0.021),
    "recess",
    0.001,
)
box(
    "Piston chrome rod",
    "support",
    (0, 0.235, -0.005),
    (0.010, 0.145, 0.010),
    "steel",
    0.0006,
)
cyl("Distal hinge ring", "support", (0, 0.34, 0), 0.037, 0.123, "gunmetal")
cyl("Distal hinge face", "support", (0, 0.34, 0), 0.027, 0.132, "steel")
cyl("Distal axle", "support", (0, 0.34, 0), 0.015, 0.139, "brass")
# Upper section: long, thin industrial blade with a fixed receiver and a separate sliding tip.
body = [
    (-0.045, -0.036),
    (0, -0.055),
    (0.12, -0.052),
    (0.57, -0.025),
    (0.655, -0.008),
    (0.655, 0.036),
    (0.12, 0.061),
    (0.025, 0.047),
    (-0.045, 0.011),
]
profile("Main blade receiver", "blade", body, 0.028, "gunmetal", 0.0018)
spine = [
    (-0.04, 0.015),
    (0.08, 0.051),
    (0.63, 0.031),
    (0.66, 0.022),
    (0.63, 0.042),
    (0.08, 0.067),
    (-0.04, 0.025),
]
profile("Continuous honed spine", "blade", spine, 0.031, "edge", 0.0005)
edge = [
    (-0.025, -0.036),
    (0.12, -0.055),
    (0.62, -0.023),
    (0.65, -0.012),
    (0.62, -0.015),
    (0.12, -0.045),
    (-0.025, -0.028),
]
profile("Honed lower edge", "blade", edge, 0.030, "steel", 0.0005)
for sign in [-1, 1]:
    panel = [
        (0.04, -0.029),
        (0.16, -0.030),
        (0.59, -0.007),
        (0.60, 0.025),
        (0.16, 0.043),
        (0.04, 0.029),
    ]
    o = profile("Inset side armor", "blade", panel, 0.003, "steel", 0.0005)
    o.location.x = sign * 0.0162
    box(
        "Receiver slide groove",
        "blade",
        (sign * 0.0188, 0.355, 0.025),
        (0.0013, 0.451, 0.004),
        "recess",
        0,
        True,
    )
    for y in [0.095, 0.31, 0.575]:
        bolt("Receiver countersunk screw", "blade", (sign * 0.0194, y, 0.005), 0.0065)
    # Large readable industrial markings, encoded in the atlas during the colour bake.
    for i in range(5):
        box(
            "Identification ticks",
            "blade",
            (sign * 0.0193, 0.19 + i * 0.006, -0.008),
            (0.001, 0.002, 0.010),
            "letter",
            0,
            True,
        )
    for i in range(4):
        box(
            "Serial marks",
            "blade",
            (sign * 0.0193, 0.245 + i * 0.009, -0.008),
            (0.001, 0.004, 0.004),
            "letter",
            0,
            True,
        )
    box(
        "Red witness mark",
        "blade",
        (sign * 0.020, 0.10, 0.006),
        (0.0013, 0.010, 0.003),
        "red",
        0,
        True,
    )
for k in range(8):
    y = 0.13 + k * 0.039
    profile(
        "Serrated heel tooth",
        "blade",
        [(y, -0.046), (y + 0.018, -0.043), (y + 0.004, -0.060)],
        0.024,
        "steel",
        0.0003,
        True,
    )
# The tip is a closed, bevelled mesh. It telescopes into the receiver, never stretches.
tip = [
    (0, -0.018),
    (0.42, -0.003),
    (0.60, 0.034),
    (0.565, 0.045),
    (0.04, 0.031),
    (0, 0.025),
]
profile("Sliding blade tip", "tip", tip, 0.019, "steel", 0.0009)
profile(
    "Tip cutting bevel",
    "tip",
    [
        (0, -0.019),
        (0.42, -0.005),
        (0.603, 0.034),
        (0.60, 0.037),
        (0.42, 0.002),
        (0, -0.011),
    ],
    0.021,
    "edge",
    0.0004,
)
for sign in [-1, 1]:
    box(
        "Tip fuller",
        "tip",
        (sign * 0.0103, 0.218, 0.014),
        (0.0007, 0.385, 0.004),
        "recess",
        0,
        True,
    )
    for k in range(3):
        box(
            "Tip datum mark",
            "tip",
            (sign * 0.0108, 0.112 + k * 0.012, 0.020),
            (0.0005, 0.003, 0.008),
            "letter",
            0,
            True,
        )
# A nested distal leaf fits within the receiver when stowed. It gives the
# extended blade reach without storing a rigid sword through the player's hand.
for source in [o for o in list(scene.objects) if o.get("part") == "tip"]:
    leaf = source.copy()
    leaf.data = source.data.copy()
    scene.collection.objects.link(leaf)
    leaf.parent = PARTS["edge"]
    leaf["part"] = "edge"
    leaf.name = "Distal " + source.name

# The middle leaf is a straight receiver, not a second pointed blade.
# Its open end overlaps the narrow distal leaf instead of leaving a gap.
for o in [o for o in list(scene.objects) if o.get("part") == "tip"]:
    bpy.data.objects.remove(o, do_unlink=True)
profile(
    "Telescoping receiver leaf",
    "tip",
    [(0, -0.025), (0.603, -0.020), (0.603, 0.024), (0, 0.026)],
    0.024,
    "steel",
    0.0009,
)
for sign in [-1, 1]:
    box(
        "Inner slide channel",
        "tip",
        (sign * 0.0126, 0.295, 0.006),
        (0.0008, 0.55, 0.004),
        "recess",
        0,
        True,
    )

# Exposed subdermal harness: long service bundles occupy the opened arm,
# instead of leaving an empty rectangular cavity under the mechanism.
box(
    "Subdermal harness spine",
    "mount",
    (0, -0.16, -0.055),
    (0.020, 0.41, 0.012),
    "recess",
    0.001,
)
for side in [-1, 1]:
    box(
        "Harness rail",
        "mount",
        (side * 0.048, -0.16, -0.05),
        (0.008, 0.40, 0.009),
        "steel",
        0.0006,
    )
    for k in range(3):
        x = side * (0.049 + k * 0.005)
        cable(
            "Long flex tendon",
            [
                (x, -0.36, -0.045),
                (x + side * 0.005, -0.25, -0.036),
                (x - side * 0.003, -0.11, -0.040),
                (x, 0.035, -0.043),
            ],
            "red" if k != 1 else "cable",
        )

for side in [-1, 1]:
    cable(
        "Inner hydraulic conduit",
        [
            (side * 0.025, -0.355, -0.044),
            (side * 0.020, -0.23, -0.034),
            (side * 0.029, -0.10, -0.04),
            (side * 0.025, 0.025, -0.046),
        ],
        "cable",
    )
for y in [-0.30, -0.18, -0.055]:
    box(
        "Harness control bridge",
        "mount",
        (0, y, -0.048),
        (0.10, 0.014, 0.007),
        "pcb",
        0.0005,
        True,
    )

# Convert/apply modelling modifiers once, record detail faces, then join each rigid part.
near = {}
for name, parent in PARTS.items():
    objects = [o for o in list(scene.objects) if o.get("part") == name]
    for o in objects:
        bpy.ops.object.select_all(action="DESELECT")
        o.select_set(True)
        bpy.context.view_layer.objects.active = o
        if o.type == "CURVE":
            bpy.ops.object.convert(target="MESH")
        for mod in list(o.modifiers):
            try:
                bpy.ops.object.modifier_apply(modifier=mod.name)
            except RuntimeError:
                pass
        attr = o.data.attributes.new("cyber_detail", "INT", "FACE")
        for v in attr.data:
            v.value = 1 if o.get("detail") else 0
    bpy.ops.object.select_all(action="DESELECT")
    for o in objects:
        o.select_set(True)
    bpy.context.view_layer.objects.active = objects[0]
    bpy.ops.object.join()
    o = bpy.context.object
    o.name = "GAME_" + name
    o["part"] = name
    # Rebase to the real hinge before changing proportions. Keep the original
    # machined details, but slim the cutting assembly and narrow the linkage.
    bpy.context.view_layer.update()
    local = parent.matrix_world.inverted() @ o.matrix_world
    o.data.transform(local)
    o.matrix_parent_inverse = Matrix.Identity(4)
    o.matrix_basis = Matrix.Identity(4)
    proportions = {
        "mount": (0.90, 0.80, 0.85),
        "support": (0.55, 0.30 / 0.34, 0.50),
        "blade": (0.55, 0.55, 0.40),
        "tip": (0.55, 0.55, 0.70),
        "edge": (0.42, 0.55, 0.65),
    }
    o.data.transform(Matrix.Diagonal((*(v * 1.35 for v in proportions[name]), 1)))
    near[name] = o
    bpy.ops.object.mode_set(mode="EDIT")
    bpy.ops.mesh.select_all(action="SELECT")
    bpy.ops.uv.smart_project(angle_limit=math.radians(66), island_margin=0.012)
    bpy.ops.object.mode_set(mode="OBJECT")
    index = list(PARTS).index(name)
    ox = (index % 3) / 3
    oy = (index // 3) * 0.5
    for uv in o.data.uv_layers.active.data:
        uv.uv = (ox + 0.008 + uv.uv.x * (1 / 3 - 0.016), oy + 0.012 + uv.uv.y * 0.476)
# One 512px diffuse+AO atlas shared by all five rigid groups and both LODs.
color = bpy.data.images.new("Mantis colour bake", width=512, height=512, alpha=False)
ao = bpy.data.images.new("Mantis AO bake", width=512, height=512, alpha=False)
ao.colorspace_settings.name = "Non-Color"
for mat in MATS.values():
    mat.node_tree.nodes.get("Principled BSDF").inputs["Metallic"].default_value = 0
for target, kind in [(color, "DIFFUSE"), (ao, "AO")]:
    for mat in MATS.values():
        node = mat.node_tree.nodes.new("ShaderNodeTexImage")
        node.image = target
        mat.node_tree.nodes.active = node
    bpy.ops.object.select_all(action="DESELECT")
    for o in near.values():
        o.select_set(True)
    bpy.context.view_layer.objects.active = near["blade"]
    bpy.ops.object.bake(
        type=kind, pass_filter={"COLOR"} if kind == "DIFFUSE" else set()
    )
c = np.array(color.pixels[:], dtype=np.float32).reshape((-1, 4))
a = np.array(ao.pixels[:], dtype=np.float32).reshape((-1, 4))
c[:, :3] *= 0.65 + 0.35 * a[:, :3]
c[:, 3] = 1
atlas = bpy.data.images.new("Mantis runtime atlas", width=512, height=512, alpha=True)
atlas.pixels.foreach_set(c.ravel())
atlas.filepath_raw = str(TEX / "mantis_blender.png")
atlas.file_format = "PNG"
atlas.save()
atlas.pack()
runtime = bpy.data.materials.new("GAME baked atlas")
runtime.use_nodes = True
node = runtime.node_tree.nodes.new("ShaderNodeTexImage")
node.image = atlas
runtime.node_tree.links.new(
    node.outputs["Color"],
    runtime.node_tree.nodes.get("Principled BSDF").inputs["Base Color"],
)
runtime.node_tree.nodes.get("Principled BSDF").inputs["Metallic"].default_value = 0.65
for o in near.values():
    o.data.materials.clear()
    o.data.materials.append(runtime)
# Distant copies discard small details and collapse the remaining topology, retaining the same UVs.
far = {}
import bmesh

for name, o in near.items():
    clone = o.copy()
    clone.data = o.data.copy()
    scene.collection.objects.link(clone)
    clone.name = "LOD_" + name
    bm = bmesh.new()
    bm.from_mesh(clone.data)
    layer = bm.faces.layers.int.get("cyber_detail")
    if layer:
        bmesh.ops.delete(bm, geom=[f for f in bm.faces if f[layer]], context="FACES")
    bm.to_mesh(clone.data)
    bm.free()
    bpy.ops.object.select_all(action="DESELECT")
    clone.select_set(True)
    bpy.context.view_layer.objects.active = clone
    mod = clone.modifiers.new("Distant simplification", "DECIMATE")
    mod.ratio = 0.32
    mod.use_collapse_triangulate = True
    bpy.ops.object.modifier_apply(modifier=mod.name)
    clone.hide_render = True
    clone.hide_set(True)
    far[name] = clone
# Optimized near meshes keep baked surface shading while collapsing redundant bevel topology.
source_triangles = sum(len(o.data.polygons) * 2 for o in near.values())
optimized = {}
for name, o in near.items():
    clone = o.copy()
    clone.data = o.data.copy()
    scene.collection.objects.link(clone)
    clone.name = "RUNTIME_" + name
    bpy.ops.object.select_all(action="DESELECT")
    clone.select_set(True)
    bpy.context.view_layer.objects.active = clone
    mod = clone.modifiers.new("Near mesh optimization", "DECIMATE")
    mod.ratio = 0.43
    mod.use_collapse_triangulate = True
    bpy.ops.object.modifier_apply(modifier=mod.name)
    clone.hide_render = True
    clone.hide_set(True)
    optimized[name] = clone
# Local-part export. Ready-pose parents are removed, so each pivot is controlled by Minecraft.
report = {
    "atlas": [512, 512],
    "parts": {},
    "near_triangles": 0,
    "far_triangles": 0,
    "schema": 1,
}


def utf(f, s):
    b = s.encode()
    f.write(struct.pack(">H", len(b)))
    f.write(b)


def export(path, objects, lod):
    with path.open("wb") as f:
        f.write(b"CYMB")
        f.write(struct.pack(">II", 1, len(objects)))
        for name, o in objects.items():
            mesh = o.data
            mesh.calc_loop_triangles()
            local = PARTS[name].matrix_world.inverted() @ o.matrix_world
            normal = local.to_3x3().inverted().transposed()
            verts = []
            indices = []
            lookup = {}
            positions = []
            for tri in mesh.loop_triangles:
                # Z reflection reverses winding; restore it for a normal right-handed triangle list.
                for li in reversed(tri.loops):
                    p = local @ mesh.vertices[mesh.loops[li].vertex_index].co
                    n = (normal @ mesh.corner_normals[li].vector).normalized()
                    uv = mesh.uv_layers.active.data[li].uv
                    item = tuple(
                        round(v, 6)
                        for v in (p.x, p.y, -p.z, uv.x, 1 - uv.y, n.x, n.y, -n.z)
                    )
                    if item not in lookup:
                        lookup[item] = len(verts)
                        verts.append(item)
                        positions.append(item[:3])
                    indices.append(lookup[item])
            utf(f, name)
            f.write(struct.pack(">II", len(verts), len(indices)))
            for v in verts:
                f.write(struct.pack(">8f", *v))
            for index in indices:
                f.write(struct.pack(">I", index))
            report["parts"].setdefault(name, {})[lod] = {
                "vertices": len(verts),
                "triangles": len(indices) // 3,
                "bounds_min": [min(p[k] for p in positions) for k in range(3)],
                "bounds_max": [max(p[k] for p in positions) for k in range(3)],
            }
            report[lod + "_triangles"] += len(indices) // 3


export(OUT / "near.cymesh", optimized, "near")
export(OUT / "far.cymesh", far, "far")
(ART / "mesh-report.json").write_text(json.dumps(report, indent=2))
# Save an editable Blender source with named pivots; runtime texture is packed for portability.
for o in near.values():
    o.hide_set(False)
for o in far.values():
    o.hide_set(True)
scene["runtime_axes"] = (
    "X width; +Y toward hand; Blender +Z dorsal maps to Minecraft -Z"
)
scene["runtime_budget"] = json.dumps(report)
world = scene.world or bpy.data.worlds.new("Studio")
scene.world = world
world.use_nodes = True
world.node_tree.nodes["Background"].inputs[0].default_value = (0.035, 0.045, 0.055, 1)


def aim(o, point):
    o.rotation_euler = (Vector(point) - o.location).to_track_quat("-Z", "Y").to_euler()


bpy.ops.object.camera_add(location=(1.55, 2.1, 1.55))
camera = bpy.context.object
aim(camera, (0, 0.82, 0.32))
camera.data.type = "ORTHO"
camera.data.ortho_scale = 1.7
scene.camera = camera
for loc, power, size in [
    ((2, 1, 3), 600, 3),
    ((-2, 0, 2), 450, 2),
    ((0, -2, 1), 300, 2),
]:
    bpy.ops.object.light_add(type="AREA", location=loc)
    l = bpy.context.object
    l.data.energy = power
    l.data.shape = "DISK"
    l.data.size = size
    aim(l, (0, 0.6, 0.3))
scene.render.resolution_x = 1400
scene.render.resolution_y = 900
scene.render.resolution_percentage = 100
scene.view_settings.view_transform = "AgX"
scene.render.image_settings.file_format = "PNG"
scene.render.filepath = str(ART / "blender-preview.png")
bpy.ops.wm.save_as_mainfile(filepath=str(ART / "mantis_blades.blend"))
bpy.ops.render.render(write_still=True)
print("MANTIS_EXPORT", json.dumps(report))
