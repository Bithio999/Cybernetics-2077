"""Baked industrial modules. Light channels are geometry, not mismatched UV stripes."""
from PIL import Image,ImageDraw,ImageFont
import math

def build_furnishings(asset_root,model,cube):
    folder=asset_root/'textures/block'
    for name,color in [('neon_lime',(174,230,123)),('opal_lens',(202,226,208)),('task_lens',(220,237,231))]:
        im=Image.new('RGB',(32,32),color)
        if name!='neon_lime':
            d=ImageDraw.Draw(im)
            for x in range(0,32,4):d.line((x,0,x,31),fill=tuple(v-21 for v in color))
            for y in range(0,32,4):d.line((0,y,31,y),fill=tuple(v-21 for v in color))
        im.save(folder/(name+'.png'))
    # Edge-to-edge channels on vertical faces; a separate top/bottom texture runs along Z.
    for color,lit in [('cyan',(86,221,222)),('red',(224,99,102)),('green',(174,230,123))]:
        for axis in ['side','run']:
            im=Image.new('RGB',(64,64),(33,45,48));d=ImageDraw.Draw(im)
            if axis=='side':
                d.rectangle((0,27,63,36),fill=tuple(v//3 for v in lit));d.rectangle((0,30,63,33),fill=lit)
            else:
                d.rectangle((27,0,36,63),fill=tuple(v//3 for v in lit));d.rectangle((30,0,33,63),fill=lit)
            im.save(folder/f'{color}_channel_{axis}.png')
        e=cube([0,0,0],[16,16,16],'side')
        for f in ['up','down']:e['faces'][f]['texture']='#run'
        model(color+'_strip',[e],{'side':color+'_channel_side','run':color+'_channel_run'})
    # A four-lens task light with a visible ceiling/rod mount, directed at the patient.
    elements=[cube([6,14,6],[10,16,10],'metal'),cube([2,12,2],[14,14,14],'metal')]
    for x in (3,9):
        for z in (3,9):elements.append(cube([x,10,z],[x+4,12,z+4],'lens'))
    model('surgical_light',elements,{'metal':'reinforced_panel','lens':'task_lens'})
    # Compact ceiling canopy with a honeycomb-like diffuser and uninterrupted edge band.
    model('canopy_panel',[
        cube([0,13,0],[16,16,16],'metal'),cube([0,12,0],[16,13,16],'light'),
        cube([1,11,1],[15,12,15],'lens')
    ],{'metal':'reinforced_panel','light':'neon_lime','lens':'opal_lens'})
    # Thin fixtures elsewhere use the same neutral diffuser without a green ring.
    model('ceiling_diffuser',[
        cube([0,14,0],[16,16,16],'metal'),cube([1,12,1],[15,14,15],'lens')
    ],{'metal':'reinforced_panel','lens':'task_lens'})
    # Full metal wall backing, deeply recessed service panel, bolted perimeter frame.
    model('recessed_panel',[
        cube([0,0,5],[16,16,16],'metal'),cube([0,0,0],[16,2,5],'metal'),
        cube([0,14,0],[16,16,5],'metal'),cube([0,2,0],[2,14,5],'metal'),
        cube([14,2,0],[16,14,5],'metal')
    ],{'metal':'reinforced_panel'})
    # A repeated duct module, complete with wall backing and clamps at shared seams.
    model('facade_duct',[
        cube([0,0,14],[16,16,16],'metal'),cube([0,5,2],[16,11,12],'metal'),
        cube([0,4,4],[16,12,10],'metal'),cube([0,3,6],[16,13,8],'metal'),
        cube([7,3,1],[9,13,14],'frame')
    ],{'metal':'reinforced_panel','frame':'medical_cabinet'})
    im=Image.new('RGB',(64,64),(38,51,54));d=ImageDraw.Draw(im)
    d.rectangle((3,3,60,60),outline=(62,77,78),width=2)
    for y in (18,42,54):d.line((9,y,55,y),fill=(46,61,62),width=1)
    im.save(folder/'bar_counter.png')
    # Counter and four stepped/chamfered corner modules use the same Y=12..13 light channel.
    def counter_body(rects,path):
        out=[]
        for x,z,X,Z in rects:
            out.append(cube([x,0,z],[X,12,Z],'metal'))
            out.append(cube([x,13,z],[X,16,Z],'metal'))
        # The ring fills the structural gap, eliminating coplanar surfaces and z-fighting.
        for a,b in zip(path,path[1:]+path[:1]):
            x,z=a;X,Z=b
            if x==X:
                xx=x if x==0 else x-.25
                out.append(cube([xx,12,min(z,Z)],[xx+.25,13,max(z,Z)],'light'))
            else:
                zz=z if z==0 else z-.25
                out.append(cube([min(x,X),12,zz],[max(x,X),13,zz+.25],'light'))
        return out
    model('bar_counter',counter_body([(0,0,16,16)],[(0,0),(16,0),(16,16),(0,16)]),{'metal':'bar_counter','light':'neon_lime'})
    model('bar_corner',counter_body([(0,8,4,16),(4,4,8,16),(8,0,16,16)],[(0,8),(4,8),(4,4),(8,4),(8,0),(16,0),(16,16),(0,16)]),{'metal':'bar_counter','light':'neon_lime'})
    im=Image.new('RGB',(64,64),(33,57,57));d=ImageDraw.Draw(im)
    for x in range(0,64,16):
        d.rectangle((x+1,2,x+14,61),outline=(19,37,40),width=2)
        d.line((x+4,5,x+12,5),fill=(52,80,75),width=1)
    im.save(folder/'booth_padding.png')
    model('booth_seat',[
        cube([1,0,2],[15,4,15],'metal'),cube([0,4,1],[16,6,16],'metal'),
        cube([0,6,1],[16,9,13],'pad'),cube([0,9,12],[16,16,16],'pad')
    ],{'metal':'reinforced_panel','pad':'booth_padding'})
    # An original wordmark with a slanted face and extended underline, guided by the primary reference.
    im=Image.new('RGB',(192,64),(8,22,24));d=ImageDraw.Draw(im)
    try:font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-BoldOblique.ttf',23)
    except OSError:font=ImageFont.load_default(size=23)
    d.text((18,18),'AFTERLIFE',font=font,fill='#c7f197',stroke_width=0)
    d.polygon([(11,45),(172,45),(180,40),(21,40)],fill='#b7ec85')
    d.polygon([(10,46),(30,9),(40,9),(26,34)],fill='#c9ef98')
    d.line((4,53,188,53),fill='#53714d',width=1)
    im.save(folder/'afterlife_sign.png')
