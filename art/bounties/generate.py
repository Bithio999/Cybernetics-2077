"""Original pixel-art bounty dossier; no extracted assets."""
from pathlib import Path
from PIL import Image, ImageDraw
import json
root=Path(__file__).resolve().parents[2]
a=root/'src/main/resources/assets/cybernetics2077'
im=Image.new('RGBA',(32,32)); d=ImageDraw.Draw(im)
d.polygon([(5,3),(24,3),(28,7),(28,25),(24,29),(4,29),(4,5)],fill='#14231f',outline='#83c4ab')
d.rectangle((7,6,10,25),fill='#d2a357');d.rectangle((12,6,24,8),fill='#3d6856')
d.rectangle((12,11,24,24),fill='#0b1718');d.ellipse((15,13,21,19),outline='#ed6d6e',width=1)
d.line((18,11,18,14),fill='#ed6d6e');d.line((18,18,18,21),fill='#ed6d6e')
d.line((13,16,16,16),fill='#ed6d6e');d.line((20,16,23,16),fill='#ed6d6e')
d.rectangle((12,26,20,26),fill='#8bbba8');d.point((24,26),fill='#ed6d6e')
(a/'textures/item/bounty_contract.png').parent.mkdir(parents=True,exist_ok=True);im.save(a/'textures/item/bounty_contract.png')
(a/'models/item/bounty_contract.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'cybernetics2077:item/bounty_contract'}},indent=2)+'\n')
p=a/'lang/en_us.json';lang=json.loads(p.read_text());lang['item.cybernetics2077.bounty_contract']='Bounty Dossier';p.write_text(json.dumps(lang,indent=2,ensure_ascii=False)+'\n')
