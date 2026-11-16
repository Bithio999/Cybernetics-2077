"""Build the editable Blender source for the Cybernetics 2077 diagnostic body.
Run: blender -b --python art/blender/build_hologram.py
The source uses Minecraft's 8/12/12 proportions. Geometry, normals and UVs are
processed by Blender, then exported by export_hologram.py for the native renderer.
"""
import bpy, bmesh, math, random, sys
from pathlib import Path
from mathutils import Vector, noise
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(Path(__file__).resolve().parent))
random.seed(2077)
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
for col in list(bpy.data.collections):
 if col.name!='Collection':bpy.data.collections.remove(col)
scene=bpy.context.scene
scene.unit_settings.system='NONE'
COL=bpy.data.collections.new('01 • Anatomical body');scene.collection.children.link(COL)
# Roles: outer film, tissue, structural, neural, vascular, cortex, optics, dermal, seams, chip.
colors=[(.66,.36,.46,.30),(.45,.21,.29,.24),(.53,.56,.64,1),(.73,.57,.36,1),(.55,.23,.34,1),(.68,.63,.66,1),(.68,.72,.75,1),(.39,.58,.59,.25),(.72,.62,.67,.65),(.63,.48,.27,1),(.31,.29,.34,.58),(.49,.29,.36,.34)]
materials={}
for role,name in enumerate(['Dermal film','Muscle / fascia','Endoskeleton','Nerve plexus','Circulation','Cerebral cortex','Embedded optics','Subdermal mesh','Dermal sutures','Neural interface','Facial tissue mantle','Thoracic tissue mantle']):
 m=bpy.data.materials.new(name);m.diffuse_color=colors[role];m.use_nodes=True
 bs=m.node_tree.nodes.get('Principled BSDF');bs.inputs['Base Color'].default_value=colors[role];bs.inputs['Metallic'].default_value=.24 if role in [0,6,8,9] else .08;bs.inputs['Roughness'].default_value=.22 if role==0 else .36;bs.inputs['Alpha'].default_value=colors[role][3]
 if role==0:bs.inputs['Transmission Weight'].default_value=.45
 if role in [3,6,8]:bs.inputs['Emission Color'].default_value=colors[role];bs.inputs['Emission Strength'].default_value=.15
 m.surface_render_method='DITHERED';materials[role]=m

def tag(o,role,region):
 o['cyber_role']=role;o['cyber_region']=region;o.data.materials.clear();o.data.materials.append(materials[role])
 for old in list(o.users_collection):old.objects.unlink(o)
 COL.objects.link(o)
 if o.type=='MESH':
  for p in o.data.polygons:p.use_smooth=True
 return o

def mesh(name,verts,faces,role,region,uv=None):
 data=bpy.data.meshes.new(name);data.from_pydata(verts,[],faces);data.update()
 bm=bmesh.new();bm.from_mesh(data);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(data);bm.free()
 o=bpy.data.objects.new(name,data);COL.objects.link(o);tag(o,role,region)
 layer=data.uv_layers.new(name='Anatomical UV')
 for poly in data.polygons:
  for li in poly.loop_indices:
   vi=data.loops[li].vertex_index;v=data.vertices[vi].co
   layer.data[li].uv=uv[vi] if uv else (v.x*.15,v.z*.13)
 return o

def curve(name,pts,r,role,region,radii=None,cyclic=False):
 data=bpy.data.curves.new(name,'CURVE');data.dimensions='3D';data.resolution_u=8;data.bevel_depth=r;data.bevel_resolution=2;data.resolution_u=6
 s=data.splines.new('BEZIER');s.bezier_points.add(len(pts)-1);s.use_cyclic_u=cyclic
 for i,(p,co) in enumerate(zip(s.bezier_points,pts)):
  p.co=co;p.handle_left_type='AUTO';p.handle_right_type='AUTO';p.radius=radii[i] if radii else 1
 o=bpy.data.objects.new(name,data);COL.objects.link(o);return tag(o,role,region)

