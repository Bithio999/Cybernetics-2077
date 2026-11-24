"""Real FFmpeg/yt-dlp fixture tests; no external site or account is needed."""

import hashlib
import http.client
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from unittest.mock import patch
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
import app


def until(predicate, timeout=30):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        value = predicate()
        if value:
            return value
        time.sleep(0.04)
    raise AssertionError("Timed out waiting for the real media worker")


def completed(workshop, ids):
    def result():
        jobs = {j["id"]: j for j in workshop.snapshot()["jobs"]}
        values = [jobs[i] for i in ids]
        return values if all(j["state"] in app.TERMINAL for j in values) else None

    return until(result)


class Rules(unittest.TestCase):
    def test_time_formats_and_fractional_precision(self):
        for value, expected in [
            (0, 0),
            ("2.75", 2.75),
            ("01:02.500", 62.5),
            ("2:03:04.125", 7384.125),
        ]:
            self.assertEqual(app.timestamp(value), expected)

    def test_invalid_or_nonfinite_times(self):
        for value in [
            "nan",
            "inf",
            "-1",
            "1:60",
            "1:2:60",
            "1.2:30",
            "1:2:3:4",
            "",
            None,
        ]:
            with self.subTest(value=value), self.assertRaises(app.MediaError):
                app.timestamp(value)

    def test_only_normal_youtube_and_twitch_urls(self):
        for url in [
            "https://youtu.be/abc",
            "https://www.youtube.com/watch?v=abc",
            "https://m.youtube.com/shorts/abc",
            "https://www.twitch.tv/videos/123",
            "https://clips.twitch.tv/Example",
        ]:
            self.assertEqual(app.supported_url(url), url)
        for url in [
            "https://youtube.com.evil.test/a",
            "https://twitch.tv.evil.test",
            "https://evil.test/youtube.com",
            "file:///a.mp4",
            "https://user:pass@youtube.com/x",
            "https://youtube.com:999/x",
            "--exec=bad",
            "https://youtube.com:bad/a",
        ]:
            with self.subTest(url=url), self.assertRaises(app.MediaError):
                app.supported_url(url)

    def test_cross_platform_names(self):
        self.assertEqual(app.safe_name("a/b\\c:de?f*"), "a_b_c_de_f_")
        self.assertEqual(app.safe_name("CON.mp4"), "_CON.mp4")
        self.assertNotIn("\n", app.safe_name("a\nb"))
        self.assertLessEqual(len(app.safe_name("x" * 300)), 120)

    def test_formats_and_qualities(self):
        self.assertEqual(app.options("mp4", "1080"), ("mp4", 1080))
        for fmt, height in [("exe", 1080), ("mp3", 123), ("mp4", "bad")]:
            with self.assertRaises(app.MediaError):
                app.options(fmt, height)

    def test_playlist_and_channel_links_cannot_expand_into_unbounded_downloads(self):
        for url in [
            "https://www.youtube.com/playlist?list=example",
            "https://www.youtube.com/@channel",
            "https://www.twitch.tv/channel",
            "https://youtu.be/",
        ]:
            with self.subTest(url=url), self.assertRaises(app.MediaError):
                app.supported_url(url)
        self.assertEqual(
            app.supported_url("https://youtube.com/watch?v=abc&list=example"),
            "https://youtube.com/watch?v=abc&list=example",
        )


class Fixture:
    @classmethod
    def setUpClass(cls):
        cls.fixture = tempfile.TemporaryDirectory()
        cls.source = Path(cls.fixture.name) / "fixture.mp4"
        subprocess.run(
            [
                app.executable("ffmpeg"),
                "-v",
                "error",
                "-f",
                "lavfi",
                "-i",
                "testsrc2=size=160x90:rate=20",
                "-f",
                "lavfi",
                "-i",
                "sine=frequency=880:sample_rate=44100",
                "-t",
                "4",
                "-c:v",
                "libx264",
                "-threads",
                "2",
                "-c:a",
                "aac",
                "-pix_fmt",
                "yuv420p",
                str(cls.source),
            ],
            check=True,
        )

    @classmethod
    def tearDownClass(cls):
        cls.fixture.cleanup()

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.workshop = app.Workshop(Path(self.temp.name) / "library", workers=2)
        copied = self.workshop.directory / "fixture.mp4"
        copied.write_bytes(self.source.read_bytes())
        self.original_hash = hashlib.sha256(copied.read_bytes()).hexdigest()
        self.media = self.workshop.register(copied, "Fixture")

    def tearDown(self):
        self.workshop.close()
        self.temp.cleanup()

    def assert_success(self, ids):
        jobs = completed(self.workshop, ids)
        for job in jobs:
            self.assertEqual(job["state"], "complete", job["error"])
        return [self.workshop.asset(job["asset_id"]) for job in jobs]


