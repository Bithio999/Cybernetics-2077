"""Original Minecraft-scale implant sprites and variant atlases; no recipes."""
from pathlib import Path
import json
from PIL import Image,ImageDraw,ImageOps
ROOT=Path(__file__).resolve().parents[2];ASSET=ROOT/'src/main/resources/assets/cybernetics2077'
FAMILIES=[('smart_link','Smart Link',1),('ballistic_coprocessor','Ballistic Coprocessor',1),('microgenerator','Microgenerator',1),('handle_wrap','Handle Wrap',3),('shock_absorber','Shock Absorber',1),('immovable_force','Immovable Force',2)]
DARK='#152d38';STEEL='#547886';LIGHT='#bdd9dc';CYAN='#62dedb';GOLD='#d8aa64'
langpath=ASSET/'lang/en_us.json';lang=json.loads(langpath.read_text())
for index,(name,title,first) in enumerate(FAMILIES):
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 if name in ('smart_link','handle_wrap','immovable_force'):
  d.polygon([(8,13),(8,7),(11,5),(13,8),(13,3),(16,2),(18,6),(19,4),(22,5),(22,12),(25,10),(28,12),(25,20),(23,26),(12,29),(6,23),(3,17),(5,14)],fill=STEEL,outline=LIGHT)
  for x in (11,16,21):d.line([(x,8),(x,17)],fill=DARK,width=2)
  if name=='smart_link':
   d.ellipse((11,15,23,27),fill=DARK,outline=CYAN,width=2);d.line((17,17,17,24),fill=LIGHT);d.line((13,21,21,21),fill=LIGHT)
  elif name=='handle_wrap':
   for y in (13,18,23):d.polygon([(6,y),(24,y-3),(25,y),(8,y+4)],fill=GOLD,outline=DARK)
  else:
   d.rectangle((10,13,23,25),fill=DARK,outline=GOLD,width=2)
   for x in (13,19):d.line((x,15,x,23),fill=LIGHT,width=2)
 elif name=='ballistic_coprocessor':
  d.rectangle((7,7,25,25),fill=DARK,outline=LIGHT,width=2)
  for x in (10,15,20):
   for y in (3,27):d.rectangle((x,y,x+1,y+3),fill=GOLD)
  d.line([(10,20),(18,12),(23,17)],fill=CYAN,width=2);d.polygon([(23,17),(19,17),(23,21)],fill=CYAN)
 elif name=='microgenerator':
  d.polygon([(7,5),(22,5),(28,12),(25,25),(9,28),(3,22),(4,10)],fill=STEEL,outline=LIGHT)
  for x in (8,17):
   d.rectangle((x,9,x+6,22),fill=DARK,outline=GOLD)
   for y in range(11,22,3):d.line((x+1,y,x+5,y),fill=CYAN)
 else:
  for x in (5,18):
   d.rectangle((x,5,x+8,25),fill=DARK,outline=LIGHT)
   d.line((x+4,3,x+4,28),fill=GOLD,width=2)
   for y in range(9,23,4):d.line((x+1,y,x+6,y+2),fill=STEEL,width=2)
 im.save(ASSET/'textures/item'/f'{name}.png');im.save(ROOT/'docs/wiki/assets/icons'/f'{name}.png')
 lang['cybernetics2077.augment.'+name]=title
 for t in range(first,6):
  ident=f'{name}_tier_{t}';lang['item.cybernetics2077.'+ident]=title
  (ASSET/'models/item'/f'{ident}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'cybernetics2077:item/'+name}},indent=2)+'\n')
base=Image.open(ASSET/'textures/entity/mantis_blender.png').convert('RGBA')
for variant,color in [('thermal',(255,125,42)),('electrical',(75,195,255)),('toxic',(150,218,67))]:
 # Preserve baked surface detail and UVs; new hardware uses four dedicated swatches.
 gray=ImageOps.grayscale(base).convert('RGB');grade=Image.new('RGB',base.size,color)
 if variant=='thermal':
  tint=Image.blend(base.convert('RGB'),Image.new('RGB',base.size,(19,25,30)),.52)
 elif variant=='electrical':
  tint=Image.blend(base.convert('RGB'),Image.new('RGB',base.size,(156,191,205)),.27)
 else:
  tint=Image.blend(base.convert('RGB'),Image.new('RGB',base.size,(74,85,43)),.40)
 atlas=Image.new('RGBA',(512,544));atlas.paste(tint,(0,0));atlas.putalpha(255)
 draw=ImageDraw.Draw(atlas)
 for k,c in enumerate([(24,34,42),(183,209,215),color,tuple(min(255,int(v*.5+125)) for v in color)]):draw.rectangle((k*128,512,k*128+127,543),fill=(*c,255))
 atlas.save(ASSET/'textures/entity'/f'mantis_{variant}.png')
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 profile={
  'thermal':[(4,25),(8,29),(16,23),(22,17),(29,7),(28,3),(24,4),(14,13),(9,19)],
  'electrical':[(4,25),(7,28),(14,21),(17,15),(26,6),(26,3),(23,5),(14,14),(10,19)],
  'toxic':[(4,25),(8,29),(15,21),(19,19),(18,16),(23,14),(22,11),(27,8),(29,3),(24,5),(14,14),(9,20)]
 }[variant]
 d.polygon(profile,fill=({'thermal':'#535355','electrical':LIGHT,'toxic':'#8b9872'}[variant]),outline=STEEL)
 d.line([(6,26),(12,20),(17,17)],fill=GOLD,width=2)
 if variant=='thermal':
  for x,y in [(13,18),(17,14),(21,10)]:d.line((x-1,y-2,x+3,y),fill=color,width=2)
 elif variant=='electrical':
  d.line([(18,17),(29,6),(29,3)],fill=CYAN,width=2)
  d.line([(12,20),(15,18),(15,15),(20,11),(20,9),(26,4)],fill=color,width=2)
  for x,y in [(6,22),(9,19)]:d.ellipse((x-1,y-2,x+2,y+1),outline=color)
 else:
  d.ellipse((4,21,8,28),fill=color,outline=DARK)
  for x,y in [(16,16),(20,12),(24,8)]:d.polygon([(x,y),(x+3,y),(x+1,y+3)],fill=color)
 im.save(ASSET/'textures/item'/f'{variant}_mantis_blades.png');im.save(ROOT/'docs/wiki/assets/icons'/f'{variant}_mantis_blades.png')
 title={'thermal':'Thermal','electrical':'Electrifying','toxic':'Toxic'}[variant]+' Mantis Blades'
 for t in range(2,6):
  ident=f'{variant}_mantis_blades_tier_{t}';lang['item.cybernetics2077.'+ident]=title
  (ASSET/'models/item'/f'{ident}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'cybernetics2077:item/{variant}_mantis_blades'}},indent=2)+'\n')
for ident in ['mantis_thermal','mantis_electrical','mantis_toxic','mantis_bleed','hand_discharge']:
 lang['death.attack.'+ident]='%1$s was overcome by cyberware'
 lang['death.attack.'+ident+'.player']='%1$s was overcome by %2$s’s cyberware'
# One small soft ribbon texture, shared by all articulated elemental effects.
# No persistent particles or per-frame texture generation.
import math
fx=Image.new('RGBA',(64,32));pixels=fx.load()
for y in range(32):
 for x in range(64):
  u=x/63;v=y/31
  core=max(0,1-abs(v-.5)*2)**2.4
  taper=math.sin(math.pi*u)**.55
  pixels[x,y]=(255,255,255,round(235*core*taper))
fx.save(ASSET/'textures/entity/mantis_elemental.png')
langpath.write_text(json.dumps(lang,ensure_ascii=False,indent=2)+'\n')
