#!/usr/bin/env python3
"""Assemble exact, unmodified item sprites into numbered reference figures and a searchable document."""
from pathlib import Path
import base64,csv,html,json,math,re,os,tempfile
os.environ.setdefault("MPLCONFIGDIR", str(Path(tempfile.gettempdir())/"cyber-icon-reference-mpl"))
os.environ.setdefault("XDG_CACHE_HOME", str(Path(tempfile.gettempdir())/"cyber-icon-reference-cache"))
import matplotlib
matplotlib.use('Agg')
from matplotlib.figure import Figure
from matplotlib.backends.backend_agg import FigureCanvasAgg
from matplotlib import image as mpimg
from matplotlib.font_manager import FontProperties
from matplotlib.patches import Rectangle
from reportlab.pdfgen import canvas
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.pagesizes import A3
ROOT=Path(__file__).resolve().parents[2]
ASSET=ROOT/'src/main/resources/assets/cybernetics2077'
OUT=ROOT/'docs/images/icon-index'
DOWNLOAD=ROOT/'docs/wiki/assets'
OUT.mkdir(parents=True,exist_ok=True)
lang=json.loads((ASSET/'lang/en_us.json').read_text())
category_ids=['frontal_cortex','operating_system','arms','face','skeleton','hands','nervous_system','circulatory_system','integumentary_system','legs','quickhacks','utility']
category_names={x:lang.get('cybernetics2077.category.'+x,{'quickhacks':'Quickhacks','utility':'Utility items'}.get(x,x)) for x in category_ids}
entries=[]
def add(icon,category,title=None):
    assert (ASSET/'textures/item'/(icon+'.png')).is_file(),icon
    title=title or lang.get('item.cybernetics2077.'+icon+'_tier_5') or lang.get('item.cybernetics2077.'+icon)
    assert title,icon
    if icon.startswith('iconic_requisition_tier_'):title+=' — Tier '+icon[-1]
    entries.append({'icon':icon,'category':category,'name':title})
for icon,category in re.findall(r'^\s*[A-Z_0-9]+\("([a-z_0-9]+)", CyberwareCategory\.([A-Z_]+),',(ROOT/'src/main/java/dev/cybernetics2077/cyberware/Augment.java').read_text(),re.M):add(icon,category.lower())
add('optical_scanner','face')
for icon,title in [('cyberdeck_tier_1','Militech Paraline'),('raven_microcyber',None),('arasaka',None),('biotech_sigma',None),('netwatch_netdriver',None),('tetratronic_rippler',None)]:add(icon,'operating_system',title)
for icon,title in [('mantis_claws_tier_1','Physical Mantis Blades'),('thermal_mantis_blades',None),('electrical_mantis_blades',None),('toxic_mantis_blades',None)]:add(icon,'arms',title)
for icon in ['short_circuit_tier_1','overheat','contagion','reboot_optics','cripple_movement','synapse_burnout','weapon_glitch','cyberware_malfunction']:add(icon,'quickhacks')
used={e['icon'] for e in entries}
for p in (ASSET/'textures/item').glob('*.png'):
    if p.stem not in used:add(p.stem,'utility')
entries.sort(key=lambda e:(category_ids.index(e['category']),e['name'].casefold(),e['icon']))
numberfile=Path(__file__).with_name('numbers.json')
numbering=json.loads(numberfile.read_text()) if numberfile.exists() else {}
for e in entries:
    if e['icon'] not in numbering:numbering[e['icon']]=max(numbering.values(),default=0)+1
    e['number']=numbering[e['icon']]
