#!/usr/bin/env python3
"""Original 32px street-built spinal implant. No extracted game textures."""
from pathlib import Path
import json
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[2]
a=root/'src/main/resources/assets/cybernetics2077'
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
# Split shoulder yoke, exposed vertebrae, paired cyan buses and a magenta reserve coil.
d.polygon([(7,3),(13,2),(16,5),(19,2),(25,3),(27,8),(24,12),(23,25),(18,30),(13,30),(9,25),(8,12),(5,8)],fill='#101b25')
d.line([(7,4),(12,3),(15,6),(18,3),(24,4),(26,8)],fill='#a2b9c2',width=1)
d.rectangle((13,7,18,28),fill='#263a44')
d.line([(9,10),(10,23),(14,28)],fill='#12a999',width=2)
d.line([(22,10),(21,23),(17,28)],fill='#197a96',width=2)
for y in (10,15,20,25):
 d.polygon([(9,y),(14,y-2),(18,y-2),(23,y),(22,y+2),(18,y+1),(14,y+1),(10,y+2)],fill='#4b6570')
 d.line([(10,y),(14,y-1),(18,y-1),(22,y)],fill='#9caeb5')
 d.rectangle((14,y-1,17,y),fill='#5bffe0')
 d.point((11,y+1),fill='#168e98');d.point((20,y+1),fill='#52e4e6')
d.rectangle((11,5,20,8),fill='#3f4758');d.line([(12,5),(15,5),(17,7),(20,7)],fill='#ef6cbb')
d.rectangle((14,28,17,29),fill='#a4ffe5')
for path in [a/'textures/item/davids_sandevistan.png',root/'docs/wiki/assets/icons/davids_sandevistan.png']:
 path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
(a/'models/item/davids_sandevistan_tier_5.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'cybernetics2077:item/davids_sandevistan'}},indent=2)+'\n')
p=a/'lang/en_us.json';lang=json.loads(p.read_text());lang.update({'cybernetics2077.augment.davids_sandevistan':"David's Sandevistan",'item.cybernetics2077.davids_sandevistan_tier_5':"David's Sandevistan"});p.write_text(json.dumps(lang,indent=2,ensure_ascii=False)+'\n')
