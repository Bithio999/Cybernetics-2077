"""Account selection/privacy and actual cookie-authenticated local downloads.

Only synthetic cookies are used. No installed browser profile is opened.
"""

from contextlib import redirect_stderr
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
import io
import json
import os
from pathlib import Path
import stat
import sys
import tempfile
import threading
import unittest
from unittest.mock import patch

import test_workshop as fixtures
from test_workshop import Fixture, app, completed, until
from media_fixtures import local_stream_command
from download_auth import (
    AccountError,
    AccountSessions,
    CONFIG_NAME,
    COOKIE_NAME,
    account_arguments,
    cleanup_job_account,
)

SECRET = "fixture-session-only-not-a-real-credential"
COOKIES = (
    "# Netscape HTTP Cookie File\n"
    f"#HttpOnly_127.0.0.1\tFALSE\t/\tFALSE\t0\tsubscription_fixture\t{SECRET}\n"
).encode()


class AccountRules(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.accounts = AccountSessions()

    def tearDown(self):
        self.accounts.close()
        self.temp.cleanup()

    def test_public_download_does_not_read_any_browser_or_cookie_file(self):
        import yt_dlp

        command = app.download_command(
            "https://youtu.be/fixture", self.root, "mp4", 1080
        )
        options = yt_dlp.parse_options(command[3:]).ydl_opts
        self.assertFalse(options.get("cookiesfrombrowser"))
        self.assertFalse(options.get("cookiefile"))

    def test_browser_profile_is_one_argument_and_disables_shared_account_caching(self):
        import yt_dlp

        selection = self.accounts.validate(
            {"kind": "browser", "browser": "firefox", "profile": "Work Profile"}
        )
        self.accounts.prepare(self.root, selection)
        command = app.download_command(
            "https://youtu.be/fixture", self.root, "mp4", 1080
        )
        options = yt_dlp.parse_options(command[3:]).ydl_opts
        self.assertEqual(options["cookiesfrombrowser"][:2], ("firefox", "Work Profile"))
        self.assertFalse(options["cachedir"])
        self.assertFalse((self.root / COOKIE_NAME).exists())

    def test_cookie_import_keeps_session_cookies_and_isolates_job_jars(self):
        from yt_dlp.cookies import YoutubeDLCookieJar

        selection = self.accounts.import_cookies(COOKIES)
        self.assertEqual(self.accounts.import_cookies(COOKIES), selection)
        self.assertEqual(len(self.accounts.sessions), 1)
        self.assertNotIn(SECRET, json.dumps(selection))
        self.assertFalse(list(self.root.iterdir()))
        files = []
        for name in ("first", "second"):
            folder = self.root / name
            folder.mkdir()
            self.accounts.prepare(folder, selection)
            path = folder / COOKIE_NAME
            files.append(path)
            self.assertEqual(account_arguments(folder), ["--cookies", str(path)])
            self.assertNotIn(SECRET, (folder / CONFIG_NAME).read_text())
            jar = YoutubeDLCookieJar(str(path))
            jar.load()
            self.assertIn(SECRET, jar.get_cookie_header("http://127.0.0.1/video.mp4"))
            if os.name != "nt":
                self.assertEqual(stat.S_IMODE(path.stat().st_mode), 0o600)
        files[0].write_text("changed by a downloader")
        self.assertIn(SECRET, files[1].read_text())
        for path in files:
            cleanup_job_account(path.parent)
            self.assertFalse(path.exists())

    def test_invalid_cookie_input_never_echoes_its_contents(self):
        for data in (
            b"",
            json.dumps({"token": SECRET}).encode(),
            (
                "# Netscape HTTP Cookie File\n.twitch.tv\tTRUE\t/\tTRUE\twrong\tauth-token\t"
                + SECRET
            ).encode(),
            (
                "# Netscape HTTP Cookie File\n.twitch.tv\tTRUE\t/\tTRUE\t²\tauth-token\t"
                + SECRET
            ).encode(),
        ):
            with (
                self.subTest(data_type=len(data)),
                redirect_stderr(io.StringIO()) as errors,
            ):
                with self.assertRaises(AccountError) as error:
                    self.accounts.import_cookies(data)
                self.assertNotIn(SECRET, str(error.exception))
                self.assertEqual(errors.getvalue(), "")

    def test_expired_only_cookie_file_is_rejected(self):
        expired = COOKIES.replace(b"\t0\t", b"\t1\t")
        with self.assertRaises(AccountError):
            self.accounts.import_cookies(expired)

    def test_arbitrary_auth_fields_and_unknown_sessions_are_rejected(self):
        for value in (
            {"kind": "cookies", "path": "/etc/passwd"},
            {"kind": "cookies", "session": "0" * 32},
            {"kind": "browser", "browser": "--exec"},
            {"kind": "browser", "browser": []},
            {"kind": "browser", "browser": "firefox", "profile": "a\n--exec"},
            {"kind": "public", "token": SECRET},
        ):
            with self.subTest(value=value), self.assertRaises(AccountError):
                self.accounts.validate(value)

    def test_restart_removes_stale_cookie_jars_and_requires_reimport(self):
        folder = self.root / "jobs" / ("a" * 32)
        folder.mkdir(parents=True)
        selection = self.accounts.import_cookies(COOKIES)
        self.accounts.prepare(folder, selection)
        orphan = self.root / "jobs" / ("b" * 32)
        orphan.mkdir()
        self.accounts.prepare(orphan, selection)
        (folder / "result.txt").write_text("keep media")
        (self.root / "library.json").write_text(
            json.dumps(
                {
                    "assets": [],
                    "jobs": [
                        {
                            "id": "a" * 32,
                            "state": "running",
                            "spec": {
                                "kind": "download",
                                "url": "https://youtu.be/fixture",
                                "auth": selection,
                            },
                        }
                    ],
                }
            )
        )
        instance = app.Workshop(self.root)
        try:
            self.assertFalse((folder / COOKIE_NAME).exists())
            self.assertFalse((folder / CONFIG_NAME).exists())
            self.assertFalse((orphan / COOKIE_NAME).exists())
            self.assertTrue((folder / "result.txt").exists())
            with self.assertRaisesRegex(AccountError, "Import cookies.txt again"):
                instance.retry("a" * 32)
        finally:
            instance.close()


class AccountDownloads(Fixture, unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        root = Path(cls.fixture.name)
        cls.authorized = 0
        cls.denied = 0

        class Handler(SimpleHTTPRequestHandler):
            def __init__(self, *args, **kwargs):
                super().__init__(*args, directory=str(root), **kwargs)

            def log_message(self, *args):
                pass

            def do_GET(self):
                if f"subscription_fixture={SECRET}" not in self.headers.get(
                    "Cookie", ""
                ):
                    cls.denied += 1
                    self.send_error(403, "Subscription required")
                    return
                cls.authorized += 1
                super().do_GET()

            def do_HEAD(self):
                if f"subscription_fixture={SECRET}" not in self.headers.get(
                    "Cookie", ""
                ):
                    self.send_error(403, "Subscription required")
                    return
                super().do_HEAD()

            def handle(self):
                try:
                    super().handle()
                except (ConnectionResetError, BrokenPipeError):
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

    def commands(self):
        original = app.download_command
        url = f"http://127.0.0.1:{self.server.server_address[1]}/{self.source.name}"

        def command(*args):
            result = original(*args)
            result[-1] = url
            return result

        return (
            patch.object(app, "download_command", side_effect=command),
            patch.object(
                app,
                "streaming_mp3_command",
                side_effect=lambda ignored, out, folder: local_stream_command(
                    url, out, folder
                ),
            ),
        )

    def test_authorized_session_unlocks_both_formats_and_is_not_saved_in_history(self):
        selection = self.workshop.accounts.import_cookies(COOKIES)
        normal, stream = self.commands()
        with normal, stream:
            public = self.workshop.download(["https://youtu.be/fixture"], "mp4")
            self.assertEqual(completed(self.workshop, public)[0]["state"], "failed")
            ids = self.workshop.download(
                ["https://youtu.be/fixture"], "mp4", auth=selection
            )
            ids += self.workshop.download(
                ["https://youtu.be/fixture"], "mp3", auth=selection
            )
            assets = self.assert_success(ids)
        self.assertEqual({a["format"] for a in assets}, {"mp4", "mp3"})
        self.assertTrue(self.authorized)
        self.assertNotIn(SECRET, (self.workshop.directory / "library.json").read_text())
        self.assertNotIn(SECRET, json.dumps(self.workshop.snapshot()))
        self.assertFalse(list((self.workshop.directory / "jobs").glob("*/.account.*")))

    def test_retry_can_replace_a_missing_signin_with_an_imported_session(self):
        normal, stream = self.commands()
        with normal, stream:
            ids = self.workshop.download(["https://youtu.be/fixture"], "mp4")
            self.assertEqual(completed(self.workshop, ids)[0]["state"], "failed")
            selected = self.workshop.accounts.import_cookies(COOKIES)
            retry = self.workshop.retry(ids[0], selected)
            self.assert_success(retry)
        self.assertEqual(self.workshop.jobs[retry[0]]["spec"]["auth"], selected)

    def test_error_output_redacts_imported_cookie_values(self):
        selected = self.workshop.accounts.import_cookies(COOKIES)
        command = [sys.executable, "-c", f"raise SystemExit({SECRET!r})"]
        with patch.object(app, "download_command", return_value=command):
            ids = self.workshop.download(
                ["https://youtu.be/fixture"], "mp4", auth=selected
            )
            job = completed(self.workshop, ids)[0]
        self.assertEqual(job["state"], "failed")
        self.assertIn("[session]", job["error"])
        self.assertNotIn(SECRET, (self.workshop.directory / "library.json").read_text())
        self.assertFalse(list((self.workshop.directory / "jobs").glob("*/.account.*")))

    def test_cancel_removes_the_temporary_session_file(self):
        selected = self.workshop.accounts.import_cookies(COOKIES)
        command = [
            sys.executable,
            "-c",
            "import time; print('ready', flush=True); time.sleep(30)",
        ]
        with patch.object(app, "download_command", return_value=command):
            ids = self.workshop.download(
                ["https://youtu.be/fixture"], "mp4", auth=selected
            )
            until(lambda: ids[0] in self.workshop.processes)
            self.assertTrue(
                (self.workshop.directory / "jobs" / ids[0] / COOKIE_NAME).exists()
            )
            self.workshop.cancel(ids[0])
            self.assertEqual(completed(self.workshop, ids)[0]["state"], "cancelled")
        until(lambda: ids[0] not in self.workshop.cancels)
        self.assertFalse(list((self.workshop.directory / "jobs").glob("*/.account.*")))


class AccountHTTP(Fixture, unittest.TestCase):
    http = fixtures.HTTP.http

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

    def test_cookie_import_requires_token_and_same_origin(self):
        self.assertEqual(self.http("POST", "/api/account-cookies", COOKIES)[0], 403)
        headers = {
            "X-Workshop-Token": self.server.token,
            "Origin": "https://unrelated.invalid",
        }
        self.assertEqual(
            self.http("POST", "/api/account-cookies", COOKIES, headers)[0], 403
        )
        self.assertFalse(self.workshop.accounts.sessions)

    def test_cookie_import_returns_only_a_reference_and_is_not_downloadable(self):
        headers = {"X-Workshop-Token": self.server.token}
        status, _, body = self.http("POST", "/api/account-cookies", COOKIES, headers)
        self.assertEqual(status, 201, body)
        selected = json.loads(body)["auth"]
        self.assertEqual(self.workshop.accounts.validate(selected), selected)
        self.assertNotIn(SECRET.encode(), body)
        self.assertNotIn(SECRET.encode(), self.http("GET", "/api/state")[2])
        self.assertEqual(self.http("GET", "/.account.cookies")[0], 404)
        self.assertFalse(list(self.workshop.directory.rglob(COOKIE_NAME)))

    def test_cookie_import_rejects_oversize_and_malformed_files_without_echo(self):
        headers = {"X-Workshop-Token": self.server.token}
        status, _, body = self.http(
            "POST", "/api/account-cookies", SECRET.encode(), headers
        )
        self.assertEqual(status, 400)
        self.assertNotIn(SECRET.encode(), body)
        headers["Content-Length"] = str(4 * 1024 * 1024 + 1)
        self.assertEqual(
            self.http("POST", "/api/account-cookies", b"x", headers)[0], 400
        )
        self.assertFalse(self.workshop.accounts.sessions)