numberfile.write_text(json.dumps(numbering,indent=2)+'\n')
assert len(entries)==len({e['number'] for e in entries})==129
assert {e['icon'] for e in entries}=={p.stem for p in (ASSET/'textures/item').glob('*.png')}
(REGULAR,BOLD)=('/usr/share/fonts/truetype/open-sans/OpenSans-Regular.ttf','/usr/share/fonts/truetype/open-sans/OpenSans-Semibold.ttf')
pdfmetrics.registerFont(TTFont('IconRegular',REGULAR));pdfmetrics.registerFont(TTFont('IconBold',BOLD))
W,M,CW,CH,GAP=1280,26,236,232,12
BG='#0b141c';CARD='#14232d';INK='#e6e7df';MUTED='#9eafb5';ACCENT='#8cdbd2'
pages=[category_ids[:2],category_ids[2:5],category_ids[5:7],category_ids[7:9],category_ids[9:]]
records=[]
def wrap(text,width=212,size=18):
    words=text.split();lines=[];line=''
    for word in words:
        if line and pdfmetrics.stringWidth(line+' '+word,'IconRegular',size)>width:lines.append(line);line=''
        # Keep full names, including long hyphenated words.
        while pdfmetrics.stringWidth(word,'IconRegular',size)>width:
            cut=max(i for i in range(1,len(word)) if pdfmetrics.stringWidth(word[:i],'IconRegular',size)<=width)
            if line:lines.append(line);line=''
            lines.append(word[:cut]);word=word[cut:]
        line=(line+' '+word).strip()
    if line:lines.append(line)
    assert len(lines)<=4,(text,lines)
    return lines
