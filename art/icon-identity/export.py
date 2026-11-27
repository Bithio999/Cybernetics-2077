#!/usr/bin/env python3
"""Texture packing only: crop source sprites, limit their palette, fit and pad without stretching."""
import hashlib,json,subprocess
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
HERE=Path(__file__).resolve().parent
ASSETS=ROOT/'src/main/resources/assets/cybernetics2077/textures/item'
WIKI=ROOT/'docs/wiki/assets/icons'
manifest=json.loads((HERE/'manifest.json').read_text())
report=[]
for sheet in manifest['sheets']:
    atlas=HERE/(sheet['name']+'.png')
    with Image.open(atlas) as img:w,h=img.size
    for entry in sheet['icons']:
        col,row=entry['cell'];x,y=round(col*w/4),round(row*h/4)
        cw,ch=round((col+1)*w/4)-x,round((row+1)*h/4)-y
        out=ASSETS/(entry['id']+'.png')
        subprocess.run(['magick',str(atlas),'-crop',f'{cw}x{ch}+{x}+{y}','+repage','-channel','A','-threshold','50%','+channel','-trim','+repage','-filter','Point','-resize','26x26','-gravity','center','-background','none','-extent','32x32','-dither','None','-colors','9','-channel','A','-threshold','50%','+channel','-strip','PNG32:'+str(out)],check=True)
        (WIKI/out.name).write_bytes(out.read_bytes())
        with Image.open(out) as im:
            assert im.size==(32,32) and im.mode=='RGBA'
            alpha=im.getchannel('A');box=alpha.getbbox()
            assert box and min(box[0],box[1],32-box[2],32-box[3])>=3,(entry['id'],box)
            pixels=[im.getpixel((x,y)) for y in range(32) for x in range(32)]
            assert {p[3] for p in pixels}<={0,255}
            colors={p for p in pixels if p[3]}
            assert len(colors)<=9
        report.append({'id':entry['id'],'bounds':box,'opaqueColors':len(colors),'sha256':hashlib.sha256(out.read_bytes()).hexdigest()})
assert len(report)==len({r['id'] for r in report})==84
assert len({r['sha256'] for r in report})==84,'Two different implants received the same sprite'
(HERE/'export-report.json').write_text(json.dumps(report,indent=2)+'\n')
print('Exported 84 unique, centered 32px sprites; 3px padding, binary alpha, <=9 opaque colors, wiki parity.')
