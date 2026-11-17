#!/usr/bin/env python3
"""Trim a 60-fps desktop recording using timestamps of this run's native captures."""
import argparse
import subprocess
from pathlib import Path

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('recording', type=Path)
p.add_argument('capture', type=Path)
p.add_argument('recording_start', type=float, help='Unix time immediately before starting ffmpeg')
a = p.parse_args()
out = Path(__file__).resolve().parents[1] / 'media'
out.mkdir(exist_ok=True)

def encode(name, segments):
    filters = []
    for i, (first, last) in enumerate(segments):
        start = (a.capture / (first + '.png')).stat().st_mtime - a.recording_start - .15
        end = (a.capture / (last + '.png')).stat().st_mtime - a.recording_start + .20
        assert 0 <= start < end, (first, start, end)
        filters.append(f'[0:v]trim=start={start:.3f}:end={end:.3f},setpts=PTS-STARTPTS[v{i}]')
    filters.append(''.join(f'[v{i}]' for i in range(len(segments))) +
                   f'concat=n={len(segments)}:v=1:a=0,fps=60,format=yuv420p[v]')
    subprocess.run(['ffmpeg', '-y', '-v', 'error', '-threads', '2', '-i', str(a.recording),
                    '-filter_complex_threads', '2', '-filter_complex', ';'.join(filters),
                    '-map', '[v]', '-c:v', 'libx264', '-threads', '2', '-preset', 'medium',
                    '-crf', '18', '-movflags', '+faststart', str(out / (name + '.mp4'))], check=True)

encode('mantis-claws', [
    ('mantis-deploy-000', 'mantis-first-person'),
    ('mantis-combo-000', 'mantis-combo-052'),
    ('mantis-heavy-000', 'mantis-heavy-040'),
    ('mantis-retract-000', 'mantis-retract-026'),
    ('mantis-ground-leap-000', 'mantis-ground-leap-017'),
    ('mantis-air-leap-000', 'mantis-air-leap-019'),
    ('mantis-free-leap-000', 'mantis-free-leap-023'),
    ('mantis-connected-idle', 'mantis-connected-cut-013')])
encode('short-circuit', [('short-circuit-scanner', 'short-circuit-frame-025')])

encode('quickhack-management', [('cyberware-quickhack-entry', 'cyberware-quickhack-return')])
