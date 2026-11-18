"""Four original 64px Minecraft skins with shaded faces and separate outer clothing layers."""
from PIL import Image, ImageDraw

def build_skins(asset_root):
    specs = [
        # Distinct age, palette, hair, clothing and physical arm model, rather than recolours.
        ('ripperdoc',False,'#ae8269','#d4d9cd','#273e45','#8d928d','vale'),
        ('ripperdoc_iris',True,'#81563f','#316c71','#27343d','#9a482d','iris'),
        ('fixer_mara',True,'#c48c70','#7c303e','#242a3c','#24202c','mara'),
        ('fixer_ren',False,'#c5a380','#263845','#384737','#c2b596','ren'),
    ]
    for name,slim,skin,coat,pants,hair,style in specs:
        im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
        def rgb(c):return tuple(int(c[i:i+2],16) for i in (1,3,5)) if isinstance(c,str) else c
        def tint(c,n):return tuple(max(0,min(255,v+n)) for v in rgb(c))
        def rect(b,c):d.rectangle(b,fill=c)
        def part(u,v,w,h,depth,c):
            c=rgb(c)
            faces=[((u+depth,v,u+depth+w-1,v+depth-1),16),
                   ((u+depth+w,v,u+depth+2*w-1,v+depth-1),-17),
                   ((u,v+depth,u+depth-1,v+depth+h-1),-15),
                   ((u+depth,v+depth,u+depth+w-1,v+depth+h-1),0),
                   ((u+depth+w,v+depth,u+2*depth+w-1,v+depth+h-1),-7),
                   ((u+2*depth+w,v+depth,u+2*(depth+w)-1,v+depth+h-1),-11)]
            for b,n in faces:rect(b,tint(c,n))
        arm=3 if slim else 4
        part(0,0,8,8,8,skin);part(16,16,8,12,4,coat)
        for u,v in [(40,16),(32,48)]:part(u,v,arm,12,4,coat)
        for u,v in [(0,16),(16,48)]:part(u,v,4,12,4,pants)
        # Face: stepped cheek/temple shading, asymmetric brows, nose and mouth.
        rect((8,8,15,10),hair);rect((8,11,8,15),tint(skin,-14));rect((15,11,15,15),tint(skin,-9))
        for x in (10,13):
            rect((x,11,x+1,11),tint(hair,-8));rect((x,12,x+1,12),'#d5d8ca')
            d.point((x+(0 if x==10 else 1),12),fill='#355b58')
        d.point((12,13),fill=tint(skin,-23));rect((11,14,13,14),tint(skin,-35))
        # Hair follows all sides; the outer hat layer adds a real raised silhouette.
        for b in [(0,8,7,10),(16,8,23,10),(24,8,31,12),(8,0,23,7)]:rect(b,hair)
        part(32,0,8,8,8,hair)
        # Clear the outer face, leaving a shaped fringe and side locks.
        rect((40,9,47,15),(0,0,0,0));rect((40,8,47,8),tint(hair,9))
        for x,y in [(41,9),(42,9),(45,9),(46,10)]:d.point((x,y),fill=hair)
        # Clothing outer layer: lapels, hems, seams, collar and equipment. Most pixels stay transparent.
        rect((20,36,27,37),tint(coat,18));rect((20,38,20,46),tint(coat,22));rect((27,38,27,46),tint(coat,-24))
        rect((20,47,27,47),tint(coat,-25));rect((23,38,24,44),tint(pants,5))
        for u,v in [(40,32),(48,48)]:
            rect((u+4,v+4,u+3+arm,v+5),tint(coat,22));rect((u+4,v+12,u+3+arm,v+13),tint(coat,-28))
        # Wrists and boots on each limb, with consistent left/right UVs.
        for u,v in [(40,16),(32,48)]:
            rect((u+4,v+13,u+3+arm,v+15),skin)
        for u,v in [(0,16),(16,48)]:
            rect((u,v+13,u+15,v+15),'#19262c');rect((u+4,v+13,u+7,v+13),'#4c5654')
            d.line((u+5,v+5,u+5,v+11),fill=tint(pants,13))
        if style=='vale':
            # Silver temples, stubble, amber optical implant, white lab coat, metal right forearm.
            rect((8,8,9,10),'#9ea39a');rect((14,8,15,10),'#92988e')
            rect((9,14,10,15),'#6a665d');rect((14,14,14,15),'#6a665d')
            rect((40,9,41,12),'#a2a89e');rect((46,9,47,12),'#a2a89e')
            rect((10,12,11,12),'#9b682f');d.point((10,12),fill='#efcb6d')
            rect((25,24,27,26),'#487d75');rect((26,24,26,26),'#b7d8cf');rect((25,25,27,25),'#b7d8cf')
            rect((44,24,47,29),'#596d72');rect((44,25,47,25),'#adc1bd');rect((44,28,47,28),'#283d46')
            rect((20,45,22,46),'#a2b4a8');rect((25,45,27,46),'#a2b4a8')
        elif style=='iris':
            # Copper braids, cyan visor over one eye, work scrubs, rolled sleeves and ID badge.
            rect((40,9,41,13),hair);rect((46,9,47,15),hair)
            rect((56,8,63,15),hair)
            for x in (57,60,63):rect((x,10,x,15),tint(hair,17))
            rect((13,12,15,12),'#81d5cc');rect((13,13,14,13),'#27565d')
            rect((25,39,26,41),'#d2c3a0');d.point((25,40),fill='#315a64')
            for u,v in [(40,16),(32,48)]:rect((u+4,v+9,u+6,v+15),skin)
            rect((20,44,27,45),'#9f7f51');rect((23,44,24,45),'#c6b785')
        elif style=='mara':
            # Asymmetric black/purple bob, burgundy jacket, pale shirt and gold hardware.
            rect((40,9,41,15),hair);rect((46,9,47,12),hair)
            for x in (57,60):rect((x,9,x,15),'#44314f')
            rect((11,8,14,9),hair);rect((10,12,11,12),'#bd8f58')
            rect((22,20,25,26),'#b9b6a4');rect((21,21,21,25),'#b5aa82');rect((26,21,26,25),'#b5aa82')
            rect((22,38,22,43),'#c7a664');rect((25,38,25,43),'#c7a664')
            rect((20,44,27,45),'#2b2831');rect((24,44,25,45),'#c5a25e')
        else:
            # Swept blond undercut, visible cheek wiring, dark tactical coat and olive chest rig.
            rect((40,9,42,10),hair);rect((45,9,47,11),hair);rect((32,10,39,15),'#6d736c')
            rect((8,14,9,14),'#617e7b');rect((8,13,8,15),'#617e7b')
            rect((21,20,26,27),'#656f4e');rect((22,21,25,23),'#b5a477')
            for x in (21,24,26):rect((x,40,x,44),'#809075')
            rect((20,45,27,46),'#16292e');rect((25,24,27,26),'#a79862')
        # Deliberate hair strands and coat folds use a few nearby colours, not photographic noise.
        for u in (32,48,56):
            for x,y,h in [(u+1,9,3),(u+4,10,4),(u+6,8,3)]:
                rect((x,y,x,y+h),tint(hair,13 if style!='vale' else 8))
        for x,y in [(41,1),(44,2),(47,4),(49,1),(52,3)]:rect((x,y,x+1,y+2),tint(hair,10))
        if style=='vale':
            rect((40,9,47,11),(0,0,0,0));rect((40,9,40,12),'#969d93');rect((47,9,47,12),'#969d93')
            rect((9,9,14,10),skin)
        elif style=='iris':
            rect((46,12,47,15),(0,0,0,0));rect((46,9,47,11),hair)
        for x in (21,26):d.line((x,29,x,30),fill=tint(coat,-16))
        folder=asset_root/'textures/entity';folder.mkdir(parents=True,exist_ok=True);im.save(folder/(name+'.png'))
