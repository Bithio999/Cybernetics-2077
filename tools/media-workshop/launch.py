#!/usr/bin/env python3
"""Create the private Python environment on first launch, then open Media Workshop."""

import os
import hashlib
from pathlib import Path
import subprocess
import sys
import venv

ROOT = Path(__file__).resolve().parent


def main():
    if sys.version_info < (3, 11):
        raise SystemExit("Media Workshop requires Python 3.11 or newer.")
    directory = ROOT / ".venv"
    python = directory / ("Scripts/python.exe" if os.name == "nt" else "bin/python")
    if not python.is_file():
        print("Preparing Media Workshop (first launch only)…", flush=True)
        venv.EnvBuilder(with_pip=True).create(directory)
    installed = (
        subprocess.run(
            [str(python), "-c", "import yt_dlp, yt_dlp_ejs"],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        ).returncode
        == 0
    )
    args = sys.argv[1:]
    requirements = ROOT / "requirements.txt"
    fingerprint = hashlib.sha256(requirements.read_bytes()).hexdigest()
    stamp = directory / "requirements.sha256"
    update = "--update" in args
    if update:
        args.remove("--update")
    if (
        not installed
        or update
        or not stamp.exists()
        or stamp.read_text().strip() != fingerprint
    ):
        subprocess.run(
            [
                str(python),
                "-m",
                "pip",
                "install",
                "--upgrade",
                "-r",
                str(ROOT / "requirements.txt"),
            ],
            check=True,
        )
        stamp.write_text(fingerprint)
    return subprocess.call([str(python), str(ROOT / "app.py"), *args], cwd=ROOT)


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        raise SystemExit(130)
    except (OSError, subprocess.CalledProcessError) as error:
        raise SystemExit(f"Could not start Media Workshop: {error}")
