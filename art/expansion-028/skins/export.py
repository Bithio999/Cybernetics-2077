#!/usr/bin/env python3
"""Pack generated cybernetic surface panels into standard Minecraft 64px wide-arm UV faces."""
from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[3];source=Path(__file__).with_name('cyberpsycho-source.png')
w,h=map(int,subprocess.check_output(['magick','identify','-format','%w %h',str(source)],text=True).split())
# Panel boundaries in the source art's normalized 1280px layout; only packing/resampling.
panels={'head':(240,210,230,205),'headSide':(52,210,183,205),'headBack':(677,210,240,205),'headTop':(240,10,230,190),'chest':(303,453,290,360),'spine':(610,453,360,360),'arm':(57,455,210,355),'leg':(327,844,122,395),'legBack':(465,844,121,395)}
faces=[]
def face(name,x,y,fw,fh):faces.append((name,x,y,fw,fh))
for name,x,y in [('headSide',0,8),('head',8,8),('headSide',16,8),('headBack',24,8),('headTop',8,0),('headTop',16,0)]:face(name,x,y,8,8)
for name,x,y,fw,fh in [('chest',20,20,8,12),('spine',32,20,8,12),('arm',16,20,4,12),('arm',28,20,4,12),('chest',20,16,8,4),('spine',28,16,8,4)]:face(name,x,y,fw,fh)
for tx,ty in [(40,16),(32,48),(0,16),(16,48)]:
 isleg=tx in (0,16);base='leg' if isleg else 'arm'
 for off in (0,4,8,12):face('legBack' if isleg and off==12 else base,tx+off,ty+4,4,12)
 face(base,tx+4,ty,4,4);face(base,tx+8,ty,4,4)
with tempfile.TemporaryDirectory() as tmp:
 cmd=['magick','-size','64x64','xc:none']
 for i,(name,x,y,fw,fh) in enumerate(faces):
  a,b,c,d=panels[name];a,b,c,d=round(a*w/1280),round(b*h/1280),round(c*w/1280),round(d*h/1280)
  target=str(Path(tmp)/f'{i}.png')
  subprocess.run(['magick',str(source),'-crop',f'{c}x{d}+{a}+{b}','+repage','-filter','Point','-resize',f'{fw}x{fh}!','-background','#13161b','-alpha','remove','-alpha','on','-dither','None','-colors','20','PNG32:'+target],check=True)
  cmd += [target,'-geometry',f'+{x}+{y}','-compose','Over','-composite']
 out=root/'src/main/resources/assets/cybernetics2077/textures/entity/cyberpsycho.png'
 subprocess.run(cmd+['-strip','PNG32:'+str(out)],check=True)
