#!/usr/bin/env python3
"""Original pixel-art spine and synthesized audio; no game assets are redistributed."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,math,wave,struct,subprocess,tempfile,random
ROOT=Path(__file__).resolve().parents[2];A=ROOT/'src/main/resources/assets/cybernetics2077'
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
d.polygon([(12,2),(21,2),(25,6),(23,11),(20,12),(20,26),(17,30),(12,28),(10,10),(7,8),(8,4)],fill='#203b42')
d.line([(13,3),(20,3),(23,6)],fill='#c9dec3',width=2)
d.rectangle((13,6,18,25),fill='#9ccf7d')
d.line([(16,7),(16,25)],fill='#2b6f62',width=2)
for y in range(9,26,4):
 d.polygon([(8,y),(12,y-1),(20,y-1),(24,y+1),(23,y+3),(19,y+2),(12,y+2),(8,y+3)],fill='#759c9b')
 d.line([(10,y),(21,y)],fill='#d5dc95')
 d.rectangle((14,y,17,y+1),fill='#51ebc2')
d.rectangle((14,27,17,29),fill='#b89c52');d.rectangle((8,5,10,6),fill='#e3d27a')
im.save(A/'textures/item/dynalar_sandevistan.png');im.save(ROOT/'docs/wiki/assets/icons/dynalar_sandevistan.png')
langpath=A/'lang/en_us.json';lang=json.loads(langpath.read_text())
lang['cybernetics2077.augment.dynalar_sandevistan']='Dynalar Sandevistan'
for t in range(2,6):
 id=f'dynalar_sandevistan_tier_{t}';lang['item.cybernetics2077.'+id]='Dynalar Sandevistan'
 (A/'models/item'/f'{id}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'cybernetics2077:item/dynalar_sandevistan'}},indent=2)+'\n')
lang.update({'key.cybernetics2077.sandevistan':'Toggle Sandevistan','cybernetics2077.sandy.recharging':'Sandevistan recharging — requires 20% charge.',
 'subtitles.cybernetics2077.sandy_on':'Sandevistan engages','subtitles.cybernetics2077.sandy_off':'Sandevistan disengages','subtitles.cybernetics2077.sandy_hum':'Temporal processor humming'})
langpath.write_text(json.dumps(lang,indent=2,ensure_ascii=False)+'\n')
(A/'sounds').mkdir(exist_ok=True);events={}
for name,duration in [('sandy_on',.75),('sandy_off',.55),('sandy_hum',2)]:
 rng=random.Random(2077);rate=44100;samples=[];phase=0
 for i in range(int(duration*rate)):
  t=i/rate;u=t/duration
  if name=='sandy_hum':
   v=.28*math.sin(2*math.pi*64*t)+.12*math.sin(2*math.pi*128*t)+.04*math.sin(2*math.pi*512*t)*(0.6+.4*math.sin(2*math.pi*4*t))
  else:
   f=(150+950*math.exp(-u*5)) if name=='sandy_on' else 130+700*(1-u)**2
   phase+=2*math.pi*f/rate
   envelope=min(1,t/.018)*max(0,1-u)**1.6
   v=envelope*(.48*math.sin(phase)+.18*math.sin(phase*2.001)+.08*(rng.random()*2-1))
  samples.append(struct.pack('<h',max(-32767,min(32767,int(v*24000)))))
 with tempfile.NamedTemporaryFile(suffix='.wav') as f:
  with wave.open(f.name,'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(rate);w.writeframes(b''.join(samples))
  subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',f.name,'-c:a','libvorbis','-q:a','4',str(A/'sounds'/f'{name}.ogg')],check=True)
 events[name]={'subtitle':'subtitles.cybernetics2077.'+name,'sounds':[{'name':'cybernetics2077:'+name,'stream':False}]}
(A/'sounds.json').write_text(json.dumps(events,indent=2)+'\n')
