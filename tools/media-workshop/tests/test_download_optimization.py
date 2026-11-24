"""Check skipped encoding and track preservation using actual media and packet hashes."""

import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import subprocess
import sys
import unittest
import threading
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from unittest.mock import patch

from test_workshop import Fixture, app
from media_fixtures import local_stream_command


def packet_hashes(path, stream):
    data = subprocess.check_output(
        [
            app.executable("ffprobe"),
            "-v",
            "error",
            "-select_streams",
            stream,
            "-show_packets",
            "-show_data_hash",
            "sha256",
            "-show_entries",
            "packet=data_hash",
            "-of",
            "json",
            str(path),
        ]
    )
    return [packet["data_hash"] for packet in json.loads(data)["packets"]]


class DownloadOptimization(Fixture, unittest.TestCase):
    def variant(self, name, *codec_args):
        path = Path(self.temp.name) / name
        subprocess.run(
            [
                app.executable("ffmpeg"),
                "-v",
                "error",
                "-i",
                str(self.source),
                "-t",
                "1",
                "-map",
                "0:v:0",
                "-map",
                "0:a:0",
                *codec_args,
                str(path),
            ],
            check=True,
        )
        return path

    def finish(self, source, height=1080):
        destination = Path(self.temp.name) / (source.stem + "-finished.mp4")
        method, command = app.download_finish_plan(
            source, destination, "mp4", app.probe(source), height
        )
        if command:
            subprocess.run(command, check=True, capture_output=True)
        else:
            shutil.copy2(source, destination)
        return method, destination

    def fixture_download(self, source, fmt):
        def command(url, directory, output_format, height):
            output = directory / ("download" + source.suffix)
            script = "import shutil,json,sys;shutil.copy2(sys.argv[1],sys.argv[2]);print('TITLE:'+json.dumps('Fixture'));print('FILE:'+json.dumps(sys.argv[2]))"
            return [sys.executable, "-c", script, str(source), str(output)]

        if fmt == "mp3":

            class Handler(SimpleHTTPRequestHandler):
                def __init__(self, *args, **kwargs):
                    super().__init__(*args, directory=str(source.parent), **kwargs)

                def log_message(self, *args):
                    pass

                def handle(self):
                    try:
                        super().handle()
                    except (ConnectionResetError, BrokenPipeError):
                        pass

            server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
            thread = threading.Thread(target=server.serve_forever, daemon=True)
            thread.start()
            url = f"http://127.0.0.1:{server.server_address[1]}/{source.name}"
            try:
                with patch.object(
                    app,
                    "streaming_mp3_command",
                    side_effect=lambda ignored, dest, folder: local_stream_command(
                        url, dest, folder
                    ),
                ):
                    ids = self.workshop.download(["https://youtu.be/fixture"], fmt)
                    return ids[0], self.assert_success(ids)[0]
            finally:
                server.shutdown()
                server.server_close()
                thread.join()
        with patch.object(app, "download_command", side_effect=command):
            ids = self.workshop.download(["https://youtu.be/fixture"], fmt)
            return ids[0], self.assert_success(ids)[0]

    def test_ready_mp4_is_saved_byte_for_byte_by_worker(self):
        source = self.variant("ready.mp4", "-c", "copy", "-movflags", "+faststart")
        before = hashlib.sha256(source.read_bytes()).hexdigest()
        job_id, result = self.fixture_download(source, "mp4")
        self.assertEqual(self.workshop.jobs[job_id]["method"], "Direct save")
        self.assertEqual(
            hashlib.sha256(self.workshop.asset_path(result).read_bytes()).hexdigest(),
            before,
        )
        self.assertTrue(source.is_file(), "Fixture originals must remain intact")

    def test_remux_preserves_video_and_audio_packets_and_adds_fast_start(self):
        for source in [self.source, self.variant("compatible.mkv", "-c", "copy")]:
            with self.subTest(source=source.suffix):
                method, output = self.finish(source)
                self.assertEqual(method, "Stream copy")
                self.assertEqual(
                    packet_hashes(source, "v:0"), packet_hashes(output, "v:0")
                )
                self.assertEqual(
                    packet_hashes(source, "a:0"), packet_hashes(output, "a:0")
                )
                self.assertTrue(app.fast_start_mp4(output))

    def test_only_incompatible_audio_is_encoded(self):
        source = self.variant("h264-opus.mkv", "-c:v", "copy", "-c:a", "libopus")
        method, output = self.finish(source)
        self.assertEqual(method, "Audio conversion only")
        self.assertEqual(packet_hashes(source, "v:0"), packet_hashes(output, "v:0"))
        self.assertEqual(app.probe(output)["audio_codec"], "aac")
        self.assertEqual(app.probe(output)["video_codec"], "h264")

    def test_only_incompatible_video_is_encoded(self):
        source = self.variant("mpeg4-aac.mkv", "-c:v", "mpeg4", "-c:a", "copy")
        method, output = self.finish(source)
        self.assertEqual(method, "Video conversion only")
        self.assertEqual(packet_hashes(source, "a:0"), packet_hashes(output, "a:0"))
        self.assertEqual(app.probe(output)["video_codec"], "h264")

    def test_both_incompatible_tracks_are_converted(self):
        source = self.variant("mpeg4-opus.mkv", "-c:v", "mpeg4", "-c:a", "libopus")
        method, output = self.finish(source)
        meta = app.probe(output)
        self.assertEqual(method, "Full conversion")
        self.assertEqual(
            (meta["video_codec"], meta["audio_codec"], meta["pixel_format"]),
            ("h264", "aac", "yuv420p"),
        )

    def test_quality_ceiling_still_resizes_oversize_video(self):
        source = Path(self.temp.name) / "tall.mp4"
        subprocess.run(
            [
                app.executable("ffmpeg"),
                "-v",
                "error",
                "-f",
                "lavfi",
                "-i",
                "testsrc2=size=96x500:rate=10",
                "-f",
                "lavfi",
                "-i",
                "sine=sample_rate=44100",
                "-t",
                "1",
                "-c:v",
                "libx264",
                "-threads",
                "2",
                "-c:a",
                "aac",
                "-movflags",
                "+faststart",
                str(source),
            ],
            check=True,
        )
        method, output = self.finish(source, height=480)
        self.assertEqual(method, "Video conversion only")
        self.assertEqual(app.probe(output)["height"], 480)
        self.assertEqual(packet_hashes(source, "a:0"), packet_hashes(output, "a:0"))

    def test_existing_mp3_is_saved_without_a_second_lossy_encode(self):
        source = Path(self.temp.name) / "native.mp3"
        subprocess.run(
            [
                app.executable("ffmpeg"),
                "-v",
                "error",
                "-i",
                str(self.source),
                "-vn",
                "-c:a",
                "libmp3lame",
                "-b:a",
                "128k",
                str(source),
            ],
            check=True,
        )
        job_id, output = self.fixture_download(source, "mp3")
        self.assertEqual(self.workshop.jobs[job_id]["method"], "Direct save")
        self.assertEqual(
            source.read_bytes(), self.workshop.asset_path(output).read_bytes()
        )

    @unittest.skipUnless(importlib.util.find_spec("yt_dlp"), "Install requirements.txt")
    def test_ytdlp_prefers_compatible_codecs_without_lowering_resolution(self):
        import yt_dlp

        def select(height):
            command = app.download_command(
                "https://youtu.be/fixture", Path(self.temp.name), "mp4", height
            )
            opts = yt_dlp.parse_options(command[3:]).ydl_opts
            formats = [
                {
                    "format_id": "1080-av1",
                    "ext": "webm",
                    "height": 1080,
                    "width": 1920,
                    "fps": 30,
                    "vcodec": "av01",
                    "acodec": "none",
                },
                {
                    "format_id": "1080-h264",
                    "ext": "mp4",
                    "height": 1080,
                    "width": 1920,
                    "fps": 30,
                    "vcodec": "avc1.640028",
                    "acodec": "none",
                },
                {
                    "format_id": "2160-vp9",
                    "ext": "webm",
                    "height": 2160,
                    "width": 3840,
                    "fps": 30,
                    "vcodec": "vp9",
                    "acodec": "none",
                },
                {
                    "format_id": "audio-opus",
                    "ext": "webm",
                    "vcodec": "none",
                    "acodec": "opus",
                },
                {
                    "format_id": "audio-aac",
                    "ext": "m4a",
                    "vcodec": "none",
                    "acodec": "mp4a.40.2",
                },
            ]
            for fmt in formats:
                fmt["url"] = "https://fixture.invalid/" + fmt["format_id"]
            with yt_dlp.YoutubeDL(
                {
                    "quiet": True,
                    "format": opts["format"],
                    "format_sort": opts["format_sort"],
                }
            ) as downloader:
                info = downloader.process_ie_result(
                    {"id": "fixture", "title": "Fixture", "formats": formats},
                    download=False,
                )
            return [f["format_id"] for f in info["requested_formats"]]

        self.assertEqual(select(1080), ["1080-h264", "audio-aac"])
        self.assertEqual(select(2160), ["2160-vp9", "audio-aac"])
