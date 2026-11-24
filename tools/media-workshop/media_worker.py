#!/usr/bin/env python3
"""Isolated media workers. Children stay in the worker's cancellable process group."""

from collections import deque
from bisect import bisect_left
from fractions import Fraction
import argparse
import json
import os
from pathlib import Path
import subprocess
import shutil
import sys
import threading
import time

import app

_output_lock = threading.Lock()


def window_options():
    return {"creationflags": subprocess.CREATE_NO_WINDOW} if os.name == "nt" else {}


def emit(**data):
    with _output_lock:
        print("WORKSHOP:" + json.dumps(data, ensure_ascii=True), flush=True)


def ffmpeg(command, callback=lambda seconds: None, *, cancel_event=None):
    if cancel_event is not None and cancel_event.is_set():
        raise app.MediaError("Processing interrupted")
    process = subprocess.Popen(
        command,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        encoding="utf-8",
        errors="replace",
        **window_options(),
    )
    errors = deque(maxlen=12)
    try:
        for line in process.stdout:
            if cancel_event is not None and cancel_event.is_set():
                raise app.MediaError("Processing interrupted")
            errors.append(line.strip())
            if line.startswith("out_time_us="):
                try:
                    callback(float(line.split("=", 1)[1]) / 1e6)
                except ValueError:
                    pass
        if process.wait():
            raise app.MediaError("\n".join(errors)[-2400:])
    finally:
        if process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=3)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
        process.stdout.close()


def smart_trim(source, destination, start, end, threads):
    """Index the source directly and encode audio alongside boundary rendering."""
    from concurrent.futures import ThreadPoolExecutor
    import importlib
    import av
    from smartcut.media_utils import VideoExportMode, VideoExportQuality
    from smartcut.cut_video import (
        make_adjusted_segment_times,
        make_cut_segments,
        smart_cut,
        VideoCutter,
        VideoSettings,
    )
    from video_index import index_video_window

    engine = importlib.import_module("smartcut.cut_video")

    class BudgetedCutter(VideoCutter):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, **kwargs)
            # The backend normally opens its packet reader at the file's start.
            # Always seek directly to our first indexed GOP, including late cuts.
            self.input_av_container.seek(
                args[0].gop_start_times_dts[0], stream=self.in_stream
            )
            self.demux_iter = self.input_av_container.demux(self.in_stream)

        def init_encoder(self):
            super().init_encoder()
            self.encoding_options.update(preset="veryfast", threads=str(threads))

        def _fix_packet_timestamps(self, packet):
            super()._fix_packet_timestamps(packet)
            # PyAV's boundary encoder can leave duration at zero. MP4 otherwise
            # understates the tail duration and triggers an unnecessary full encode.
            if not packet.duration and self.in_stream.guessed_rate:
                packet.duration = max(
                    1, round(1 / self.in_stream.guessed_rate / self.out_time_base)
                )

    meta = app.probe(source)
    temporary = destination.parent / "processing"
    temporary.mkdir(exist_ok=True)
    video, audio = temporary / "video.mp4", temporary / "audio.m4a"
    emit(stage="Preparing cut", progress=1)
    base = [
        app.executable("ffmpeg"),
        "-y",
        "-hide_banner",
        "-loglevel",
        "warning",
        "-nostdin",
        "-protocol_whitelist",
        "file,pipe",
    ]
    stop_audio = threading.Event()
    executor = ThreadPoolExecutor(max_workers=1)
    container = None

    def encode_audio():
        # MP4 edit lists retain the selected sample start and AAC preroll. Copy
        # compatible AAC instead of needlessly encoding the entire audio track.
        audio_codec = (
            ["-c:a", "copy"]
            if meta["audio_codec"] == "aac"
            else ["-c:a", "aac", "-b:a", "192k"]
        )
        ffmpeg(
            [
                *base,
                "-ss",
                str(start),
                "-i",
                str(source),
                "-t",
                str(end - start),
                "-map",
                "0:a:0",
                "-vn",
                *audio_codec,
                "-threads",
                str(threads),
                "-map_metadata",
                "-1",
                "-movie_timescale",
                "1000000",
                "-progress",
                "pipe:1",
                "-nostats",
                str(audio),
            ],
            cancel_event=stop_audio,
        )
        with av.open(str(audio)) as encoded:
            return float(Fraction(encoded.start_time or 0, av.time_base))

    audio_future = executor.submit(encode_audio) if meta["audio"] else None
    try:
        container = index_video_window(source, start, end, threads)
        segments = [(Fraction(str(start)), Fraction(str(end)))]
        cuts = make_cut_segments(
            container, make_adjusted_segment_times(segments, container)
        )
        copied = sum(
            float(c.end_time - c.start_time) for c in cuts if not c.require_recode
        )
        expected_frames = bisect_left(
            container.video_frame_times, segments[0][1] + container.start_time
        ) - bisect_left(
            container.video_frame_times, segments[0][0] + container.start_time
        )
        if copied < max(1, (end - start) * 0.3):
            raise app.MediaError(
                "Boundary-only processing would not save work for this cut"
            )
        emit(
            stage="Copying video and rendering cut edges",
            smart_copied_seconds=copied,
            smart_boundary_segments=sum(c.require_recode for c in cuts),
            indexed_video_frames=len(container.video_frame_times),
            audio_processing=("copy" if meta["audio_codec"] == "aac" else "encode")
            if meta["audio"]
            else "none",
        )

        class Progress:
            total = None

            def emit(self, value):
                if self.total is None:
                    self.total = max(1, value)
                else:
                    emit(progress=min(90, 5 + 85 * value / self.total))

        original_cutter = engine.VideoCutter
        engine.VideoCutter = BudgetedCutter
        try:
            error = smart_cut(
                container,
                segments,
                str(video),
                video_settings=VideoSettings(
                    VideoExportMode.SMARTCUT, VideoExportQuality.NORMAL, "copy"
                ),
                progress=Progress(),
            )
            if error:
                raise error
        finally:
            engine.VideoCutter = original_cutter
        video_meta = app.probe(video)
        if video_meta["video_frames"] != expected_frames:
            raise app.MediaError("Boundary renderer returned an unexpected frame count")
        if abs(video_meta["duration"] - (end - start)) > max(
            0.025, 1.25 / max(1, meta.get("fps", 25))
        ):
            raise app.MediaError("Boundary renderer returned an unexpected duration")
        emit(
            stage="Finishing audio"
            if audio_future and not audio_future.done()
            else "Saving cut",
            progress=90,
        )
        audio_start = audio_future.result() if audio_future else 0
        emit(stage="Saving cut", progress=95)
        inputs = ["-itsoffset", str(video_meta["start_time"]), "-i", str(video)]
        if audio_future:
            inputs += ["-itsoffset", str(audio_start), "-i", str(audio)]
        # Both tracks are already finished: the last pass only muxes/copies them.
        ffmpeg(
            [
                *base,
                *inputs,
                "-t",
                str(end - start),
                "-map",
                "0:v:0",
                *(["-map", "1:a:0"] if audio_future else []),
                "-c",
                "copy",
                "-map_metadata",
                "-1",
                "-movflags",
                "+faststart",
                "-movie_timescale",
                "1000000",
                "-progress",
                "pipe:1",
                "-nostats",
                str(destination),
            ],
            lambda t: emit(progress=min(99, 95 + 4 * t / (end - start))),
        )
        emit(progress=100)
    finally:
        stop_audio.set()
        executor.shutdown(wait=True, cancel_futures=True)
        if container is not None:
            container.close()
        shutil.rmtree(temporary, ignore_errors=True)


