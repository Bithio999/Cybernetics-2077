#!/usr/bin/env python3
"""Trim a desktop recording using timestamps of this run's native captures."""
import argparse
import json
import subprocess
from pathlib import Path

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('recording', type=Path)
p.add_argument('capture', type=Path)
p.add_argument('recording_start', type=float, help='Unix time immediately before starting ffmpeg')
p.add_argument('--fps', type=int, default=30)
p.add_argument('--mantis-only', action='store_true')
a = p.parse_args()
out = Path(__file__).resolve().parents[1] / 'media'
out.mkdir(exist_ok=True)

marks_path = a.capture / 'recording-marks.json'
marks = json.loads(marks_path.read_text()) if marks_path.exists() else {}
def timestamp(name):
    return marks[name] if name in marks else (a.capture / (name + '.png')).stat().st_mtime

def encode(name, segments):
    filters = []
    for i, (first, last) in enumerate(segments):
        start = timestamp(first) - a.recording_start - .15
        end = timestamp(last) - a.recording_start + .20
        assert 0 <= start < end, (first, start, end)
        filters.append(f'[0:v]trim=start={start:.3f}:end={end:.3f},setpts=PTS-STARTPTS[v{i}]')
    filters.append(''.join(f'[v{i}]' for i in range(len(segments))) +
                   f'concat=n={len(segments)}:v=1:a=0,fps={a.fps},format=yuv420p[v]')
    subprocess.run(['ffmpeg', '-y', '-v', 'error', '-threads', '2', '-i', str(a.recording),
                    '-filter_complex_threads', '2', '-filter_complex', ';'.join(filters),
                    '-map', '[v]', '-c:v', 'libx264', '-threads', '2', '-preset', 'medium',
                    '-crf', '18', '-movflags', '+faststart', str(out / (name + '.mp4'))], check=True)

encode('mantis-claws', [
    ('mantis-native-hand', 'mantis-retract-026'),
    ('mantis-ground-leap-000', 'mantis-ground-leap-017'),
    ('mantis-air-leap-000', 'mantis-air-leap-019'),
    ('mantis-free-leap-000', 'mantis-free-leap-023'),
    ('mantis-connected-idle', 'mantis-connected-cut-013')])
if not a.mantis_only:
    encode('short-circuit', [('short-circuit-scanner', 'short-circuit-frame-025')])

if not a.mantis_only:
    encode('quickhack-management', [('cyberware-quickhack-entry', 'cyberware-quickhack-return')])

encode('mantis-mechanism', [('mantis-dorsal-open', 'mantis-dorsal-closed'), ('mantis-dorsal-deploy-000', 'mantis-dorsal-ready')])
