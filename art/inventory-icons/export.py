#!/usr/bin/env python3
"""Export the image-generated cyberware source atlas; ImageMagick handles texture packing only."""
from pathlib import Path
import json, subprocess
ROOT=Path(__file__).resolve().parents[2]
ASSET=ROOT/'src/main/resources/assets/cybernetics2077'
ATLAS=Path(__file__).with_name('atlas.png')
NAMES=[entry['name'] for entry in json.loads(Path(__file__).with_name('manifest.json').read_text())['icons']]
assert len(NAMES)==64
size=subprocess.check_output(['magick','identify','-format','%w %h',str(ATLAS)],text=True).split()
w,h=map(int,size)
manifest=[]
for i,name in enumerate(NAMES):
 col,row=i%8,i//8
 x,y=round(col*w/8),round(row*h/8)
 width,height=round((col+1)*w/8)-x,round((row+1)*h/8)-y
 resolution=128 if name=='mod_badge' else 32
 out=ASSET/('textures/gui' if name=='mod_badge' else 'textures/item')/(name+'.png')
 subprocess.run(['magick',str(ATLAS),'-crop',f'{width}x{height}+{x}+{y}','+repage','-channel','A','-threshold','35%','+channel','-trim','+repage','-filter','Point','-resize','24x24','-gravity','center','-background','none','-extent','32x32','-channel','A','-threshold','35%','+channel','-dither','None','-colors','20','-filter','Point','-resize',f'{resolution}x{resolution}','-strip','PNG32:'+str(out)],check=True)
 if name!='mod_badge':
  wiki=ROOT/'docs/wiki/assets/icons'/(name+'.png')
  if wiki.exists():wiki.write_bytes(out.read_bytes())
 manifest.append({'name':name,'cell':[col,row],'size':resolution})
Path(__file__).with_name('manifest.json').write_text(json.dumps({'source':'atlas.png','columns':8,'rows':8,'export':'32px pixel art, nearest-neighbor, limited palette, binary alpha; badge 128px','icons':manifest},indent=2)+'\n')
print('Exported',len(NAMES),'icons')
