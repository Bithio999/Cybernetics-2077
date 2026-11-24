#!/usr/bin/env python3
"""Local YouTube/Twitch batch downloader, converter and precise MP4/MP3 trimmer."""

from __future__ import annotations

import argparse
from collections import deque
from concurrent.futures import ThreadPoolExecutor
from fractions import Fraction
import importlib.metadata
import importlib.util
import json
import math
import mimetypes
import os
from pathlib import Path
import re
import secrets
import shutil
import signal
import subprocess
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, unquote, urlsplit
import uuid
import webbrowser

ROOT = Path(__file__).resolve().parent
FORMATS = {"mp4", "mp3"}
HEIGHTS = {480, 720, 1080, 1440, 2160}
INPUTS = {
    ".mp4",
    ".mp3",
    ".m4a",
    ".mov",
    ".mkv",
    ".webm",
    ".wav",
    ".ogg",
    ".flac",
    ".aac",
}
TERMINAL = {"complete", "failed", "cancelled", "interrupted"}
MAX_BATCH = 100


class MediaError(Exception):
    pass


class Cancelled(MediaError):
    pass


def timestamp(value):
    """Accept seconds, MM:SS or HH:MM:SS, including fractional seconds."""
    try:
        parts = str(value).strip().split(":")
        if not 1 <= len(parts) <= 3:
            raise ValueError
        numbers = [float(p) for p in parts]
        if any(not math.isfinite(n) or n < 0 for n in numbers):
            raise ValueError
        if len(parts) > 1:
            if any(not p.isdigit() for p in parts[:-1]) or any(
                n >= 60 for n in numbers[1:]
            ):
                raise ValueError
        return sum(n * 60**i for i, n in enumerate(reversed(numbers)))
    except (TypeError, ValueError, OverflowError):
        raise MediaError(
            "Use seconds, MM:SS or HH:MM:SS; for example 01:23.500."
        ) from None


def supported_url(value):
    value = str(value).strip()
    try:
        p = urlsplit(value)
        host = (p.hostname or "").lower()
        valid = (
            p.scheme in {"https", "http"}
            and not p.username
            and not p.password
            and p.port in {None, 80, 443}
            and (
                host in {"youtu.be", "youtube.com", "twitch.tv"}
                or host.endswith(".youtube.com")
                or host.endswith(".twitch.tv")
            )
        )
    except ValueError:
        valid = False
    if not valid:
        raise MediaError("Enter a YouTube video or Twitch VOD/clip link, one per line.")
    pieces = [part for part in p.path.split("/") if part]
    query = parse_qs(p.query)
    if host == "youtu.be":
        individual = len(pieces) == 1
    elif host == "youtube.com" or host.endswith(".youtube.com"):
        individual = (
            p.path.rstrip("/") == "/watch" and bool(query.get("v", [""])[0])
        ) or (
            len(pieces) == 2 and pieces[0] in {"shorts", "live", "embed", "v", "clip"}
        )
    elif host == "clips.twitch.tv":
        individual = len(pieces) == 1 and (
            pieces[0] != "embed" or bool(query.get("clip", [""])[0])
        )
    else:
        individual = (
            len(pieces) == 2 and pieces[0] == "videos" and pieces[1].isdigit()
        ) or (len(pieces) == 3 and pieces[1] == "clip")
    if not individual:
        raise MediaError(
            "Use an individual video, VOD or clip link rather than a playlist or channel link."
        )
    return value


def options(fmt, height=1080):
    if fmt not in FORMATS:
        raise MediaError("Output must be MP4 or MP3.")
    try:
        height = int(height)
    except (TypeError, ValueError):
        raise MediaError("Choose a supported video quality.") from None
    if height not in HEIGHTS:
        raise MediaError("Choose 480p, 720p, 1080p, 1440p or 2160p.")
    return fmt, height


def safe_name(value):
    name = re.sub(r'[\x00-\x1f<>:"/\\|?*]', "_", str(value)).strip(" .")[:120]
    if name.split(".")[0].rstrip(" .").upper() in {
        "CON",
        "PRN",
        "AUX",
        "NUL",
        *(f"COM{i}" for i in range(1, 10)),
        *(f"LPT{i}" for i in range(1, 10)),
    }:
        name = "_" + name
    return name or "Media"


def executable(name):
    path = shutil.which(name)
    if not path:
        raise MediaError(
            f"{name} is missing. Install FFmpeg (including ffprobe), then restart the app."
        )
    return path


def dependencies():
    try:
        version = importlib.metadata.version("yt-dlp")
    except importlib.metadata.PackageNotFoundError:
        version = None
    return {
        "yt_dlp": version,
        "ffmpeg": bool(shutil.which("ffmpeg")),
        "ffprobe": bool(shutil.which("ffprobe")),
        "javascript": next(
            (n for n in ("deno", "node", "bun", "qjs") if shutil.which(n)), None
        ),
    }