def sphere(name,center,scale,role,region,segments=24,rings=16):
 bpy.ops.mesh.primitive_uv_sphere_add(segments=segments,ring_count=rings,location=center)
 o=bpy.context.object;o.name=name;o.scale=scale;bpy.ops.object.transform_apply(location=False,rotation=False,scale=True);return tag(o,role,region)

def plate(name,center,size,bevel,role,region):
 bpy.ops.mesh.primitive_cube_add(size=1,location=center);o=bpy.context.object;o.name=name;o.scale=size;bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
 mod=o.modifiers.new('Surface fillet','BEVEL');mod.width=bevel;mod.segments=3
 n=o.modifiers.new('Machined face normals','WEIGHTED_NORMAL');n.keep_sharp=True
 return tag(o,role,region)

def surface(name,center,size,role,region,kind):
 """Six welded sculpted patches: flat Minecraft envelope, shaped facial/fascial surface."""
 c=Vector(center);half=Vector(size)/2;verts=[];faces=[];uv=[];N=20 if kind=='face' else 12
 for axis in range(3):
  for sign in [-1,1]:
   first=len(verts);a=(axis+1)%3;b=(axis+2)%3
   for i in range(N+1):
    for j in range(N+1):
     u=i/N*2-1;v=j/N*2-1;p=c.copy();p[axis]+=sign*half[axis];p[a]+=u*half[a];p[b]+=v*half[b]
     # Roll only the very edge, retaining the actual cuboid envelope and broad flat planes.
     edge=max(abs(u),abs(v));roll=.10*max(0,(edge-.88)/.12)**2
     p[axis]-=sign*roll
     if axis==1 and sign==-1:
      x=p.x-c.x;z=p.z-c.z
      if kind=='face':
       sockets=sum(math.exp(-((x-s*1.88)/.92)**4-((z+.38)/.53)**2) for s in [-1,1])
       brow=sum(math.exp(-((x-s*1.88)/1.15)**4-((z-.35)/.22)**2) for s in [-1,1])
       cheek=sum(math.exp(-((x-s*2.55)/.65)**2-((z+1.18)/.72)**2) for s in [-1,1])
       nose=math.exp(-(x/.44)**2-((z+.9)/.8)**2)
       lip=math.exp(-(x/1.3)**6-((z+2.2)/.12)**2)
       p.y+=.28*sockets-.08*brow-.06*cheek-.12*nose+.08*lip
      elif kind=='torso':
       pec=sum(math.exp(-((x-s*1.9)/1.5)**4-((z-2.1)/1.22)**2) for s in [-1,1])
       abs_=sum(math.exp(-((abs(x)-.82)/.65)**2-((z-zz)/.65)**2) for zz in [-.1,-1.6,-3.1])
       p.y-=.085*pec+.055*abs_
      elif kind=='limb':p.y-=.055*math.exp(-(x/1.3)**2-(z/3.5)**4)
     verts.append(tuple(p));uv.append(((p.x-c.x)/max(size[0],1)*.7+.5,(p.z-c.z)/max(size[2],1)*.7+.5))
   for i in range(N):
    for j in range(N):
     q=first+i*(N+1)+j;f=[q,q+N+1,q+N+2,q+1]
     va,vb,vc=[Vector(verts[k]) for k in f[:3]]
     if (vb-va).cross(vc-va)[axis]*sign<0:f.reverse()
     faces.append(f)
 o=mesh(name,verts,faces,role,region,uv)
 weld=o.modifiers.new('Weld dermal boundaries','WELD');weld.merge_threshold=.015
 return o

# Exact Minecraft body envelope: head8, torso12, legs12, arms4×12×4.
surface('HEAD • translucent facial dermis',(0,0,28),(8,8,8),0,3,'face')
surface('TORSO • thoracoabdominal fascia',(0,0,18),(8,4,12),0,8,'torso')
for side in [-1,1]:
 surface(('L' if side<0 else 'R')+' ARM • dermis',(side*6,0,18),(4,4,12),0,2,'limb')
 surface(('L' if side<0 else 'R')+' LEG • dermis',(side*2,0,6),(4,4,12),0,9,'limb')
