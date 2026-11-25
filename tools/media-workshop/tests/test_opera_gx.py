"""GX profile and locked-database guidance; never inspect an installed browser."""

import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

from test_workshop import app
from download_auth import (
    AccountError,
    AccountSessions,
    account_arguments,
    account_error,
    opera_gx_profile,
)


class OperaGX(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.sessions = AccountSessions()

    def tearDown(self):
        self.sessions.close()
        self.temp.cleanup()

    def test_windows_gx_profile_is_separate_from_opera(self):
        profile = self.root / "Opera Software/Opera GX Stable"
        profile.mkdir(parents=True)
        (self.root / "Opera Software/Opera Stable").mkdir()
        job = self.root / "job"
        job.mkdir()
        self.sessions.prepare(
            job, {"kind": "browser", "browser": "opera-gx", "profile": ""}
        )
        with (
            patch.dict(os.environ, {"APPDATA": str(self.root)}),
            patch.object(sys, "platform", "win32"),
        ):
            flags = account_arguments(job)
        self.assertEqual(flags, ["--cookies-from-browser", "opera:" + str(profile)])
        import yt_dlp

        parsed = yt_dlp.parse_options(flags + ["https://youtu.be/fixture"]).ydl_opts
        self.assertEqual(parsed["cookiesfrombrowser"][:2], ("opera", str(profile)))

    def test_macos_gx_profile(self):
        profile = self.root / "Library/Application Support/com.operasoftware.OperaGX"
        profile.mkdir(parents=True)
        with (
            patch.object(sys, "platform", "darwin"),
            patch("download_auth.Path.home", return_value=self.root),
        ):
            self.assertEqual(opera_gx_profile(), str(profile))

    def test_manual_full_profile_path_can_override_auto_detection(self):
        profile = self.root / "My GX profile"
        profile.mkdir()
        self.assertEqual(opera_gx_profile('"' + str(profile) + '"'), str(profile))
        with self.assertRaisesRegex(AccountError, "full Profile path"):
            opera_gx_profile("Profile 1")

    def test_missing_gx_never_falls_back_to_regular_opera(self):
        (self.root / "Opera Software/Opera Stable").mkdir(parents=True)
        with (
            patch.dict(os.environ, {"APPDATA": str(self.root)}),
            patch.object(sys, "platform", "win32"),
        ):
            with self.assertRaisesRegex(AccountError, "GX's profile was not found"):
                opera_gx_profile()

    def test_unavailable_platform_recommends_live_connector(self):
        with patch.object(sys, "platform", "linux"):
            with self.assertRaisesRegex(AccountError, "live connector"):
                opera_gx_profile()

    def test_reported_lock_and_decryption_failures_have_gx_specific_help(self):
        config = {"kind": "browser", "browser": "opera-gx"}
        for message in [
            "ERROR: Could not copy Chrome cookie database",
            "ERROR: failed to decrypt with DPAPI",
        ]:
            result = account_error(message, config)
            self.assertIn("Live connector", result)
            self.assertIn(message, result)
            self.assertEqual(account_error(message, None), message)
        self.assertEqual(account_error("HTTP Error 404", config), "HTTP Error 404")

    def test_gx_option_is_available_to_cli_and_frontend(self):
        parsed = self.sessions.validate({"kind": "browser", "browser": "opera-gx"})
        self.assertEqual(parsed["profile"], "")
        html = (app.ROOT / "static/index.html").read_text()
        self.assertIn('value="opera-gx"', html)
        manifest = json.loads(
            (app.ROOT / "opera-gx-connector/manifest.json").read_text()
        )
        self.assertNotIn("storage", manifest["permissions"])

    @unittest.skipUnless(
        shutil.which("node"), "Node.js is needed for extension unit checks"
    )
    def test_extension_cookie_conversion_and_sender_guards(self):
        subprocess.run(
            ["node", str(Path(__file__).with_name("test_gx_connector.js"))],
            check=True,
            capture_output=True,
            text=True,
        )
