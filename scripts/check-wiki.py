#!/usr/bin/env python3
"""Check wiki navigation, local Markdown links, anchors and coverage of shipped item families."""
from pathlib import Path
import re
import sys
import urllib.parse
import yaml
import markdown

ROOT = Path(__file__).resolve().parents[1]
WIKI = ROOT / 'docs/wiki'
config = yaml.safe_load((ROOT / 'mkdocs.yml').read_text())
errors = []

def leaves(node):
    if isinstance(node, str):
        yield node
    elif isinstance(node, list):
        for child in node:
            yield from leaves(child)
    elif isinstance(node, dict):
        for child in node.values():
            yield from leaves(child)

nav = list(leaves(config['nav']))
pages = {p.relative_to(WIKI).as_posix() for p in WIKI.rglob('*.md')}
if len(nav) != len(set(nav)):
    errors.append('Duplicate pages in wiki navigation')
if set(nav) != pages:
    errors.append(f'Navigation mismatch: missing {pages-set(nav)}, unknown {set(nav)-pages}')
anchors = {}
for relative in pages:
    path = WIKI / relative
    html = markdown.markdown(path.read_text(), extensions=['tables', 'toc', 'attr_list', 'md_in_html'])
    anchors[path.resolve()] = set(re.findall(r'id="([^"]+)"', html))

for path in [ROOT / 'README.md', ROOT / 'docs/README.md', ROOT / 'docs/development/README.md',
             ROOT / 'releases/README.md', *sorted(WIKI.rglob('*.md'))]:
    content = path.read_text()
    if path.parent == WIKI and not content.startswith('# '):
        errors.append(f'{path.name}: missing page title')
    urls = re.findall(r'!?\[[^\]]*\]\(([^)\s]+)\)', content)
    urls += re.findall(r'(?:src|href)="([^"]+)"', content)
    for raw in urls:
        url = urllib.parse.urlsplit(raw)
        if url.scheme or url.netloc:
            continue
        target = (path.parent / urllib.parse.unquote(url.path)).resolve() if url.path else path.resolve()
        if not target.exists():
            errors.append(f'{path.relative_to(ROOT)}: missing {raw}')
        elif url.fragment and target in anchors and url.fragment not in anchors[target]:
            errors.append(f'{path.relative_to(ROOT)}: missing anchor {raw}')

families = {
    'optical_scanner':'kiroshi-optics', 'cyberdeck_tier_1':'militech-paraline',
    'raven_microcyber':'raven-microcyber', 'mantis_claws_tier_1':'mantis-blades',
    'dynalar_sandevistan':'dynalar-sandevistan',
    'ex_disk':'ex-disk', 'memory_boost':'memory-boost', 'biodyne_berserk':'biodyne-berserk',
    'kiroshi_clairvoyant_optics':'clairvoyant-optics', 'bionic_joints':'bionic-joints',
    'epimorphic_skeleton':'epimorphic-skeleton', 'neofiber':'neofiber',
    'adrenaline_converter':'adrenaline-converter', 'blood_pump':'blood-pump',
    'heal_on_kill':'heal-on-kill', 'subdermal_armor':'subdermal-armor',
    'optical_camo':'optical-camo', 'reinforced_tendons':'reinforced-tendons',
    'fortified_ankles':'fortified-ankles',
    'ram_upgrade':'ram-upgrade',
    'self_ice':'self-ice',
    'kiroshi_doomsayer_optics':'doomsayer-optics',
    'kiroshi_stalker_optics':'stalker-optics',
    'dense_marrow':'dense-marrow',
    'para_bellum':'para-bellum',
    'stabber':'stabber',
    'visual_cortex_support':'visual-cortex-support',
    'biomonitor':'biomonitor',
    'second_heart':'second-heart',
    'shock_n_awe':'shock-n-awe',
    'pain_editor':'pain-editor',
    'lynx_paws':'lynx-paws',
    'jenkins_tendons':'jenkins-tendons',
    'short_circuit_tier_1':'short-circuit', 'overheat':'overheat', 'contagion':'contagion',
    'reboot_optics':'reboot-optics', 'cripple_movement':'cripple-movement', 'synapse_burnout':'synapse-burnout',
}
for definition in ('cyberware/Augment.java', 'deck/QuickhackKind.java'):
    source = (ROOT / 'src/main/java/dev/cybernetics2077' / definition).read_text()
    for identifier in re.findall(r'^\s*[A-Z][A-Z_]+\("([a-z_]+)"', source, flags=re.MULTILINE):
        icon = 'short_circuit_tier_1' if identifier == 'short_circuit' else identifier
        if icon not in families:
            errors.append(f'Shipped item family lacks wiki coverage: {identifier}')

for icon, page in families.items():
    if page+'.md' not in pages:
        errors.append(f'Missing item page: {page}')
    if not (WIKI / 'assets/icons' / (icon+'.png')).is_file():
        errors.append(f'Missing item icon: {icon}')
for page in ['short-circuit','overheat','contagion','reboot-optics','cripple-movement','synapse-burnout']:
    if '**Damage type:**' not in (WIKI / (page+'.md')).read_text():
        errors.append(f'Missing damage type: {page}')

if errors:
    print('\n'.join(errors), file=sys.stderr)
    sys.exit(1)
print(f'PASS: {len(pages)} pages, complete navigation, local links/anchors, '
      f'{len(families)} item/program entries and six damage classifications')