# Shaped anatomical mantles live beneath the Minecraft silhouette. They provide
# real surface normals and facial volume rather than a skull illustration on a box.
def mantle(name,face):
 verts=[];faces=[];uv=[];N=48
 for i in range(N+1):
  t=i/N;z=(24.35+7.0*t) if face else (12.3+11.35*t)
  width=(2.02+1.18*math.sin(math.pi*t*.86)) if face else 3.72
  for j in range(N+1):
   u=j/N*2-1;x=u*width
   if face:
    y=-2.76-.44*math.sqrt(max(0,1-u*u))
    sockets=sum(math.exp(-((x-side*1.88)/.88)**4-((z-27.62)/.55)**2) for side in [-1,1])
    brows=sum(math.exp(-((x-side*1.8)/1.05)**4-((z-28.45)/.32)**2) for side in [-1,1])
    cheeks=sum(math.exp(-((x-side*2.15)/.8)**2-((z-26.75)/.75)**2) for side in [-1,1])
    bridge=math.exp(-(x/.36)**2-((z-27.7)/.9)**2)
    tip=math.exp(-(x/.48)**2-((z-26.8)/.32)**2)
    lips=math.exp(-(x/.9)**4-((z-25.68)/.22)**2)
    y+=.22*sockets-.18*brows-.2*cheeks-.37*bridge-.48*tip-.10*lips
   else:
    y=-1.25-.35*math.sqrt(max(0,1-u*u))
    pec=sum(math.exp(-((x-side*1.8)/1.5)**4-((z-21.7)/1.2)**2) for side in [-1,1])
    abs_=sum(math.exp(-((abs(x)-.88)/.7)**2-((z-zz)/.6)**2) for zz in [19.1,17.7,16.3])
    y-=.19*pec+.10*abs_
   verts.append((x,y,z));uv.append((j/N,t))
 for i in range(N):
  for j in range(N):
   ids=[i*(N+1)+j,i*(N+1)+j+1,(i+1)*(N+1)+j+1,(i+1)*(N+1)+j]
   c=sum((Vector(verts[k]) for k in ids),Vector())/4
   if face and any(((c.x-side*1.88)/.84)**2+((c.z-27.62)/.56)**2<1 for side in [-1,1]):continue
   faces.append(ids)
 obj=mesh(name,verts,faces,10 if face else 11,3 if face else 8,uv)
 if sum(p.normal.y*p.area for p in obj.data.polygons)>0:obj.data.flip_normals()
 return obj
mantle('FACE • shaped subdermal mask',True)
mantle('TORSO • shaped myofascial mantle',False)

# Finer surface sutures follow the reference's head/shoulder film, never a full wire cube.
for side in [-1,1]:
 for points in [
  [(side*2.9,-3.96,31.7),(side*2.86,-3.94,29.0),(side*3.25,-3.91,28.0)],
  [(side*3.6,-3.95,27.4),(side*2.8,-3.98,26.3),(side*1.7,-3.97,25.6),(side*.8,-3.97,24.5)],
  [(side*.1,-1.98,23.7),(side*2.0,-2.03,23.4),(side*3.8,-1.99,22.9)],
  [(side*5.95,-1.99,23.7),(side*6.9,-2.02,21.4),(side*6.8,-2.0,18.1)],
  [(side*1.9,-2.0,11.8),(side*2.6,-2.02,8.0),(side*2.9,-2.01,6.2),(side*2.75,-2.01,.4)]]:
  curve('Dermal incision / contour',points,.018,8,8)
 # Facial forms are films and recessed sockets, not a visible skull or exposed teeth.
 for x in [side*1.88]:
  socket=[(x-.94,-3.69,27.7),(x-.62,-3.79,28.05),(x+.62,-3.79,28.05),(x+.94,-3.69,27.7),(x+.62,-3.8,27.24),(x-.62,-3.8,27.24)]
  curve('Recessed orbital rim',socket,.075,1,3,cyclic=True)
  sphere('Optical globe • scleral substrate',(x,-3.28,27.62),(.73,.53,.60),9,3)
  # Iris is seated behind the dermis. Restrained scale, detailed aperture and microcontacts.
  for radius,thickness in [(.34,.027),(.255,.015),(.13,.016)]:
   points=[(x+math.cos(k*math.tau/48)*radius,-3.858,27.62+math.sin(k*math.tau/48)*radius) for k in range(48)]
   curve('Optical aperture • concentric microcontact',points,thickness,6,3,cyclic=True)
  sphere('Optical receiver',(x,-3.805,27.62),(.20,.055,.20),6,3)
  for k in range(12):
   a=k*math.tau/12
   curve('Optical radial circuit',[(x+math.cos(a)*.15,-3.875,27.62+math.sin(a)*.15),(x+math.cos(a)*.29,-3.875,27.62+math.sin(a)*.29)],.009,6,3)
  curve('Optic nerve',[(x,-2.9,27.6),(side*.9,-1.9,27.2),(side*.45,-.4,26.8)],.08,3,3)
  curve('Upper eyelid',[(x-.86,-3.96,27.6),(x-.42,-4.0,28.09),(x+.35,-4.0,28.09),(x+.86,-3.96,27.6)],.035,8,3)
 curve('Mandibular dermal seam',[(side*3.5,-3.92,26.4),(side*2.3,-3.96,24.7),(0,-3.97,24.45)],.02,8,3)
