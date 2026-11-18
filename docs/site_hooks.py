"""Publish existing media once, while keeping the source Markdown readable on GitHub."""
from pathlib import Path
import re
import posixpath
from mkdocs.structure.files import File

LINK = re.compile(r'(?P<start>\]\()(?P<kind>\.\./(?:images|media))/(?P<name>[^)\s]+)(?P<end>\))')


def on_files(files, config):
    root = Path(config.config_file_path).parent
    assets = {}
    for page in Path(config.docs_dir).rglob('*.md'):
        for match in LINK.finditer(page.read_text()):
            kind = match['kind'].split('/')[-1]
            source = root / 'docs' / kind / match['name']
            if not source.is_file():
                raise FileNotFoundError(f'Missing wiki media: {source}')
            assets[f'assets/{kind}/{match["name"]}'] = source.resolve()
    for destination, source in sorted(assets.items()):
        files.append(File.generated(config, destination, abs_src_path=str(source)))
    return files


def on_page_markdown(markdown, page, config, files):
    if page.file.src_uri == 'index.md':
        markdown = re.sub(r'(\[Download the mod\]\([^)]+\))', r'\1{ .md-button .md-button--primary }', markdown)
        markdown = markdown.replace('[Start playing](getting-started.md)', '[Start playing](getting-started.md){ .md-button }')
    markdown = re.sub(r'(src=")(assets/icons/[^"]+)(")',
        lambda m: m[1] + posixpath.relpath(m[2], posixpath.dirname(page.file.dest_uri) or '.') + m[3], markdown)
    return LINK.sub(lambda m: f'{m["start"]}assets/{m["kind"].split("/")[-1]}/{m["name"]}{m["end"]}', markdown)
