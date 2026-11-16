#!/usr/bin/env python3
"""Check the standalone JAR without starting Minecraft."""
from pathlib import Path
import json
import sys
import tomllib
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
jar = Path(sys.argv[1]) if len(sys.argv) > 1 else root / 'build/libs/Cybernetics2077-0.1.0.jar'
with ZipFile(jar) as z:
    names = set(z.namelist())
    metadata = tomllib.loads(z.read('META-INF/neoforge.mods.toml').decode())
    assert [m['modId'] for m in metadata['mods']] == ['cybernetics2077']
    assert {d['modId'] for d in metadata['dependencies']['cybernetics2077']} == {'minecraft', 'neoforge'}
    forbidden = ('review/', 'review_cortex', 'structure/empty.nbt', 'dev/heavenlyrestriction/', 'dev/wildhunt/', 'tensura', 'marcos')
    assert not [n for n in names if any(s in n.lower() for s in forbidden)], 'Foreign content or review fixtures in release'
    for path in (root / 'src/main/resources').rglob('*'):
        if path.is_file():
            entry = path.relative_to(root / 'src/main/resources').as_posix()
            assert entry in names and z.read(entry) == path.read_bytes(), f'Missing/stale resource: {entry}'
            if path.suffix == '.json':
                json.loads(z.read(entry))
    for required in ('dev/cybernetics2077/Cybernetics2077.class',
                     'dev/cybernetics2077/client/ui/CyberwareScreen.class',
                     'dev/cybernetics2077/client/ui/HologramRenderer.class',
                     'assets/cybernetics2077/font/rajdhani-license.txt'):
        assert required in names, required
print(f'PASS: standalone metadata, classes, resource parity, JSON, font license, no review fixtures: {jar.name}')