curve('Labial suture',[(-1.1,-3.98,25.58),(0,-4.02,25.52),(1.1,-3.98,25.58)],.024,8,3)
curve('Facial medial registration',[(0,-3.96,31.8),(0,-3.96,29.1),(0,-3.97,28.35)],.016,8,3)

# Cerebral surface: shaped hemispheric field with meandering physical sulci, no checker texture.
for side in [-1,1]:
 verts=[];faces=[];uv=[];NU=112;NV=64
 for i in range(NV+1):
  theta=math.pi*(i+.015)/(NV+.03)
  for j in range(NU):
   phi=j*math.tau/NU
   flow=phi+ .24*math.sin(theta*4+phi*2)+.12*math.sin(phi*5-theta*2)
   folds=math.sin(flow*13+theta*7+.8*math.sin(theta*8))
   cross=math.sin(theta*19+.7*math.sin(phi*7)+phi*2)
   groove=math.exp(-(folds/.24)**2)+.4*math.exp(-(cross/.22)**2)
   radius=1-.075*groove+.018*noise.noise_vector(Vector((math.sin(theta)*math.cos(phi)*5,math.sin(theta)*math.sin(phi)*5,math.cos(theta)*5)))[0]
   # Each medial wall reaches the midline: two lobed halves of one connected cortex.
   half_phi=j*math.pi/(NU-1)
   x=side*(.055+3.04*math.sin(theta)*math.sin(half_phi)*radius)
   y=.12+2.8*math.sin(theta)*math.cos(half_phi)*radius
   z=29.75+1.82*math.cos(theta)*radius
   verts.append((x,y,z));uv.append((j/NU,i/NV))
 for i in range(NV):
  for j in range(NU-1):faces.append((i*NU+j,i*NU+j+1,(i+1)*NU+j+1,(i+1)*NU+j))
 # Close the medial wall so Blender can resolve an unambiguous outward surface.
 for i in range(NV):faces.append((i*NU,(i+1)*NU,(i+1)*NU+NU-1,i*NU+NU-1))
 o=mesh('CORTEX • sculpted gyri '+str(side),verts,faces,5,0,uv)
 # Dense sculpt is retained in Blender; the export uses this controlled decimation modifier.
 d=o.modifiers.new('Runtime cortical topology','DECIMATE');d.ratio=.46
sphere('Interhemispheric bridge',(0,.8,29.0),(.5,.85,.35),5,0,32,16)
curve('Brain stem',[(0,1.2,28),(0,1.0,26.7),(0,.85,24)],.095,3,0)
# Cranial and cervical supports stay behind the face; there are no exposed teeth.
for side in [-1,1]:
 curve('Cranial arch',[(side*2.8,-2.8,26.1),(side*3.25,-2.3,28.5),(side*2.8,.1,31.0),(side*.25,.8,31.35)],.055,2,4)
 curve('Jaw support',[(side*2.9,-2.9,26.6),(side*2.4,-2.8,24.7),(0,-2.9,24.45)],.12,2,4)

