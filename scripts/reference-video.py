#!/usr/bin/env python3
"""Download a public YouTube reference normally, or inspect an existing local video.

Uses yt-dlp and ffmpeg without changing proxy settings, TLS trust, or authentication.
Downloaded references belong in the ignored references-local directory, not releases.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path
import shutil
import subprocess
import sys
from urllib.parse import urlsplit


class ReferenceError(Exception):
    pass


def run(command: list[str]) -> str:
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode:
        detail = (result.stderr or result.stdout).strip()
        if "Tunnel connection failed: 403" in detail or "CONNECT tunnel failed" in detail:
            raise ReferenceError(
                "The configured network proxy denied access to YouTube (CONNECT 403). "
                "This helper cannot remove that restriction. Run fetch on a computer with "
                "working YouTube access, or use inspect with an MP4 you already have."
            )
        raise ReferenceError(detail[-2500:] or f"Command failed: {command[0]}")
    return result.stdout


def executable(name: str) -> str:
    found = shutil.which(name)
    if not found:
        raise ReferenceError(f"{name} is required. Install FFmpeg and put its executables on PATH.")
    return found


def youtube_url(value: str) -> str:
    try:
        parsed = urlsplit(value)
        hostname = (parsed.hostname or "").lower()
        valid = (
            parsed.scheme in ("http", "https")
            and not parsed.username
            and not parsed.password
            and parsed.port in (None, 80, 443)
            and (hostname in ("youtube.com", "youtu.be") or hostname.endswith(".youtube.com"))
        )
    except ValueError:
        valid = False
    if not valid:
        raise argparse.ArgumentTypeError("Provide a normal https://www.youtube.com/ or https://youtu.be/ video URL.")
    return value


def fetch(url: str, output: Path, max_height: int) -> Path:
    if importlib.util.find_spec("yt_dlp") is None:
        raise ReferenceError("Install the downloader first: python -m pip install -U yt-dlp")
    executable("ffmpeg")
    executable("ffprobe")
    output.mkdir(parents=True, exist_ok=True)
    text = run([
        sys.executable, "-m", "yt_dlp",
        "--no-playlist", "--no-progress", "--restrict-filenames",
        "--retries", "0", "--extractor-retries", "0",
        "--format", f"bv[ext=mp4][height<={max_height}]+ba[ext=m4a]/b[ext=mp4][height<={max_height}]",
        "--merge-output-format", "mp4",
        "--paths", str(output.resolve()),
        "--output", "%(id)s.%(ext)s",
        "--print", "after_move:filepath",
        url,
    ])
    candidates = [Path(line.strip()).resolve() for line in text.splitlines() if line.strip()]
    videos = [p for p in candidates if p.is_file() and p.suffix.lower() == ".mp4" and p.is_relative_to(output.resolve())]
    if not videos:
        raise ReferenceError("The downloader did not return a completed MP4.")
    return videos[-1]


def inspect(video: Path, output: Path, start: float, duration: float, fps: float) -> Path:
    if not video.is_file():
        raise ReferenceError(f"Video does not exist: {video}")
    probe = json.loads(run([
        executable("ffprobe"), "-v", "error", "-select_streams", "v:0",
        "-show_entries", "stream=width,height,codec_name:format=duration", "-of", "json", str(video.resolve()),
    ]))
    if not probe.get("streams"):
        raise ReferenceError("The input has no readable video stream.")
    length = float(probe.get("format", {}).get("duration", 0))
    if length <= 0 or start >= length:
        raise ReferenceError("The requested starting time is outside this video's duration.")
    end = min(length, start + duration)
    # Use one directory per source and range, so inspecting a later passage preserves earlier work.
    destination = output / f"{video.stem}-at-{start:g}s"
    destination.mkdir(parents=True, exist_ok=True)
    run([
        executable("ffmpeg"), "-y", "-v", "error", "-ss", str(start),
        "-i", str(video.resolve()), "-t", str(end - start), "-an",
        "-vf", f"fps={fps},scale=480:-2,tile=4x4:padding=2:margin=2",
        "-fps_mode", "vfr", "-q:v", "2", str((destination / "sheet-%03d.jpg").resolve()),
    ])
    sheets = sorted(destination.glob("sheet-*.jpg"))
    if not sheets:
        raise ReferenceError("FFmpeg did not produce reference frames.")
    report = {
        "source": video.name,
        "duration_seconds": length,
        "width": probe["streams"][0]["width"],
        "height": probe["streams"][0]["height"],
        "start_seconds": start,
        "end_seconds": end,
        "sample_fps": fps,
        "sheet_layout": "4 columns, 4 rows; chronological, left to right then top to bottom",
        "sheets": [p.name for p in sheets],
        "frame_time_rule": "start_seconds + (sheet_index * 16 + cell_index) / sample_fps; indices start at zero",
        "note": "The final sheet may contain empty cells after the video ends.",
    }
    report_path = destination / "inspection.json"
    report_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    return report_path


def positive(value: str) -> float:
    try:
        number = float(value)
    except ValueError:
        raise argparse.ArgumentTypeError("Use a positive number.")
    if not 0 < number < float("inf"):
        raise argparse.ArgumentTypeError("Use a finite positive number.")
    return number


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    download = commands.add_parser("fetch", help="Download a public YouTube video to MP4 and inspect its opening passage.")
    download.add_argument("url", type=youtube_url)
    download.add_argument("--max-height", type=int, choices=(480, 720, 1080, 1440, 2160), default=1080)
    local = commands.add_parser("inspect", help="Extract reference sheets from an existing MP4 or other local video.")
    local.add_argument("video", type=Path)
    for command in (download, local):
        command.add_argument("--output", type=Path, default=Path("references-local"))
        command.add_argument("--start", type=float, default=0)
        command.add_argument("--duration", type=positive, default=30)
        command.add_argument("--fps", type=positive, default=2)
    args = parser.parse_args()
    if not 0 <= args.start < float("inf"):
        parser.error("--start must be finite and zero or greater.")
    try:
        video = fetch(args.url, args.output, args.max_height) if args.command == "fetch" else args.video
        report = inspect(video, args.output, args.start, args.duration, args.fps)
        print(f"Video: {video.resolve()}")
        print(f"Frame review: {report.resolve()}")
        return 0
    except (ReferenceError, OSError, ValueError) as error:
        print(f"Reference video error: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
