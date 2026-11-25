"""Explicit, local account selection; imported cookies stay out of saved history."""

import io
import json
import os
import re
import secrets
import threading
import warnings

BROWSERS = {
    "firefox",
    "chrome",
    "edge",
    "brave",
    "chromium",
    "opera",
    "vivaldi",
    "safari",
    "whale",
}
MAX_COOKIE_BYTES = 4 * 1024 * 1024
CONFIG_NAME = ".account.json"
COOKIE_NAME = ".account.cookies"


class AccountError(ValueError):
    pass


def browser_selection(browser, profile=""):
    if (
        not isinstance(browser, str)
        or browser not in BROWSERS
        or not isinstance(profile, str)
        or len(profile) > 500
    ):
        raise AccountError("Choose a supported browser and profile.")
    profile = profile.strip()
    if any(c in profile for c in ("\0", "\r", "\n")) or "::" in profile:
        raise AccountError(
            "Enter a browser profile name or path, without container/keyring options."
        )
    return {"kind": "browser", "browser": browser, "profile": profile}


def private_write(path, data):
    descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(descriptor, "wb") as file:
        file.write(data)
    os.chmod(path, 0o600)


def cleanup_job_account(folder):
    for name in (CONFIG_NAME, COOKIE_NAME):
        try:
            (folder / name).unlink(missing_ok=True)
        except OSError:
            pass


def account_arguments(folder):
    path = folder / CONFIG_NAME
    if not path.is_file():
        return []
    try:
        config = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(config, dict):
            raise ValueError()
    except (OSError, ValueError):
        raise AccountError(
            "The account session could not be read. Choose Account access again."
        ) from None
    if config.get("kind") == "browser":
        browser = browser_selection(config.get("browser"), config.get("profile", ""))
        value = browser["browser"] + (
            ":" + browser["profile"] if browser["profile"] else ""
        )
        return ["--cookies-from-browser", value]
    if config.get("kind") == "cookies" and (folder / COOKIE_NAME).is_file():
        return ["--cookies", str(folder / COOKIE_NAME)]
    raise AccountError(
        "The imported sign-in is unavailable. Import cookies.txt again, then Retry."
    )


class AccountSessions:
    def __init__(self):
        self.sessions = {}
        self.secret_values = set()
        self.lock = threading.RLock()

    def import_cookies(self, data):
        if not isinstance(data, bytes) or not 0 < len(data) <= MAX_COOKIE_BYTES:
            raise AccountError("Choose a cookies.txt file no larger than 4 MB.")
        message = "Use a Netscape-format cookies.txt file exported from your signed-in browser."
        try:
            from yt_dlp.cookies import YoutubeDLCookieJar

            text = data.decode("utf-8-sig").replace("\r\n", "\n")
            if any(
                ord(character) < 32 and character not in "\n\t" for character in text
            ):
                raise ValueError()
            if not text.startswith(
                ("# Netscape HTTP Cookie File", "# HTTP Cookie File")
            ):
                raise ValueError()
            # Validate before yt-dlp's loader, which otherwise logs rejected lines.
            for line in text.splitlines():
                if line.startswith("#HttpOnly_"):
                    line = line[len("#HttpOnly_") :]
                elif not line.strip() or line.startswith("#"):
                    continue
                fields = line.split("\t")
                if (
                    len(fields) != 7
                    or not fields[0]
                    or not fields[2].startswith("/")
                    or fields[1] not in {"TRUE", "FALSE"}
                    or fields[3] not in {"TRUE", "FALSE"}
                    or not re.fullmatch(r"[0-9]*", fields[4])
                    or any(c in fields[0] for c in " \r\n\0")
                ):
                    raise ValueError()
            jar = YoutubeDLCookieJar(io.StringIO(text))
            with warnings.catch_warnings():
                warnings.simplefilter("ignore")
                jar.load()
            jar.clear_expired_cookies()
            if not len(jar):
                raise ValueError()
            values = {
                cookie.value
                for cookie in jar
                if cookie.value and len(cookie.value) >= 8
            }
            canonical = io.StringIO()
            jar.save(canonical)
            raw = canonical.getvalue().encode("utf-8")
        except Exception:
            raise AccountError(message) from None
        with self.lock:
            if len(self.sessions) >= 8:
                raise AccountError(
                    "Finish your downloads and restart the app before importing more sign-ins."
                )
            session = secrets.token_hex(16)
            self.sessions[session] = raw
            self.secret_values.update(values)
            return {"kind": "cookies", "session": session}

    def validate(self, config):
        if config is None or config == {"kind": "public"}:
            return None
        if not isinstance(config, dict):
            raise AccountError("Choose an account access method.")
        if config.get("kind") == "browser" and set(config) <= {
            "kind",
            "browser",
            "profile",
        }:
            return browser_selection(config.get("browser"), config.get("profile", ""))
        if config.get("kind") == "cookies" and set(config) == {"kind", "session"}:
            session = config.get("session")
            with self.lock:
                if (
                    isinstance(session, str)
                    and re.fullmatch(r"[a-f0-9]{32}", session)
                    and session in self.sessions
                ):
                    return {"kind": "cookies", "session": session}
            raise AccountError(
                "The imported sign-in is no longer available. Import cookies.txt again, then Retry."
            )
        raise AccountError("Choose a supported account access method.")

    def prepare(self, folder, config):
        selection = self.validate(config)
        if selection is None:
            return
        if selection["kind"] == "cookies":
            with self.lock:
                private_write(folder / COOKIE_NAME, self.sessions[selection["session"]])
            selection = {"kind": "cookies"}
        private_write(folder / CONFIG_NAME, json.dumps(selection).encode("utf-8"))

    def redact(self, value):
        with self.lock:
            for secret in sorted(self.secret_values, key=len, reverse=True):
                value = value.replace(secret, "[session]")
                value = value.replace(json.dumps(secret)[1:-1], "[session]")
        return value

    def close(self):
        with self.lock:
            self.sessions.clear()
            self.secret_values.clear()