# Vertebral column, arch/process geometry, delicate ribs and cartilage.
for i in range(23):
 z=12.5+i*.56;y=.9+.18*math.sin(i*.29)
 plate('Vertebral centrum %02d'%i,(0,y,z),(.62,.55,.34),.10,2,4)
 curve('Vertebral neural arch', [(-.45,y,z+.02),(-.38,y+.48,z+.05),(0,y+.65,z+.05),(.38,y+.48,z+.05),(.45,y,z+.02)],.075,2,4)
 for side in [-1,1]:curve('Transverse pedicle',[(side*.15,y+.24,z),(side*.70,y+.29,z-.08)],.055,2,4)
curve('Spinal cord',[(0,1.1,25.6),(0,1.2,22.0),(0,1.0,17.0),(0,.86,12.4)],.065,3,6)
for side in [-1,1]:
 for i in range(9):
  z=22.9-i*.68;width=2.7+.67*math.sin((i+1)*math.pi/11)
  pts=[(side*.4,1.1,z+.18),(side*width,.55,z+.13),(side*(width+.1),-.87,z-.15),(side*2.15,-1.48,z-.55),(side*.18,-1.56,z-.72)]
  curve('Costal arch %02d'%i,pts,.09 if i<7 else .065,2,4,[.65,1,1,.8,.55])
 curve('Clavicle',[(side*.18,-1.24,23.6),(side*1.9,-1.49,23.4),(side*3.65,-.55,23.4)],.15,2,4)
 curve('Iliac crest',[(side*.42,.5,14.2),(side*2.4,.6,14.4),(side*3.25,-.1,13.6),(side*1.8,-.95,12.6),(side*.4,-.84,13.3)],.22,2,4)
 # Layered upper costal laminae, corresponding to the reference's plated rib surfaces.
 for rib in range(4):
  z=22.7-rib*.68;verts=[];faces=[]
  for k in range(13):
   t=k/12;x=side*(.25+2.85*t);y=-1.64+.62*t*t;zz=z-.55*(1-t)
   for offset in [-.22,.22]:verts.append((x,y,zz+offset*math.sin(math.pi*t)**.35))
  for k in range(12):faces.append((k*2,k*2+1,k*2+3,k*2+2))
  obj=mesh('Costal lamina',verts,faces,2,4)
  if sum(p.normal.y*p.area for p in obj.data.polygons)>0:obj.data.flip_normals()
 # Iliac wing surface fills the crest: a pelvis, rather than two empty rim loops.
 verts=[(side*1.55,.15,13.45)];N=32
 for k in range(N):
  t=k*math.tau/N;verts.append((side*(1.65+1.30*math.cos(t)),.15+.5*math.cos(t),13.45+.78*math.sin(t)))
 obj=mesh('Iliac wing plate',verts,[(0,k+1,(k+1)%N+1) for k in range(N)],2,4)
 if sum(p.normal.y*p.area for p in obj.data.polygons)>0:obj.data.flip_normals()
 # Broad chest films and stratified muscle fibers sit above the bone substrate.
 sphere('Pectoral sheet',(side*1.95,-1.66,21.8),(1.66,.18,1.22),1,4,32,16)
 for i in range(8):
  z=21.0+i*.20
  curve('Pectoral fascial fibers',[(side*.22,-1.91,z),(side*1.4,-1.94,z+.16),(side*2.8,-1.72,22.3+i*.055)],.013,1,4)
 for i in range(3):sphere('Rectus fascial plate',(side*.91,-1.65,19.2-i*1.35),(.72,.17,.52),1,4)
