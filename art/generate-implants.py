#!/usr/bin/env python3
"""Original 32px equipment sprites, with restrained Minecraft-style clusters and no filtering."""
from PIL import Image,ImageDraw
from pathlib import Path
import random
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/cybernetics2077/textures'
O='#14212b';D='#314651';M='#728c91';S='#b7c9c5';W='#e3e3cf';G='#b88941';Y='#ebc66b';C='#69d6d5';R='#bc4d46'
def canvas():
 im=Image.new('RGBA',(32,32));return im,ImageDraw.Draw(im)
def save(im,name): im.save(root/'item'/(name+'.png'))
# Kiroshi-style housing: side cable, white ceramic eye, amber iris and stepped pixel silhouette.
im,d=canvas();d.line([(23,17),(28,21),(28,26),(24,29),(19,29)],fill=O,width=5);d.line([(23,17),(27,22),(27,26),(23,28),(19,28)],fill=M,width=2)
d.polygon([(3,10),(7,6),(18,5),(25,10),(27,17),(23,23),(11,25),(5,20)],fill=O)
d.polygon([(5,11),(8,8),(18,7),(23,11),(25,17),(21,21),(11,23),(7,19)],fill=M)
d.polygon([(6,11),(11,8),(18,8),(21,11),(21,17),(17,20),(10,20),(6,17)],fill=W)
d.rectangle((11,9,18,18),fill=G);d.rectangle((13,10,19,17),fill=Y);d.rectangle((14,11,17,16),fill=O);d.rectangle((14,11,15,12),fill=C)
d.rectangle((4,13,6,17),fill=S);d.rectangle((23,12,25,16),fill=D);d.rectangle((10,22,15,23),fill=S);save(im,'optical_scanner')
# Parallel cyberdeck: chamfered PCB, ceramic modules, copper connectors and a red bus.
im,d=canvas();d.polygon([(4,8),(21,3),(27,8),(28,23),(11,29),(5,25)],fill=O)
d.polygon([(6,9),(21,5),(25,8),(26,22),(11,27),(7,24)],fill=D)
d.polygon([(7,10),(20,7),(23,9),(24,21),(11,25),(8,23)],fill='#49645c')
for i in range(4):
 x=9+i*4;d.rectangle((x,5-i//2,x+1,8-i//2),fill=Y);d.rectangle((x+1,25-i,x+2,27-i),fill=G)
d.polygon([(10,12),(18,10),(20,12),(20,19),(12,22),(10,20)],fill=O);d.rectangle((12,13,18,18),fill=S);d.rectangle((13,14,17,16),fill=M)
d.line([(7,23),(10,24),(23,19)],fill=R,width=2);d.line([(7,13),(7,11),(21,7)],fill=C,width=1)
d.rectangle((22,11,24,15),fill=Y);d.rectangle((22,16,24,17),fill=O);save(im,'cyberdeck_tier_1')
# Side-opening forearm and folded mantis knife. Deliberately large readable pixel clusters.
im,d=canvas();d.polygon([(4,22),(7,10),(12,6),(22,11),(25,23),(21,29),(9,28)],fill=O)
d.polygon([(6,21),(9,11),(12,9),(17,12),(14,24),(10,26)],fill=S);d.polygon([(16,12),(20,13),(23,23),(20,26),(16,24)],fill=M)
d.line([(10,12),(13,11),(18,14),(17,24)],fill=D,width=3);d.line([(12,13),(14,14),(13,23)],fill=G,width=2)
d.rectangle((14,19,17,22),fill=O);d.rectangle((15,19,17,20),fill=Y)
d.polygon([(15,19),(18,10),(25,2),(28,2),(24,11),(20,20),(18,23)],fill=O)
d.polygon([(17,19),(20,10),(26,4),(24,11),(20,20)],fill=W);d.polygon([(17,20),(20,11),(23,8),(19,20)],fill=M)
d.rectangle((9,24,13,25),fill=R);save(im,'mantis_claws_tier_1')
# Program chip: cyan substrate, gold contacts and a bold lightning glyph.
im,d=canvas();d.polygon([(7,3),(22,3),(26,7),(26,27),(6,27),(6,5)],fill=O)
d.polygon([(8,5),(21,5),(24,8),(24,25),(8,25)],fill=C);d.rectangle((10,7,22,22),fill=D);d.rectangle((11,8,21,21),fill=O)
for x in [9,13,17,21]:d.rectangle((x,27,x+1,30),fill=Y);d.rectangle((x,1,x+1,3),fill=G)
d.polygon([(17,8),(12,15),(16,15),(13,21),(21,12),(17,12),(20,8)],fill=W)
d.rectangle((9,23,13,24),fill=M);d.rectangle((20,23,23,24),fill=R);save(im,'short_circuit_tier_1')
# Machined surfaces have subtle variation without imprinting noise across the shape.
r=random.Random(2077);im=Image.new('RGBA',(32,32));px=im.load()
for y in range(32):
 for x in range(32):
  n=r.randrange(242,256);px[x,y]=(n,n,n,255)
im.save(root/'entity/mantis_mechanism.png')
Image.new('RGBA',(2,2),(255,255,255,255)).save(root/'entity/effect_white.png')
