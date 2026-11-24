#!/usr/bin/env python3
"""Build a reproducible source/launcher ZIP without dependencies or downloaded media."""

import argparse
import hashlib
from pathlib import Path
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "tools/media-workshop"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--output", type=Path, default=ROOT / "releases/tools/Media-Workshop-1.1.0.zip"
    )
    args = parser.parse_args()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    paths = [
        SOURCE / name
        for name in (
            "app.py",
            "launch.py",
            "Start.bat",
            "start.sh",
            "requirements.txt",
            "requirements-dev.txt",
            "README.md",
            "VALIDATION.md",
            "preview.png",
        )
    ]
    paths += sorted((SOURCE / "static").glob("*"))
    paths += sorted((SOURCE / "tests").glob("*.py"))
    paths += sorted((SOURCE / "tests/results").glob("*.json"))
    with ZipFile(
        args.output, "w", compression=ZIP_DEFLATED, compresslevel=9
    ) as archive:
        for path in paths:
            entry = ZipInfo(
                "Media-Workshop/" + path.relative_to(SOURCE).as_posix(),
                (2026, 11, 24, 0, 0, 0),
            )
            entry.compress_type = ZIP_DEFLATED
            entry.external_attr = (
                0o100755 if path.name == "start.sh" else 0o100644
            ) << 16
            data = path.read_bytes()
            if path.suffix != ".png":
                data = data.replace(b"\r\n", b"\n")
                if path.suffix == ".bat":
                    data = data.replace(b"\n", b"\r\n")
            archive.writestr(entry, data)
    digest = hashlib.sha256(args.output.read_bytes()).hexdigest()
    archives = set(args.output.parent.glob("Media-Workshop-*.zip")) | {args.output}
    (args.output.parent / "SHA256SUMS").write_text(
        "".join(
            hashlib.sha256(path.read_bytes()).hexdigest() + "  " + path.name + "\n"
            for path in sorted(archives)
        )
    )
    print(
        f"Created {args.output.name}: {args.output.stat().st_size:,} bytes; SHA256 {digest}"
    )


if __name__ == "__main__":
    main()