def probe(path):
    result = subprocess.run(
        [
            executable("ffprobe"),
            "-v",
            "error",
            "-protocol_whitelist",
            "file,pipe",
            "-show_entries",
            "format=duration,start_time,format_name:stream=codec_type,codec_name,pix_fmt,width,height,avg_frame_rate,nb_frames:stream_disposition=attached_pic",
            "-of",
            "json",
            str(path),
        ],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=30,
        creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0,
    )
    try:
        data = json.loads(result.stdout)
        duration = float(data["format"]["duration"])
        start_time = float(data["format"].get("start_time", 0) or 0)
        video = next(
            (
                s
                for s in data.get("streams", [])
                if s["codec_type"] == "video"
                and not s.get("disposition", {}).get("attached_pic")
            ),
            None,
        )
        audio = next(
            (s for s in data.get("streams", []) if s["codec_type"] == "audio"), None
        )
        if (
            result.returncode
            or not math.isfinite(duration)
            or duration <= 0
            or not (video or audio)
        ):
            raise ValueError
        return {
            "duration": duration,
            "video": bool(video),
            "audio": bool(audio),
            "width": video.get("width", 0) if video else 0,
            "height": video.get("height", 0) if video else 0,
            "video_codec": video.get("codec_name") if video else None,
            "pixel_format": video.get("pix_fmt") if video else None,
            "audio_codec": audio.get("codec_name") if audio else None,
            "video_frames": int(video.get("nb_frames", 0))
            if video and str(video.get("nb_frames", "")).isdigit()
            else 0,
            "container": data["format"].get("format_name", ""),
            "start_time": start_time if math.isfinite(start_time) else 0,
            "fps": float(Fraction(video.get("avg_frame_rate", "0/1")))
            if video and video.get("avg_frame_rate") not in {None, "0/0"}
            else 0,
        }
    except (ValueError, KeyError, TypeError):
        raise MediaError(
            "This file has no readable audio/video or finite duration. Use a finished media file."
        ) from None


def download_command(url, directory, fmt, height):
    if not dependencies()["yt_dlp"]:
        raise MediaError("yt-dlp is missing. Start with launch.py to install it.")
    command = [
        sys.executable,
        "-m",
        "yt_dlp",
        "--ignore-config",
        "--no-cache-dir",
        "--no-playlist",
        "--no-colors",
        "--encoding",
        "utf-8",
        "--newline",
        "--progress",
        "--no-simulate",
        "--progress-template",
        "download:PROGRESS:%(progress)j",
        "--print",
        "before_dl:TITLE:%(title)j",
        "--print",
        "after_move:FILE:%(filepath)j",
        "--match-filter",
        "!is_live",
        "--socket-timeout",
        "20",
        "--retries",
        "2",
        "--fragment-retries",
        "2",
        "--extractor-retries",
        "1",
        "--concurrent-fragments",
        "3",
        "--ffmpeg-location",
        str(Path(executable("ffmpeg")).parent),
        "--paths",
        str(directory),
        "--output",
        "source.%(ext)s",
    ]
    for binary, runtime in (("node", "node"), ("bun", "bun"), ("qjs", "quickjs")):
        if shutil.which(binary):
            command += ["--js-runtimes", runtime + ":" + shutil.which(binary)]
    if fmt == "mp3":
        command += ["--format", "bestaudio/best"]
    else:
        command += [
            "--format",
            f"bv*[height<=?{height}]+ba/b[height<=?{height}]",
            "--format-sort",
            "lang,quality,res,fps,vcodec:h264,acodec:aac",
            "--merge-output-format",
            "mkv",
        ]
    return [*command, "--", supported_url(url)]


def convert_command(
    source, destination, fmt, start, duration, height=None, *, threads=2, preset="fast"
):
    command = [
        executable("ffmpeg"),
        "-hide_banner",
        "-loglevel",
        "warning",
        "-nostdin",
        "-y",
        "-filter_threads",
        "1",
        "-protocol_whitelist",
        "file,pipe",
        "-ss",
        str(start),
        "-i",
        str(source),
        "-t",
        str(duration),
    ]
    if fmt == "mp3":
        command += [
            "-map",
            "0:a:0",
            "-vn",
            "-c:a",
            "libmp3lame",
            "-b:a",
            "192k",
            "-id3v2_version",
            "3",
        ]
    else:
        scale = (
            f"scale=-2:trunc(min(ih\\,{height})/2)*2"
            if height
            else "scale=trunc(iw/2)*2:trunc(ih/2)*2"
        )
        command += [
            "-map",
            "0:v:0",
            "-map",
            "0:a:0?",
            "-c:v",
            "libx264",
            "-preset",
            preset,
            "-crf",
            "20",
            "-pix_fmt",
            "yuv420p",
            "-vf",
            scale,
            "-c:a",
            "aac",
            "-b:a",
            "192k",
            "-movflags",
            "+faststart",
        ]
    return [
        *command,
        "-threads",
        str(threads),
        "-map_metadata",
        "-1",
        "-progress",
        "pipe:1",
        "-nostats",
        str(destination),
    ]


def fast_start_mp4(path):
    """Read only top-level MP4 box headers; never scan the video payload."""
    size = path.stat().st_size
    with path.open("rb") as stream:
        offset, ftyp = 0, False
        for _ in range(256):
            if offset + 8 > size:
                return False
            stream.seek(offset)
            header = stream.read(8)
            length, kind = int.from_bytes(header[:4], "big"), header[4:]
            minimum = 8
            if length == 1:
                extension = stream.read(8)
                if len(extension) != 8:
                    return False
                length, minimum = int.from_bytes(extension, "big"), 16
            elif length == 0:
                length = size - offset
            if length < minimum or offset + length > size:
                return False
            if kind == b"ftyp":
                ftyp = True
            elif kind == b"mdat":
                return False
            elif kind == b"moov":
                return ftyp
            offset += length
    return False


