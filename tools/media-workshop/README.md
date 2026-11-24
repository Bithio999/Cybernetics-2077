# Media Workshop

A local browser app for downloading **YouTube videos and Twitch VODs/clips** to MP4 or MP3, with a built-in preview and trimmer. Runs independently of Minecraft.

![Media Workshop](preview.png)

## Install and open

Requires **Python 3.11+**, **FFmpeg with ffprobe**, and **Deno or Node.js 22+** for current YouTube support. Python **3.13 is recommended**. The launcher installs the download and cut-processing dependencies into a private `.venv` folder on first use.

**Windows**

Install these once in PowerShell (or use the linked projects' installers):

```powershell
winget install --exact --id Python.Python.3.13
winget install --exact --id Gyan.FFmpeg
winget install --exact --id DenoLand.Deno
```

Reopen your terminal after installation. Extract the program ZIP, then double-click **Start.bat**. Keep its terminal open while using the app. It opens `http://127.0.0.1:8766` in your browser.

**macOS**

With [Homebrew](https://brew.sh/):

```sh
brew install python ffmpeg deno
cd /path/to/Media-Workshop
sh start.sh
```

**Linux**

Install Python 3.11+, its `venv` module, FFmpeg and ffprobe through your package manager; install [Deno](https://docs.deno.com/runtime/getting_started/installation/) or Node.js 22+. Then run `sh start.sh` from the program folder.

The examples below use `python`; use `python3` on systems that name it that way.

Official installers: [Python](https://www.python.org/downloads/) · [FFmpeg](https://ffmpeg.org/download.html) · [Deno](https://docs.deno.com/runtime/getting_started/installation/) · [Node.js](https://nodejs.org/en/download).

When updating from an older version, replace the program files and retain your **data/** folder (and **.venv/**). Existing media and queue history remain available. The launcher automatically installs changed requirements on the next start.

## Download a batch

1. Paste one YouTube or Twitch link per line. Duplicate links in the batch are combined.
2. Choose **MP4** or **MP3**, and a maximum video quality from 480p to 4K.
3. Click **Add to download queue**. Two jobs run simultaneously by default; up to 100 unfinished jobs can be queued.
4. Use **Save file** for a browser download, or **Open in editor** to trim the result.

Each job has its own progress, cancellation and retry controls. One failed link does not stop the rest. **Cancel unfinished** stops active and waiting jobs; **Clear finished** clears history while retaining library files.

MP4 downloads prefer H.264 video and AAC audio while retaining the selected resolution/frame rate. Ready-to-use, fast-start MP4s are saved directly. Other compatible files are remuxed using **stream copy**, preserving their compressed audio and video without re-encoding. If one track is incompatible, only that track is converted. Necessary video conversions use a faster preset and a bounded CPU-thread budget shared between the configured workers.

The queue shows **Direct save**, **Stream copy**, or which tracks require conversion. Resolution still respects your quality ceiling, and sources are not upscaled. A 4K source is not replaced with a lower-resolution H.264 source just to avoid conversion.

**MP3 conversion starts during the download**, rather than waiting for the entire source to finish. Existing MP3 downloads are preserved at their source bitrate without encoding; other audio downloads are converted to **192 kbps**. If a source requires a seekable file, the app automatically downloads it first and then converts it.

Individual videos, Shorts, Twitch VODs and clips are supported through yt-dlp. Playlist/channel URLs are not expanded into batches, and ongoing live streams are not recorded. Site availability, authentication requirements and network restrictions can still prevent a download. This app has no login or DRM-unlocking feature.

## Trim or convert

1. Download media or choose **Choose audio or video** to import files from your computer. Multiple selection is supported.
2. Select a source, play the preview, and choose start/end times with the sliders or **Mark in / Mark out**.
3. Use **Preview selection** to play only the chosen range.
4. Select MP4 or MP3 and click **Export cut**.

There is **one accurate cut workflow**, optimized automatically. Your selected start/end times stay in place, within the source's frame/audio-sample precision.

- Longer compatible H.264 cuts copy complete video sections and re-encode only the boundaries. Audio is encoded for accurate timing.
- Shorter cuts and other codecs use a fast accurate encoder automatically. If boundary processing cannot preserve the requested timing, the app uses that encoder instead.
- Exporting the whole source copies compatible tracks when possible. Originals remain intact.
- MP3 cuts are encoded at 192 kbps to retain precise timing without compressed-frame cut shifts.

Boundary processing uses smartcut on Python 3.11–3.14; other supported Python versions retain the accurate FFmpeg path. Performance depends on the codec, resolution, keyframe spacing and selected range. Faster encoding can produce larger files than slower presets.

For **batch trimming**, check files in the media library, then enable **Apply this range to all checked library files**. All selected files use the same time range and output format. An invalid range or incompatible file prevents the entire batch from being queued, with an explanation. Leave End blank to use each file's own end.

Times accept seconds (`83.5`), minutes/seconds (`01:23.500`) or hours/minutes/seconds (`00:01:23.500`). Audio-only media exports as MP3; video with no audio cannot export as MP3. Some imported codecs cannot preview in a particular browser but can still be converted.

Original files are never overwritten. Imports are copied into the library, and each export has a separate folder. Imports stream to disk rather than loading an entire large file into memory.

## Your files and queue

The library lives in **`data/` beside app.py**; its location is also shown at the bottom of the app. Queue records and finished media survive closing and reopening. Jobs interrupted by a process crash become **Interrupted**, with a Retry button. An orderly Ctrl+C cancels unfinished jobs. Closing the browser tab leaves the app running; press **Ctrl+C in its terminal** to quit.

**Save file** makes a separate browser download. **Delete** removes a file from the app's library, leaving files saved elsewhere intact. Files with pending exports cannot be deleted. Only one app instance may open a particular library at once.

To change the library folder, parallel worker count or local port:

```sh
python launch.py gui --data-dir "D:/Media Library" --workers 3 --port 8767
```

Use `--port 0` to choose an available port or `--no-browser` to open the printed address yourself. The server listens only on your own computer; the interface loads no external fonts, scripts or analytics.

To update yt-dlp after a site changes:

```sh
python launch.py --update
```

On Windows, `Start.bat --update` works too. Install FFmpeg itself, not the unrelated Python package called `ffmpeg`.

## Command line

The same queue/encoder is available without the browser:

```sh
python launch.py download "https://youtu.be/VIDEO_ID" "https://www.twitch.tv/videos/VOD_ID" --format mp4 --height 1080
python launch.py download "https://clips.twitch.tv/CLIP_ID" --format mp3
python launch.py trim "first.mp4" "second.mp4" --start 00:00:10 --end 00:00:25.500 --format mp4
python launch.py trim "recording.mp4" --start 10.250 --end 25.500 --format mp4
python launch.py trim "recording.mp4" --format mp3
```

Use `python app.py --help` for commands. Each command accepts `--data-dir` and `--workers`. The CLI uses the same automatic accurate workflow, prints the cut start and output duration, and exits nonzero if any job fails. Old saved cut-mode settings are ignored.

## Testing

```sh
python -m pip install -r requirements.txt
python -m unittest discover -s tests -v
```

Tests exercise real FFmpeg MP4/MP3 exports, batch jobs, original-file preservation, actual child-process cancellation, recovery, uploads, seeking and download responses. The real yt-dlp subprocess is exercised against a local media fixture. Public YouTube/Twitch access is a separate check; it is not simulated by those fixtures. See [VALIDATION.md](VALIDATION.md) for the observed results and platform limits.

Optional browser review (creates test media and screenshots):

```sh
python -m pip install -r requirements-dev.txt
python -m playwright install chromium
python tests/browser_review.py
```

Use `--browser /path/to/chromium` for an existing Chromium installation, or `--output /path/to/review` to change the capture directory.

Download backend: [yt-dlp](https://github.com/yt-dlp/yt-dlp) · YouTube runtime requirements: [yt-dlp EJS](https://github.com/yt-dlp/yt-dlp/wiki/EJS) · Encoding: [FFmpeg](https://ffmpeg.org/ffmpeg.html) · Boundary processing: [smartcut](https://github.com/skeskinen/smartcut) by Santtu Keskinen ([MIT license](https://github.com/skeskinen/smartcut/blob/main/LICENSE)) using [PyAV](https://pyav.org/). Dependencies are installed separately; their source and binaries are not bundled in the ZIP.
