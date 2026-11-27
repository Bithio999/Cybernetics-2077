#!/usr/bin/env python3
"""Pack the original generated atlas into padded, limited-palette pixel sprites."""
import json,subprocess
from pathlib import Path
root=Path(__file__).resolve().parents[2]
data=json.loads(Path(__file__).with_name('manifest.json').read_text())
atlas=Path(__file__).with_name('atlas.png')
w,h=map(int,subprocess.check_output(['magick','identify','-format','%w %h',str(atlas)],text=True).split())
for i,name in enumerate(data['icons']):
 c,r=i%9,i//9;x,y=round(c*w/9),round(r*h/8);cw,ch=round((c+1)*w/9)-x,round((r+1)*h/8)-y
 out=root/'src/main/resources/assets/cybernetics2077/textures/item'/(name+'.png')
 subprocess.run(['magick',str(atlas),'-crop',f'{cw}x{ch}+{x}+{y}','+repage','-channel','A','-threshold','40%','+channel','-trim','+repage','-filter','Point','-resize','24x24','-gravity','center','-background','none','-extent','32x32','-dither','None','-colors','20','-channel','A','-threshold','40%','+channel','-strip','PNG32:'+str(out)],check=True)
 wiki=root/'docs/wiki/assets/icons'/(name+'.png');wiki.write_bytes(out.read_bytes())
print('Exported',len(data['icons']),'32px sprites')