for z in [18.2,18.9,19.6,20.3,21,21.7,22.4,23.1]:plate('Sternal segment',(0,-1.57,z),(.34,.19,.55),.07,2,4)
# Heart/vascular tree: integrated anatomical mass with branching flow.
for x,y,z,s in [(.63,-.5,20.15,(.44,.4,.72)),(1.1,-.25,20.65,(.38,.4,.48)),(.45,.03,20.7,(.33,.35,.38))]:sphere('Cardiac chamber',(x,y,z),s,4,7)
curve('Aortic arch',[(.7,-.1,20.2),(.7,-.2,21.7),(-.15,.1,22.1),(-.5,.5,20.9),(-.42,.6,14.4)],.14,4,7)
for side in [-1,1]:
 curve('Carotid / subclavian',[(side*.4,.2,21.8),(side*1.0,-.1,23.7),(side*.85,.1,26.5)],.065,4,7)
 curve('Brachial vascular route',[(side*.65,.1,22),(side*4.2,.2,23.2),(side*6.1,-.3,21.0),(side*6.25,-.4,17.3),(side*6.4,-.6,13)],.057,4,7)
 curve('Femoral vascular route',[(side*.45,.5,15.2),(side*1.4,.2,12.5),(side*2.35,-.35,8.8),(side*2.3,-.5,4.0),(side*2.2,-.6,.7)],.065,4,7)
# Embedded neural interface prepared for future OS implants, no oversized external machinery.
plate('Spinal interface',(0,1.55,22.1),(.68,.25,1.25),.10,9,1)
for i in range(7):curve('Interface fine traces', [(-.35,1.7,21.6+i*.17),(.35,1.7,21.6+i*.17)],.012,9,1)

for side in [-1,1]:
 # Anatomical limb scaffolds within unmodified cuboid exterior.
 x=side*6
 for a,b,r in [((x,0,23.4),(x,0,18.5),.25),((x-.34,0,18),(x-.45,-.12,13.6),.17),((x+.34,0,18),(x+.45,-.12,13.6),.14)]:curve('Arm bone • tapered', [a,tuple((Vector(a)+Vector(b))*.5),b],r,2,2,[1.25,.8,1.1])
 sphere('Humeral head',(x,0,23.3),(.65,.62,.58),2,2)
 sphere('Elbow articulation',(x,0,18.2),(.49,.45,.38),2,2)
 for i in range(4):
  px=x-.93+i*.62
  curve('Metacarpal',[(px,-.1,13.9),(px,-.4,13.1),(px,-.8,12.3)],.07,2,5)
  curve('Flexor tendon',[(px,-.48,15.8),(px,-.68,13.1),(px,-.95,12.2)],.018,1,5)
 curve('Thumb ray',[(x-side*.7,0,14.0),(x-side*1.38,-.45,13.3),(x-side*1.43,-.85,12.5)],.09,2,5)
 for c in [-.8,0,.8]:sphere('Deltoid / forearm fascicle',(x+c*.4,-1.4,21),(1.02,.18,2.12),1,2)
 lx=side*2
 curve('Femoral shaft',[(lx,0,11.4),(lx+.1,0,8.8),(lx,-.07,6.4)],.29,2,9,[1.35,.8,1.1])
 sphere('Patellar articulation',(lx,-.64,6.2),(.53,.34,.46),2,9)
 for sx,r in [(-.25,.21),(.55,.12)]:curve('Tibia / fibula',[(lx+sx,0,5.85),(lx+sx*.95,-.05,3.6),(lx+sx,-.12,.9)],r,2,9,[1.1,.8,1.1])
 for i in range(4):curve('Metatarsal',[(lx-.8+i*.5,-.2,.8),(lx-.8+i*.5,-1.1,.6),(lx-.8+i*.5,-1.5,.45)],.065,2,9)
 sphere('Quadriceps fascicle',(lx,-1.25,9.0),(1.28,.32,2.2),1,9)
 sphere('Shin fascia',(lx+.12,-1.3,3.6),(.76,.20,2.0),1,9)
 # Fine nerves branch at roots, then run through each limb.
 for i in range(11):
  z=23.1-i*.82
  curve('Thoracic nerve root',[(side*.23,1.35,z),(side*1.0,1.15,z-.16),(side*2.8,.45,z-.42)],.017,3,6)
 for pts in [[(side*.3,1.15,23.7),(side*3.2,.8,23.3),(x+.35,.55,20.3),(x+.48,.2,17),(x+.35,-.2,12.4)],[(side*.25,1.1,14),(side*1.65,.6,12),(lx+.6,.4,9),(lx+.7,.15,5.7),(lx+.5,-.2,.6)]]:
  curve('Peripheral neural bundle',pts,.027,3,6)
  for k in range(2,len(pts)):
   p=Vector(pts[k]);curve('Peripheral arbor', [tuple(p),tuple(p+Vector((side*.5,-.2,.25))),tuple(p+Vector((side*.85,-.55,.62)))],.012,3,6)
 # Subdermal hexagonal lattice becomes legible only in the dermal close-up.
 for i in range(6):
  for j in range(15):
   cx=x-1.52+i*.57+(j%2)*.285;z=14+j*.56
   if abs(cx-x)>1.65 or z>22.7:continue
   pts=[(cx+.29*math.cos(k*math.tau/6),-1.76,z+.29*math.sin(k*math.tau/6)) for k in range(6)]
   curve('Subdermal hex cell',pts,.012,7,8,cyclic=True)
   fill=[(cx,-1.78,z)]+[(cx+.265*math.cos(k*math.tau/6),-1.78,z+.265*math.sin(k*math.tau/6)) for k in range(6)]
   mesh('Subdermal hex lamina',fill,[(0,k+1,(k+1)%6+1) for k in range(6)],7,8)
   if (i+j)%3==0:
    curve('Subdermal contact',[(cx+.08*math.cos(k*math.tau/6),-1.79,z+.08*math.sin(k*math.tau/6)) for k in range(6)],.012,7,8,cyclic=True)