def stream_source_command(url, folder):
    """Retain normal yt-dlp network/security settings, but send media to a pipe."""
    original = app.download_command(url, folder, "mp3", 1080)
    command, i = [], 0
    while i < len(original) - 2:  # final -- URL is appended below
        if original[i] in {"--paths", "--output", "--print"}:
            i += 2
        else:
            command.append(original[i])
            i += 1
    metadata = folder / "stream-metadata.json"
    metadata.unlink(missing_ok=True)
    template = 'before_dl:{"title":%(title)j,"ext":%(ext)j,"acodec":%(acodec|null)j,"duration":%(duration|null)j}'
    command += ["--output", "-", "--print-to-file", template, str(metadata), "--", url]
    return command, metadata


def mp3_stream(url, destination, folder):
    command, metadata_path = stream_source_command(url, folder)
    downloader = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, **window_options()
    )
    converter = None
    messages = deque(maxlen=12)
    state = {"download": 0.0, "encode": 0.0, "duration": 0.0, "native": False}
    state_lock = threading.Lock()

    def report():
        with state_lock:
            fraction = (
                state["download"]
                if state["native"] or not state["duration"]
                else min(state["download"], state["encode"])
            )
        emit(progress=min(99, fraction * 99))

    def watch_download():
        for raw in downloader.stderr:
            line = raw.decode("utf-8", "replace").strip()
            messages.append(line)
            if line.startswith("PROGRESS:"):
                try:
                    info = json.loads(line[9:])
                    total = (
                        info.get("total_bytes") or info.get("total_bytes_estimate") or 0
                    )
                    with state_lock:
                        if info.get("status") == "finished":
                            state["download"] = 1.0
                        elif total:
                            state["download"] = min(
                                1, (info.get("downloaded_bytes") or 0) / total
                            )
                    report()
                except (ValueError, TypeError):
                    pass

    reader = threading.Thread(target=watch_download, daemon=True)
    reader.start()
    try:
        emit(stage="Connecting to source", progress=0)
        deadline = time.monotonic() + 90
        data = None
        while time.monotonic() < deadline:
            if metadata_path.exists():
                try:
                    data = json.loads(
                        metadata_path.read_text(encoding="utf-8").splitlines()[0]
                    )
                    break
                except (ValueError, IndexError):
                    pass
            if downloader.poll() is not None:
                raise app.MediaError(
                    "\n".join(messages) or "Source metadata unavailable"
                )
            time.sleep(0.01)
        if data is None:
            raise app.MediaError("The source took too long to provide audio metadata")
        try:
            duration = float(data.get("duration") or 0)
        except (TypeError, ValueError):
            duration = 0
        native = data.get("ext") == "mp3"
        with state_lock:
            state.update(duration=duration, native=native)
        emit(
            title=app.safe_name(data.get("title") or "Audio"),
            method="Direct save" if native else "Streaming audio conversion",
            stage="Downloading MP3" if native else "Downloading and converting MP3",
            encoder_started_before_download_finished=not native
            and downloader.poll() is None,
        )
        if native:
            with destination.open("wb") as output:
                while chunk := downloader.stdout.read(1024 * 1024):
                    output.write(chunk)
        else:
            copying = data.get("acodec") == "mp3"
            encoding = (
                ["-c:a", "copy"] if copying else ["-c:a", "libmp3lame", "-b:a", "192k"]
            )
            converter = subprocess.Popen(
                [
                    app.executable("ffmpeg"),
                    "-y",
                    "-hide_banner",
                    "-loglevel",
                    "warning",
                    "-nostdin",
                    "-protocol_whitelist",
                    "file,pipe",
                    "-i",
                    "pipe:0",
                    "-map",
                    "0:a:0",
                    "-vn",
                    *encoding,
                    "-id3v2_version",
                    "3",
                    "-map_metadata",
                    "-1",
                    "-progress",
                    "pipe:1",
                    "-nostats",
                    str(destination),
                ],
                stdin=downloader.stdout,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                encoding="utf-8",
                errors="replace",
                **window_options(),
            )
            downloader.stdout.close()
            errors = deque(maxlen=12)
            unseekable = False
            for line in converter.stdout:
                errors.append(line.strip())
                if any(
                    text in line.lower()
                    for text in [
                        "moov atom not found",
                        "partial file",
                        "invalid data found",
                    ]
                ):
                    unseekable = True
                if line.startswith("out_time_us=") and duration > 0:
                    try:
                        with state_lock:
                            state["encode"] = max(
                                0, min(1, float(line.split("=", 1)[1]) / 1e6 / duration)
                            )
                        report()
                    except ValueError:
                        pass
            failed = converter.wait()
            if failed or unseekable:
                detail = "\n".join(errors)
                if unseekable:
                    raise app.MediaError("STREAM_FALLBACK: " + detail)
                raise app.MediaError(detail)
        if downloader.wait():
            reader.join(timeout=2)
            raise app.MediaError(
                "\n".join(messages)[-2400:] or "The audio download failed"
            )
        emit(stage="Finishing MP3", progress=99)
        try:
            output = app.probe(destination)
        except app.MediaError as error:
            raise app.MediaError("STREAM_FALLBACK: " + str(error)) from error
        if output["audio_codec"] != "mp3":
            raise app.MediaError("The source did not produce a valid MP3")
        emit(progress=100)
    finally:
        for process in [converter, downloader]:
            if process and process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=3)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()
        reader.join(timeout=2)
        for stream in [
            downloader.stdout,
            downloader.stderr,
            converter.stdout if converter else None,
        ]:
            if stream and not stream.closed:
                stream.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    cut = commands.add_parser("cut")
    cut.add_argument("source", type=Path)
    cut.add_argument("destination", type=Path)
    cut.add_argument("start", type=float)
    cut.add_argument("end", type=float)
    cut.add_argument("threads", type=int)
    audio = commands.add_parser("mp3")
    audio.add_argument("url")
    audio.add_argument("destination", type=Path)
    audio.add_argument("folder", type=Path)
    args = parser.parse_args()
    if args.command == "cut":
        smart_trim(args.source, args.destination, args.start, args.end, args.threads)
    else:
        mp3_stream(args.url, args.destination, args.folder)


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(str(error), file=sys.stderr, flush=True)
        raise SystemExit(1)
