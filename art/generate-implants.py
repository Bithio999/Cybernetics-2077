#!/usr/bin/env python3
"""Prepare attributed equipment icons without redrawing them; generate original mechanism surfaces."""
from PIL import Image, ImageDraw
from pathlib import Path
import random
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/cybernetics2077/textures'
(root/'entity').mkdir(exist_ok=True)
# Preserve the reference pixels and aspect ratios. Transparent square padding is the only
# preparation required by Minecraft's generated item model; do not redraw the equipment.
for source in sorted((Path(__file__).with_name('cyberpunk-icons')).glob('*.png')):
 icon=Image.open(source).convert('RGBA')
 icon=icon.crop(icon.getbbox())
 size=max(icon.size)+8
 canvas=Image.new('RGBA',(size,size))
 canvas.alpha_composite(icon,((size-icon.width)//2,(size-icon.height)//2))
 canvas.save(root/'item'/source.name)
r=random.Random(2077);im=Image.new('RGBA',(32,32));px=im.load()
for y in range(32):
 for x in range(32):
  n=r.randrange(242,256);px[x,y]=(n,n,n,255)
im.save(root/'entity/mantis_mechanism.png')
Image.new('RGBA',(2,2),(255,255,255,255)).save(root/'entity/effect_white.png')
