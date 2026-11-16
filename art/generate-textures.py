#!/usr/bin/env python3
"""Original UV-aligned diagnostic skin and soft backdrop textures; no game artwork."""
from pathlib import Path
from PIL import Image, ImageDraw
import math
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/cybernetics2077/textures'
scale=8
im=Image.new('RGBA',(64*scale,64*scale));d=ImageDraw.Draw(im)
def box(rect,fill,width=1,outline=None):
    q=tuple(int(v*scale) for v in rect)
    if outline:d.rectangle(q,outline=outline,width=max(1,int(width*scale)))
    else:d.rectangle(q,fill=fill)
def line(points,fill,width=.12):d.line([(int(x*scale),int(y*scale)) for x,y in points],fill=fill,width=max(1,int(width*scale)),joint='curve')
faces=[(8,8,8,8,'head'),(0,8,8,8,'back'),(16,8,8,8,'back'),(24,8,8,8,'back'),(8,0,8,8,'back'),(16,0,8,8,'back'),
 (20,20,8,12,'chest'),(16,20,4,12,'back'),(28,20,4,12,'back'),(32,20,8,12,'spine'),(20,16,8,4,'back'),(28,16,8,4,'back')]
for x,y in [(44,20),(36,52),(4,20),(20,52)]:
    faces.extend([(x,y,4,12,'limb'),(x-4,y,4,12,'limb'),(x+4,y,4,12,'limb'),(x+8,y,4,12,'limb'),(x,y-4,4,4,'back'),(x+4,y-4,4,4,'back')])
for x,y,w,h,kind in faces:
    box((x,y,x+w-.05,y+h-.05),(87,101,122,205))
    box((x+.12,y+.12,x+w-.12,y+h-.12),None,.10,(173,198,214,180))
    for k in range(2,int(h),2):
        line([(x+.3,y+k),(x+w-.3,y+k)],(119,149,173,52),.055)
    for k in range(2,int(w),2):
        line([(x+k,y+.3),(x+k,y+h-.3)],(120,153,177,45),.05)
    line([(x+w/2,y+.2),(x+w/2,y+h-.2)],(213,225,237,220),.16)
    if kind=='chest':
        for k in range(4):
            yy=y+2.4+k*1.5
            line([(x+.5,yy-.3),(x+2,yy+.2),(x+3.6,yy+.1)],(228,222,230,215),.17)
            line([(x+w-.5,yy-.3),(x+w-2,yy+.2),(x+w-3.6,yy+.1)],(228,222,230,215),.17)
        box((x+1,y+1,x+2.7,y+2.2),None,.1,(255,210,221,240))
        box((x+1.4,y+1.3,x+2.3,y+1.9),(211,227,238,180))
        line([(x+2,y+9),(x+1,y+10.4),(x+3.8,y+11.6),(x+w-1,y+10.4),(x+w-2,y+9)],(165,216,229,200),.16)
    elif kind=='head':
        for sign in [-1,1]:
            cx=x+w/2+sign*1.8
            box((cx-.85,y+2.3,cx+.85,y+3.7),None,.13,(205,233,241,230))
        line([(x+2,y+5.8),(x+w-2,y+5.8)],(214,231,243,230),.13)
        line([(x+2,y+6.2),(x+3,y+6.8),(x+5,y+6.8),(x+6,y+6.2)],(187,221,237,180),.14)
    elif kind=='limb':
        for yy in [y+1,y+5.2,y+6.5,y+10.6]:box((x+.35,yy,x+w-.35,yy+.4),None,.10,(158,208,229,215))
        line([(x+.6,y+2),(x+1.3,y+3),(x+1.3,y+4.8)],(230,225,235,210),.13)
        line([(x+w-.6,y+7.4),(x+w-1.3,y+8.4),(x+w-1.3,y+10.1)],(172,224,235,210),.13)
    elif kind=='spine':
        for k in range(7):box((x+3.4,y+1+k*1.4,x+4.6,y+1.6+k*1.4),None,.1,(220,226,238,220))
im.save(ROOT/'entity/diagnostic.png')
for name,color in [('red_glow',(197,34,65)),('cyan_glow',(31,174,194))]:
    im=Image.new('RGBA',(256,256));px=im.load()
    for y in range(256):
        for x in range(256):
            r=math.hypot((x-127.5)/128,(y-127.5)/128)
            a=round(110*max(0,1-r)**2.2)
            px[x,y]=(*color,a)
    im.save(ROOT/'gui'/f'{name}.png')
