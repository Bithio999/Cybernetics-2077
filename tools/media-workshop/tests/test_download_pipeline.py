"""Real yt-dlp component downloads/merging against local, pre-extracted fixtures."""

from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
import subprocess
import threading
import unittest
from unittest.mock import patch

from test_workshop import Fixture, app
from test_download_optimization import packet_hashes


class DownloadPipeline(Fixture, unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        root = Path(cls.fixture.name)
        cls.video, cls.audio, cls.opus = (
            root / "video.mp4",
            root / "audio.m4a",
            root / "audio.webm",
        )
        for path, args in (
            (cls.video, ["-map", "0:v:0", "-c", "copy"]),
            (cls.audio, ["-map", "0:a:0", "-c", "copy"]),
            (cls.opus, ["-map", "0:a:0", "-c:a", "libopus"]),
        ):
            subprocess.run(
                [
                    app.executable("ffmpeg"),
                    "-v",
                    "error",
                    "-i",
                    str(cls.source),
                    *args,
                    str(path),
                ],
                check=True,
            )

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

        cls.server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join()
        super().tearDownClass()

    def merged_download(self, audio, codec):
        base = f"http://127.0.0.1:{self.server.server_address[1]}"
        manifest = Path(self.temp.name) / "fixture-info.json"
        manifest.write_text(
            json.dumps(
                {
                    "id": "fixture",
                    "title": "Separate tracks",
                    "extractor": "generic",
                    "extractor_key": "Generic",
                    "webpage_url": base + "/fixture",
                    "formats": [
                        {
                            "format_id": "video",
                            "url": base + "/" + self.video.name,
                            "ext": "mp4",
                            "vcodec": "avc1.64000a",
                            "acodec": "none",
                            "width": 160,
                            "height": 90,
                            "fps": 20,
                        },
                        {
                            "format_id": "audio",
                            "url": base + "/" + audio.name,
                            "ext": audio.suffix[1:],
                            "vcodec": "none",
                            "acodec": codec,
                        },
                    ],
                }
            )
        )
        original_command, run = app.download_command, self.workshop.run
        lines, stages = [], []

        def command(*args):
            value = original_command(*args)
            return [*value[:-2], "--load-info-json", str(manifest), *value[-2:]]

        def observe(job_id, command, callback):
            def update(line):
                lines.append(line)
                callback(line)
                stages.append(self.workshop.jobs[job_id]["stage"])

            return run(job_id, command, update)

        with (
            patch.object(app, "download_command", side_effect=command),
            patch.object(self.workshop, "run", side_effect=observe),
        ):
            ids = self.workshop.download(["https://youtu.be/fixture"], "mp4")
            asset = self.assert_success(ids)[0]
        return (
            self.workshop.jobs[ids[0]],
            self.workshop.asset_path(asset),
            lines,
            stages,
        )

    def test_compatible_tracks_merge_directly_to_ready_mp4_without_another_remux(self):
        job, output, lines, stages = self.merged_download(self.audio, "mp4a.40.2")
        self.assertEqual(job["method"], "Direct save")
        self.assertTrue(app.fast_start_mp4(output))
        self.assertEqual(packet_hashes(self.video, "v:0"), packet_hashes(output, "v:0"))
        self.assertEqual(packet_hashes(self.audio, "a:0"), packet_hashes(output, "a:0"))
        reports = [
            json.loads(line[9:]) for line in lines if line.startswith("PROGRESS:")
        ]
        self.assertTrue(reports)
        self.assertTrue(
            all(
                set(r)
                == {"status", "downloaded_bytes", "total_bytes", "total_bytes_estimate"}
                for r in reports
            )
        )
        self.assertTrue(
            all(len(line) < 400 for line in lines if line.startswith("PROGRESS:"))
        )
        self.assertIn("Combining tracks", stages)

    def test_incompatible_combination_retains_matroska_fallback_and_selective_conversion(
        self,
    ):
        job, output, _, _ = self.merged_download(self.opus, "opus")
        self.assertEqual(job["method"], "Audio conversion only")
        self.assertEqual(app.probe(output)["audio_codec"], "aac")
        self.assertEqual(packet_hashes(self.video, "v:0"), packet_hashes(output, "v:0"))

    def test_extractor_cache_is_shared_between_jobs_inside_the_library(self):
        import yt_dlp

        root = self.workshop.directory / "jobs"
        options = []
        for name in ("first", "second"):
            command = app.download_command(
                "https://youtu.be/fixture", root / name, "mp4", 1080
            )
            options.append(yt_dlp.parse_options(command[3:]).ydl_opts)
        cache = Path(options[0]["cachedir"])
        self.assertTrue(cache.is_relative_to(self.workshop.directory))
        self.assertEqual(options[0]["cachedir"], options[1]["cachedir"])
        with yt_dlp.YoutubeDL({**options[0], "quiet": True}) as first:
            first.cache.store("fixture", "public-player-data", {"test": "reusable"})
        with yt_dlp.YoutubeDL({**options[1], "quiet": True}) as second:
            self.assertEqual(
                second.cache.load("fixture", "public-player-data"), {"test": "reusable"}
            )
