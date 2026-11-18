#!/usr/bin/env python3
"""Check the standalone JAR without starting Minecraft."""
from pathlib import Path
import json
import sys
import struct
import tomllib
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
jar = Path(sys.argv[1]) if len(sys.argv) > 1 else root / 'build/libs/Cybernetics2077-0.8.0.jar'
with ZipFile(jar) as z:
    names = set(z.namelist())
    metadata = tomllib.loads(z.read('META-INF/neoforge.mods.toml').decode())
    assert metadata['mods'][0]['version']=='0.8.0'
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
                     'dev/cybernetics2077/client/mantis/MantisMaterial.class',
                     'dev/cybernetics2077/client/mantis/MantisSkinPanels.class',
                     'assets/cybernetics2077/meshes/mantis/near.cymesh',
                     'assets/cybernetics2077/meshes/mantis/far.cymesh',
                     'assets/cybernetics2077/textures/entity/mantis_blender.png',
                     'dev/cybernetics2077/client/mantis/MantisRig.class',
                     'dev/cybernetics2077/client/mantis/MantisView.class',
                     'dev/cybernetics2077/client/mantis/MantisDeployment.class',
                     'dev/cybernetics2077/client/mantis/NativeArmSkin.class',
                     'dev/cybernetics2077/combat/LungePath.class',
                     'dev/cybernetics2077/combat/MantisHands.class',
                     'dev/cybernetics2077/cyberware/OpticsItem.class',
                     'dev/cybernetics2077/cyberware/MantisBladesItem.class',
                     'dev/cybernetics2077/cyberware/CyberwareTier.class',
                     'dev/cybernetics2077/cyberware/Augment.class',
                     'dev/cybernetics2077/cyberware/AugmentItem.class',
                     'dev/cybernetics2077/cyberware/AugmentState.class',
                     'dev/cybernetics2077/cyberware/AugmentRuntime.class',
                     'dev/cybernetics2077/client/ui/AugmentClient.class',
                     'dev/cybernetics2077/mixin/OpticalCamoMixin.class',
                     'dev/cybernetics2077/mixin/MantisHandsMixin.class',
                     'cybernetics2077.common.mixins.json',
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
    lang = json.loads(z.read('assets/cybernetics2077/lang/en_us.json'))
    for original, prefix, title in (
        ('optical_scanner', 'kiroshi_optics', 'Basic Kiroshi Optics'),
        ('cyberdeck_tier_1', 'militech_paraline', 'Militech Paraline'),
        ('mantis_claws_tier_1', 'mantis_blades', 'Mantis Blades'),
        ('short_circuit_tier_1', 'short_circuit', 'Short Circuit')):
        assert lang['item.cybernetics2077.' + original] == title
        for tier in range(2, 6):
            item = f'{prefix}_tier_{tier}'
            previous = original if tier == 2 else f'{prefix}_tier_{tier - 1}'
            assert lang['item.cybernetics2077.' + item] == title
            model = json.loads(z.read(f'assets/cybernetics2077/models/item/{item}.json'))
            assert model['parent'] == 'cybernetics2077:item/' + original
            if prefix == 'mantis_blades' and tier == 2:
                assert f'data/cybernetics2077/recipe/{item}.json' not in names
                continue
            recipe = json.loads(z.read(f'data/cybernetics2077/recipe/{item}.json'))
            assert recipe['type'] == 'minecraft:smithing_transform'
            assert recipe['base']['item'] == 'cybernetics2077:' + previous
            assert recipe['result']['id'] == 'cybernetics2077:' + item
    augment_starts = {
        'ex_disk': 3, 'memory_boost': 2, 'biodyne_berserk': 2, 'kiroshi_clairvoyant_optics': 3,
        'bionic_joints': 1, 'epimorphic_skeleton': 4, 'neofiber': 3, 'adrenaline_converter': 1,
        'blood_pump': 2, 'heal_on_kill': 1, 'subdermal_armor': 1, 'optical_camo': 2,
        'reinforced_tendons': 2, 'fortified_ankles': 2}
    assert 'data/cybernetics2077/recipe/mantis_claws_tier_1.json' not in names
    assert not any('microgenerator' in n or 'CrossbowReloadMixin' in n for n in names), 'Withdrawn hands content shipped'
    for implant, first in augment_starts.items():
        png = z.read(f'assets/cybernetics2077/textures/item/{implant}.png')
        assert struct.unpack('>II', png[16:24]) == (32, 32)
        for tier in range(first, 6):
            item = f'{implant}_tier_{tier}'
            assert lang['item.cybernetics2077.' + item] == lang['cybernetics2077.augment.' + implant]
            model = json.loads(z.read(f'assets/cybernetics2077/models/item/{item}.json'))
            assert model['textures']['layer0'] == 'cybernetics2077:item/' + implant
            assert f'data/cybernetics2077/recipe/{item}.json' not in names, 'Unrequested expansion recipe'
        if first > 1:
            assert f'assets/cybernetics2077/models/item/{implant}_tier_{first - 1}.json' not in names
    for mixin in json.loads(z.read('cybernetics2077.mixins.json'))['client']:
        assert 'dev/cybernetics2077/mixin/client/'+mixin+'.class' in names
    for mixin in json.loads(z.read('cybernetics2077.common.mixins.json'))['mixins']:
        assert 'dev/cybernetics2077/mixin/'+mixin+'.class' in names
print(f'PASS: standalone metadata, classes, resource parity, JSON, font license, no review fixtures: {jar.name}')
