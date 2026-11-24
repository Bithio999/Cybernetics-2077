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
                assert not errors, errors
                (root / "browser-results.json").write_text(
                    json.dumps(
                        {
                            "status": "passed",
                            "browser": "system Chromium / Playwright",
                            "checks": [
                                "empty state",
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
