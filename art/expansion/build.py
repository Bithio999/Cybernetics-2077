#!/usr/bin/env python3
"""Original 32px implant sprites and their tier models; no recipes are generated."""
from pathlib import Path
import json
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[2]
ASSET=ROOT/'src/main/resources/assets/cybernetics2077'
FAMILIES=[
 ('ram_upgrade','RAM Upgrade',1,'Frontal Cortex',8),
 ('self_ice','Self-ICE',3,'Frontal Cortex',5),
 ('kiroshi_doomsayer_optics','Kiroshi “Doomsayer” Optics',1,'Face',6),
 ('kiroshi_stalker_optics','Kiroshi “Stalker” Optics',1,'Face',6),
 ('dense_marrow','Dense Marrow',2,'Skeleton',16),
 ('para_bellum','Para Bellum',3,'Skeleton',25),
 ('stabber','Stabber',2,'Nervous System',12),
 ('visual_cortex_support','Visual Cortex Support',1,'Nervous System',16),
 ('biomonitor','Biomonitor',1,'Circulatory System',14),
 ('second_heart','Second Heart',4,'Circulatory System',30),
 ('shock_n_awe','Shock-n-Awe',1,'Integumentary System',25),
 ('pain_editor','Pain Editor',4,'Integumentary System',35),
 ('lynx_paws','Lynx Paws',2,'Legs',5),
 ('jenkins_tendons','Jenkins’ Tendons',2,'Legs',6),
]
DARK='#172d38'; STEEL='#4c7382'; LIGHT='#a7cbd1'; CYAN='#59e5df'; GOLD='#d8ab62'; RED='#e96e77'
def sprite(name):
 im=Image.new('RGBA',(32,32)); d=ImageDraw.Draw(im)
 def rect(box,color=STEEL): d.rectangle(box,fill=color)
 def line(points,color=LIGHT,w=1): d.line(points,fill=color,width=w)
 def poly(points,color=STEEL): d.polygon(points,fill=color)
 def ring(box,fill=CYAN):
  d.ellipse(box,fill=DARK,outline=LIGHT,width=1)
  x,y,r,b=box;d.ellipse((x+2,y+2,r-2,b-2),outline=fill,width=2)
 if name=='ram_upgrade':
  poly([(5,6),(23,6),(27,10),(27,23),(5,23)])
  rect((8,9,13,17),DARK);rect((17,9,22,17),DARK)
  for x in range(8,25,3): rect((x,23,x+1,26),GOLD)
  for y in (10,13,16):line([(9,y),(12,y)],CYAN);line([(18,y),(21,y)],CYAN)
  line([(6,7),(22,7)]);line([(7,20),(24,20)],GOLD)
 elif name=='self_ice':
  poly([(6,6),(15,3),(25,7),(25,17),(21,24),(15,29),(8,23),(5,16)],DARK)
  line([(6,6),(15,3),(25,7),(25,17),(21,24),(15,29),(8,23),(5,16),(6,6)],LIGHT)
  rect((11,13,20,21),STEEL);d.arc((12,7,19,17),180,360,fill=CYAN,width=2)
  rect((15,16,16,18),GOLD);line([(8,8),(8,16),(12,22)],CYAN)
 elif 'optics' in name:
  poly([(3,13),(8,8),(23,8),(29,13),(29,20),(23,24),(8,23),(3,19)],STEEL)
  line([(4,13),(9,9),(23,9),(27,13)]);rect((5,14,9,18),DARK)
  ring((10,10,24,24),GOLD if 'doomsayer' in name else CYAN)
  rect((14,14,20,20),DARK)
  if 'doomsayer' in name:
   poly([(17,13),(21,20),(13,20)],GOLD);rect((17,16,17,17),DARK)
  else:
   line([(17,12),(17,21)],CYAN);line([(12,17),(22,17)],CYAN)
  line([(6,25),(11,28),(20,28)],GOLD)
 elif name=='dense_marrow':
  poly([(9,4),(13,3),(17,7),(16,12),(23,22),(27,23),(28,27),(24,29),(20,26),(18,23),(11,15),(7,13),(4,9),(5,5)],LIGHT)
  poly([(10,9),(13,9),(22,23),(20,24)],STEEL)
  for y in range(10,24,4):rect((y-1,y+1,y+2,y+3),GOLD)
  line([(7,5),(10,5)]);line([(23,25),(26,26)],CYAN)
 elif name=='para_bellum':
  poly([(7,5),(12,3),(20,3),(25,6),(28,14),(24,28),(8,28),(4,15)],DARK)
  for x in (6,18):
   poly([(x,9),(x+7,7),(x+8,17),(x+5,25),(x+1,23)],STEEL)
   line([(x+1,10),(x+5,9),(x+6,16)],LIGHT)
  line([(15,5),(15,25)],GOLD,2);line([(8,19),(12,20)],CYAN);line([(20,20),(24,19)],CYAN)
 elif name=='stabber':
  poly([(14,3),(20,3),(22,7),(20,25),(17,29),(12,25),(10,8)],DARK)
  rect((14,6,18,24),STEEL);line([(16,7),(16,23)],CYAN,2)
  for y in (9,15,21):
   line([(13,y),(8,y-2),(6,y+1)],GOLD,2);line([(19,y),(24,y-2),(26,y+1)],LIGHT,2)
  poly([(16,3),(19,7),(17,12),(16,8),(14,10)],RED)
 elif name=='visual_cortex_support':
  poly([(6,6),(14,3),(24,6),(28,14),(26,22),(18,27),(8,25),(3,16)],DARK)
  ring((9,8,24,23));line([(5,10),(8,11)],GOLD,2)
  line([(4,17),(7,17),(9,22),(13,25)],STEEL,2)
  rect((15,13,18,18),GOLD);line([(26,10),(29,8)],LIGHT);line([(25,23),(28,26)],CYAN)
 elif name=='biomonitor':
  poly([(8,3),(22,3),(27,8),(27,24),(22,28),(8,28),(4,24),(4,8)],STEEL)
  rect((7,8,24,22),DARK);line([(8,16),(11,16),(13,11),(16,20),(18,14),(21,16),(23,16)],CYAN)
  rect((10,4,19,5),LIGHT);rect((10,25,19,26),GOLD)
 elif name=='second_heart':
  poly([(8,8),(12,5),(17,8),(21,5),(27,9),(28,17),(22,25),(16,29),(7,22),(3,14)],RED)
  poly([(8,10),(12,8),(16,12),(14,22),(9,21),(6,15)],STEEL)
  line([(18,10),(24,9),(25,15),(20,23)],LIGHT,2)
  line([(9,6),(9,2),(14,2)],GOLD,2);line([(22,5),(22,2),(27,2)],STEEL,2)
  rect((15,14,19,18),DARK);line([(16,15),(18,17)],CYAN)
 elif name=='shock_n_awe':
  poly([(7,4),(24,4),(28,9),(26,25),(21,28),(8,26),(3,20),(4,9)],STEEL)
  poly([(9,7),(23,7),(24,22),(18,25),(8,22),(6,17)],DARK)
  poly([(17,7),(11,17),(16,17),(13,25),(23,13),(18,13),(22,7)],GOLD)
  line([(7,10),(7,14)],CYAN,2);line([(24,18),(23,22)],CYAN,2)
 elif name=='pain_editor':
  poly([(7,4),(23,4),(27,8),(27,24),(23,28),(7,28),(3,24),(3,8)],STEEL)
  rect((7,8,23,23),DARK)
  line([(9,13),(12,13),(14,18),(17,11),(20,16),(22,16)],RED,2)
  line([(6,7),(24,7)],LIGHT);rect((7,25,10,26),GOLD);rect((20,25,23,26),GOLD)
 elif name=='lynx_paws':
  poly([(10,3),(19,3),(21,16),(25,21),(28,22),(28,27),(8,27),(5,23),(8,15)],STEEL)
  poly([(13,5),(17,5),(18,17),(21,21),(13,21),(10,18)],DARK)
  line([(8,24),(25,24)],LIGHT,2);line([(12,7),(12,15),(10,19)],CYAN)
  for x in (11,17,23):rect((x,28,x+2,29),GOLD)
 else:
  poly([(11,2),(20,2),(21,15),(18,20),(21,25),(27,26),(28,29),(9,29),(6,25),(11,16)],STEEL)
  rect((13,4,17,13),DARK);line([(12,16),(17,19),(14,24)],GOLD,2)
  line([(10,26),(24,27)],LIGHT,2);line([(5,7),(8,7),(7,12)],CYAN)
  line([(2,13),(6,13),(4,19)],CYAN)
 return im

def build():
 langfile=ASSET/'lang/en_us.json';lang=json.loads(langfile.read_text())
 for ident,title,first,category,capacity in FAMILIES:
  sprite(ident).save(ASSET/'textures/item'/f'{ident}.png')
  sprite(ident).save(ROOT/'docs/wiki/assets/icons'/f'{ident}.png')
  lang['cybernetics2077.augment.'+ident]=title
  for t in range(first,6):
   item=f'{ident}_tier_{t}'
   lang['item.cybernetics2077.'+item]=title
   (ASSET/'models/item'/f'{item}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'cybernetics2077:item/'+ident}},indent=2)+'\n')
 lang.update({'cybernetics2077.self_ice.blocked':'Self-ICE neutralized an incoming quickhack.',
  'cybernetics2077.self_ice.rejected':'Quickhack neutralized by Self-ICE.',
  'cybernetics2077.second_heart.revived':'Second Heart restored your vital signs.',
  'death.attack.cybernetic_discharge':"%1$s was shocked by %2$s’s cyberware",
  'death.attack.cybernetic_discharge.player':"%1$s was shocked by %2$s’s cyberware"})
 langfile.write_text(json.dumps(lang,ensure_ascii=False,indent=2)+'\n')
if __name__=='__main__':build()
