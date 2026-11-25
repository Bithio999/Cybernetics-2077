#!/usr/bin/env python3
"""Check the standalone JAR without starting Minecraft."""
from pathlib import Path
import json
import sys
import struct
import tomllib
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
jar = Path(sys.argv[1]) if len(sys.argv) > 1 else root / 'build/libs/Cybernetics2077-0.16.0.jar'
with ZipFile(jar) as z:
    names = set(z.namelist())
    metadata = tomllib.loads(z.read('META-INF/neoforge.mods.toml').decode())
    assert metadata['mods'][0]['version']=='0.16.0'
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
                     'dev/cybernetics2077/mob/EnhancedMobs.class',
                     'dev/cybernetics2077/mob/SalvageOdds.class',
                     'dev/cybernetics2077/mob/DamagedCyberneticPart.class',
                     'dev/cybernetics2077/client/EnhancedMobLayer.class',
                     'dev/cybernetics2077/city/DeliveryContracts.class',
                     'dev/cybernetics2077/combat/QuickhackEffects.class',
                     'dev/cybernetics2077/deck/QuickhackKind.class',
                     'dev/cybernetics2077/city/DailyStock.class',
                     'dev/cybernetics2077/city/DeliveryContract.class',
                     'dev/cybernetics2077/city/ClinicDirectory.class',
                     'assets/cybernetics2077/models/item/delivery_contract.json',
                     'assets/cybernetics2077/textures/item/delivery_contract.png',
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
                     'dev/cybernetics2077/cyberware/AugmentExpansion.class',
                     'dev/cybernetics2077/temporal/Sandevistan.class',
                     'dev/cybernetics2077/client/temporal/TemporalMesh.class',
                     'dev/cybernetics2077/client/temporal/TrailStyle.class',
                     'dev/cybernetics2077/client/temporal/SandevistanLens.class',
                     'data/cybernetics2077/damage_type/cybernetic_discharge.json',
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
    recipes = {n for n in names if n.startswith('data/cybernetics2077/recipe/') and n.endswith('.json')}
    unlocks = {n for n in names if n.startswith('data/cybernetics2077/advancement/recipes/') and n.endswith('.json')}
    assert recipes == {'data/cybernetics2077/recipe/cybernetics_bench.json'}, 'Cyberware crafting/upgrade recipes shipped'
    assert unlocks == {'data/cybernetics2077/advancement/recipes/cybernetics_bench.json'}, 'Removed recipe unlocks shipped'
    bench = json.loads(z.read(next(iter(recipes))))
    assert bench['type'] == 'minecraft:crafting_shaped' and bench['result']['id'] == 'cybernetics2077:cybernetics_bench'
    advancement = json.loads(z.read(next(iter(unlocks))))
    assert advancement['rewards']['recipes'] == ['cybernetics2077:cybernetics_bench']
    lang = json.loads(z.read('assets/cybernetics2077/lang/en_us.json'))
    for tier in range(1,6):
        part=f'damaged_cybernetic_part_tier_{tier}'
        assert lang['item.cybernetics2077.'+part].endswith(f'Tier {tier}')
        model=json.loads(z.read(f'assets/cybernetics2077/models/item/{part}.json'))
        assert model['textures']['layer0']=='cybernetics2077:item/'+part
        png=z.read(f'assets/cybernetics2077/textures/item/{part}.png')
        assert struct.unpack('>II',png[16:24])==(32,32)
    for original, prefix, title in (
        ('optical_scanner', 'kiroshi_optics', 'Basic Kiroshi Optics'),
        ('cyberdeck_tier_1', 'militech_paraline', 'Militech Paraline'),
        ('mantis_claws_tier_1', 'mantis_blades', 'Mantis Blades'),
        ('short_circuit_tier_1', 'short_circuit', 'Short Circuit')):
        assert lang['item.cybernetics2077.' + original] == title
        for tier in range(2, 6):
            item = f'{prefix}_tier_{tier}'
            assert lang['item.cybernetics2077.' + item] == title
            model = json.loads(z.read(f'assets/cybernetics2077/models/item/{item}.json'))
            assert model['parent'] == 'cybernetics2077:item/' + original
    for tier in range(3,6):
        item=f'raven_microcyber_tier_{tier}'
        assert lang['item.cybernetics2077.'+item]=='Raven Microcyber'
        assert f'assets/cybernetics2077/models/item/{item}.json' in names
    for kind, first in {'overheat':1,'contagion':2,'reboot_optics':1,'cripple_movement':2,'synapse_burnout':3}.items():
        png=z.read(f'assets/cybernetics2077/textures/item/{kind}.png')
        assert struct.unpack('>II',png[16:24])==(32,32)
        for tier in range(first,6):
            item=f'{kind}_tier_{tier}'
            assert 'item.cybernetics2077.'+item in lang
            assert f'assets/cybernetics2077/models/item/{item}.json' in names
        for tier in range(1,first):
            assert f'assets/cybernetics2077/models/item/{kind}_tier_{tier}.json' not in names
    augment_starts = {
        'ex_disk': 3, 'memory_boost': 2, 'biodyne_berserk': 2, 'kiroshi_clairvoyant_optics': 3,
        'bionic_joints': 1, 'epimorphic_skeleton': 4, 'neofiber': 3, 'adrenaline_converter': 1,
        'blood_pump': 2, 'heal_on_kill': 1, 'subdermal_armor': 1, 'optical_camo': 2,
        'reinforced_tendons': 2, 'fortified_ankles': 2, 'dynalar_sandevistan': 2,
        'ram_upgrade': 1,
        'self_ice': 3,
        'kiroshi_doomsayer_optics': 1,
        'kiroshi_stalker_optics': 1,
        'dense_marrow': 2,
        'para_bellum': 3,
        'stabber': 2,
        'visual_cortex_support': 1,
        'biomonitor': 1,
        'second_heart': 4,
        'shock_n_awe': 1,
        'pain_editor': 4,
        'lynx_paws': 2,
        'jenkins_tendons': 2,
        'smart_link': 1, 'ballistic_coprocessor': 1, 'microgenerator': 1,
        'handle_wrap': 3, 'shock_absorber': 1, 'immovable_force': 2,
    }
    assert 'data/cybernetics2077/recipe/mantis_claws_tier_1.json' not in names
    assert not any('CrossbowReloadMixin' in n for n in names), 'Obsolete crossbow reload hook shipped'
    for variant, title in [('thermal', 'Thermal'), ('electrical', 'Electrifying'), ('toxic', 'Toxic')]:
        icon = variant + '_mantis_blades'
        assert struct.unpack('>II', z.read(f'assets/cybernetics2077/textures/item/{icon}.png')[16:24]) == (32, 32)
        assert struct.unpack('>II', z.read(f'assets/cybernetics2077/textures/entity/mantis_{variant}.png')[16:24]) == (512, 544)
        for lod in ('near', 'far'):
            assert f'assets/cybernetics2077/meshes/mantis/{variant}_{lod}.cymesh' in names
        for tier in range(2, 6):
            item = f'{icon}_tier_{tier}'
            assert lang['item.cybernetics2077.' + item] == title + ' Mantis Blades'
            assert f'assets/cybernetics2077/models/item/{item}.json' in names
        assert f'assets/cybernetics2077/models/item/{icon}_tier_1.json' not in names
    for cls in ('combat/HandsRuntime', 'combat/HandsRules', 'combat/MantisEffects',
                'combat/SecondaryDamage', 'cyberware/MantisVariant', 'client/ui/HandsClient',
                'mixin/LivingDamageMemory'):
        assert f'dev/cybernetics2077/{cls}.class' in names
    for damage in ('mantis_thermal', 'mantis_electrical', 'mantis_toxic', 'mantis_bleed', 'hand_discharge'):
        assert f'data/cybernetics2077/damage_type/{damage}.json' in names

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
    for sound in ('sandy_on', 'sandy_off', 'sandy_hum'):
        assert z.read(f'assets/cybernetics2077/sounds/{sound}.ogg').startswith(b'OggS')
        assert sound in json.loads(z.read('assets/cybernetics2077/sounds.json'))
    for shader in ('temporal_ghost', 'temporal_lens'):
        for extension in ('json', 'vsh', 'fsh'):
            assert f'assets/cybernetics2077/shaders/core/{shader}.{extension}' in names
    for mixin in json.loads(z.read('cybernetics2077.mixins.json'))['client']:
        assert 'dev/cybernetics2077/mixin/client/'+mixin+'.class' in names
    for mixin in json.loads(z.read('cybernetics2077.common.mixins.json'))['mixins']:
        assert 'dev/cybernetics2077/mixin/'+mixin+'.class' in names
    city_blocks=('reinforced_panel','clinic_tile','ribbed_floor','medical_cabinet','vent_grille',
                 'diagnostic_monitor','surgical_chair','surgical_footrest','ceiling_truss',
                 'surgical_light','service_pipe','cyan_strip','red_strip','green_strip',
                 'bar_counter','bar_stool','ripperdoc_sign','afterlife_sign','bottle_shelf',
                 'bar_corner','canopy_panel','ceiling_diffuser','recessed_panel','facade_duct','booth_seat')
    for block in city_blocks:
        assert 'block.cybernetics2077.'+block in lang
        model=json.loads(z.read(f'assets/cybernetics2077/models/block/{block}.json'))
        assert model['parent']=='minecraft:block/block', 'Furniture lacks standard block display inheritance'
        item_model=json.loads(z.read(f'assets/cybernetics2077/models/item/{block}.json'))
        for view,budget in [('gui',.625),('firstperson_righthand',.4),('firstperson_lefthand',.4),('thirdperson_righthand',.375),('thirdperson_lefthand',.375),('ground',.25),('fixed',.5)]:
            assert view in item_model['display'] and max(item_model['display'][view]['scale'])<=budget
        states=json.loads(z.read(f'assets/cybernetics2077/blockstates/{block}.json'))
        assert set(states['variants'])=={'facing='+d for d in ('north','east','south','west')}
        assert f'assets/cybernetics2077/models/item/{block}.json' in names
        assert f'data/cybernetics2077/loot_table/blocks/{block}.json' in names
        for texture in model['textures'].values():
            assert 'assets/'+texture.replace(':','/textures/')+'.png' in names, texture
        for part in model['elements']:
            assert all(-16<=v<=32 for v in part['from']+part['to']), 'Out-of-range geometry'
            if part['from']==[0,0,0] and part['to']==[16,16,16]:
                assert all(face.get('cullface')==name for name,face in part['faces'].items()), 'Unculled full cube'
    for skin in ('ripperdoc','ripperdoc_iris','fixer_mara','fixer_ren'):
        png=z.read('assets/cybernetics2077/textures/entity/'+skin+'.png');assert struct.unpack('>II',png[16:24])==(64,64)
    for cls in ('CityContent','CityPoolElement','VillageServices','CityResident','RipperdocMenu','FixerMenu','StreetAccount','CyberwareCatalogue'):
        assert f'dev/cybernetics2077/city/{cls}.class' in names
    for cls in ('RipperdocScreen','FixerScreen','CityResidentRenderer'):
        assert f'dev/cybernetics2077/client/ui/{cls}.class' in names
    sys.path.insert(0,str(root/'art/city'))
    from nbt import read as read_structure
    for building,people in [('clinic_small',1),('clinic_large',1),('afterlife',2)]:
        template=read_structure(z.read(f'data/cybernetics2077/structure/village/{building}.nbt'))
        assert len(template['entities'])==people
        assert sum(template['palette'][b['state']]['Name']=='minecraft:jigsaw' for b in template['blocks'])==3
        assert sum(template['palette'][b['state']]['Name']=='minecraft:barrel' for b in template['blocks'])==1
        for entry in template['palette']:
            if entry['Name'].startswith('cybernetics2077:'):
                assert entry['Name'].split(':')[1] in city_blocks
        pool=json.loads(z.read(f'data/cybernetics2077/worldgen/template_pool/village/{building}.json'))
        assert pool['elements'][0]['element']['element_type']=='cybernetics2077:supported_building'
        assert f'data/cybernetics2077/loot_table/chests/{building}.json' in names
print(f'PASS: standalone metadata, classes, resource parity, JSON, font license, no review fixtures: {jar.name}')
