"""Local-only worker invocation for real downloader integration tests."""

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def local_stream_command(url, destination, folder, *, pace_cached=False):
    script = """
import sys
from pathlib import Path
sys.path.insert(0, sys.argv[1])
import app, media_worker
app.supported_url = lambda value: value
"""
    if pace_cached:
        script += """
real = media_worker.ffmpeg
def paced(command, callback=lambda t: None, **kwargs):
    command = list(command)
    command.insert(command.index("-i"), "-re")
    return real(command, callback, **kwargs)
media_worker.ffmpeg = paced
"""
    script += "media_worker.mp3_stream(sys.argv[2],Path(sys.argv[3]),Path(sys.argv[4]))"
    return [sys.executable, "-c", script, str(ROOT), url, str(destination), str(folder)]
