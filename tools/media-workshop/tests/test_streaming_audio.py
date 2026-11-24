"""Real yt-dlp/FFmpeg streaming against a throttled local fixture, no external sites."""

from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from unittest.mock import patch

from test_workshop import app
import media_worker


class StreamingAudio(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.fixture = tempfile.TemporaryDirectory()
        cls.root = Path(cls.fixture.name)
        cls.audio = cls.root / "audio.m4a"
        subprocess.run(
            [
                app.executable("ffmpeg"),
                "-v",
                "error",
                "-f",
                "lavfi",
                "-i",
                "sine=frequency=880:sample_rate=48000",
                "-t",
                "20",
                "-c:a",
                "aac",
                "-b:a",
                "160k",
                "-movflags",
                "+faststart",
                str(cls.audio),
            ],
            check=True,
        )
        cls.native = cls.root / "native.mp3"
        subprocess.run(
            [
                app.executable("ffmpeg"),
                "-v",
                "error",
                "-i",
                str(cls.audio),
                "-c:a",
                "libmp3lame",
                "-b:a",
                "192k",
                str(cls.native),
            ],
            check=True,
        )
        root = cls.root

        class Handler(SimpleHTTPRequestHandler):
            def __init__(self, *args, **kwargs):
                super().__init__(*args, directory=str(root), **kwargs)

            def log_message(self, *args):
                pass

            def handle(self):
                try:
                    super().handle()
                except (BrokenPipeError, ConnectionResetError):
                    pass

            def copyfile(self, source, destination):
                while chunk := source.read(4096):
                    destination.write(chunk)
                    time.sleep(0.01 if "slow=1" in self.path else 0.001)

        cls.server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join()
        cls.fixture.cleanup()

    def run_stream(self, source):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        folder = Path(temporary.name)
        output = folder / "output.mp3"
        events = []
        url = f"http://127.0.0.1:{self.server.server_address[1]}/{source.name}"
        with (
            patch.object(app, "supported_url", side_effect=lambda value: value),
            patch.object(
                media_worker, "emit", side_effect=lambda **values: events.append(values)
            ),
        ):
            media_worker.mp3_stream(url, output, folder)
        return output, events

    def test_conversion_starts_before_download_finishes_and_is_playable(self):
        output, events = self.run_stream(self.audio)
        self.assertTrue(
            any(e.get("encoder_started_before_download_finished") for e in events)
        )
        self.assertTrue(
            any(e.get("stage") == "Downloading and converting MP3" for e in events)
        )
        self.assertEqual(app.probe(output)["audio_codec"], "mp3")
        self.assertAlmostEqual(app.probe(output)["duration"], 20, delta=0.1)
        decoded = subprocess.run(
            [
                app.executable("ffmpeg"),
                "-v",
                "error",
                "-i",
                str(output),
                "-f",
                "null",
                "-",
            ],
            capture_output=True,
        )
        self.assertEqual(decoded.returncode, 0, decoded.stderr)
        self.assertEqual(decoded.stderr, b"")

    def test_stream_keeps_all_audio_samples_without_chunk_join_gaps(self):
        output, _ = self.run_stream(self.audio)

        def pcm(path):
            return subprocess.check_output(
                [
                    app.executable("ffmpeg"),
                    "-v",
                    "error",
                    "-i",
                    str(path),
                    "-map",
                    "0:a:0",
                    "-ac",
                    "1",
                    "-ar",
                    "48000",
                    "-f",
                    "s16le",
                    "-",
                ]
            )

        self.assertEqual(len(pcm(output)), len(pcm(self.audio)))
        rate = subprocess.check_output(
            [
                app.executable("ffprobe"),
                "-v",
                "error",
                "-show_entries",
                "stream=bit_rate",
                "-of",
                "json",
                str(output),
            ]
        )
        self.assertEqual(int(json.loads(rate)["streams"][0]["bit_rate"]), 192000)

    def test_native_mp3_is_preserved_byte_for_byte(self):
        output, events = self.run_stream(self.native)
        self.assertEqual(output.read_bytes(), self.native.read_bytes())
        self.assertTrue(any(e.get("method") == "Direct save" for e in events))
        self.assertFalse(
            any(e.get("encoder_started_before_download_finished") for e in events)
        )

    def test_real_nonstreamable_input_requests_seekable_fallback(self):
        source = self.root / "seekable-only.m4a"
        subprocess.run(
            [
                app.executable("ffmpeg"),
                "-v",
                "error",
                "-y",
                "-i",
                str(self.audio),
                "-c:a",
                "copy",
                str(source),
            ],
            check=True,
        )
        with self.assertRaisesRegex(app.MediaError, "STREAM_FALLBACK:"):
            self.run_stream(source)

    def test_failed_downloader_never_succeeds_even_with_decodable_audio(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            metadata = folder / "meta.json"
            script = "import sys,json;from pathlib import Path;Path(sys.argv[2]).write_text(json.dumps({'title':'fixture','ext':'mp3','duration':20}));sys.stdout.buffer.write(Path(sys.argv[1]).read_bytes());raise SystemExit(3)"
            command = [sys.executable, "-c", script, str(self.native), str(metadata)]
            with (
                patch.object(
                    media_worker,
                    "stream_source_command",
                    return_value=(command, metadata),
                ),
                patch.object(media_worker, "emit"),
            ):
                with self.assertRaises(app.MediaError):
                    media_worker.mp3_stream("fixture", folder / "out.mp3", folder)

    def test_nonstreamable_container_uses_the_safe_existing_download_path(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        directory = temporary.name
        app_instance = app.Workshop(Path(directory) / "library")
        self.addCleanup(app_instance.close)

        def normal(url, folder, fmt, height):
            target = folder / "source.m4a"
            script = "import sys,shutil,json;shutil.copyfile(sys.argv[1],sys.argv[2]);print('FILE:'+json.dumps(sys.argv[2]))"
            return [sys.executable, "-c", script, str(self.audio), str(target)]

        with (
            patch.object(
                app,
                "streaming_mp3_command",
                return_value=[
                    sys.executable,
                    "-c",
                    "raise SystemExit('STREAM_FALLBACK: input needs a seekable file')",
                ],
            ),
            patch.object(app, "download_command", side_effect=normal),
        ):
            ids = app_instance.download(["https://youtu.be/fixture"], "mp3")
            from test_workshop import completed

            jobs = completed(app_instance, ids)
        self.assertEqual(jobs[0]["state"], "complete", jobs[0]["error"])
        self.assertTrue(jobs[0]["audio_stream_fallback"])
        self.assertEqual(app_instance.asset(jobs[0]["asset_id"])["audio_codec"], "mp3")

    def test_cancelling_streaming_job_stops_processing_and_removes_partial_output(self):
        from media_fixtures import local_stream_command
        from test_workshop import until, completed

        with tempfile.TemporaryDirectory() as directory:
            instance = app.Workshop(Path(directory) / "library")
            try:
                url = (
                    f"http://127.0.0.1:{self.server.server_address[1]}/audio.m4a?slow=1"
                )
                with patch.object(
                    app,
                    "streaming_mp3_command",
                    side_effect=lambda ignored, out, folder: local_stream_command(
                        url, out, folder
                    ),
                ):
                    ids = instance.download(["https://youtu.be/fixture"], "mp3")
                    until(
                        lambda: instance.snapshot()["jobs"][0].get(
                            "encoder_started_before_download_finished"
                        )
                    )
                    instance.cancel(ids[0])
                    job = completed(instance, ids)[0]
                self.assertEqual(job["state"], "cancelled")
                self.assertFalse(instance.assets)
                self.assertFalse(instance.processes)
                self.assertFalse((instance.directory / "jobs" / ids[0]).exists())
            finally:
                instance.close()
