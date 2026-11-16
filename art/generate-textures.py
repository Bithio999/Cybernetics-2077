#!/usr/bin/env python3
"""Original scanner implant, workstation surfaces and diagnostic backdrop textures."""
from pathlib import Path
from PIL import Image, ImageDraw
import math
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/cybernetics2077'
for sub in ['textures/item','textures/block','textures/entity','textures/gui','models/item','models/block','blockstates']:(root/sub).mkdir(parents=True,exist_ok=True)
Image.new('RGBA',(1,1),'white').save(root/'textures/entity/white.png')
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
d.line([(9,19),(4,23),(4,27),(14,27),(17,23)],fill='#50697e',width=3)
d.line([(9,19),(4,23),(4,27),(14,27),(17,23)],fill='#8de4e9',width=1)
d.polygon([(7,10),(11,6),(21,6),(26,10),(28,15),(25,21),(20,24),(10,22),(6,17)],fill='#394658')
d.polygon([(8,10),(12,7),(21,7),(25,11),(25,18),(21,21),(11,20),(8,16)],fill='#ccd4d4')
d.polygon([(9,11),(13,8),(21,9),(23,12),(23,17),(20,20),(12,19),(9,16)],fill='#758896')
d.ellipse((11,10,22,20),fill='#132d40',outline='#eee3bb',width=1)
d.ellipse((13,11,21,19),fill='#39afb8',outline='#76edf2',width=1)
d.ellipse((16,12,20,16),fill='#b7ffff');d.rectangle((16,14,18,17),fill='#0a2535')
for x,y in [(9,9),(24,11),(21,22),(8,18)]:d.rectangle((x,y,x+1,y+1),fill='#e7b46f')
for y in [9,12,15]:d.line((26,y,29,y),fill='#d5b46c')
im.save(root/'textures/item/optical_scanner.png')
for name in ['bench_top','bench_side','bench_frame']:
 im=Image.new('RGBA',(32,32),'#182533');d=ImageDraw.Draw(im)
 for y in range(0,32,4):d.line((0,y,31,y),fill='#1e3040')
 d.rectangle((0,0,31,31),outline='#52616e',width=2)
 if name=='bench_top':
  d.rectangle((4,4,27,27),outline='#3d5663');d.line((7,14,24,14),fill='#a44a58');d.line((15,6,15,26),fill='#31515e')
  for x,y in [(3,3),(27,3),(3,27),(27,27)]:d.rectangle((x,y,x+1,y+1),fill='#a1adb1')
  d.rectangle((6,19,13,24),outline='#68d1d9');d.line((8,21,11,21),fill='#a8ffff')
 elif name=='bench_side':
  for y in [5,18]:d.rectangle((3,y,28,y+9),fill='#223343',outline='#64717b');d.line((12,y+4,20,y+4),fill='#d96874')
  d.rectangle((4,4,5,6),fill='#80dce1')
 else:
  for y in range(5,27,4):d.line((5,y,26,y),fill='#0c1825')
  d.line((3,2,3,29),fill='#5a7786')
 im.save(root/f'textures/block/{name}.png')
for name,color in [('red_glow',(197,34,65)),('cyan_glow',(31,174,194))]:
 im=Image.new('RGBA',(256,256));px=im.load()
 for y in range(256):
  for x in range(256):
   r=math.hypot((x-127.5)/128,(y-127.5)/128);px[x,y]=(*color,round(110*max(0,1-r)**2.2))
 im.save(root/f'textures/gui/{name}.png')
