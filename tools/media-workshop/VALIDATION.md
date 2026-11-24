# Media Workshop 1.3 — validation

Checked 24 November 2026 using Python 3.12.14, yt-dlp 2026.11.23, smartcut 1.7, PyAV 16.0.1, FFmpeg/ffprobe 7.1.5, Node.js 24.19.0 and Chromium 154 on Linux.

## Automated checks

**43 tests pass**, using actual FFmpeg processes and temporary media:

- Accurate MP4/MP3 cuts, fractional timing, decoded audio sample counts, batches and unchanged original files.
- Boundary-only H.264 processing: matching source packet hashes through the copied middle, expected frame counts, first-frame comparison, full decoding without errors, nonzero timestamps and cutting an already-edited result again.
- Automatic accurate fallback after boundary processing fails; whole-source exports preserve their originals; old cut-mode settings cannot change timing.
- Actual yt-dlp subprocess downloads over local HTTP. MP3 conversion starts before download completion, retains every decoded sample, and produces 192 kbps audio. Native MP3 downloads remain byte-identical.
- A real nonstreamable M4A requests a seekable-file fallback; a separate queue test verifies that fallback completes. Failed downloads cannot publish apparently playable partial output.
- Cancellation kills the streaming worker/process group and removes partial files. Queue retry, interrupted-job recovery, persistent libraries and exclusive library locking remain covered.
- HTTP imports/downloads, seeking, deletion, URL/host/token checks, invalid ranges, filenames and prevention of playlist expansion.
- Existing MP4 direct-save, stream-copy and selective-conversion tests, including compatible-track packet preservation and the requested resolution ceiling.

Reproduce with `python -m unittest discover -s tests -v` after installing `requirements.txt`. Ruff undefined/unused-name checks and JavaScript syntax checks also pass.

## One accurate cut workflow

The interface has one **Export cut** workflow. Longer 8-bit H.264 clips use smartcut to encode cut boundaries while copying complete middle sections. A video-only temporary window avoids indexing the entire source or retaining its compressed audio in memory. The adapter checks frame count and duration before accepting the video. Audio is cut accurately with FFmpeg. Short clips, other codecs and processing failures use the accurate encoder automatically. No path deliberately shifts the start to a preceding keyframe.

The optional smart backend is pinned to the tested smartcut 1.7 API and installed on Python 3.11–3.14. Its isolated worker adapts encoder thread/preset settings without editing installed dependency source. Newer Python versions retain accurate FFmpeg encoding. Updated requirements are installed automatically when the launcher sees their fingerprint change.

On a generated 60-second **1280×720 / 30 fps H.264 + AAC MP4** with two-second keyframes, the same requested **3.25–55.25-second** cut took **6.89 seconds median** with full accurate encoding and **3.25 seconds median** with automatic boundary processing, approximately **2.1× faster**. The latter copied 50 seconds of video. Durations were 52.000 and 52.016 seconds, within one source frame of the requested range. Three sequential runs include probes and worker launch, and exclude fixture generation/import. This is a local fixture measurement, not a universal speed guarantee. [Raw results](tests/results/unified-1.3.0.json).

Earlier download/remux measurements remain in [speed-1.1.0.json](tests/results/speed-1.1.0.json). The older [trim-1.2.0.json](tests/results/trim-1.2.0.json) describes the former two-mode workflow; its preceding-keyframe Fast cut was not timing-equivalent. Those modes are no longer presented or used.

## MP3 download conversion

yt-dlp's audio stream now feeds FFmpeg directly through a bounded pipe. Download and conversion overlap instead of running as two complete sequential stages, and no full intermediate source is needed on the streaming path. Both processes must succeed before the file enters the library. Nonstreamable source containers automatically use the existing seekable-file path. Cancellation covers the downloader, encoder and worker together.

MP3 remains **192 kbps CBR** using the existing LAME quality settings; native MP3 is copied without encoding. FFmpeg reports that libmp3lame has no threading capability: assigning it extra threads does not make one conversion faster. The optimization overlaps work and removes intermediate file I/O, rather than lowering audio quality or joining separately encoded chunks.

For a generated 90-second stereo AAC source over throttled local HTTP, three sequential runs took **5.55 seconds median** for download-then-encode and **4.54 seconds** with overlapping conversion, about **18% less elapsed time**. Both used the same 192 kbps output settings. The fixture sends 4,096 bytes every 8 ms; timings include real yt-dlp extraction, transfer, encoding and probing. They do not establish a public-site throughput improvement. [Raw results and assumptions](tests/results/unified-1.3.0.json).

## Browser and package review

`tests/browser_review.py` passes in real headless Chromium through Playwright. It imports two generated videos, previews a range, exports a 2.5-second MP4, batch-exports two MP3s, saves a playable MP3 and plays a longer accurately cut video. It verifies there is no cut-mode selector, stable queue action elements during progress polling, link deduplication and a 390px layout without horizontal overflow. No page JavaScript errors occurred. The README image is from that running app; Playwright is only a test dependency.

The release ZIP contains source, launchers, tests, documentation and UI assets. It excludes Python, FFmpeg, browsers, virtual environments, downloaded media and credentials. Its contents were checked against source and extracted into a separate directory. The extracted launcher installed its dependencies into a fresh environment and completed a real CLI cut. The Minecraft mod/release is unchanged.

## External-site and platform limits

Earlier public-site attempts were blocked separately: YouTube returned **proxy CONNECT 403**, and Twitch failed TLS certificate verification through the cloud network. Current successful public YouTube/Twitch extraction is therefore **not established** by these tests. The local fixtures exercise real yt-dlp/FFmpeg processes using the generic extractor; they do not simulate successful public-site access.

The updater installs current yt-dlp components because sites change. Windows and macOS launch paths are included, but native execution was tested on Linux. Boundary processing depends on source codec/keyframe layout; audio encoding still takes CPU time even when it overlaps the download.