pdf=canvas.Canvas(str(DOWNLOAD/'icon-reference.pdf'),pagesize=A3)
pdf.setTitle('Cybernetics 2077 — Numbered Icon Reference')
for page,groups in enumerate(pages,1):
    subset=[e for e in entries if e['category'] in groups]
    H=106+sum(42+math.ceil(sum(e['category']==g for e in subset)/5)*(CH+GAP) for g in groups)+36
    fig=Figure(figsize=(W/100,H/100),dpi=100,facecolor=BG);FigureCanvasAgg(fig);ax=fig.add_axes([0,0,1,1]);ax.set_xlim(0,W);ax.set_ylim(H,0);ax.set_axis_off();ax.set_autoscale_on(False)
    pw,ph=A3;scale=min(pw/W,ph/H);pdf.saveState();pdf.translate((pw-W*scale)/2,(ph-H*scale)/2);pdf.scale(scale,scale);pdf.setFillColor(BG);pdf.rect(0,0,W,H,fill=1,stroke=0)
    def text(x,y,s,size=18,color=INK,bold=False,center=False):
        ax.text(x,y,s,ha='center' if center else 'left',va='top',color=color,fontproperties=FontProperties(fname=BOLD if bold else REGULAR,size=size*.72))
        pdf.setFillColor(color);pdf.setFont('IconBold' if bold else 'IconRegular',size);(pdf.drawCentredString if center else pdf.drawString)(x,H-y-size*.8,s)
    text(M,20,'CYBERNETICS 2077  /  ICON REFERENCE',26,ACCENT,True)
    text(M,60,f"Sheet {page} of 5  ·  #{min(e['number'] for e in subset):03d}–#{max(e['number'] for e in subset):03d}",18,MUTED)
    y=106
    for group in groups:
        rows=[e for e in entries if e['category']==group]
        text(M,y,category_names[group].upper(),21,ACCENT,True);y+=42
        for i,e in enumerate(rows):
            x=M+(i%5)*(CW+GAP);top=y+(i//5)*(CH+GAP)
            ax.add_patch(Rectangle((x,top),CW,CH,facecolor=CARD,edgecolor='none',zorder=1))
            pdf.setFillColor(CARD);pdf.rect(x,H-top-CH,CW,CH,fill=1,stroke=0)
            text(x+12,top+10,'#'+str(e['number']).zfill(3),24,ACCENT,True)
            icon=ASSET/'textures/item'/(e['icon']+'.png');ix=x+(CW-96)/2;iy=top+43
            ax.imshow(mpimg.imread(icon),extent=(ix,ix+96,iy+96,iy),interpolation='nearest',zorder=3,aspect='auto')
            pdf.drawImage(ImageReader(str(icon)),ix,H-iy-96,96,96,mask='auto')
            for line,s in enumerate(wrap(e['name'])):text(x+CW/2,top+152+line*20,s,18,INK,center=True)
        y+=math.ceil(len(rows)/5)*(CH+GAP)
    text(M,H-26,'Current 0.28.2 artwork · exact item textures · use the #number to identify an icon',15,MUTED)
    filename=f'sheet-{page:02d}.png';fig.savefig(OUT/filename,dpi=100,facecolor=BG);fig.clear();pdf.restoreState();pdf.showPage()
    records.append({'sheet':page,'file':filename,'first':min(e['number'] for e in subset),'last':max(e['number'] for e in subset),'count':len(subset),'categories':groups})
pdf.save()
with (DOWNLOAD/'icon-reference.csv').open('w',newline='') as f:
    writer=csv.writer(f,lineterminator="\n");writer.writerow(['Number','Name','Category','Texture'])
    for e in entries:writer.writerow([e['number'],e['name'],category_names[e['category']],e['icon']])
cards=[]
for e in entries:
    data=base64.b64encode((ASSET/'textures/item'/(e['icon']+'.png')).read_bytes()).decode()
    cards.append(f'<article data-number="{e["number"]}" data-search="{html.escape(str(e["number"])+" "+e["name"]+" "+category_names[e["category"]],quote=True)}"><b>#{e["number"]:03d}</b><img src="data:image/png;base64,{data}" alt="{html.escape(e["name"],quote=True)}"><h2>{html.escape(e["name"])}</h2><small>{html.escape(category_names[e["category"]])}</small></article>')
pagehtml='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Cybernetics 2077 — Numbered Icons</title><style>*{box-sizing:border-box}body{margin:0;background:#0b141c;color:#e6e7df;font:16px system-ui,sans-serif}header{position:sticky;top:0;padding:18px 24px;background:#0b141cf5;border-bottom:1px solid #39545e;z-index:1}h1{margin:0 0 10px;font-size:24px;color:#8cdbd2}header p{margin:8px 0;color:#9eafb5}input{width:min(600px,100%);padding:12px;border:1px solid #6a9397;border-radius:4px;background:#14232d;color:#fff;font:inherit}main{display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:12px;padding:24px;max-width:1500px;margin:auto}article{background:#14232d;padding:14px;min-height:220px;display:flex;flex-direction:column;align-items:center;text-align:center}b{align-self:flex-start;color:#8cdbd2;font-size:22px}img{width:96px;height:96px;image-rendering:pixelated;margin:8px}h2{font-size:16px;font-weight:600;margin:8px 0;overflow-wrap:anywhere}small{color:#9eafb5}article[hidden]{display:none}</style><header><h1>CYBERNETICS 2077 / NUMBERED ICONS</h1><p>All 129 item sprites. One entry per implant/program design; utility tiers are shown separately.</p><input id="search" type="search" placeholder="Find a name, category or #number" aria-label="Search icons"><p id="count">129 icons</p></header><main>'''+''.join(cards)+r'''</main><script>const cards=[...document.querySelectorAll('article')];document.querySelector('#search').addEventListener('input',e=>{const q=e.target.value.toLowerCase().trim(),number=/^#?\d+$/.test(q)?String(Number(q.replace('#',''))):null;let n=0;for(const card of cards){card.hidden=number!==null?card.dataset.number!==number:!card.dataset.search.toLowerCase().includes(q);if(!card.hidden)n++}document.querySelector('#count').textContent=n+' icons';});</script></html>'''
(DOWNLOAD/'icon-reference.html').write_text(pagehtml)
(OUT/'index.json').write_text(json.dumps({'version':'0.28.2','entries':entries,'sheets':records},indent=2,ensure_ascii=False)+'\n')
print('129 unique icons; 5 numbered/name-labelled PNG sheets; 5-page PDF; self-contained searchable HTML; CSV.')
