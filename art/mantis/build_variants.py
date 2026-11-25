"""Blender-authored variant hardware, merged into the existing six-pivot near/far exports.
Run: blender -b -t 2 --python art/mantis/build_variants.py -- /repository
Original physical meshes and their deployment are preserved.
"""
import bpy, struct, sys, json, math
from pathlib import Path
ROOT=Path(sys.argv[sys.argv.index('--')+1]).resolve()
OUT=ROOT/'src/main/resources/assets/cybernetics2077/meshes/mantis'
PARTS=('mount','carriage','support','blade','tip','edge')
def read(path):
 data=memoryview(path.read_bytes()); pos=12; result={}
 for _ in range(6):
  n=struct.unpack_from('>H',data,pos)[0];pos+=2;name=bytes(data[pos:pos+n]).decode();pos+=n
  count,num=struct.unpack_from('>II',data,pos);pos+=8
  vs=[list(struct.unpack_from('>8f',data,pos+32*i)) for i in range(count)];pos+=count*32
  ids=list(struct.unpack_from('>'+str(num)+'I',data,pos));pos+=4*num
  # Expanded atlas reserves a separate swatch strip for new hardware.
  for v in vs:v[4]*=512/544
  result[name]=(vs,ids)
 return result
report={}
for variant in ('thermal','electrical','toxic'):
 bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
 additions={p:[] for p in PARTS}
 def shape(name,part,loc,size,swatch=0,cylinder=False):
  if cylinder:
   bpy.ops.mesh.primitive_cylinder_add(vertices=12,radius=1,depth=2,location=loc)
   obj=bpy.context.object;obj.rotation_euler.x=math.pi/2;obj.scale=(size[0]/2,size[0]/2,size[1]/2)
  else:
   bpy.ops.mesh.primitive_cube_add(size=1,location=loc);obj=bpy.context.object;obj.scale=size
  obj.name=name;obj['part']=part;obj['swatch']=swatch;additions[part].append(obj)
  # Local groups use runtime axes; each remains editable in the source scene.
  return obj
 if variant=='thermal':
  for side in (-1,1):
   shape('Ceramic heat shield', 'blade',(side*.023,.14,-.033),(.005,.21,.023),0)
   for i in range(6):shape('Copper heat exchanger louver','blade',(side*.025,.055+i*.032,-.047),(.008,.008,.016),2)
   shape('Incandescent heater strip','blade',(side*.026,.15,-.050),(.003,.22,.004),3)
  shape('Heat manifold','mount',(0,-.12,.081),(.08,.10,.012),0)
 elif variant=='electrical':
  for side in (-1,1):
   shape('Insulated conductor rail','blade',(side*.024,.18,-.038),(.006,.30,.012),0)
   shape('Live electrode','blade',(side*.028,.18,-.041),(.003,.27,.005),3)
   for i in range(5):shape('Induction coil','support',(side*.056,.10+i*.043,-.03),(.011,.012,.011),2,True)
   shape('Ceramic contact block','mount',(side*.06,-.15,.08),(.024,.08,.014),1)
 else:
  for side in (-1,1):
   shape('Chemical reservoir','mount',(side*.062,-.18,.075),(.025,.15,.025),2,True)
   for y in (-.265,-.095):shape('Reservoir seal','mount',(side*.062,y,.075),(.029,.016,.029),0,True)
   shape('Fluid delivery line','support',(side*.058,.20,-.045),(.006,.30,.008),3)
   shape('Injection channel','blade',(side*.024,.18,-.043),(.004,.27,.006),2)
  for i in range(7):
   o=shape('Micro serration','edge',(0,.025+i*.032,-.043),(.012,.012,.007),1);o.rotation_euler.x=.35
 bpy.context.view_layer.update()
 for lod in ('near','far'):
  model=read(OUT/(lod+'.cymesh'))
  for part,objects in additions.items():
   vs,ids=model[part]
   for k,obj in enumerate(objects):
    # Distant variants retain the signature silhouette, with fewer small fins/coils.
    if lod=='far' and ('louver' in obj.name or 'coil' in obj.name or 'serration' in obj.name) and k%2:continue
    mesh=obj.data;mesh.calc_loop_triangles();mat=obj.matrix_world;normal=mat.to_3x3().inverted().transposed();sw=obj['swatch']
    for tri in mesh.loop_triangles:
     for corner in tri.vertices:
      v=mat@mesh.vertices[corner].co;n=(normal@mesh.polygons[tri.polygon_index].normal).normalized()
      ids.append(len(vs));vs.append([v.x,v.y,v.z,(sw*128+64)/512,528/544,n.x,n.y,n.z])
  with (OUT/(variant+'_'+lod+'.cymesh')).open('wb') as f:
   f.write(b'CYMB'+struct.pack('>II',1,6))
   for part in PARTS:
    vs,ids=model[part];b=part.encode();f.write(struct.pack('>H',len(b))+b+struct.pack('>II',len(vs),len(ids)))
    for v in vs:f.write(struct.pack('>8f',*v))
    f.write(struct.pack('>'+str(len(ids))+'I',*ids))
  report.setdefault(variant,{})[lod+'_triangles']=sum(len(ids)//3 for vs,ids in model.values())
 bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'art/mantis'/('mantis_'+variant+'.blend')))
(ROOT/'art/mantis/variant-report.json').write_text(json.dumps(report,indent=2)+'\n')