class Media(Fixture, unittest.TestCase):
    def test_real_mp4_and_mp3_exports_preserve_original_and_exact_range(self):
        ids = self.workshop.trim([self.media["id"]], "mp4", "1.200", "2.700")
        ids += self.workshop.trim([self.media["id"]], "mp3", ".5", "1.8")
        video, audio = self.assert_success(ids)
        self.assertAlmostEqual(video["duration"], 1.5, delta=0.08)
        self.assertAlmostEqual(audio["duration"], 1.3, delta=0.10)
        self.assertTrue(video["video"] and video["audio"])
        self.assertFalse(audio["video"])
        self.assertTrue(audio["audio"])
        self.assertEqual(
            hashlib.sha256(
                self.workshop.asset_path(self.media).read_bytes()
            ).hexdigest(),
            self.original_hash,
        )
        self.assertNotEqual(video["path"], audio["path"])

    def test_batch_exports_and_unique_outputs(self):
        extra = self.workshop.register(
            self.workshop.asset_path(self.media), "Same title"
        )
        ids = self.workshop.trim([self.media["id"], extra["id"]], "mp4", 0, 1)
        output = self.assert_success(ids)
        self.assertEqual(len({a["path"] for a in output}), 2)

    def test_invalid_batch_is_atomic(self):
        with self.assertRaises(app.MediaError):
            self.workshop.download(["https://youtu.be/abc", "https://evil.test"], "mp4")
        with self.assertRaises(app.MediaError):
            self.workshop.trim([self.media["id"], "missing"], "mp4", 0, 1)
        self.assertEqual(self.workshop.jobs, {})

    def test_invalid_and_past_end_ranges(self):
        for start, end in [(2, 1), (0, 7), (5, ""), ("nan", 1)]:
            with self.assertRaises(app.MediaError):
                self.workshop.trim([self.media["id"]], "mp4", start, end)
        self.assertFalse(self.workshop.jobs)

    def test_audio_cannot_be_exported_as_fake_video(self):
        audio = self.assert_success(
            self.workshop.trim([self.media["id"]], "mp3", 0, 1)
        )[0]
        with self.assertRaisesRegex(app.MediaError, "no video"):
            self.workshop.trim([audio["id"]], "mp4", 0, 0.5)

    def test_same_library_cannot_be_opened_twice(self):
        with self.assertRaisesRegex(app.MediaError, "already open"):
            app.Workshop(self.workshop.directory)

    def test_persistent_library_and_interrupted_job_recovery(self):
        self.assert_success(self.workshop.trim([self.media["id"]], "mp3", 0, 1))
        directory = self.workshop.directory
        self.workshop.close()
        state_path = directory / "library.json"
        state = json.loads(state_path.read_text())
        state["jobs"][0]["state"] = "running"
        state_path.write_text(json.dumps(state))
        self.workshop = app.Workshop(directory)
        self.assertEqual(len(self.workshop.assets), 2)
        self.assertEqual(
            next(iter(self.workshop.jobs.values()))["state"], "interrupted"
        )
        self.assertTrue(
            self.workshop.asset_path(self.workshop.asset(self.media["id"])).is_file()
        )

    def test_cancel_real_running_process_and_queued_job(self):
        self.workshop.pool.shutdown(wait=True)
        from concurrent.futures import ThreadPoolExecutor

        self.workshop.pool = ThreadPoolExecutor(max_workers=1)
        ready = threading.Event()
        original_run = self.workshop.run

        def run(job_id, command, callback):
            def observed(line):
                if line == "READY":
                    ready.set()
                callback(line)

            return original_run(job_id, command, observed)

        command = [
            sys.executable,
            "-u",
            "-c",
            "import time; print('READY', flush=True); time.sleep(60)",
        ]
        with (
            patch.object(app, "download_command", return_value=command),
            patch.object(self.workshop, "run", side_effect=run),
        ):
            ids = self.workshop.download(
                ["https://youtu.be/first", "https://youtu.be/second"], "mp4"
            )
            self.assertTrue(ready.wait(5))
            self.workshop.cancel(ids[1])
            self.workshop.cancel(ids[0])
            jobs = completed(self.workshop, ids)
        self.assertTrue(all(j["state"] == "cancelled" for j in jobs))
        self.assertFalse(self.workshop.processes)
        self.assertEqual(len(self.workshop.assets), 1)

    def test_failed_job_can_be_retried_without_poisoning_batch(self):
        command = [sys.executable, "-c", "raise SystemExit('fixture error')"]
        with patch.object(app, "download_command", return_value=command):
            ids = self.workshop.download(["https://youtu.be/one"], "mp4")
            self.assertEqual(completed(self.workshop, ids)[0]["state"], "failed")
            retry = self.workshop.retry(ids[0])
            self.assertEqual(completed(self.workshop, retry)[0]["state"], "failed")
        self.assertNotEqual(ids, retry)

    @unittest.skipUnless(
        importlib.util.find_spec("yt_dlp"),
        "Install requirements.txt for the actual downloader integration",
    )
    def test_actual_ytdlp_pipeline_against_local_media_fixture(self):
        class FixtureHandler(SimpleHTTPRequestHandler):
            def __init__(self, *args, **kwargs):
                super().__init__(*args, directory=Media.fixture.name, **kwargs)

            def log_message(self, *args):
                pass

        fixture_server = ThreadingHTTPServer(("127.0.0.1", 0), FixtureHandler)
        thread = threading.Thread(target=fixture_server.serve_forever, daemon=True)
        thread.start()
        url = f"http://127.0.0.1:{fixture_server.server_address[1]}/fixture.mp4"
        try:
            # Local test-only generic extractor. Production still restricts links to YouTube/Twitch.
            with patch.object(app, "supported_url", side_effect=lambda value: value):
                ids = self.workshop.download([url], "mp4")
                ids += self.workshop.download([url], "mp3")
                video, audio = self.assert_success(ids)
            self.assertTrue(video["video"])
            self.assertTrue(audio["audio"] and not audio["video"])
            self.assertAlmostEqual(video["duration"], 4, delta=0.1)
        finally:
            fixture_server.shutdown()
            fixture_server.server_close()
            thread.join()


