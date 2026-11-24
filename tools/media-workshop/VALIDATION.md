# Media Workshop 1.4.1 — validation

Checked 24 November 2026 using Python 3.12.14, yt-dlp 2026.11.23, smartcut 1.7, PyAV 16.0.1, FFmpeg/ffprobe 7.1.5, Node.js 24.19.0 and Chromium 154 on Linux.

## Automated checks

**53 tests pass**, using actual FFmpeg processes and temporary media:

- Accurate MP4/MP3 cuts, fractional timing, decoded audio sample counts, batches and unchanged original files.
- Direct bounded H.264 indexing and boundary-only processing: matching source packet hashes through the copied middle, expected frame counts, first-frame comparison, full decoding without errors, nonzero timestamps and cutting an already-edited result again.
- AAC packet preservation and decoded-sample agreement at fractional starts for 44.1/48 kHz noise, preservation of a delayed audio track, Opus-to-AAC conversion, silent late-source cuts, six-second smart cuts, end-of-source frame preservation, and cancellation of parallel audio work.
- Correct video/audio track ends, lossless repair of duration-summary errors, rejection of actually shifted timestamps, and automatic accurate fallback after boundary processing fails; whole-source exports preserve their originals; old cut-mode settings cannot change timing.
- Actual yt-dlp subprocess downloads over local HTTP. MP3 conversion starts before download completion, retains every decoded sample, and produces 192 kbps audio. Native MP3 downloads remain byte-identical.
- A real nonstreamable M4A requests a seekable-file fallback; a separate queue test verifies that fallback completes. Failed downloads cannot publish apparently playable partial output.
- Cancellation kills the streaming worker/process group and removes partial files. Queue retry, interrupted-job recovery, persistent libraries and exclusive library locking remain covered.
- HTTP imports/downloads, seeking, deletion, URL/host/token checks, invalid ranges, filenames and prevention of playlist expansion.
- Existing MP4 direct-save, stream-copy and selective-conversion tests, including compatible-track packet preservation and the requested resolution ceiling.

Reproduce with `python -m unittest discover -s tests -v` after installing `requirements.txt`. Ruff undefined/unused-name checks and JavaScript syntax checks also pass.

## One accurate cut workflow

Version **1.4.1 fixes track-duration validation**. Video is checked against the selected video packets' own end, separately from the audio/container end. A source's audio can finish tens of milliseconds after its last video frame; this previously rejected a correctly copied video when cutting to the file's end. The audio tail is retained instead of requiring nonexistent additional video frames.

A synthetic 80×48 source reproduces the reported **185,628 frames at 60000/1001 fps** and the exact **1314.974–3096.961-second** selection, with audio extending past the video. The old 1.4 worker fails with the reported duration error; 1.4.1 succeeds, retains 106,808 video frames, copies 1,777.776 seconds of interior video and copies AAC audio. Full output decoding reports no errors. The synthetic output's 1,782.007-second duration includes about 20 ms of compressed AAC tail. The user's actual footage was not downloaded; this test reproduces the timing/track-end condition, not its pixels or HD processing speed. [Reproduction record](tests/results/duration-fallback-1.4.1.json).

Two additional regressions verify that misleading duration metadata can be repaired through stream-copy muxing when the actual first/last presentation timestamps are correct, and that a genuine two-second timestamp shift is rejected. Frame-count checks remain strict. Failed duration checks now report measured/expected timeline values.

The queue also exposes the saved boundary error, selected range, source/dependency context and a copy button. Old saved fallback reasons remain inspectable. A real failed subprocess followed by successful accurate encoding is covered by the browser review: the explanation survives polling/completion, copied text matches the displayed error, progress chatter is hidden, and the expanded panel remains readable at a 390px viewport.

The interface has one **Export cut** workflow. Compatible 8-bit H.264 clips of at least six seconds use smartcut to render cut boundaries and copy complete middle sections when enough can be copied. Other codecs, very short clips and genuine processing failures use the accurate encoder automatically. No path deliberately moves the start to a preceding keyframe.

Version 1.4 removes a temporary video remux. A bounded index reads only video packet timestamps around the selected range directly from the source, without decoding, buffering audio, or scanning the file prefix. The renderer also seeks directly to that indexed window. Compatible AAC audio uses MP4 edit lists/preroll for accurate leading samples and is copied without quality loss. Non-AAC audio is encoded concurrently with video processing. The final pass only muxes the finished tracks. An AAC export can retain less than one compressed frame of trailing audio; the start is checked against decoded reference samples.

Zero-duration packets from the boundary encoder previously caused some correct-frame-count cuts to fail duration validation and trigger a full encode. The adapter now supplies missing packet durations. A reproduced 10 fps regression fails the 1.3 worker and succeeds in 1.4 with copied interior video/audio; the delayed-audio test also checks its original offset. Frame-count and duration checks remain in place, with accurate fallback for genuine failures. The queue now says when it is copying/rendering boundaries, finishing audio, or using full encoding.

The backend remains pinned to smartcut 1.7 on Python 3.11–3.14. Its isolated worker adapts the tested backend API without editing installed dependency source. Newer Python versions retain accurate FFmpeg encoding. Existing libraries and queues need no migration.

On the same generated 60-second **1280×720 / 30 fps H.264 + AAC MP4**, a **3.25–55.25-second** cut took **3.13 seconds median in 1.3** and **1.10 seconds in 1.4**, approximately **2.8× faster**. Three sequential runs per version each launch a fresh worker and include parent input/output probes. The current cut copies 50 seconds of video and the AAC audio. Every benchmark output decodes without errors. This is a local fixture measurement, not a universal speed guarantee or a measurement of the user's hardware/file. [Raw results and the duration regression](tests/results/trim-1.4.0.json).

The prior [1.3 measurements](tests/results/unified-1.3.0.json), [1.2 trimming measurements](tests/results/trim-1.2.0.json), and [1.1 download/remux measurements](tests/results/speed-1.1.0.json) remain historical records. The older separate cut modes are no longer presented or used.

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
