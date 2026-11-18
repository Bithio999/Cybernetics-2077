#!/usr/bin/env python3
"""Original 32px expansion sprites: simple Minecraft clusters, a distinct silhouette per implant."""
from pathlib import Path
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/cybernetics2077/textures/item'
O='#14212b';D='#314651';M='#728c91';S='#b7c9c5';W='#e3e3cf';G='#b88941';Y='#ebc66b';C='#69d6d5';R='#bc4d46'
def canvas():
 im=Image.new('RGBA',(32,32));return im,ImageDraw.Draw(im)
def save(im,name):im.save(root/(name+'.png'))
def chip(d,x,y,w,h):
 d.polygon([(x+3,y),(x+w-3,y),(x+w,y+3),(x+w,y+h-3),(x+w-3,y+h),(x,y+h),(x,y+3)],fill=O)
 d.rectangle((x+2,y+3,x+w-2,y+h-2),fill=D)
 for a in range(x+4,x+w-2,4):d.rectangle((a,y-2,a+1,y),fill=Y);d.rectangle((a,y+h,a+1,y+h+2),fill=G)
# Disk wafer and separate RAM daughterboard.
im,d=canvas();chip(d,5,6,22,20);d.ellipse((9,10,23,24),fill=M);d.ellipse((11,11,21,21),fill=S);d.rectangle((14,13,18,17),fill=O);d.rectangle((6,9,10,12),fill=C);save(im,'ex_disk')
im,d=canvas();chip(d,3,9,26,14)
for x in [7,14,21]:d.rectangle((x,12,x+4,19),fill=M);d.rectangle((x+1,12,x+3,14),fill=C)
save(im,'memory_boost')
# Berserk core, ceramic chassis and bright red power bus.
im,d=canvas();chip(d,6,4,20,23);d.rectangle((9,8,23,22),fill=M);d.rectangle((11,10,21,20),fill=R);d.polygon([(15,10),(12,17),(16,17),(15,22),(22,13),(18,13),(20,10)],fill=Y);d.rectangle((4,10,7,20),fill=S);save(im,'biodyne_berserk')
# Kiroshi lens with a cyan sensor array, retaining a clear stepped eye silhouette.
im,d=canvas();d.line([(23,17),(28,21),(28,27),(20,29)],fill=O,width=4);d.line([(23,17),(27,22),(27,26),(20,28)],fill=M,width=2)
d.polygon([(3,11),(8,6),(20,6),(27,13),(25,22),(10,26),(4,21)],fill=O);d.polygon([(5,12),(9,8),(19,8),(24,13),(22,21),(10,23),(6,19)],fill=S);d.rectangle((10,9,21,20),fill=D);d.rectangle((12,11,19,18),fill=C);d.rectangle((14,12,17,17),fill=O);d.rectangle((21,22,24,23),fill=Y);save(im,'kiroshi_clairvoyant_optics')
# Two different skeletal reinforcements: hinge vs. a torso/rib cage.
im,d=canvas();d.line([(8,4),(12,13)],fill=O,width=9);d.line([(8,4),(12,13)],fill=S,width=5);d.line([(16,17),(25,28)],fill=O,width=10);d.line([(16,17),(25,28)],fill=M,width=6);d.ellipse((7,10,22,25),fill=O);d.ellipse((9,12,20,23),fill=G);d.ellipse((12,15,17,20),fill=D);d.rectangle((13,16,15,18),fill=C);save(im,'bionic_joints')
im,d=canvas();d.line([(15,3),(15,28)],fill=O,width=7);d.line([(15,4),(15,27)],fill=S,width=3)
for y,w in [(8,8),(13,10),(18,8)]:
 d.line([(14,y),(6,y-2),(4,y+2),(14,y+3)],fill=O,width=5);d.line([(17,y),(25,y-2),(27,y+2),(17,y+3)],fill=O,width=5);d.line([(14,y),(6,y-2),(5,y+1),(14,y+2)],fill=M,width=2);d.line([(17,y),(25,y-2),(26,y+1),(17,y+2)],fill=S,width=2)