def download_finish_plan(
    source, destination, fmt, meta, height, threads=2, *, allow_direct=True
):
    """A None command means a same-filesystem rename, with no media processing."""
    if (
        allow_direct
        and fmt == "mp3"
        and meta.get("container") == "mp3"
        and meta.get("audio_codec") == "mp3"
    ):
        return "Direct save", None
    video_copy = (
        meta.get("video_codec") == "h264"
        and meta.get("pixel_format") in {"yuv420p", "yuvj420p"}
        and meta["width"] > 0
        and meta["height"] > 0
        and meta["width"] % 2 == 0
        and meta["height"] % 2 == 0
        and meta["height"] <= height
    )
    audio_copy = not meta["audio"] or meta.get("audio_codec") == "aac"
    if (
        allow_direct
        and fmt == "mp4"
        and video_copy
        and audio_copy
        and fast_start_mp4(source)
    ):
        return "Direct save", None
    command = [
        executable("ffmpeg"),
        "-hide_banner",
        "-loglevel",
        "warning",
        "-nostdin",
        "-y",
        "-filter_threads",
        "1",
        "-protocol_whitelist",
        "file,pipe",
        "-i",
        str(source),
    ]
    if fmt == "mp3":
        copy_mp3 = meta.get("audio_codec") == "mp3"
        method = "Stream copy" if copy_mp3 else "Audio conversion"
        command += [
            "-map",
            "0:a:0",
            "-vn",
            "-c:a",
            "copy" if copy_mp3 else "libmp3lame",
        ]
        if not copy_mp3:
            command += ["-b:a", "192k"]
        command += ["-id3v2_version", "3"]
    else:
        method = (
            "Stream copy"
            if video_copy and audio_copy
            else "Audio conversion only"
            if video_copy
            else "Video conversion only"
            if audio_copy
            else "Full conversion"
        )
        command += [
            "-map",
            "0:v:0",
            "-map",
            "0:a:0?",
            "-c:v",
            "copy" if video_copy else "libx264",
        ]
        if not video_copy:
            command += [
                "-preset",
                "veryfast",
                "-crf",
                "20",
                "-pix_fmt",
                "yuv420p",
                "-vf",
                f"scale=-2:trunc(min(ih\\,{height})/2)*2",
            ]
        command += ["-c:a", "copy" if audio_copy else "aac"]
        if not audio_copy:
            command += ["-b:a", "192k"]
        command += ["-movflags", "+faststart"]
    return method, [
        *command,
        "-threads",
        str(threads),
        "-map_metadata",
        "-1",
        "-progress",
        "pipe:1",
        "-nostats",
        str(destination),
    ]


def keyframe_before(source, meta, start):
    """Seek the local index and inspect half a second of packets, without decoding frames."""
    if start == 0:
        return 0.0
    offset = meta.get("start_time", 0)
    try:
        result = subprocess.run(
            [
                executable("ffprobe"),
                "-v",
                "error",
                "-protocol_whitelist",
                "file,pipe",
                "-select_streams",
                "v:0",
                "-read_intervals",
                f"{start + offset:.9f}%+0.5",
                "-show_packets",
                "-show_entries",
                "packet=pts_time,flags",
                "-of",
                "json",
                str(source),
            ],
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=10,
            creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0,
        )
        if result.returncode:
            return None
        candidates = [
            float(packet["pts_time"]) - offset
            for packet in json.loads(result.stdout).get("packets", [])
            if "K" in packet.get("flags", "") and "pts_time" in packet
        ]
        candidates = [
            v for v in candidates if math.isfinite(v) and v <= start + 0.000001
        ]
        return max(0.0, max(candidates)) if candidates else None
    except (ValueError, KeyError, subprocess.TimeoutExpired):
        return None


def streaming_mp3_command(url, destination, folder):
    return [
        sys.executable,
        str(ROOT / "media_worker.py"),
        "mp3",
        url,
        str(destination),
        str(folder),
    ]


def trim_plan(source, destination, fmt, meta, start, end, threads):
    """Choose an accurate implementation automatically; cut points never move."""
    if start == 0 and end >= meta["duration"] - 0.000001:
        _, command = download_finish_plan(
            source, destination, fmt, meta, max(2, meta["height"]), threads
        )
        return command, "whole-source"
    smart = (
        fmt == "mp4"
        and meta.get("video_codec") == "h264"
        and meta.get("pixel_format") in {"yuv420p", "yuvj420p"}
        and end - start >= 6
        and importlib.util.find_spec("smartcut") is not None
    )
    if smart:
        return [
            sys.executable,
            str(ROOT / "media_worker.py"),
            "cut",
            str(source),
            str(destination),
            str(start),
            str(end),
            str(threads),
        ], "smart"
    return convert_command(
        source, destination, fmt, start, end - start, threads=threads, preset="veryfast"
    ), "encoder"