class HTTP(Fixture, unittest.TestCase):
    def setUp(self):
        super().setUp()
        self.server = app.Server(self.workshop)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()
        super().tearDown()

    def http(self, method, path, data=None, headers=None):
        connection = http.client.HTTPConnection(
            "127.0.0.1", self.server.server_address[1], timeout=10
        )
        connection.request(method, path, body=data, headers=headers or {})
        response = connection.getresponse()
        result = response.status, dict(response.getheaders()), response.read()
        connection.close()
        return result

    def test_media_seeking_uses_correct_byte_ranges(self):
        raw = self.workshop.asset_path(self.media).read_bytes()
        path = "/media/" + self.media["id"]
        status, headers, body = self.http("GET", path, headers={"Range": "bytes=10-29"})
        self.assertEqual(status, 206)
        self.assertEqual(headers["Content-Range"], f"bytes 10-29/{len(raw)}")
        self.assertEqual(body, raw[10:30])
        self.assertEqual(
            self.http("GET", path, headers={"Range": "bytes=-8"})[2], raw[-8:]
        )
        self.assertEqual(
            self.http("GET", path, headers={"Range": "bytes=99999999-"})[0], 416
        )

    def test_post_token_and_host_guards(self):
        body = json.dumps({"urls": ["https://youtu.be/one"], "format": "mp3"})
        self.assertEqual(self.http("POST", "/api/download", body)[0], 403)
        self.assertEqual(
            self.http("GET", "/api/state", headers={"Host": "malicious.test"})[0], 403
        )
        self.assertEqual(self.http("GET", "/media/../../library.json")[0], 404)
        self.assertEqual(
            self.http(
                "POST", "/api/trim", "[]", {"X-Workshop-Token": self.server.token}
            )[0],
            400,
        )
        self.assertFalse(self.workshop.jobs)

    def test_real_upload_export_download_and_delete(self):
        headers = {
            "X-Workshop-Token": self.server.token,
            "X-File-Name": quote("Test vidéo.mp4"),
        }
        status, _, body = self.http(
            "POST", "/api/import", self.source.read_bytes(), headers
        )
        self.assertEqual(status, 201, body)
        imported = json.loads(body)["asset"]
        data = json.dumps(
            {"assets": [imported["id"]], "format": "mp3", "start": ".5", "end": "1.5"}
        )
        status, _, body = self.http("POST", "/api/trim", data, headers)
        self.assertEqual(status, 202, body)
        output = self.assert_success(json.loads(body)["ids"])[0]
        status, download_headers, raw = self.http(
            "GET", "/media/" + output["id"] + "?download=1"
        )
        self.assertEqual(status, 200)
        self.assertIn("attachment", download_headers["Content-Disposition"])
        self.assertEqual(raw, self.workshop.asset_path(output).read_bytes())
        self.assertEqual(
            self.http("POST", "/api/delete", json.dumps({"id": output["id"]}), headers)[
                0
            ],
            200,
        )
        self.assertEqual(self.http("GET", "/media/" + output["id"])[0], 404)


if __name__ == "__main__":
    unittest.main()
