#!/usr/bin/env python3
"""Optional real-browser regression review using generated media, without external sites."""

import argparse
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import threading
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
import app


def main():
    from playwright.sync_api import sync_playwright

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--browser", help="Optional installed Chromium executable")
    parser.add_argument("--output", type=Path, default=ROOT / "data/review")
    args = parser.parse_args()
    root = args.output.resolve()
    root.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="media-workshop-review-") as directory:
        directory = Path(directory)
        fixture = directory / "sample.mp4"
        second = directory / "Second source.mp4"
        subprocess.run(
            [
                app.executable("ffmpeg"),
                "-v",
                "error",
                "-f",
                "lavfi",
                "-i",
                "testsrc2=size=640x360:rate=24",
                "-f",
                "lavfi",
                "-i",
                "sine=frequency=660:sample_rate=44100",
                "-t",
                "20",
                "-c:v",
                "libx264",
                "-g",
                "48",
                "-keyint_min",
                "48",
                "-sc_threshold",
                "0",
                "-threads",
                "2",
                "-c:a",
                "aac",
                "-pix_fmt",
                "yuv420p",
                str(fixture),
            ],
            check=True,
        )
        shutil.copy2(fixture, second)
        workshop = app.Workshop(directory / "library")
        server = app.Server(workshop)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        base = f"http://127.0.0.1:{server.server_address[1]}"
        try:
            with sync_playwright() as p:
                browser = p.chromium.launch(
                    headless=True,
                    **({"executable_path": args.browser} if args.browser else {}),
                )
                page = browser.new_page(
                    viewport={"width": 1440, "height": 1100}, accept_downloads=True
                )
                errors = []
                page.on("pageerror", lambda error: errors.append(str(error)))
                page.goto(base, wait_until="networkidle")
                page.screenshot(path=str(root / "initial.png"), full_page=True)
                assert page.locator("#export").is_disabled()
                assert page.locator("#trim-mode").count() == 0
                page.locator("#urls").fill(
                    "https://youtu.be/first\nhttps://clips.twitch.tv/Second\nhttps://youtu.be/first"
                )
                assert page.locator("#link-count").inner_text() == "2 links"
                page.locator("#urls").fill("")
                # Exercise account controls with a synthetic local-only cookie.
                # Never open the machine's actual browser profiles.
                page.locator(".account-access summary").click()
                assert not page.locator("#account-profile").is_visible()
                page.locator("#account-source").select_option("firefox")
                page.locator("#account-profile").fill("Review Profile")
                page.reload(wait_until="networkidle")
                page.locator(".account-access summary").click()
                assert page.locator("#account-source").input_value() == "firefox"
                assert (
                    page.locator("#account-profile").input_value() == "Review Profile"
                )
                page.screenshot(path=str(root / "account-access.png"), full_page=True)
                page.locator("#account-source").select_option("cookies")
                fixture_secret = "synthetic-browser-review-cookie-not-real"
                page.locator("#account-file").set_input_files(
                    {
                        "name": "cookies.txt",
                        "mimeType": "text/plain",
                        "buffer": (
                            "# Netscape HTTP Cookie File\n127.0.0.1\tFALSE\t/\tFALSE\t0\tfixture\t"
                            + fixture_secret
                            + "\n"
                        ).encode(),
                    }
                )
                page.wait_for_function(
                    '() => document.querySelector("#account-file-status").textContent.startsWith("Sign-in ready")'
                )
                submitted = []

                def account_request(route):
                    submitted.append(route.request.post_data_json)
                    route.fulfill(status=202, json={"ids": []})

                page.route("**/api/download", account_request)
                page.locator("#urls").fill("https://youtu.be/account-fixture")
                page.locator('#download-form button[type="submit"]').click()
                page.wait_for_function(
                    '() => document.querySelector("#urls").value === ""'
                )
                assert submitted[0]["auth"]["kind"] == "cookies"
                assert len(submitted[0]["auth"]["session"]) == 32
                assert fixture_secret not in json.dumps(submitted)
                assert fixture_secret not in page.content()
                assert (
                    fixture_secret not in page.request.get(base + "/api/state").text()
                )
                remembered = page.evaluate("JSON.stringify(localStorage)")
                assert fixture_secret not in remembered
                assert submitted[0]["auth"]["session"] not in remembered
                page.unroute("**/api/download", account_request)
                page.set_viewport_size({"width": 390, "height": 844})
                assert page.evaluate(
                    "document.documentElement.scrollWidth <= window.innerWidth"
                )
                page.locator("#account-source").select_option("firefox")
                assert page.evaluate(
                    "document.documentElement.scrollWidth <= window.innerWidth"
                )
                page.set_viewport_size({"width": 1440, "height": 1100})
                page.locator("#account-source").select_option("public")
                page.locator(".account-access summary").click()
                page.locator("#files").set_input_files([str(fixture), str(second)])
                page.wait_for_function(
                    '() => document.querySelector("#library-count").textContent === "2"'
                )
                page.wait_for_function(
                    '() => document.querySelector("#player").readyState >= 1'
                )
                page.locator("#start").fill("00:01.250")
                page.locator("#end").fill("00:03.750")
                page.locator("#end").dispatch_event("input")
                assert page.locator("#cut-duration").inner_text() == "00:02.500"
                page.locator("#preview-cut").click()
                page.wait_for_timeout(400)
                assert (
                    1.2 <= page.locator("#player").evaluate("(v)=>v.currentTime") <= 3.8
                )
                page.locator("#player").evaluate("(v)=>v.pause()")
                page.locator("#export").click()

                def state():
                    return page.request.get(base + "/api/state").json()

                def finish(count):
                    for _ in range(150):
                        s = state()
                        if len(s["jobs"]) == count and all(
                            j["state"] == "complete" for j in s["jobs"]
                        ):
                            return s
                        if any(j["state"] == "failed" for j in s["jobs"]):
                            raise AssertionError(s["jobs"])
                        page.wait_for_timeout(150)
                    raise AssertionError("export timeout")

                s = finish(1)
                assert abs(s["assets"][-1]["duration"] - 2.5) < 0.1
                for a in s["assets"][:2]:
                    page.locator('[data-asset="' + a["id"] + '"]').check()
                page.locator("#batch-trim").check()
                page.locator("#export-format").select_option("mp3")
                page.locator("#export").click()
                s = finish(3)
                assert all(a["format"] == "mp3" for a in s["assets"][-2:])
                page.wait_for_function(
                    '() => document.querySelectorAll(".job .state.complete").length===3'
                )
                with page.expect_download() as saved:
                    page.locator(".job-actions a").first.click()
                saved.value.save_as(root / "saved.mp3")
                assert saved.value.suggested_filename.endswith(".mp3")
                assert app.probe(root / "saved.mp3")["audio"]
                # Rendered progress polling preserves the same action nodes (no focus/flicker resets).
                page.locator(".job-actions button").first.evaluate(
                    "(n)=>{window.testButton=n;return true}"
                )
                page.wait_for_timeout(1200)
                assert page.locator(".job-actions button").first.evaluate(
                    "(n)=>n===window.testButton"
                )
                page.locator("#batch-trim").uncheck()
                page.locator("#export-format").select_option("mp4")
                page.locator("#end").fill("00:18.250")
                page.locator("#export").click()
                s = finish(4)
                fast_job = s["jobs"][-1]
                assert "mode" not in fast_job["spec"]
                assert fast_job["method"] == "Accurate cut"
                assert fast_job["trim_start"] == fast_job["spec"]["start"]
                if app.importlib.util.find_spec("smartcut"):
                    assert fast_job["processing_backend"] == "smart", fast_job.get(
                        "fallback_reason"
                    )
                    assert fast_job["smart_copied_seconds"] >= 14
                assert abs(s["assets"][-1]["duration"] - 17) < 0.08
                page.locator(f'.job[data-id="{fast_job["id"]}"] button').first.click()
                page.wait_for_function(
                    '() => document.querySelector("#player").readyState >= 2'
                )
                page.locator("#player").evaluate("v => v.play()")
                page.wait_for_timeout(250)
                assert page.locator("#player").evaluate(
                    "v => v.currentTime > 0 && !v.error"
                )
                page.locator("#player").evaluate("v => v.pause()")
                page.wait_for_timeout(5000)
                page.screenshot(path=str(root / "desktop.png"), full_page=True)
                page.set_viewport_size({"width": 390, "height": 844})
                page.screenshot(path=str(root / "mobile.png"), full_page=True)
                assert page.evaluate(
                    "document.documentElement.scrollWidth <= window.innerWidth"
                )

                # A real worker failure should remain inspectable/copyable after
                # accurate fallback succeeds, without collapsing during polling.
                def broken_boundary(*args):
                    return [
                        sys.executable,
                        "-c",
                        "print('WORKSHOP:{\"progress\":1}',flush=True);raise SystemExit('Fixture: boundary encoder unavailable')",
                    ], "smart"

                with patch.object(app, "trim_plan", side_effect=broken_boundary):
                    page.locator("#source").select_option(s["assets"][0]["id"])
                    page.locator("#start").fill("1.25")
                    page.locator("#end").fill("3.75")
                    page.locator("#export").click()
                    s = finish(5)
                failed_boundary = s["jobs"][-1]
                assert failed_boundary["processing_backend"] == "encoder"
                assert failed_boundary["fallback_info"]["video_codec"] == "h264"
                details = page.locator(
                    f'.job[data-id="{failed_boundary["id"]}"] details'
                )
                details.locator("summary").click()
                assert (
                    details.locator("pre")
                    .inner_text()
                    .startswith("Fixture: boundary encoder unavailable")
                )
                assert "Source: h264" in details.locator("pre").inner_text()
                assert "WORKSHOP:" not in details.locator("pre").inner_text()
                assert (
                    "Selected range: 1.25–3.75 s" in details.locator("pre").inner_text()
                )
                assert details.bounding_box()["width"] > 200
                details.evaluate("n => window.testDetails=n")
                page.wait_for_timeout(1200)
                assert details.evaluate("n => n===window.testDetails && n.open")
                page.context.grant_permissions(
                    ["clipboard-read", "clipboard-write"], origin=base
                )
                details.get_by_role("button", name="Copy details").click()
                page.get_by_text("Processing details copied.", exact=True).wait_for()
                assert (
                    page.evaluate("navigator.clipboard.readText()")
                    == details.locator("pre").inner_text()
                )
                assert page.evaluate(
                    "document.documentElement.scrollWidth <= window.innerWidth"
                )
                page.screenshot(path=str(root / "fallback-details.png"), full_page=True)
                assert not errors, errors
                (root / "browser-results.json").write_text(
                    json.dumps(
                        {
                            "status": "passed",
                            "browser": "system Chromium / Playwright",
                            "checks": [
                                "empty state",
                                "account browser/profile selection and persistence without reading real profiles",
                                "synthetic cookie import and opaque download selection without credential persistence",
                                "responsive account controls",
                                "batch link count and deduplication",
                                "multi-file import",
                                "native video preview and selected-range playback",
                                "real MP4 export and duration",
                                "two-file MP3 batch export",
                                "browser Save file download",
                                "stable queue action DOM during polling",
                                "mobile layout without horizontal overflow",
                                "one accurate workflow without a cut-mode selector",
                                "accurate boundary-only video cut and native playback",
                                "copyable fallback reason/source diagnostics with stable expanded details",
                            ],
                            "jobs": len(s["jobs"]),
                            "assets": len(s["assets"]),
                            "consoleErrors": errors,
                        },
                        indent=2,
                    )
                )
                browser.close()
                print(
                    "PASS: browser import, preview, trim, batch, save, stable polling and responsive layout"
                )
        finally:
            server.shutdown()
            server.server_close()
            thread.join()
            workshop.close()


if __name__ == "__main__":
    main()