class Workshop:
    def __init__(self, directory, workers=2):
        self.directory = Path(directory).resolve()
        self.directory.mkdir(parents=True, exist_ok=True)
        self.library_lock = (self.directory / "library.lock").open("a+b")
        try:
            if os.name == "nt":
                import msvcrt

                self.library_lock.write(b"0")
                self.library_lock.flush()
                self.library_lock.seek(0)
                msvcrt.locking(self.library_lock.fileno(), msvcrt.LK_NBLCK, 1)
            else:
                import fcntl

                fcntl.flock(self.library_lock.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
        except OSError:
            self.library_lock.close()
            raise MediaError(
                "This library is already open in another Media Workshop. Close that app or use another --data-dir."
            ) from None
        self.jobs = {}
        self.assets = {}
        self.lock = threading.RLock()
        self.cancels = {}
        self.processes = {}
        self.closing = False
        self.last_save = 0
        state = self.directory / "library.json"
        if state.exists():
            try:
                data = json.loads(state.read_text(encoding="utf-8"))
                self.assets = {
                    a["id"]: a for a in data["assets"] if self.asset_path(a).is_file()
                }
                self.jobs = {j["id"]: j for j in data["jobs"]}
                for job in self.jobs.values():
                    if job["state"] not in TERMINAL:
                        job.update(
                            state="interrupted",
                            stage="Interrupted; choose Retry to continue",
                        )
            except (ValueError, KeyError, TypeError, OSError) as error:
                self.library_lock.close()
                raise MediaError(
                    f"Cannot read {state}. Preserve this file and restore a library backup: {error}"
                ) from error
        self.pool = ThreadPoolExecutor(max_workers=workers, thread_name_prefix="media")
        self.workers = workers
        self.save()

    def asset_path(self, asset):
        path = (self.directory / asset["path"]).resolve()
        if not path.is_relative_to(self.directory):
            raise MediaError("Invalid media path.")
        return path

    def save(self):
        with self.lock:
            target = self.directory / "library.json"
            temporary = self.directory / "library.json.tmp"
            temporary.write_text(
                json.dumps(
                    {
                        "jobs": list(self.jobs.values()),
                        "assets": list(self.assets.values()),
                    },
                    ensure_ascii=False,
                    indent=2,
                ),
                encoding="utf-8",
            )
            temporary.replace(target)
            self.last_save = time.monotonic()

    def update(self, job_id, **values):
        with self.lock:
            self.jobs[job_id].update(values)
            if "state" in values or time.monotonic() - self.last_save > 1:
                self.save()

    def snapshot(self):
        with self.lock:
            return json.loads(
                json.dumps(
                    {
                        "jobs": list(self.jobs.values()),
                        "assets": list(self.assets.values()),
                        "dependencies": dependencies(),
                        "workers": self.workers,
                        "directory": str(self.directory),
                    }
                )
            )

    def asset(self, asset_id):
        with self.lock:
            asset = self.assets.get(str(asset_id))
            if not asset or not self.asset_path(asset).is_file():
                raise MediaError("This media file is no longer available.")
            return dict(asset)

    def register(self, path, title, details=None):
        details = details or probe(path)
        asset = {
            "id": uuid.uuid4().hex,
            "title": safe_name(title),
            "format": path.suffix[1:].lower(),
            "path": str(path.relative_to(self.directory)),
            "bytes": path.stat().st_size,
            "created": time.time(),
            **details,
        }
        with self.lock:
            self.assets[asset["id"]] = asset
            self.save()
        return asset

    def enqueue(self, specs):
        with self.lock:
            if self.closing:
                raise MediaError("The app is closing.")
            pending = sum(j["state"] not in TERMINAL for j in self.jobs.values())
            if pending + len(specs) > MAX_BATCH:
                raise MediaError(f"The queue holds up to {MAX_BATCH} unfinished jobs.")
            ids = []
            for spec in specs:
                job_id = uuid.uuid4().hex
                job = {
                    "id": job_id,
                    "created": time.time(),
                    "state": "queued",
                    "stage": "Queued",
                    "progress": 0,
                    "error": "",
                    "asset_id": None,
                    "title": spec.get("title", "Preparing media"),
                    "spec": spec,
                }
                self.jobs[job_id] = job
                self.cancels[job_id] = threading.Event()
                ids.append(job_id)
            self.save()
            for job_id in ids:
                self.pool.submit(self.work, job_id)
            return ids

    def download(self, urls, fmt, height=1080):
        fmt, height = options(fmt, height)
        if not isinstance(urls, list) or not 1 <= len(urls) <= MAX_BATCH:
            raise MediaError("Add between 1 and 100 links.")
        # Validate the entire batch before queuing anything; deduplicate without losing order.
        links = list(dict.fromkeys(supported_url(url) for url in urls))
        return self.enqueue(
            [
                {
                    "kind": "download",
                    "url": url,
                    "format": fmt,
                    "height": height,
                    "title": url,
                }
                for url in links
            ]
        )

    def trim(self, asset_ids, fmt, start, end, mode=None):
        fmt, _ = options(fmt)
        if not isinstance(asset_ids, list) or not 1 <= len(asset_ids) <= MAX_BATCH:
            raise MediaError("Select between 1 and 100 media files.")
        start = timestamp(start)
        end = None if end is None or str(end).strip() == "" else timestamp(end)
        specs = []
        for asset_id in dict.fromkeys(asset_ids):
            asset = self.asset(asset_id)
            finish = asset["duration"] if end is None else end
            if (
                start >= asset["duration"]
                or finish > asset["duration"] + 0.05
                or finish <= start
            ):
                raise MediaError(
                    f"{asset['title']}: choose an end after the start and within the file's duration."
                )
            if not asset["audio"] and fmt == "mp3":
                raise MediaError(f"{asset['title']} has no audio to export as MP3.")
            if not asset["video"] and fmt == "mp4":
                raise MediaError(
                    f"{asset['title']} has no video to export as MP4. Choose MP3."
                )
            specs.append(
                {
                    "kind": "trim",
                    "source": asset_id,
                    "format": fmt,
                    "start": start,
                    "end": min(finish, asset["duration"]),
                    "title": asset["title"] + " — trimmed",
                }
            )
        return self.enqueue(specs)

    def check_cancel(self, job_id):
        if self.cancels[job_id].is_set():
            raise Cancelled("Cancelled")

    def run(self, job_id, command, callback):
        self.check_cancel(job_id)
        kwargs = (
            {
                "creationflags": subprocess.CREATE_NO_WINDOW
                | subprocess.CREATE_NEW_PROCESS_GROUP
            }
            if os.name == "nt"
            else {"start_new_session": True}
        )
        process = subprocess.Popen(
            command,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            encoding="utf-8",
            errors="replace",
            **kwargs,
        )
        with self.lock:
            self.processes[job_id] = process
        lines = deque(maxlen=12)
        try:
            if self.cancels[job_id].is_set():
                self.stop_process(process)
            for line in process.stdout:
                text = line.strip()
                if text:
                    lines.append(text)
                    callback(text)
            code = process.wait()
            self.check_cancel(job_id)
            if code:
                raise MediaError(
                    "\n".join(lines)[-2500:]
                    or "The media processor exited unexpectedly."
                )
        finally:
            if process.poll() is None:
                self.stop_process(process)
            process.stdout.close()
            with self.lock:
                self.processes.pop(job_id, None)

    @staticmethod
    def stop_process(process):
        if process.poll() is not None:
            return
        try:
            if os.name == "nt":
                subprocess.run(
                    ["taskkill", "/PID", str(process.pid), "/T", "/F"],
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                    creationflags=subprocess.CREATE_NO_WINDOW,
                )
            else:
                os.killpg(process.pid, signal.SIGTERM)
            process.wait(timeout=3)
        except (ProcessLookupError, subprocess.TimeoutExpired):
            if process.poll() is None:
                if os.name == "nt":
                    process.kill()
                else:
                    os.killpg(process.pid, signal.SIGKILL)
                process.wait()

    def worker_progress(self, job_id, line):
        if not line.startswith("WORKSHOP:"):
            return
        values = json.loads(line[9:])
        allowed = {
            "stage",
            "method",
            "title",
            "progress",
            "smart_copied_seconds",
            "smart_boundary_segments",
            "encoder_started_before_download_finished",
            "indexed_video_frames",
            "audio_processing",
        }
        values = {k: v for k, v in values.items() if k in allowed}
        if "progress" in values:
            values["progress"] = max(0, min(99, values["progress"]))
        self.update(job_id, **values)

    def stream_mp3(self, job_id, folder, spec):
        output = folder / "result"
        output.mkdir(exist_ok=True)
        destination = output / "audio.mp3"
        self.run(
            job_id,
            streaming_mp3_command(spec["url"], destination, folder),
            lambda line: self.worker_progress(job_id, line),
        )
        self.check_cancel(job_id)
        title = self.jobs[job_id]["title"]
        final = output / (safe_name(title) + ".mp3")
        if final != destination:
            destination.replace(final)
        asset = self.register(final, title)
        self.update(
            job_id, state="complete", stage="Ready", progress=100, asset_id=asset["id"]
        )

    def work(self, job_id):
        folder = self.directory / "jobs" / job_id
        try:
            self.check_cancel(job_id)
            folder.mkdir(parents=True, exist_ok=True)
            spec = self.jobs[job_id]["spec"]
            self.update(job_id, state="running", stage="Preparing", error="")
            executable("ffmpeg")
            executable("ffprobe")
            if spec["kind"] == "download" and spec["format"] == "mp3":
                try:
                    self.stream_mp3(job_id, folder, spec)
                    return
                except MediaError as error:
                    self.check_cancel(job_id)
                    if "STREAM_FALLBACK:" not in str(error):
                        raise
                    self.update(
                        job_id,
                        progress=0,
                        stage="Preparing audio",
                        audio_stream_fallback=True,
                    )
                    (folder / "result/audio.mp3").unlink(missing_ok=True)
            if spec["kind"] == "download":
                paths = []

                def progress(line):
                    if line.startswith("TITLE:"):
                        self.update(job_id, title=safe_name(json.loads(line[6:])))
                    elif line.startswith("FILE:"):
                        paths.append(Path(json.loads(line[5:])).resolve())
                    elif line.startswith("PROGRESS:"):
                        info = json.loads(line[9:])
                        total = (
                            info.get("total_bytes")
                            or info.get("total_bytes_estimate")
                            or 0
                        )
                        pct = (
                            min(89, info.get("downloaded_bytes", 0) / total * 90)
                            if total
                            else 0
                        )
                        self.update(job_id, progress=round(pct, 1), stage="Downloading")

                self.update(job_id, stage="Connecting to source")
                self.run(
                    job_id,
                    download_command(
                        spec["url"], folder, spec["format"], spec["height"]
                    ),
                    progress,
                )
                files = [p for p in paths if p.is_relative_to(folder) and p.is_file()]
                if not files:
                    raise MediaError(
                        "No finished video was returned. Use a public video, Twitch VOD or clip; live streams are not recorded."
                    )
                source = files[-1]
                meta = probe(source)
                start, duration, base = 0, meta["duration"], 90
            else:
                asset = self.asset(spec["source"])
                source = self.asset_path(asset)
                meta = probe(source)
                start, duration, base = spec["start"], spec["end"] - spec["start"], 0
            if spec["format"] == "mp3" and not meta["audio"]:
                raise MediaError("This source has no audio track.")
            if spec["format"] == "mp4" and not meta["video"]:
                raise MediaError("This source has no video track. Choose MP3.")
            title = self.jobs[job_id]["title"]
            result_folder = folder / "result"
            result_folder.mkdir(exist_ok=True)
            destination = result_folder / (safe_name(title) + "." + spec["format"])
            threads = max(1, min(8, (os.cpu_count() or 2) // self.workers))
            if spec["kind"] == "download":
                method, command = download_finish_plan(
                    source, destination, spec["format"], meta, spec["height"], threads
                )
                stage = (
                    ("Saving" if command is None else "Finalizing")
                    + " "
                    + spec["format"].upper()
                )
            else:
                command, backend = trim_plan(
                    source,
                    destination,
                    spec["format"],
                    meta,
                    start,
                    spec["end"],
                    threads,
                )
                method = "Accurate cut"
                self.update(job_id, trim_start=start, processing_backend=backend)
                stage = (
                    "Preparing accurate cut"
                    if backend == "smart"
                    else "Encoding " + spec["format"].upper()
                )
            self.update(job_id, stage=stage, method=method, progress=base)

            def encoded(line):
                if line.startswith("out_time_us="):
                    try:
                        pct = float(line.split("=", 1)[1]) / 1e6 / duration
                        self.update(
                            job_id,
                            progress=round(base + min(0.999, pct) * (100 - base), 1),
                        )
                    except ValueError:
                        pass

            self.check_cancel(job_id)
            if command is None:
                if spec["kind"] == "trim":
                    shutil.copyfile(source, destination)
                else:
                    source.replace(destination)
            elif spec["kind"] == "trim" and backend == "smart":
                try:
                    self.run(
                        job_id, command, lambda line: self.worker_progress(job_id, line)
                    )
                except MediaError as error:
                    self.check_cancel(job_id)
                    shutil.rmtree(result_folder / "processing", ignore_errors=True)
                    self.update(
                        job_id,
                        progress=0,
                        processing_backend="encoder",
                        fallback_reason=str(error),
                        stage="Encoding MP4 (compatibility fallback)",
                    )
                    self.run(
                        job_id,
                        convert_command(
                            source,
                            destination,
                            spec["format"],
                            start,
                            duration,
                            threads=threads,
                            preset="veryfast",
                        ),
                        encoded,
                    )
            else:
                self.run(job_id, command, encoded)
            self.check_cancel(job_id)
            result = self.register(destination, title)
            self.update(
                job_id,
                state="complete",
                stage="Ready",
                progress=100,
                asset_id=result["id"],
            )
            if spec["kind"] == "download" and source != destination:
                try:
                    source.unlink(missing_ok=True)
                except OSError:
                    pass  # A temporary source cleanup failure must not remove a finished export.
        except Cancelled:
            self.update(job_id, state="cancelled", stage="Cancelled")
            shutil.rmtree(folder, ignore_errors=True)
        except Exception as error:
            self.update(job_id, state="failed", stage="Failed", error=str(error))
            shutil.rmtree(folder, ignore_errors=True)
        finally:
            with self.lock:
                self.cancels.pop(job_id, None)

    def cancel(self, job_id):
        with self.lock:
            job = self.jobs.get(job_id)
            if not job:
                raise MediaError("Job not found.")
            if job["state"] in TERMINAL:
                return
            self.cancels[job_id].set()
            process = self.processes.get(job_id)
            self.update(job_id, stage="Cancelling…")
        if process:
            self.stop_process(process)

    def retry(self, job_id):
        with self.lock:
            job = self.jobs.get(job_id)
            if not job or job["state"] not in {"failed", "cancelled", "interrupted"}:
                raise MediaError(
                    "Only failed, cancelled or interrupted jobs can be retried."
                )
            if job["spec"]["kind"] == "trim":
                self.asset(job["spec"]["source"])
            return self.enqueue([dict(job["spec"])])

    def remove_asset(self, asset_id):
        with self.lock:
            asset = self.asset(asset_id)
            if any(
                j["state"] not in TERMINAL and j["spec"].get("source") == asset_id
                for j in self.jobs.values()
            ):
                raise MediaError(
                    "Wait for this file's queued exports to finish before deleting it."
                )
            self.asset_path(asset).unlink()
            del self.assets[asset_id]
            self.save()

    def close(self):
        with self.lock:
            self.closing = True
            ids = [j["id"] for j in self.jobs.values() if j["state"] not in TERMINAL]
        for job_id in ids:
            self.cancel(job_id)
        self.pool.shutdown(wait=True)
        self.save()
        self.library_lock.close()


class Server(ThreadingHTTPServer):
    daemon_threads = True

    def __init__(self, workshop, port=0):
        self.workshop = workshop
        self.token = secrets.token_urlsafe(32)
        super().__init__(("127.0.0.1", port), Handler)


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, *args):
        pass

    def host_ok(self):
        port = self.server.server_address[1]
        return self.headers.get("Host") in {f"127.0.0.1:{port}", f"localhost:{port}"}

    def headers_for(self, status, length, content_type):
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(length))
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("X-Frame-Options", "DENY")
        self.send_header("Referrer-Policy", "no-referrer")
        self.send_header("Cache-Control", "no-store")
        self.send_header(
            "Content-Security-Policy",
            "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; media-src 'self' blob:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'",
        )

    def reply(self, status, value):
        data = json.dumps(value).encode()
        self.headers_for(status, len(data), "application/json; charset=utf-8")
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        try:
            if not self.host_ok():
                self.reply(403, {"error": "Local access only."})
                return
            path = urlsplit(self.path).path
            if path == "/api/state":
                self.reply(
                    200, {**self.server.workshop.snapshot(), "token": self.server.token}
                )
            elif path.startswith("/media/"):
                asset = self.server.workshop.asset(path.rsplit("/", 1)[-1])
                self.file(
                    self.server.workshop.asset_path(asset),
                    asset,
                    "download=1" in urlsplit(self.path).query,
                )
            elif path in {"/", "/app.js", "/style.css"}:
                file = ROOT / "static" / ("index.html" if path == "/" else path[1:])
                data = file.read_bytes()
                self.headers_for(
                    200, len(data), mimetypes.guess_type(file.name)[0] or "text/plain"
                )
                self.end_headers()
                self.wfile.write(data)
            else:
                self.reply(404, {"error": "Not found."})
        except MediaError as error:
            self.reply(404, {"error": str(error)})
        except (BrokenPipeError, ConnectionResetError):
            pass

    def file(self, path, asset, download=False):
        size = path.stat().st_size
        start, end, partial = 0, size - 1, False
        requested = self.headers.get("Range")
        if requested:
            match = re.fullmatch(r"bytes=(\d*)-(\d*)", requested)
            if not match or not any(match.groups()):
                self.headers_for(416, 0, "application/octet-stream")
                self.send_header("Content-Range", f"bytes */{size}")
                self.end_headers()
                return
            a, b = match.groups()
            if a:
                start, end = int(a), min(int(b), size - 1) if b else size - 1
            else:
                start = max(0, size - int(b))
            if start >= size or end < start:
                self.headers_for(416, 0, "application/octet-stream")
                self.send_header("Content-Range", f"bytes */{size}")
                self.end_headers()
                return
            partial = True
        self.headers_for(
            206 if partial else 200,
            end - start + 1,
            mimetypes.guess_type(path.name)[0] or "application/octet-stream",
        )
        self.send_header("Accept-Ranges", "bytes")
        if partial:
            self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
        if download:
            from urllib.parse import quote

            self.send_header(
                "Content-Disposition",
                "attachment; filename*=UTF-8''" + quote(asset["title"] + path.suffix),
            )
        self.end_headers()
        with path.open("rb") as stream:
            stream.seek(start)
            remaining = end - start + 1
            while remaining:
                chunk = stream.read(min(1024 * 1024, remaining))
                if not chunk:
                    break
                self.wfile.write(chunk)
                remaining -= len(chunk)

    def do_POST(self):
        try:
            # Do not reuse a connection with an unread/rejected request body.
            self.close_connection = True
            if not self.host_ok() or not secrets.compare_digest(
                self.headers.get("X-Workshop-Token", ""), self.server.token
            ):
                self.reply(
                    403, {"error": "Reload this local app before making changes."}
                )
                return
            origin = self.headers.get("Origin")
            if origin and origin != "http://" + self.headers.get("Host", ""):
                self.reply(403, {"error": "Local access only."})
                return
            size = int(self.headers.get("Content-Length", "0"))
            if size <= 0:
                raise MediaError("An empty request was received.")
            if self.path == "/api/import":
                self.upload(size)
                return
            if size > 256 * 1024:
                raise MediaError("This request is too large.")
            data = json.loads(self.rfile.read(size))
            if not isinstance(data, dict):
                raise MediaError("Expected a JSON object.")
            app = self.server.workshop
            if self.path == "/api/download":
                self.reply(
                    202,
                    {
                        "ids": app.download(
                            data.get("urls"),
                            data.get("format"),
                            data.get("height", 1080),
                        )
                    },
                )
            elif self.path == "/api/trim":
                self.reply(
                    202,
                    {
                        "ids": app.trim(
                            data.get("assets"),
                            data.get("format"),
                            data.get("start", 0),
                            data.get("end"),
                        )
                    },
                )
            elif self.path == "/api/cancel":
                app.cancel(data.get("id"))
                self.reply(200, {"ok": True})
            elif self.path == "/api/retry":
                self.reply(202, {"ids": app.retry(data.get("id"))})
            elif self.path == "/api/delete":
                app.remove_asset(data.get("id"))
                self.reply(200, {"ok": True})
            elif self.path == "/api/clear":
                with app.lock:
                    app.jobs = {
                        k: j for k, j in app.jobs.items() if j["state"] not in TERMINAL
                    }
                    app.save()
                self.reply(200, {"ok": True})
            else:
                self.reply(404, {"error": "Not found."})
        except (BrokenPipeError, ConnectionResetError):
            pass
        except (
            MediaError,
            ValueError,
            TypeError,
            OSError,
            subprocess.TimeoutExpired,
        ) as error:
            self.reply(400, {"error": str(error)})

    def upload(self, size):
        name = safe_name(unquote(self.headers.get("X-File-Name", "media")))
        suffix = Path(name).suffix.lower()
        if suffix not in INPUTS:
            raise MediaError(
                "Choose an MP4, MP3 or another supported audio/video file."
            )
        folder = self.server.workshop.directory / "imports" / uuid.uuid4().hex
        folder.mkdir(parents=True)
        path = folder / name
        try:
            with path.open("wb") as stream:
                remaining = size
                while remaining:
                    chunk = self.rfile.read(min(1024 * 1024, remaining))
                    if not chunk:
                        raise MediaError("The file import was interrupted.")
                    stream.write(chunk)
                    remaining -= len(chunk)
            asset = self.server.workshop.register(path, Path(name).stem)
        except Exception:
            shutil.rmtree(folder, ignore_errors=True)
            raise
        self.reply(201, {"asset": asset})


def wait_for_jobs(app, ids):
    last = None
    while True:
        jobs = {j["id"]: j for j in app.snapshot()["jobs"]}
        selected = [jobs[i] for i in ids]
        status = " | ".join(
            f"{j['title'][:55]}: {j['stage']} {j['progress']:.0f}%" for j in selected
        )
        if status != last:
            print(status, flush=True)
            last = status
        if all(j["state"] in TERMINAL for j in selected):
            for job in selected:
                if job["error"]:
                    print(job["error"], file=sys.stderr)
                if job["asset_id"]:
                    asset = app.asset(job["asset_id"])
                    if job.get("trim_start") is not None:
                        print(
                            f"{job['method']}: source start {job['trim_start']:.3f}s "
                            f"(requested {job['spec']['start']:.3f}s), output duration {asset['duration']:.3f}s"
                        )
                    print(app.asset_path(asset))
            return 0 if all(j["state"] == "complete" for j in selected) else 1
        time.sleep(0.3)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command")
    gui = commands.add_parser("gui", help="Open the local browser app (default).")
    gui.add_argument(
        "--port",
        type=int,
        default=8766,
        help="Local port; use 0 for an available port.",
    )
    gui.add_argument("--no-browser", action="store_true")
    download = commands.add_parser(
        "download", help="Batch-download YouTube/Twitch links."
    )
    download.add_argument("urls", nargs="+")
    download.add_argument("--format", choices=sorted(FORMATS), default="mp4")
    download.add_argument("--height", type=int, choices=sorted(HEIGHTS), default=1080)
    trim = commands.add_parser("trim", help="Trim/convert one or several local files.")
    trim.add_argument("files", nargs="+", type=Path)
    trim.add_argument("--start", default="0")
    trim.add_argument("--end", default="", help="Blank means the end of each source.")
    trim.add_argument("--format", choices=sorted(FORMATS), default="mp4")
    trim.add_argument("--mode", help=argparse.SUPPRESS)  # Legacy spelling; ignored.
    for command in (gui, download, trim):
        command.add_argument("--data-dir", type=Path, default=ROOT / "data")
        command.add_argument("--workers", type=int, choices=(1, 2, 3, 4), default=2)
    args = parser.parse_args(sys.argv[1:] or ["gui"])
    app = Workshop(args.data_dir, args.workers)
    server = None
    try:
        if args.command == "download":
            return wait_for_jobs(app, app.download(args.urls, args.format, args.height))
        if args.command == "trim":
            assets = []
            # Copy inputs into the library; source files are never modified.
            for source in args.files:
                source = source.resolve(strict=True)
                details = probe(source)
                folder = app.directory / "imports" / uuid.uuid4().hex
                folder.mkdir(parents=True)
                path = folder / safe_name(source.name)
                shutil.copy2(source, path)
                assets.append(app.register(path, source.stem, details)["id"])
            return wait_for_jobs(
                app, app.trim(assets, args.format, args.start, args.end)
            )
        server = Server(app, args.port)
        url = f"http://127.0.0.1:{server.server_address[1]}"
        print(
            f"Media Workshop: {url}\nMedia library: {app.directory}\nKeep this terminal open. Press Ctrl+C to quit.",
            flush=True,
        )
        if not args.no_browser:
            threading.Timer(0.4, lambda: webbrowser.open(url)).start()
        server.serve_forever(poll_interval=0.2)
    except KeyboardInterrupt:
        print("\nStopping jobs…", flush=True)
        return 130
    finally:
        if server:
            server.server_close()
        app.close()


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (MediaError, OSError, subprocess.TimeoutExpired) as error:
        raise SystemExit(f"Media Workshop: {error}")
