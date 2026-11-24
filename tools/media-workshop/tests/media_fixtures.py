"""Local-only worker invocation for real downloader integration tests."""

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def local_stream_command(url, destination, folder):
    script = (
        "import sys;from pathlib import Path;"
        "sys.path.insert(0,sys.argv[1]);import app,media_worker;"
        "app.supported_url=lambda value:value;"
        "media_worker.mp3_stream(sys.argv[2],Path(sys.argv[3]),Path(sys.argv[4]))"
    )
    return [sys.executable, "-c", script, str(ROOT), url, str(destination), str(folder)]
