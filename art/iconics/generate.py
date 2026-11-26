#!/usr/bin/env python3
"""Original 32px Minecraft silhouettes: armored Apogee spine and tiered requisition chips."""
from pathlib import Path
from PIL import Image, ImageDraw
import json

ROOT = Path(__file__).resolve().parents[2]
A = ROOT / 'src/main/resources/assets/cybernetics2077'
ICONS = ROOT / 'docs/wiki/assets/icons'

def save(name, image):
    image.save(A / 'textures/item' / (name+'.png'))
    image.save(ICONS / (name+'.png'))

image = Image.new('RGBA', (32, 32))
d = ImageDraw.Draw(image)
# A broad shielded top, mechanical spine, gold interleaved vertebrae and paired red conduits.
d.polygon([(10,2),(21,2),(25,6),(23,10),(21,11),(21,25),(18,30),(13,30),(10,25),(10,11),(7,9),(7,5)], fill='#131b25')
d.line([(10,3),(21,3),(24,6)], fill='#d4c39a', width=2)
d.rectangle((12,7,19,26), fill='#4a515b')
d.rectangle((14,7,17,27), fill='#9c6b32')
d.line([(10,10),(10,23),(13,27)], fill='#e15d50')
d.line([(21,10),(21,23),(18,27)], fill='#e15d50')
for y in (10,15,20):
    d.polygon([(8,y),(12,y-1),(19,y-1),(24,y),(23,y+3),(19,y+2),(12,y+2),(8,y+3)], fill='#6b747a')
    d.line([(9,y),(13,y),(14,y-1),(19,y-1),(22,y)], fill='#edcc78')
    d.rectangle((14,y,17,y+1), fill='#a9f2cc')
d.rectangle((11,5,20,7), fill='#505966')
d.line([(13,5),(18,5)], fill='#f6dc92')
d.point((9,6), fill='#f8dc84');d.point((22,6), fill='#f8dc84')
d.rectangle((14,28,17,29), fill='#f1c363')
save('militech_apogee_sandevistan', image)
models = A / 'models/item'
(models/'militech_apogee_sandevistan_tier_5.json').write_text(json.dumps({
    'parent':'minecraft:item/generated', 'textures':{'layer0':'cybernetics2077:item/militech_apogee_sandevistan'}}, indent=2)+'\n')
colors = ['#c7d5da','#6fe69c','#68baf5','#bf8dff','#ffbd58']
for tier, color in enumerate(colors, 1):
    im=Image.new('RGBA',(32,32));p=ImageDraw.Draw(im)
    p.polygon([(6,4),(24,4),(27,7),(27,24),(23,28),(6,28),(4,26),(4,6)], fill='#151e2c')
    p.line([(7,5),(23,5),(26,8),(26,23),(22,27),(7,27),(5,25),(5,7),(7,5)], fill=color)
    p.rectangle((8,9,22,21), fill='#2b3e48')
    p.rectangle((11,11,19,19), fill='#c6ab62')
    p.rectangle((13,13,17,17), fill='#e9d298')
    # Key / authorization motif at native pixels, distinct from broken spare-part silhouettes.
    p.rectangle((14,14,16,15), fill='#28313a')
    p.line([(15,16),(15,18),(17,18)], fill='#28313a')
    for x in (9,12,15,18,21):
        p.line([(x,6),(x,8)], fill='#b79a54')
        p.line([(x,22),(x,24)], fill='#b79a54')
    for n in range(tier):p.point((10+2*n,25), fill=color)
    name=f'iconic_requisition_tier_{tier}';save(name,im)
    (models/(name+'.json')).write_text(json.dumps({'parent':'minecraft:item/generated',
        'textures':{'layer0':'cybernetics2077:item/'+name}},indent=2)+'\n')
langpath=A/'lang/en_us.json'
lang=json.loads(langpath.read_text())
lang.update({
 'cybernetics2077.augment.militech_apogee_sandevistan':'Militech “Apogee” Sandevistan',
 'item.cybernetics2077.militech_apogee_sandevistan_tier_5':'Militech “Apogee” Sandevistan',
 'cybernetics2077.implant.iconic':'Iconic',
 'cybernetics2077.iconic.requisition':'Authorizes one Tier %s Iconic cyberware purchase.',
 'cybernetics2077.iconic.requirements':'Requires current ripperdoc stock, Street Cred and full Iconic price.',
 'cybernetics2077.iconic.consumed':'Consumed on purchase. Does not grant a discount or an implant directly.',
 'cybernetics2077.sandy.recharging':'Sandevistan recharging — insufficient charge or rearming.'})
for t in range(1,6):lang['item.cybernetics2077.iconic_requisition_tier_'+str(t)]='Iconic Requisition Chip'
langpath.write_text(json.dumps(lang,indent=2,ensure_ascii=False)+'\n')
