"""Export evaluated Blender geometry; no Blender or model library is required at runtime.
Can also export an edited source: blender -b art/blender/cyberware_hologram.blend
  --python art/blender/export_hologram.py
"""
import bpy, struct, json, hashlib, math
from pathlib import Path
from collections import defaultdict

def export(root):
 root=Path(root);out=root/'src/main/resources/assets/cybernetics2077/models/hologram/body.cyh'
 groups=defaultdict(list);counts={};bounds=[[float('inf')]*3,[-float('inf')]*3]
 # Apply export LOD before requesting the dependency graph; preserve the dense source.
 for obj in bpy.context.scene.objects:
  if obj.type!='CURVE' or 'cyber_role' not in obj:continue
  role=int(obj['cyber_role'])
  obj.data.bevel_resolution=0 if role==7 else 1
  obj.data.resolution_u=1 if role in [6,7] else 4
  if role==7:
   for spline in obj.data.splines:
    if spline.type=='BEZIER':
     for point in spline.bezier_points:point.handle_left_type=point.handle_right_type='VECTOR'
 bpy.context.view_layer.update()
 graph=bpy.context.evaluated_depsgraph_get()
 for obj in sorted(bpy.context.scene.objects,key=lambda o:o.name):
  if 'cyber_role' not in obj:continue
  role=int(obj['cyber_role']);region=int(obj['cyber_region'])
  evaluated=obj.evaluated_get(graph);mesh=evaluated.to_mesh();mesh.calc_loop_triangles()
  matrix=obj.matrix_world;normal_matrix=matrix.to_3x3().inverted().transposed()
  vertex_uv=mesh.uv_layers.active.data if mesh.uv_layers.active else None
  for tri in mesh.loop_triangles:
   if tri.area<1e-10:continue
   vertices=[]
   for li in tri.loops:
    loop=mesh.loops[li];v=matrix @ mesh.vertices[loop.vertex_index].co;n=(normal_matrix @ mesh.corner_normals[li].vector).normalized()
    # Blender Z-up / front -Y -> Minecraft Y-down / front -Z.
    pos=(v.x/16,1.5-v.z/16,v.y/16);normal=(n.x,-n.z,n.y)
    uv=tuple(vertex_uv[li].uv) if vertex_uv else (v.x*.15,v.z*.13)
    vertices.append((*pos,*normal,*uv))
    for i in range(3):bounds[0][i]=min(bounds[0][i],pos[i]);bounds[1][i]=max(bounds[1][i],pos[i])
   # This is a rigid +90-degree rotation about X; preserve Blender winding.
   groups[(role,region)].extend(vertices)
  counts[obj.name]=len(mesh.loop_triangles);evaluated.to_mesh_clear()
 out.parent.mkdir(parents=True,exist_ok=True)
 with out.open('wb') as f:
  f.write(b'CYH3');f.write(struct.pack('>II',1,len(groups)))
  for (role,region),vertices in sorted(groups.items()):
   name=f'{role:02d}_{region:02d}'.encode();f.write(struct.pack('>H',len(name)));f.write(name)
   # Material RGB is interpreted by the holographic shader, alpha is role-specific.
   mat=next(o.data.materials[0] for o in bpy.context.scene.objects if o.get('cyber_role')==role)
   f.write(struct.pack('>BBI4f',role,region,len(vertices),*mat.diffuse_color))
   for v in vertices:f.write(struct.pack('>8f',*v))
 report={'generator':'Blender '+bpy.app.version_string,'format':'CYH3 / 1','source':'art/blender/cyberware_hologram.blend','groups':len(groups),'triangles':sum(len(v)//3 for v in groups.values()),'bounds':bounds,'sha256':hashlib.sha256(out.read_bytes()).hexdigest(),'objectTriangles':counts}
 (root/'art/blender/model-report.json').write_text(json.dumps(report,indent=2)+'\n')
 print('HOLOGRAM_EXPORT',json.dumps({k:v for k,v in report.items() if k!='objectTriangles'}))

if __name__=='__main__':export(Path(__file__).resolve().parents[2])
