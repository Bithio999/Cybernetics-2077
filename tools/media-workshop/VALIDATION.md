# Media Workshop 1.1 — validation

Checked 24 November 2026 using Python 3.12.14, yt-dlp 2026.11.23, FFmpeg/ffprobe 7.1.5, Node.js 24.19.0 and Chromium 154 on Linux.

## Automated checks

**27 tests pass**, using actual FFmpeg processes and real temporary files:

- MP4 and MP3 encoding, fractional start/end cuts and measured output durations.
- Two-file batch exports, unique destinations and unchanged source-file hashes.
- Rejection of invalid ranges, incompatible audio-only exports and malformed batches before queuing.
- Actual yt-dlp subprocess downloads from a local HTTP media fixture, followed by both output formats. The fixture deliberately uses the generic extractor; production URLs remain limited to individual YouTube/Twitch videos.
- Cancellation of a running child process and a waiting job; failed-job retry.
- Saved-library reopening, interrupted-job recovery and exclusive library locking.
- Real HTTP imports, playable-file downloads, deletion, byte-range seeking and invalid ranges.
- Local host/token checks, URL validation, filename handling and prevention of playlist/channel expansion.

Reproduce with `python -m unittest discover -s tests -v` after installing `requirements.txt`.

## Download optimization

Eight additional real-media tests verify byte-identical direct saves for ready MP4 and MP3 files, video/audio packet hashes through remuxing, preservation of the compatible track during selective conversion, full fallback conversion, and enforcing the requested resolution ceiling. An offline test uses yt-dlp's actual format selector to verify that H.264/AAC is preferred at the same resolution while a requested 4K source remains 4K.

A generated **15-second, 1280×720 / 30 fps H.264 + AAC MKV** was processed three times with each path. The former full encode took **4.19 seconds median**; the new stream-copy path took **0.212 seconds median**, approximately **19.7× faster**. These timings include input/output probes and exclude fixture generation and network transfer. They describe this local Linux CPU/filesystem fixture, not a universal speedup or network-throughput improvement. Raw measurements are in [the benchmark record](tests/results/speed-1.1.0.json).

Ready fast-start MP4s use a same-filesystem rename instead of either processing path. Necessary video conversions use `veryfast` at CRF 20 and a CPU-thread budget of at most eight per job, divided against the configured worker count. That preset can produce larger files than the previous `fast` preset. Exact trims retain the existing accurate encoding path. Existing libraries need no migration; older records without codec/method fields remain usable.

## Browser review

The included `tests/browser_review.py` passes in real headless Chromium through Playwright. It imports two generated videos, plays the selected range, exports a 2.5-second MP4, batch-exports two MP3 files and saves a playable MP3 through the browser's download flow. It checks link counting/deduplication, stable queue action elements across progress polling and a 390px layout without horizontal overflow. No page JavaScript errors were observed.

The screenshot in the README is from that running app, with generated FFmpeg test media. Playwright is only a development/test dependency.

## External-site checks and limits

Earlier public-site download attempts were made separately. The cloud network denied YouTube with **proxy CONNECT 403**. Twitch failed **TLS certificate verification** through this environment. Consequently, successful current YouTube/Twitch downloads could not be verified here. The successful local-fixture tests establish the real download/conversion pipeline; they do not establish external access or simulate successful YouTube/Twitch extraction.

The program delegates site extraction to the current yt-dlp release. Its updater is provided because those sites change. Windows and macOS launch paths and setup instructions are included; native execution was tested on Linux, not on those operating systems.

The ZIP contains source, launchers, tests, documentation and UI assets. It does not bundle Python, FFmpeg, browser binaries, downloaded videos, credentials or a virtual environment. The Minecraft mod and its current release are unchanged.

The ZIP was extracted into a separate directory. Its launcher successfully created a fresh virtual environment and installed its requirements. A real two-file CLI batch through that extracted launcher produced both MP3 cuts successfully. Archive contents were checked against the source and screened for virtual environments, caches and downloaded media.
