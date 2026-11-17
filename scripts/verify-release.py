#!/usr/bin/env python3
"""Check the standalone JAR without starting Minecraft."""
from pathlib import Path
import json
import sys
import struct
import tomllib
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
jar = Path(sys.argv[1]) if len(sys.argv) > 1 else root / 'build/libs/Cybernetics2077-0.5.2.jar'
with ZipFile(jar) as z:
    names = set(z.namelist())
    metadata = tomllib.loads(z.read('META-INF/neoforge.mods.toml').decode())
    assert metadata['mods'][0]['version']=='0.5.2'
    assert [m['modId'] for m in metadata['mods']] == ['cybernetics2077']
    assert {d['modId'] for d in metadata['dependencies']['cybernetics2077']} == {'minecraft', 'neoforge'}
    forbidden = ('review/', 'armskinboundsreview', 'review_cortex', 'structure/empty.nbt', 'dev/heavenlyrestriction/', 'dev/wildhunt/', 'tensura', 'marcos')
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
                     'dev/cybernetics2077/bench/CyberneticsBenchMenu.class',
                     'dev/cybernetics2077/client/ui/ScannerClient.class',
                     'dev/cybernetics2077/deck/CyberdeckMenu.class',
                     'dev/cybernetics2077/combat/CyberwareRuntime.class',
                     'dev/cybernetics2077/client/mantis/MantisMesh.class',
                     'dev/cybernetics2077/client/mantis/MantisArticulation.class',
                     'dev/cybernetics2077/client/mantis/MantisMeshData.class',
                     'dev/cybernetics2077/client/mantis/MantisBlenderMesh.class',
                     'assets/cybernetics2077/meshes/mantis/near.cymesh',
                     'assets/cybernetics2077/meshes/mantis/far.cymesh',
                     'assets/cybernetics2077/textures/entity/mantis_blender.png',
                     'dev/cybernetics2077/client/mantis/MantisRig.class',
                     'dev/cybernetics2077/client/mantis/MantisView.class',
                     'dev/cybernetics2077/client/mantis/MantisDeployment.class',
                     'dev/cybernetics2077/client/mantis/NativeArmSkin.class',
                     'dev/cybernetics2077/combat/LungePath.class',
                     'dev/cybernetics2077/mixin/client/MantisTravelMixin.class',
                     'assets/cybernetics2077/textures/item/cyberdeck_tier_1.png',
                     'assets/cybernetics2077/textures/item/mantis_claws_tier_1.png',
                     'data/cybernetics2077/damage_type/short_circuit.json',
                     'cybernetics2077.mixins.json',
                     'assets/cybernetics2077/font/rajdhani-license.txt'):
        assert required in names, required
    assert not any(n.endswith(('.blend','.py','.cyh')) or 'DiagnosticModel' in n for n in names), 'Replaced hologram pipeline leaked into release'
    assert 'assets/cybernetics2077/textures/entity/diagnostic.png' not in names, 'Diagnostic placeholder skin still shipped'
    for icon in ('optical_scanner','cyberdeck_tier_1','mantis_claws_tier_1','short_circuit_tier_1'):
        png=z.read('assets/cybernetics2077/textures/item/'+icon+'.png')
        assert struct.unpack('>II',png[16:24]) == (32,32), 'Equipment sprite is not 32px pixel art: '+icon
    for mixin in json.loads(z.read('cybernetics2077.mixins.json'))['client']:
        assert 'dev/cybernetics2077/mixin/client/'+mixin+'.class' in names
print(f'PASS: standalone metadata, classes, resource parity, JSON, font license, no review fixtures: {jar.name}')
