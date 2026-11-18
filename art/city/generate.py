#!/usr/bin/env python3
"""Original 64px materials, baked furniture and audited vanilla jigsaw templates.
Run from any directory; no download, runtime renderer or external asset dependency.
"""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import json,random,sys,collections
from nbt import *
ROOT=Path(__file__).resolve().parents[2];RES=ROOT/'src/main/resources';A=RES/'assets/cybernetics2077';DATA=RES/'data/cybernetics2077'
def js(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n')
def texture(name,base,kind):
 im=Image.new('RGB',(64,64),base);d=ImageDraw.Draw(im)
 # Large coherent material regions; restrained wear rather than noisy microgeometry.
 dark=tuple(int(c*.55) for c in base);light=tuple(min(255,int(c*1.22)+8) for c in base)
 d.rectangle((0,0,63,63),outline=dark,width=2);d.line((2,2,61,2),fill=light)
 for x,y in [(4,4),(59,4),(4,59),(59,59)]:d.rectangle((x-1,y-1,x+1,y+1),fill=light)
 if kind=='floor':
  for y in range(8,60,12):
   for x in range(6+(y//12%2)*6,60,12):d.line((x,y,x+5,y-3),fill=light,width=2)
 elif kind=='tile':
  for n in (21,42):d.line((n,0,n,63),fill=dark,width=2);d.line((0,n,63,n),fill=dark,width=2)
 elif kind=='panel':
  d.rectangle((8,9,55,54),outline=dark,width=2);d.rectangle((12,14,51,44),fill=tuple(max(0,c-4) for c in base))
  for y in range(48,55,3):d.line((15,y,48,y),fill=dark)
 elif kind=='cabinet':
  d.line((1,20,62,20),fill=dark,width=2);d.line((1,42,62,42),fill=dark,width=2)
  for y in (11,32,53):d.rectangle((26,y,38,y+2),fill=(151,165,164))
 elif kind=='screen':
  d.rectangle((4,4,59,59),fill=(5,22,28),outline=(78,149,149),width=2)
  d.text((9,8),'BIO // 01',fill=(89,237,216))
  for y in (27,42):
   pts=[(7,y),(20,y),(24,y-7),(27,y+7),(31,y-2),(35,y),(56,y)]
   d.line(pts,fill=(75,232,195),width=2)
  d.line((8,54,42,54),fill=(188,112,65))
 elif kind in ('cyan','green','red'):
  col=(69,218,227) if kind=='cyan' else (241,91,101) if kind=='red' else (98,235,144)
  d.rectangle((1,22,62,42),fill=tuple(c//3 for c in col));d.rectangle((3,27,60,37),fill=col);d.line((4,30,59,30),fill=(211,255,224),width=2)
 elif kind=='counter':
  d.rectangle((4,8,59,55),outline=(60,81,79),width=2);d.line((2,6,61,6),fill=(108,233,166),width=3)
 elif kind=='shelf':
  for y in (9,34):
   d.rectangle((4,y+20,59,y+23),fill=(83,92,75))
   for x,col in [(8,(55,138,93)),(21,(180,127,63)),(35,(92,148,154)),(48,(124,85,103))]:
    d.rectangle((x,y+7,x+7,y+18),fill=col);d.rectangle((x+2,y+2,x+5,y+8),fill=col);d.line((x+1,y+12,x+6,y+12),fill=(203,193,152),width=2)
 elif kind in ('afterlife','ripperdoc'):
  # Three-metre marquee: texture aspect ratio preserves the lettering proportions.
  im=Image.new('RGB',(192,64),(8,23,26));d=ImageDraw.Draw(im)
  col=(112,255,169) if kind=='afterlife' else (81,235,229)
  d.rounded_rectangle((1,2,190,61),radius=5,outline=col,width=2)
  text='AFTERLIFE' if kind=='afterlife' else 'RIPPERDOC'
  font=ImageFont.load_default(size=23);box=d.textbbox((0,0),text,font=font)
  d.text(((192-(box[2]-box[0]))//2,21),text,font=font,fill=col,stroke_width=1)
  if kind=='ripperdoc':d.rectangle((92,6,99,18),fill=col);d.rectangle((88,10,103,14),fill=col)
  else:d.line((55,13,82,13,88,7,96,19,102,13,137,13),fill=col,width=2)
  d.text((64,50),'NIGHT CITY // 77',fill=(85,133,112),font=ImageFont.load_default(size=7))
 p=A/'textures/block'/f'{name}.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
materials={
 'reinforced_panel':((37,49,56),'panel'),'clinic_tile':((155,169,164),'tile'),
 'ribbed_floor':((49,57,59),'floor'),'medical_cabinet':((102,131,130),'cabinet'),
 'monitor_display':((13,33,36),'screen'),'chair_padding':((57,83,79),'panel'),
 'red_strip':((48,29,35),'red'),'cyan_strip':((28,49,56),'cyan'),'green_strip':((29,51,42),'green'),
 'bar_counter':((40,53,49),'counter'),'bar_stool':((70,48,41),'panel'),
 'ripperdoc_sign':((25,39,43),'ripperdoc'),'afterlife_sign':((25,39,43),'afterlife'),
 'bottle_shelf':((26,39,35),'shelf')}
for name,(b,k) in materials.items():texture(name,b,k)
def cube(a,b,tex,faces=None):
 out={'from':a,'to':b,'faces':{f:{'texture':'#'+tex,'uv':[0,0,16,16]} for f in (faces or ['down','up','north','south','west','east'])}}
 for f,axis,value in [('down',1,0),('up',1,16),('north',2,0),('south',2,16),('west',0,0),('east',0,16)]:
  edge=a[axis] if value==0 else b[axis]
  if f in out['faces'] and edge==value:out['faces'][f]['cullface']=f
 return out
def write_item_model(name,elements):
 bounds=[(min(e['from'][i] for e in elements),max(e['to'][i] for e in elements)) for i in range(3)]
 fit=min(1.,16/max(b-a for a,b in bounds));center=[(a+b)/2 for a,b in bounds]
 def transform(scale,rotation,translation):
  k=scale*fit
  # Rotate the centring offset before applying the display translation.
  import math
  x,y,z=[(8-c)*k for c in center];rx,ry,rz=map(math.radians,rotation)
  x,y=x*math.cos(rz)-y*math.sin(rz),x*math.sin(rz)+y*math.cos(rz)
  x,z=x*math.cos(ry)+z*math.sin(ry),-x*math.sin(ry)+z*math.cos(ry)
  y,z=y*math.cos(rx)-z*math.sin(rx),y*math.sin(rx)+z*math.cos(rx)
  return {'rotation':rotation,'translation':[round(v+t,4) for v,t in zip((x,y,z),translation)],'scale':[round(k,5)]*3}
 display={
  'gui':transform(.625,[30,225,0],[0,0,0]),
  'ground':transform(.25,[0,0,0],[0,3,0]),
  'fixed':transform(.5,[0,0,0],[0,0,0]),
  'thirdperson_righthand':transform(.375,[75,45,0],[0,2.5,0]),
  'thirdperson_lefthand':transform(.375,[75,225,0],[0,2.5,0]),
  'firstperson_righthand':transform(.4,[0,45,0],[0,0,0]),
  'firstperson_lefthand':transform(.4,[0,225,0],[0,0,0])}
 js(A/'models/item'/f'{name}.json',{'parent':'cybernetics2077:block/'+name,'display':display})
def model(name,elements,textures):
 for element in elements:
  lit_only=True
  for face in element['faces'].values():
   key=face['texture'].removeprefix('#')
   lit=key in ('light','lens') or (name.endswith('_sign') and key=='front') or (name.endswith('_strip') and key in ('side','run'))
   if lit or key=='screen':face['neoforge_data']={'block_light':15 if lit else 12,'sky_light':15 if lit else 12,'ambient_occlusion':False}
   lit_only=lit_only and lit
  if lit_only:element['shade']=False
 js(A/'models/block'/f'{name}.json',{'parent':'minecraft:block/block','ambientocclusion':True,'textures':{'particle':'cybernetics2077:block/reinforced_panel',**{k:'cybernetics2077:block/'+v for k,v in textures.items()}},'elements':elements})
 js(A/'blockstates'/f'{name}.json',{'variants':{'facing='+f:{'model':'cybernetics2077:block/'+name,'y':y} for f,y in [('north',0),('east',90),('south',180),('west',270)]}})
 write_item_model(name,elements)
 js(DATA/'loot_table/blocks'/f'{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'cybernetics2077:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
for name in ['reinforced_panel','clinic_tile','ribbed_floor','red_strip','cyan_strip','green_strip']:
 model(name,[cube([0,0,0],[16,16,16],'all')],{'all':name})
for name in ('medical_cabinet','bar_counter','bottle_shelf'):
 e=cube([0,0,0],[16,16,16],'metal');e['faces']['north']['texture']='#front'
 model(name,[e],{'metal':'reinforced_panel','front':name})
vent=[cube([0,0,1],[16,16,16],'metal'),cube([0,0,0],[16,2,1],'metal'),cube([0,14,0],[16,16,1],'metal'),cube([0,2,0],[2,14,1],'metal'),cube([14,2,0],[16,14,1],'metal')]
for y in (4,7,10,13):vent.append(cube([2,y,0],[14,y+1,1],'metal'))
model('vent_grille',vent,{'metal':'reinforced_panel'})
model('diagnostic_monitor',[cube([3,0,4],[13,1,13],'metal'),cube([7,1,8],[9,5,10],'metal'),cube([1,5,7],[15,14,10],'metal'),cube([2,6,6.8],[14,13,7],'screen')],{'metal':'reinforced_panel','screen':'monitor_display'})
model('surgical_chair',[
 cube([3,0,3],[13,2,14],'metal'),cube([5,2,6],[11,8,11],'metal'),
 cube([6,3,5],[7,9,12],'metal'),cube([9,3,5],[10,9,12],'metal'),
 cube([2,8,2],[14,10,13],'metal'),cube([3,10,2],[13,12,12],'pad'),
 cube([3,12,11],[13,22,14],'pad'),cube([4,22,11],[12,25,14],'pad'),
 cube([0,11,3],[2,13,12],'metal'),cube([14,11,3],[16,13,12],'metal'),
 cube([0,13,4],[2,14,7],'screen'),cube([2,4,12],[4,9,14],'metal')
],{'metal':'reinforced_panel','pad':'chair_padding','screen':'monitor_display'})
model('surgical_footrest',[cube([3,6,0],[13,8,16],'pad'),cube([6,0,2],[10,6,6],'metal'),cube([4,0,0],[12,1,8],'metal'),cube([4,6,13],[12,10,18],'metal')],{'metal':'reinforced_panel','pad':'chair_padding'})
model('ceiling_truss',[cube([0,12,0],[16,14,3],'metal'),cube([0,12,13],[16,14,16],'metal'),cube([0,14,0],[2,16,16],'metal'),cube([14,14,0],[16,16,16],'metal')],{'metal':'reinforced_panel'})
model('bar_stool',[cube([5,0,5],[11,1,11],'metal'),cube([7,1,7],[9,10,9],'metal'),cube([3,10,3],[13,12,13],'pad'),cube([4,4,4],[12,5,12],'metal')],{'metal':'reinforced_panel','pad':'bar_stool'})
model('surgical_light',[cube([6,14,6],[10,16,10],'metal'),cube([2,12,2],[14,14,14],'metal'),cube([3,11,3],[13,12,13],'light')],{'metal':'reinforced_panel','light':'cyan_strip'})
model('service_pipe',[cube([5,0,5],[11,16,11],'metal'),cube([4,0,4],[12,2,12],'metal'),cube([4,14,4],[12,16,12],'metal')],{'metal':'reinforced_panel'})
for name in ('ripperdoc_sign','afterlife_sign'):
 e=cube([-16,0,14],[32,16,16],'metal');e['faces']['north']['texture']='#front'
 model(name,[e],{'metal':'reinforced_panel','front':name})

from furnishings import build_furnishings
build_furnishings(A,model,cube)
for name in ('fixer_spawn_egg','ripperdoc_spawn_egg'):js(A/'models/item'/f'{name}.json',{'parent':'minecraft:item/template_spawn_egg'})
# Credit chip is pixel art at the same scale as existing implants.
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);d.polygon([(5,5),(25,5),(28,8),(28,25),(5,25)],fill='#24453f',outline='#7bdaa7');d.rectangle((11,9,22,21),fill='#c4a462');d.line((16,10,16,20),fill='#213c35',width=2)
for y in (13,17):d.line((13,y,20,y),fill='#213c35',width=2)
im.save(A/'textures/item/eurodollar_chip.png');js(A/'models/item/eurodollar_chip.json',{'parent':'minecraft:item/generated','textures':{'layer0':'cybernetics2077:item/eurodollar_chip'}})
from skins import build_skins
build_skins(A)

PANEL='reinforced_panel';FLOOR='ribbed_floor';TILE='clinic_tile'
CUSTOM=set(['reinforced_panel','clinic_tile','ribbed_floor','medical_cabinet','vent_grille','diagnostic_monitor','surgical_chair','surgical_footrest','ceiling_truss','surgical_light','service_pipe','red_strip','cyan_strip','green_strip','bar_counter','bar_stool','ripperdoc_sign','afterlife_sign','bottle_shelf','canopy_panel','ceiling_diffuser','recessed_panel','facade_duct','bar_corner','booth_seat'])
class Plan:
 def __init__(self,name,w,h,d):
  self.name,self.w,self.h,self.d=name,w,h,d;self.b={};self.npcs=[];self.goals=[];self.fixtures=[]
  self.fill(0,0,0,w-1,h-1,d-1,'air',role='cleared interior and roof envelope')
  self.fill(0,0,0,w-1,0,d-1,FLOOR,role='continuous foundation slab')
 def put(self,x,y,z,name,role='architecture',**props):
  if name in CUSTOM:props={'facing':'north',**props};name='cybernetics2077:'+name
  elif ':' not in name:name='minecraft:'+name
  assert 0<=x<self.w and 0<=y<self.h and 0<=z<self.d,(self.name,x,y,z)
  self.b[x,y,z]=(name,props,role,None)
 def fill(self,x,y,z,X,Y,Z,name,role='architecture',**props):
  for a in range(x,X+1):
   for b in range(y,Y+1):
    for c in range(z,Z+1):self.put(a,b,c,name,role,**props)
 def door(self,x,y,z):
  for n,half in enumerate(('lower','upper')):self.put(x,y+n,z,'dark_oak_door',role='complete usable doorway',facing='south',half=half,hinge='left',open='false',powered='false')
 def wall(self,x,y,z,X,Y,Z,name=PANEL):self.fill(x,y,z,X,Y,Z,name,role='continuous room enclosure')
 def furniture(self,x,y,z,name,facing='north'):
  self.put(x,y,z,name,role='purpose-built '+name.replace('_',' '),facing=facing);self.fixtures.append((x,y,z,name))
 def supplies(self,x,y,z,table):
  self.furniture(x,y,z,'barrel',facing='up')
  name,props,role,_=self.b[x,y,z]
  self.b[x,y,z]=(name,props,'accessible supply container on solid floor',{'id':S('minecraft:barrel'),'LootTable':S('cybernetics2077:chests/'+table)})
 def npc(self,x,y,z,kind,style=0):self.npcs.append((x+.5,y,z+.5,kind,style));self.goals.append((x,y,z))
 def connector(self,x,y,z):
  self.put(x,y,z,'jigsaw',role='village road connection',orientation='north_up')
  name,p,role,_=self.b[x,y,z];self.b[x,y,z]=(name,p,role,{'name':S('minecraft:building_entrance'),'target':S('minecraft:building_entrance'),'pool':S('minecraft:empty'),'final_state':S('minecraft:polished_deepslate'),'joint':S('aligned'),'id':S('minecraft:jigsaw')});self.entry=(x,y+1,z)
 def shell(self,front=2,walltop=5):
  self.front,self.walltop=front,walltop
  self.wall(0,1,front,0,walltop,self.d-1);self.wall(self.w-1,1,front,self.w-1,walltop,self.d-1)
  self.wall(0,1,self.d-1,self.w-1,walltop,self.d-1);self.wall(0,1,front,self.w-1,walltop,front)
  self.fill(0,walltop+1,front,self.w-1,walltop+1,self.d-1,PANEL,role='sealed structural roof')
  # Exposed roof edge and columns supply depth without holes or floating decoration.
  for x in (0,self.w-1):
   for z in range(front,self.d):
    if z in (front,self.d-1) or (z-front)%5==0:
     self.fill(x,1,z,x,walltop,z,'polished_deepslate',role='vertical structural pilaster connected to foundation and roof')
    elif self.name!='afterlife':
     # Set the continuous wall one block behind the pilasters, producing real facade depth.
     inner=1 if x==0 else self.w-2
     self.fill(inner,1,z,inner,walltop,z,TILE,role='sealed recessed clinical side wall')
     self.fill(x,1,z,x,walltop,z,'air',role='intentional exterior recess between structural pilasters')
  for x in range(1,self.w-1):self.put(x,walltop+1,front,'cyan_strip' if self.name!='afterlife' else 'green_strip',role='supported facade light cornice',facing='east')
 def window(self,x,z,width=3):
  self.fill(x,2,z,x+width-1,3,z,'light_blue_stained_glass',role='glazed window in sealed wall')
  self.fill(x,1,z,x+width-1,1,z,'polished_deepslate',role='solid window sill')
 def audit(self):
  assert len(self.b)==self.w*self.h*self.d
  # Every occupied block has a declared purpose. Furniture has immediate physical support.
  for (x,y,z),(n,p,r,t) in self.b.items():
   assert r and (y!=0 or n!='minecraft:air')
  for x,y,z,n in self.fixtures:
   support=(x,y+1,z) if n in ('surgical_light','ceiling_diffuser') else ((x,y,z+1) if n.endswith('_sign') else (x,y-1,z))
   assert support in self.b and self.b[support][0] not in ('minecraft:air','minecraft:light_blue_stained_glass'),(self.name,'unsupported',n,(x,y,z),support)
  for x,y,z,n in self.fixtures:
   if n.endswith('_sign'):
    for dx in (-1,0,1):assert self.b[x+dx,y,z+1][0]!='minecraft:air',(self.name,'marquee wing lacks wall backing')
   if n=='surgical_light':
    patients=[yy for yy in range(1,y) if self.b[x,yy,z][0] in ('cybernetics2077:surgical_chair','cybernetics2077:surgical_footrest')]
    assert patients and 1<(y+10/16)-(max(patients)+.5)<=2.5,(self.name,'task light is not positioned over a patient at working height')
   if n=='surgical_footrest':assert self.b[x,y,z+1][0]=='cybernetics2077:surgical_chair'
   if n=='surgical_chair':
    assert self.b[x,y,z-1][0]=='cybernetics2077:surgical_footrest'
    assert self.b[x,y+1,z][0]=='minecraft:air',(self.name,'chair headrest lacks space')
   if n=='diagnostic_monitor':assert self.b[x,y-1,z][0]=='cybernetics2077:medical_cabinet'
  for x,y,z,kind,style in self.npcs:
   for yy in (int(y),int(y)+1):assert self.b[int(x),yy,int(z)][0]=='minecraft:air',(self.name,'NPC body obstructed')
  def walk(p):
   x,y,z=p
   if not(0<=x<self.w and 1<=y<self.h-1 and 0<=z<self.d):return False
   def clear(k):
    n=self.b[k][0];return n in ('minecraft:air','minecraft:dark_oak_door','cybernetics2077:service_pipe')
   below=self.b[x,y-1,z][0]
   return clear((x,y,z)) and clear((x,y+1,z)) and below not in ('minecraft:air','minecraft:dark_oak_door')
  seen={self.entry};q=collections.deque(seen)
  while q:
   x,y,z=q.popleft()
   for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]:
    for dy in (0,1,-1):
     n=(x+dx,y+dy,z+dz)
     if n not in seen and walk(n):seen.add(n);q.append(n)
  for p in self.goals:assert p in seen,(self.name,'unreachable room/NPC',p)
  # Roof: no top-down exposed interior column. Exterior front apron is deliberately open.
  for x in range(1,self.w-1):
   for z in range(3,self.d-1):
    assert any(self.b[x,y,z][0] not in ('minecraft:air',) for y in range(5,self.h)),('roof',x,z)
  # Every non-air block connects to the foundation through local six-neighbour contacts.
  occupied={k for k,v in self.b.items() if v[0]!='minecraft:air'}
  connected={k for k in occupied if k[1]==0};q=collections.deque(connected)
  while q:
   x,y,z=q.popleft()
   for dx,dy,dz in [(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]:
    n=(x+dx,y+dy,z+dz)
    if n in occupied and n not in connected:connected.add(n);q.append(n)
  assert connected==occupied,(self.name,'disconnected blocks',occupied-connected)
  # Air must not leak through walls/roof. Doors and the intended bar entrance are sealed for this test.
  exterior={k for k in self.b if any((k[0]==0,k[0]==self.w-1,k[1]==self.h-1,k[2]==0,k[2]==self.d-1)) and k not in occupied}
  q=collections.deque(exterior)
  while q:
   x,y,z=q.popleft()
   for dx,dy,dz in [(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]:
    n=(x+dx,y+dy,z+dz)
    entrance=self.name=='afterlife' and n[2]==2 and 5<=n[0]<=7 and 3<=n[1]<=4
    if n in self.b and n not in occupied and n not in exterior and not entrance:exterior.add(n);q.append(n)
  assert all(g not in exterior for g in self.goals),(self.name,'unintended wall/roof air leak')
  def seals(k,axis):
   name,props,_,_=self.b[k]
   if name=='minecraft:air':return False
   if name=='minecraft:jigsaw':return True
   if name.startswith('minecraft:'):
    if name.endswith('_door'):return axis==(2 if props.get('facing') in ('north','south') else 0) and props.get('open')=='false'
    if name.endswith('_slab'):return axis==1 or props.get('type')=='double'
    if name.endswith(('_stairs','_bars','_fence')):return False
    return True
   local_axis=axis
   if props.get('facing') in ('east','west') and axis in (0,2):local_axis=2-axis
   model_data=json.loads((A/'models/block'/(name.split(':')[1]+'.json')).read_text())
   across=[n for n in range(3) if n!=local_axis]
   return any(all(e['from'][n]<=0 and e['to'][n]>=16 for n in across) and e['to'][local_axis]>e['from'][local_axis] for e in model_data['elements'])
  shell_checks=0
  for y in range(1,self.walltop+1):
   for z in range(self.front,self.d):
    for edge,inner in [(0,1),(self.w-1,self.w-2)]:
     assert seals((edge,y,z),0) or (self.name!='afterlife' and seals((inner,y,z),0)),(self.name,'geometric side-wall gap',(edge,y,z))
     shell_checks+=1
   for x in range(1,self.w-1):
    assert seals((x,y,self.d-1),2),(self.name,'geometric rear-wall gap',(x,y,self.d-1))
    entry=self.name=='afterlife' and 5<=x<=7 and 3<=y<=4
    if not entry:assert seals((x,y,self.front),2),(self.name,'geometric front-wall gap',(x,y,self.front))
    shell_checks+=2
  for x in range(1,self.w-1):
   for z in range(self.front+1,self.d-1):
    assert seals((x,self.walltop+1,z),1),(self.name,'geometric roof gap',(x,z))
    shell_checks+=1
  return {'template':self.name,'size':[self.w,self.h,self.d],'blocksReviewed':len(self.b),'fixtureSupportChecks':len(self.fixtures),'reachableDestinations':len(self.goals),'walkablePositions':len(seen),'connectedOccupiedBlocks':len(connected),'sealedEnvelope':True,'geometryShellSurfacesChecked':shell_checks,'roles':dict(collections.Counter(v[2] for v in self.b.values()))}
 def save(self):
  audit=self.audit();palette=[];index={};blocks=[]
  for (x,y,z),(name,props,role,tag) in sorted(self.b.items(),key=lambda kv:(kv[0][1],kv[0][2],kv[0][0])):
   key=(name,tuple(sorted(props.items())))
   if key not in index:
    index[key]=len(palette);v={'Name':S(name)}
    if props:v['Properties']=C({k:S(v) for k,v in props.items()})
    palette.append(v)
   v={'pos':L(3,[x,y,z]),'state':I(index[key])}
   if tag:v['nbt']=C(tag)
   blocks.append(v)
  entities=[]
  for x,y,z,kind,style in self.npcs:
   entities.append({'pos':L(6,[x,float(y),z]),'blockPos':L(3,[int(x),int(y),int(z)]),'nbt':C({'id':S('cybernetics2077:'+kind),'Pos':L(6,[x,float(y),z]),'Rotation':L(5,[180.,0.]),'PersistenceRequired':B(1),'CityStyle':I(style)})})
  write(DATA/'structure/village'/f'{self.name}.nbt',{'DataVersion':I(3955),'size':L(3,[self.w,self.h,self.d]),'palette':L(10,palette),'blocks':L(10,blocks),'entities':L(10,entities)})
  js(ROOT/'art/city/audits'/f'{self.name}.json',audit)
  # Exact block manifest: local context can be reviewed without opaque NBT tooling.
  js(ROOT/'art/city/audits'/f'{self.name}-fixtures.json',{'fixtures':self.fixtures,'NPCs':self.npcs,'entrance':self.entry,'destinations':self.goals})
  return audit

def clinic(large):
 p=Plan('clinic_large' if large else 'clinic_small',13 if large else 11,16 if large else 9,13);p.shell(walltop=12 if large else 5);mid=p.w//2
 p.fill(1,0,3,p.w-2,0,p.d-2,TILE,role='wipe-clean clinical floor')
 p.fill(1,1,p.d-1,p.w-2,3,p.d-1,TILE,role='sealed wipe-clean rear wall')
 p.door(mid,1,2);p.put(mid,3,2,TILE);p.window(2,2);p.window(p.w-5,2)
 p.furniture(mid,4,1,'ripperdoc_sign');p.fill(mid-1,5,0,mid+1,5,1,PANEL,role='supported entrance canopy joined to lintel')
 p.connector(mid,0,0);p.connector(2,0,0);p.connector(p.w-3,0,0)
 p.supplies(5 if large else 2,1,10,'clinic_large' if large else 'clinic_small')
 desk=9 if large else 3
 for x in range(desk-1,desk+2):p.furniture(x,1,5,'medical_cabinet')
 p.furniture(desk,2,5,'diagnostic_monitor',facing='south');p.npc(desk,1,6,'ripperdoc',1 if large else 0)
 p.goals += [(mid,1,4),(mid,1,10)]
 if large:
  # A genuine larger clinic inside village-sized lots: two floors, three treatment bays.
  p.fill(1,6,3,11,6,11,TILE,role='continuous load-bearing upper clinical floor')
  p.fill(2,6,3,3,6,9,'air',role='intentional stairwell opening')
  for step in range(6):
   y,z=step+1,step+4
   if y>1:p.fill(2,1,z,3,y-1,z,'polished_deepslate',role='solid supporting staircase wedge')
   p.fill(2,y,z,3,y,z,'polished_deepslate_stairs',role='continuous two-wide clinical staircase',facing='south',half='bottom',shape='straight',waterlogged='false')
  p.fill(4,7,3,4,7,9,'iron_bars',role='stairwell guardrail on solid upper-floor edge',east='false',west='false',north='true',south='true',waterlogged='false')
  for x in (6,10):
   p.furniture(x,7,8,'surgical_chair');p.furniture(x,7,7,'surgical_footrest');p.furniture(x,9,7,'surgical_light');p.fill(x,10,7,x,12,7,'service_pipe',role='continuous surgical-light suspension to roof')
   p.furniture(x,7,10,'medical_cabinet');p.furniture(x,8,10,'diagnostic_monitor')
   p.goals.append((x-1,7,9))
  p.furniture(9,1,9,'surgical_chair');p.furniture(9,1,8,'surgical_footrest');p.furniture(9,3,8,'surgical_light');p.fill(9,4,8,9,5,8,'service_pipe',role='task-light suspension to upper floor')
  for x in (2,8):p.fill(x,8,2,x+2,10,2,'light_blue_stained_glass',role='sealed upper-storey facade window')
  for x in (5,9):p.furniture(x,12,5,'ceiling_diffuser')
  p.goals.append((3,7,10))
 else:
  for z in (4,6):p.furniture(p.w-3,1,z,'bar_stool')
  p.furniture(8,1,9,'surgical_chair');p.furniture(8,1,8,'surgical_footrest');p.furniture(8,3,8,'surgical_light');p.fill(8,4,8,8,5,8,'service_pipe',role='task-light suspension centred over patient to roof');p.goals.append((7,1,9))
  for x in range(2,6):p.furniture(x,1,11,'medical_cabinet')
  p.furniture(3,2,11,'diagnostic_monitor')
 for x,z in ([(6,4),(6,10)] if large else [(3,7),(5,10)]):p.furniture(x,5,z,'ceiling_diffuser')
 for x in (2,p.w-3):p.put(x,4,12,'vent_grille',facing='south',role='rear service exhaust connected to roof plant above')
 if large:
  for x in (2,10):p.put(x,10,12,'vent_grille',facing='south',role='upper service exhaust in sealed rear wall')
 roof=13 if large else 6
 for x in (2,p.w-3):
  p.put(x,roof+1,10,PANEL,role='roof ventilation enclosure supported by roof');p.furniture(x,roof+2,10,'service_pipe')
 return p

def afterlife():
 p=Plan('afterlife',13,12,13);p.shell(front=2,walltop=6);mid=6
 # Street apron and a real recessed industrial threshold, not a glass-fronted shop.
 p.fill(0,0,0,12,2,1,'polished_deepslate',role='continuous street-height entrance terrace')
 p.fill(4,0,2,8,2,3,'polished_deepslate',role='supported entrance vestibule podium')
 p.fill(5,3,2,7,4,2,'air',role='wide bar entrance')
 p.fill(5,2,3,7,2,3,'deepslate_brick_stairs',role='first supported descending entry tread',facing='north',half='bottom',shape='straight',waterlogged='false')
 p.fill(5,1,4,7,1,4,'deepslate_brick_stairs',role='second supported descending entry tread',facing='north',half='bottom',shape='straight',waterlogged='false')
 p.fill(5,3,3,7,5,4,'air',role='stairwell headroom')
 for x in (4,8):p.fill(x,3,1,x,5,3,'polished_andesite',role='deep structural portal cheek supported by entrance podium')
 p.fill(4,5,1,8,5,1,'polished_deepslate',role='doorway lintel spanning between portal cheeks')
 p.furniture(mid,5,0,'afterlife_sign')
 p.connector(mid,2,0);p.connector(2,2,0);p.connector(10,2,0)
 # Ribbed service shell: real recessed panels, clamped duct bands and distinct rear bays.
 for x in (0,12):
  facing='west' if x==0 else 'east'
  for z in range(3,12):
   if z in (3,6,9,11):p.fill(x,1,z,x,6,z,'polished_andesite',role='continuous facade structural rib')
   else:
    for y in (3,4):p.put(x,y,z,'recessed_panel',facing=facing,role='inset steel wall bay between structural ribs')
   p.put(x,5,z,'facade_duct',facing=facing,role='continuous clamped exterior service duct with sealed wall backing')
 for x in range(1,12):
  if x in (1,4,8,11):p.fill(x,1,12,x,6,12,'polished_andesite',role='rear elevation structural pier')
  else:
   for y in (3,4):p.put(x,y,12,'recessed_panel',facing='south',role='deep rear service-wall recess')
  p.put(x,5,12,'facade_duct',facing='south',role='rear duct band joining facade service run')
 # A secondary exposed duct run frames the entrance, following the location artist's reference.
 for x in range(1,12):p.put(x,6,1,'facade_duct',role='header service pipe mounted to solid entrance wall')
 for x in (1,2,10,11):
  for y in (3,4):p.put(x,y,2,'recessed_panel',role='sealed inset equipment panel beside entrance')
 for x in range(13):p.put(x,7,2,'polished_andesite',role='continuous solid lintel cornice below parapet')
 # An elongated coffin-shaped island bar. Centre cells are a clear bartender aisle.
 for z in range(6,11):
  for x in (3,5):p.furniture(x,1,z,'bar_counter')
 for z in (6,10):p.furniture(4,1,z,'bar_counter')
 for x,z,f in [(3,6,'north'),(5,6,'east'),(5,10,'south'),(3,10,'west')]:
  p.put(x,1,z,'bar_corner',facing=f,role='chamfered island-bar end with continuous light channel')
 for x,z,f in [(2,7,'east'),(2,9,'east'),(6,7,'west'),(6,9,'west')]:p.furniture(x,1,z,'bar_stool',facing=f)
 # The luminous canopy uses neutral opal diffusers and a single aligned lime edge, not cube UVs.
 for z in (6,10):p.fill(4,2,z,4,6,z,'polished_deepslate',role='bar service column connecting counter canopy and roof')
 for x in range(3,6):
  for z in range(6,11):
   if not(x==4 and z in (6,10)):p.put(x,5,z,'canopy_panel',role='linked opal canopy suspended between structural bar columns')
 # Glass-fronted side niches and discreet cove lighting reference the former morgue interior.
 for z in (5,7,9,11):p.put(0,3,z,'bottle_shelf',facing='east',role='recessed backbar display in sealed side wall')
 for z in (5,10):
  p.fill(1,6,z,11,6,z,'ceiling_truss',role='roof-mounted open truss spanning side walls')
  for x in (1,7,10):p.furniture(x,6,z,'ceiling_diffuser')
 # Enclosed fixer alcoves remain accessible from a two-block-wide concourse.
 for z in (5,8):p.wall(8,1,z,11,3,z)
 for z in (6,7,9,10,11):p.furniture(11,1,z,'booth_seat',facing='west')
 for z,style in ((7,0),(11,1)):
  p.furniture(9,1,z-1,'bar_counter');p.npc(10,1,z,'fixer',style);p.goals.append((8,1,z))
  p.put(12,4,z,'green_strip',role='recessed meeting-booth cove lighting')
 p.supplies(1,1,10,'afterlife')
 # Stepped parapets and substantial rooftop mechanical cases finish every elevation.
 for x in range(13):
  for z in (2,12):p.put(x,8,z,'polished_andesite_slab',type='bottom',waterlogged='false',role='roof-edge cap supported continuously by roof cornice')
 for z in range(3,12):
  for x in (0,12):p.put(x,8,z,'polished_andesite_slab',type='bottom',waterlogged='false',role='side parapet on continuous roof slab')
 for x in (2,10):
  p.fill(x-1,8,8,x+1,10,11,PANEL,role='solid mechanical roof housing on sealed roof deck')
  p.put(x,9,8,'vent_grille',role='air-handler intake inside mechanical housing')
  p.furniture(x,11,10,'service_pipe')
 p.goals += [(7,1,6),(7,1,11),(1,1,8)]
 return p

reports=[clinic(False).save(),clinic(True).save(),afterlife().save()]
for name in ('clinic_small','clinic_large','afterlife'):
 js(DATA/'worldgen/template_pool/village'/f'{name}.json',{'fallback':'minecraft:empty','elements':[{'weight':1,'element':{'element_type':'cybernetics2077:supported_building','location':'cybernetics2077:village/'+name,'processors':[],'projection':'rigid'}}]})
for name,lo,hi in [('clinic_small',1,3),('clinic_large',2,4),('afterlife',2,5)]:
 js(DATA/'loot_table/chests'/f'{name}.json',{'type':'minecraft:chest','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'cybernetics2077:eurodollar_chip','functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':lo,'max':hi}}]}]},{'rolls':2,'entries':[{'type':'minecraft:item','name':'minecraft:iron_ingot','weight':2},{'type':'minecraft:item','name':'minecraft:redstone','weight':2},{'type':'minecraft:item','name':'minecraft:paper','weight':1}]}]})
# Mineable tags include every city block; all drops are explicit, explosion-safe loot tables.
blocks=list(CUSTOM)
js(RES/'data/minecraft/tags/block/mineable/pickaxe.json',{'replace':False,'values':['cybernetics2077:cybernetics_bench']+['cybernetics2077:'+n for n in sorted(blocks)]})
langpath=A/'lang/en_us.json';lang=json.loads(langpath.read_text())
for name in blocks:lang['block.cybernetics2077.'+name]=name.replace('_',' ').title()
lang.update({'entity.cybernetics2077.ripperdoc':'Ripperdoc','entity.cybernetics2077.fixer':'Afterlife Fixer','item.cybernetics2077.ripperdoc_spawn_egg':'Ripperdoc Spawn Egg','item.cybernetics2077.fixer_spawn_egg':'Fixer Spawn Egg','item.cybernetics2077.eurodollar_chip':'Eurodollar Credit Chip','cybernetics2077.city.chip.description':'Use to deposit %s eddies.','cybernetics2077.city.deposit':'Deposit received. Balance: €$ %s','cybernetics2077.city.balance':'%s: €$ %s','cybernetics2077.city.doctor':'Dr. Vale · Ripperdoc','cybernetics2077.city.doctor_iris':'Dr. Iris · Ripperdoc','cybernetics2077.city.fixer_mara':'Mara Voss · Fixer','cybernetics2077.city.fixer_ren':'Ren Calder · Fixer'})
js(langpath,lang)
print(json.dumps(reports,indent=2))
