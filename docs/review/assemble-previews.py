#!/usr/bin/env python3
"""Encode unedited native review frames; requires ffmpeg on PATH."""
from pathlib import Path
import argparse
import subprocess
import tempfile

parser=argparse.ArgumentParser()
parser.add_argument('capture',type=Path)
args=parser.parse_args()
out=Path(__file__).resolve().parents[1]/'media'
out.mkdir(exist_ok=True)

def make(name,start,prefix,count,end):
    # Latest frames also handle captures left over from an older review run.
    frames=sorted(args.capture.glob(prefix+'*.png'),key=lambda p:p.stat().st_mtime)[-count:]
    assert len(frames)==count,(prefix,len(frames))
    timeline=[(args.capture/start,1.0)]+[(p,.05) for p in frames]+[(args.capture/end,1.8)]
    with tempfile.TemporaryDirectory() as temp:
        listing=Path(temp)/'frames.txt'
        text=''
        for image,duration in timeline:
            assert image.exists(),image
            # ffmpeg concat file quoting, separate from shell quoting.
            safe=str(image.resolve()).replace("'", "'\\''")
            text+=f"file '{safe}'\nduration {duration}\n"
        text+=f"file '{str(timeline[-1][0].resolve())}'\n"
        listing.write_text(text)
        subprocess.run(['ffmpeg','-y','-v','error','-f','concat','-safe','0','-i',str(listing),'-vf','fps=20,format=yuv420p','-c:v','libx264','-crf','18','-preset','medium','-movflags','+faststart',str(out/(name+'.mp4'))],check=True)
make('face-zoom','installed-optics-overview.png','face-zoom-',13,'installed-optics-detail.png')
make('scanner-opening','scan-opening-0.png','scan-opening-',16,'optical-scanner.png')

# Optional new-content captures. Existing Mantis footage may be a higher-framerate native recording;
# do not overwrite it unless explicitly requested by deleting the destination first.
if (args.capture/'short-circuit-frame-000.png').exists():
    make('short-circuit','short-circuit-scanner.png','short-circuit-frame-',26,'short-circuit-frame-025.png')
if not (out/'mantis-claws.mp4').exists() and (args.capture/'mantis-deploy-000.png').exists():
    timeline=[]
    for prefix in ['mantis-deploy-','mantis-combo-','mantis-heavy-','mantis-retract-','mantis-third-']:
        timeline.extend((p,.05) for p in sorted(args.capture.glob(prefix+'[0-9]*.png')))
    with tempfile.TemporaryDirectory() as temp:
        listing=Path(temp)/'frames.txt'
        listing.write_text(''.join("file '"+str(p.resolve()).replace("'", "'\\''")+"'\nduration "+str(d)+"\n" for p,d in timeline)+"file '"+str(timeline[-1][0].resolve())+"'\n")
        subprocess.run(['ffmpeg','-y','-v','error','-f','concat','-safe','0','-i',str(listing),'-vf','fps=20,format=yuv420p','-c:v','libx264','-crf','18','-preset','medium','-movflags','+faststart',str(out/'mantis-claws.mp4')],check=True)