# Cyclic micro-surface map authored as a Blender Image; packed into the .blend.
N=256;image=bpy.data.images.new('Dermal micro-surface • packed',width=N,height=N,alpha=True)
pixels=[]
for y in range(N):
 for x in range(N):
  a=(math.sin(x*12.9898+y*78.233)*43758.5453)%1
  trace=math.exp(-((y-(90+18*math.sin(x/22)))/.75)**2)*(.35 if 24<x<220 else 0)
  pixels.extend([.48+(a-.5)*.14,trace,.32+.15*a,1])
image.pixels[:]=pixels;image.filepath_raw=str(ROOT/'src/main/resources/assets/cybernetics2077/textures/entity/dermal_detail.png');image.file_format='PNG';image.save();image.pack();image.filepath='//../../src/main/resources/assets/cybernetics2077/textures/entity/dermal_detail.png'
# Blender inspection cameras are part of the editable source, not shipped as screenshots.
scene.world.color=(.009,.012,.018)
for name,pos,target in [('Overview',(37,-80,26),(0,0,16)),('Face',(11,-36,29),(0,0,28)),('Cortex',(6,-34,31),(0,0,29)),('Spine',(10,38,24),(0,0,19))]:
 data=bpy.data.cameras.new(name);o=bpy.data.objects.new(name,data);scene.collection.objects.link(o);o.location=pos;o.rotation_euler=(Vector(target)-o.location).to_track_quat('-Z','Y').to_euler();data.type='ORTHO';data.ortho_scale=35 if name=='Overview' else 12
 if name=='Overview':scene.camera=o
for name,pos,color,energy,size in [('Rose key',(-15,-16,38),(1,.36,.45),2100,12),('Cyan rim',(18,5,13),(.25,.73,1),2600,10),('Pale face',(-2,-20,30),(1,.85,.9),1000,8)]:
 data=bpy.data.lights.new(name,'AREA');data.energy=energy;data.color=color;data.shape='DISK';data.size=size;o=bpy.data.objects.new(name,data);scene.collection.objects.link(o);o.location=pos;o.rotation_euler=(Vector((0,0,18))-o.location).to_track_quat('-Z','Y').to_euler()
scene.render.engine='CYCLES';scene.cycles.samples=32;scene.render.resolution_x=1200;scene.render.resolution_y=1200;scene.render.resolution_percentage=100
scene['reference']='User supplied Cyberpunk cyberware video, 00:21–01:05; original Minecraft-proportioned adaptation.'
scene['dimensions']='head 8x8x8; torso 8x4x12; arms 4x4x12; legs 4x4x12; local Z-up, front -Y.'
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'art/blender/cyberware_hologram.blend'))
from export_hologram import export
export(ROOT)