d.rectangle((13,24,18,27),fill=Y);save(im,'epimorphic_skeleton')
# A woven fibre bundle and an asymmetric neural relay.
im,d=canvas()
for x in [8,14,20]:
 d.line([(x,3),(x+5,10),(x-2,20),(x+3,29)],fill=O,width=6);d.line([(x,3),(x+5,10),(x-2,20),(x+3,29)],fill=M,width=3)
d.rectangle((8,8,26,11),fill=S);d.rectangle((5,21,23,24),fill=G);save(im,'neofiber')
im,d=canvas();chip(d,7,8,18,15);d.line([(9,9),(6,5),(3,5)],fill=C,width=2);d.line([(23,12),(28,8),(28,4)],fill=R,width=2);d.line([(20,22),(24,28),(29,28)],fill=M,width=3);d.polygon([(13,12),(20,12),(16,16),(20,16),(12,23),(14,18),(10,18)],fill=Y);save(im,'adrenaline_converter')
# Mechanical pump versus healing circulatory valve.
im,d=canvas();d.line([(11,11),(11,4),(16,4),(16,10)],fill=O,width=6);d.line([(11,11),(11,4),(16,4),(16,10)],fill=R,width=3);d.polygon([(5,14),(8,10),(14,10),(18,13),(23,10),(28,15),(25,24),(17,29),(8,24)],fill=O);d.polygon([(7,15),(10,12),(14,12),(18,16),(23,13),(25,16),(23,22),(17,26),(10,22)],fill=R);d.rectangle((13,16,20,22),fill=M);d.rectangle((16,14,18,24),fill=S);save(im,'blood_pump')
im,d=canvas();d.line([(6,3),(6,9),(11,14),(11,22),(6,28)],fill=O,width=6);d.line([(6,3),(6,9),(11,14),(11,22),(6,28)],fill=R,width=3);d.ellipse((9,8,28,27),fill=O);d.ellipse((11,10,26,25),fill=M);d.rectangle((16,12,20,23),fill=C);d.rectangle((13,15,23,19),fill=C);save(im,'heal_on_kill')
# Layered dermal plates and an optical metamaterial mesh.
im,d=canvas()
for x,y in [(5,5),(16,4),(3,15),(15,15)]:
 d.polygon([(x+2,y),(x+9,y),(x+12,y+4),(x+9,y+12),(x+1,y+10),(x,y+3)],fill=O);d.polygon([(x+3,y+2),(x+8,y+2),(x+10,y+5),(x+8,y+9),(x+2,y+8)],fill=M);d.line([(x+3,y+2),(x+8,y+2)],fill=S,width=1)
save(im,'subdermal_armor')
im,d=canvas();d.polygon([(9,4),(23,4),(29,13),(23,28),(7,28),(3,16)],fill=O)
for row in range(4):
 for col in range(3):
  x=7+col*6+(row%2)*2;y=7+row*5;d.polygon([(x+2,y),(x+5,y),(x+6,y+3),(x+3,y+5),(x,y+3)],fill=C if (col+row)%3==0 else D)
d.line([(6,25),(23,25)],fill=M,width=2);save(im,'optical_camo')
# Twin tensile tendons versus a broad charged-jump ankle actuator.
im,d=canvas()
for x in [7,18]:
 d.line([(x,3),(x+2,16),(x-1,24),(x+5,29)],fill=O,width=8);d.line([(x,4),(x+2,16),(x-1,24),(x+5,28)],fill=S,width=4);d.line([(x+2,8),(x+4,20)],fill=R,width=2);d.rectangle((x-2,17,x+4,21),fill=G)
save(im,'reinforced_tendons')
im,d=canvas();d.polygon([(9,2),(22,2),(23,17),(27,23),(29,28),(6,29),(4,25),(10,17)],fill=O);d.polygon([(11,4),(20,4),(20,17),(16,23),(8,24)],fill=M);d.rectangle((10,8,21,11),fill=S);d.rectangle((10,14,21,17),fill=G);d.line([(17,17),(20,24),(26,26)],fill=C,width=2);d.rectangle((8,26,27,27),fill=S);save(im,'fortified_ankles')
print('Wrote 14 original 32px implant sprites')
