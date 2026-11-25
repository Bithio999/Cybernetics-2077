#!/usr/bin/env python3
"""GX connector review with synthetic cookies; --fixture simulates the browser API."""

import argparse
import json
from pathlib import Path
import sys
import tempfile
import threading

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
import app


def main():
    from playwright.sync_api import sync_playwright

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--browser", default="/usr/bin/chromium")
    parser.add_argument("--output", type=Path, default=ROOT / "data/gx-review")
    parser.add_argument(
        "--fixture",
        action="store_true",
        help="Test content script in an isolated world with a synthetic runtime API when extension loading is blocked; not a native-extension test.",
    )
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="media-workshop-gx-review-") as directory:
        root = Path(directory)
        workshop = app.Workshop(root / "library")
        server = app.Server(workshop)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        base = f"http://127.0.0.1:{server.server_address[1]}"
        secret = "gx-review-synthetic-not-an-account-token"
        try:
            with sync_playwright() as p:
                extension = ROOT / "opera-gx-connector"
                context = p.chromium.launch_persistent_context(
                    str(root / "fresh-browser"),
                    headless=True,
                    executable_path=args.browser,
                    ignore_default_args=["--disable-extensions"],
                    args=[]
                    if args.fixture
                    else [
                        f"--disable-extensions-except={extension}",
                        f"--load-extension={extension}",
                    ],
                    viewport={"width": 1440, "height": 1100},
                )
                context.route(
                    "**/*",
                    lambda route: (
                        route.continue_()
                        if route.request.url.startswith((base, "chrome-extension://"))
                        else route.abort()
                    ),
                )
                context.add_cookies(
                    [
                        {
                            "name": "auth-token",
                            "value": secret,
                            "domain": ".twitch.tv",
                            "path": "/",
                            "secure": True,
                            "httpOnly": True,
                        },
                        {
                            "name": "fixture",
                            "value": "youtube-" + secret,
                            "domain": ".youtube.com",
                            "path": "/",
                            "secure": True,
                        },
                        {
                            "name": "unrelated",
                            "value": "never-read-this-cookie",
                            "domain": ".example.com",
                            "path": "/",
                            "secure": True,
                        },
                    ]
                )
                page = context.new_page()
                client = context.new_cdp_session(page)
                client.send("Page.enable")
                if args.fixture:
                    fake_cookies = [
                        {
                            "name": "auth-token",
                            "value": secret,
                            "domain": ".twitch.tv",
                            "path": "/",
                            "secure": True,
                            "httpOnly": True,
                            "session": True,
                        },
                        {
                            "name": "fixture",
                            "value": "youtube-" + secret,
                            "domain": ".youtube.com",
                            "path": "/",
                            "secure": True,
                            "session": True,
                        },
                        {
                            "name": "unrelated",
                            "value": "never-read-this-cookie",
                            "domain": ".example.com",
                            "path": "/",
                            "session": True,
                        },
                    ]
                    source = (
                        "document.addEventListener('DOMContentLoaded', () => {\n"
                        + (extension / "cookie-format.js").read_text()
                        + "\nglobalThis.__gxFixtureCookies = "
                        + json.dumps(fake_cookies)
                        + ";\nconst chrome = {runtime: {sendMessage: async () => ({cookies: workshopCookieFile(globalThis.__gxFixtureCookies)})}};\n"
                        + (extension / "content.js").read_text()
                        + "\n}, {once:true});"
                    )
                    client.send(
                        "Page.addScriptToEvaluateOnNewDocument",
                        {"source": source, "worldName": "gx-connector-fixture"},
                    )
                errors = []
                page.on("pageerror", lambda e: errors.append(str(e)))
                page.goto(base, wait_until="networkidle")
                page.wait_for_function(
                    '() => document.documentElement.dataset.workshopGxConnector === "1"'
                )
                page.locator(".account-access summary").click()
                page.locator("#account-source").select_option("opera-gx")
                assert page.locator("#gx-method").input_value() == "connector"
                assert not page.locator("#account-profile").is_visible()
                # Synthetic page clicks must not read or import browser cookies.
                page.locator("#gx-connect").evaluate("button => button.click()")
                page.wait_for_timeout(350)
                assert not workshop.accounts.sessions
                page.reload(wait_until="networkidle")
                page.locator(".account-access summary").click()
                page.locator("#gx-connect").click()
                page.wait_for_function(
                    '() => document.querySelector("#gx-status").textContent.startsWith("Opera GX connected")',
                    timeout=15000,
                )
                assert len(workshop.accounts.sessions) == 1
                raw = next(iter(workshop.accounts.sessions.values()))
                assert secret.encode() in raw
                assert b"never-read-this-cookie" not in raw
                assert not list(workshop.directory.rglob(".account.cookies"))
                assert secret not in page.content()
                assert secret not in page.request.get(base + "/api/state").text()
                assert secret not in page.evaluate("JSON.stringify(localStorage)")
                assert secret not in (workshop.directory / "library.json").read_text()
                captured = []

                def download(route):
                    captured.append(route.request.post_data_json)
                    route.fulfill(status=202, json={"ids": []})

                page.route("**/api/download", download)
                page.locator("#urls").fill("https://clips.twitch.tv/GxFixture")
                page.locator("#add-batch").click()
                page.wait_for_function(
                    '() => document.querySelector("#urls").value === ""'
                )
                auth = captured[0]["auth"]
                assert workshop.accounts.validate(auth) == auth
                assert secret not in json.dumps(captured)
                job = root / "isolated-job"
                job.mkdir()
                workshop.accounts.prepare(job, auth)
                from yt_dlp.cookies import YoutubeDLCookieJar

                jar = YoutubeDLCookieJar(str(job / ".account.cookies"))
                jar.load()
                assert secret in jar.get_cookie_header("https://gql.twitch.tv/")
                assert secret not in (
                    jar.get_cookie_header("https://example.com/") or ""
                )
                app.cleanup_job_account(job)
                page.locator("#message").evaluate("node => node.hidden = true")
                page.screenshot(path=str(args.output / "opera-gx.png"), full_page=True)
                page.set_viewport_size({"width": 390, "height": 844})
                assert page.evaluate(
                    "document.documentElement.scrollWidth <= innerWidth"
                )
                page.locator("#gx-method").select_option("profile")
                assert page.locator("#account-profile").is_visible()
                assert page.evaluate(
                    "document.documentElement.scrollWidth <= innerWidth"
                )
                page.reload(wait_until="networkidle")
                page.locator(".account-access summary").click()
                assert page.locator("#gx-method").input_value() == "profile"
                page.locator("#gx-method").select_option("connector")
                assert secret not in page.content()
                # Reconnection refreshes the session from the live browser, not a saved export.
                context.clear_cookies()
                context.add_cookies(
                    [
                        {
                            "name": "auth-token",
                            "value": "renewed-" + secret,
                            "domain": ".twitch.tv",
                            "path": "/",
                            "secure": True,
                        }
                    ]
                )
                if args.fixture:
                    frame_id = client.send("Page.getFrameTree")["frameTree"]["frame"][
                        "id"
                    ]
                    world = client.send(
                        "Page.createIsolatedWorld",
                        {"frameId": frame_id, "worldName": "gx-connector-fixture"},
                    )
                    client.send(
                        "Runtime.evaluate",
                        {
                            "contextId": world["executionContextId"],
                            "expression": "globalThis.__gxFixtureCookies[0].value = "
                            + json.dumps("renewed-" + secret),
                        },
                    )
                page.locator("#gx-connect").click()
                page.wait_for_function(
                    '() => document.querySelector("#gx-status").textContent.startsWith("Opera GX connected")'
                )
                assert any(
                    ("renewed-" + secret).encode() in raw
                    for raw in workshop.accounts.sessions.values()
                )
                assert not errors, errors
                context.close()
                (args.output / "results.json").write_text(
                    json.dumps(
                        {
                            "status": "passed",
                            "browser": "fresh Chromium; isolated content script with simulated runtime/cookies API"
                            if args.fixture
                            else "fresh Chromium with real MV3 extension; not native Opera GX",
                            "checks": [
                                "real protected loopback cookie import; simulated browser API"
                                if args.fixture
                                else "real cookies API/message passing/loopback import",
                                "requires trusted click",
                                "site-scoped cookie permissions",
                                "opaque queue references",
                                "private jar preserves Twitch authentication",
                                "no cookie values in DOM/state/history/localStorage",
                                "responsive connection/direct-profile UI",
                                "reconnect reads changed session",
                            ],
                            "pageErrors": errors,
                        },
                        indent=2,
                    )
                )
                print(
                    "PASS: GX connector review ("
                    + (
                        "simulated browser API"
                        if args.fixture
                        else "real MV3 extension"
                    )
                    + "), protected local import and responsive UI"
                )
        finally:
            server.shutdown()
            server.server_close()
            thread.join()
            workshop.close()


if __name__ == "__main__":
    main()
