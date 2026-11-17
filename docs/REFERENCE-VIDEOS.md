# Reference video helper

Download a public YouTube reference as MP4 using **yt-dlp**, then extract chronological frame sheets with **FFmpeg**. Existing uploaded MP4s can be inspected without YouTube access.

## Install

Requires Python 3.10 or newer, plus `ffmpeg` and `ffprobe` on PATH.

```sh
python -m pip install -U yt-dlp
```

## Download and inspect

```sh
python scripts/reference-video.py fetch "https://www.youtube.com/watch?v=VIDEO_ID"
```

The helper downloads one video, up to 1080p, to `references-local/VIDEO_ID.mp4`. It merges the video and audio into MP4, then samples the first 30 seconds at two frames per second. It preserves the inherited network and certificate settings.

To inspect a particular animation at higher temporal resolution:

```sh
python scripts/reference-video.py inspect "references-local/VIDEO_ID.mp4" --start 21 --duration 8 --fps 8
```

Each contact sheet has four columns and four rows, ordered left to right, then top to bottom. `inspection.json` records the source, resolution, time range and frame timing. MP4s and extracted reference sheets stay in the Git-ignored `references-local/` folder.

## Cloud access limitation

The current cloud environment rejects the YouTube connection with a proxy-level **CONNECT 403**, before the page or video is reached. This was reproduced using the browser, curl and yt-dlp. The configured network policy reports unrestricted access, but that setting does not remove this upstream rejection.

The helper does not change routing or bypass that denial. Run `fetch` on a computer where YouTube is accessible, then attach the resulting MP4 in the conversation. The cloud can inspect the uploaded file locally. This is a working path for reference review; it is not a claim that cloud YouTube downloading has been restored.

## Verification

- Installed and tested yt-dlp against the supplied `LD5z3GmQRXQ` URL: download blocked by CONNECT 403.
- Ran `inspect` on the user's uploaded Cyberpunk scanner reference and checked the generated sheets against the original video.
- Successful downloading from YouTube could not be verified inside this cloud environment.
