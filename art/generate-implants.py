#!/usr/bin/env python3
"""Original pixel artwork, guided by the silhouettes of CP2077's equipment icons, not game rips."""
from PIL import Image, ImageDraw
from pathlib import Path
import random
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/cybernetics2077/textures'
(root/'entity').mkdir(exist_ok=True)
# Optics: dark tapered barrel, pale ocular lens, gold iris, trailing connector.
im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
d.line([(20,42),(12,50),(11,55),(18,58),(27,51)],fill='#171f28',width=5)
d.line([(20,42),(12,50),(11,55),(18,58),(27,51)],fill='#879a9f',width=2)
d.polygon([(15,35),(28,20),(45,27),(30,47),(19,47),(12,42)],fill='#101720')
d.polygon([(17,34),(28,23),(39,29),(27,44),(18,44),(14,41)],fill='#35454e')
d.line([(18,36),(26,27)],fill='#94a8a9',width=2)
d.polygon([(29,17),(36,12),(46,13),(53,20),(54,29),(48,36),(37,36),(29,29)],fill='#1c242c')
d.polygon([(31,18),(37,13),(46,15),(52,21),(52,29),(47,34),(38,34),(31,28)],fill='#bbb9a7')
d.polygon([(33,18),(39,15),(47,17),(51,23),(48,29),(41,31),(34,27)],fill='#e2ddc9')
d.ellipse((39,18,49,29),fill='#9e7841');d.ellipse((42,20,49,27),fill='#35322c');d.ellipse((44,20,48,26),fill='#09131c');d.rectangle((44,20,45,21),fill='#e6f5ec')
d.line([(29,30),(37,36),(46,37)],fill='#65767d',width=2)
for x,y in [(26,25),(21,42),(35,32),(48,33)]:d.rectangle((x,y,x+2,y+2),fill='#bca778')
d.line([(19,41),(23,39)],fill='#bc7563',width=1)
im.save(root/'item/optical_scanner.png')
# Deck: compact graphite cartridge with exposed red PCB, ceramic modules and gold contacts.
im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
d.polygon([(12,12),(43,7),(53,17),(51,48),(40,56),(12,51),(8,39)],fill='#17212b',outline='#70828b')
d.polygon([(16,15),(40,11),(48,19),(46,44),(38,50),(16,47)],fill='#703440',outline='#ba636c')
for y in range(19,44,4):d.line((18,y,44,y-2),fill='#b57570')
d.rectangle((23,23,38,37),fill='#212c35',outline='#b7c1bc');d.rectangle((26,26,35,34),fill='#8b9f9d')
for x,y in [(16,18),(37,14),(18,38),(39,39)]:d.rectangle((x,y,x+5,y+4),fill='#d0c6a3');d.rectangle((x+1,y+1,x+4,y+2),fill='#f1e8ca')
for x in range(17,42,5):d.polygon([(x,48),(x+3,48),(x+3,54),(x,53)],fill='#c3aa69')
d.line((11,15,10,39),fill='#aab6b8',width=2);d.line((46,17,50,20),fill='#d4dacf',width=2)
im.save(root/'item/cyberdeck_tier_1.png')
# Program chip: etched silicon plate with contact teeth and a readable electrical glyph.
im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
for i in range(5):
 x=16+i*7;d.rectangle((x,7,x+3,14),fill='#c6b77e');d.rectangle((x,48,x+3,56),fill='#ad9967')
 y=16+i*7;d.rectangle((7,y,14,y+3),fill='#ac976e');d.rectangle((48,y,56,y+3),fill='#c6b77e')
d.polygon([(13,13),(44,13),(51,20),(51,44),(44,51),(13,51)],fill='#253a45',outline='#8ac1c7')
d.polygon([(18,18),(42,18),(46,22),(46,44),(18,44)],fill='#13212d',outline='#628d9b')
d.polygon([(35,20),(24,34),(31,34),(27,44),(42,29),(34,29),(39,20)],fill='#e5eef0')
d.line((19,25,22,25),fill='#b46267');d.line((19,40,22,40),fill='#b46267')
im.save(root/'item/short_circuit_tier_1.png')
# Arm implant: folded white ceramic forearm, bronze hinge, curved steel blade extended above it.
im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
d.polygon([(9,38),(19,30),(38,34),(47,43),(43,51),(22,53),(10,48)],fill='#202a33',outline='#819092')
d.polygon([(12,39),(21,33),(36,36),(41,42),(31,45),(18,44)],fill='#c6c8bd')
d.polygon([(13,45),(29,47),(39,44),(43,46),(39,50),(21,51)],fill='#8c9893')
d.ellipse((32,29,44,41),fill='#a88e5c',outline='#d5c49a',width=1);d.ellipse((35,32,41,38),fill='#33434c')
d.polygon([(38,30),(35,20),(25,17),(14,16),(5,13),(17,11),(32,13),(43,18),(54,26),(58,34),(49,31),(43,26)],fill='#52656c',outline='#a4b7b8')
d.polygon([(7,13),(25,14),(39,19),(52,27),(55,31),(40,23),(29,19),(14,17)],fill='#dee5df')
d.line([(38,30),(44,24),(50,25)],fill='#c5aa73',width=2)
d.line((18,39,30,41),fill='#5a737a',width=2);d.rectangle((16,45,20,46),fill='#bf6257')
im.save(root/'item/mantis_claws_tier_1.png')
r=random.Random(2077);im=Image.new('RGBA',(32,32));px=im.load()
for y in range(32):
 for x in range(32):
  n=r.randrange(242,256);px[x,y]=(n,n,n,255)
im.save(root/'entity/mantis_mechanism.png')
Image.new('RGBA',(2,2),(255,255,255,255)).save(root/'entity/effect_white.png')
