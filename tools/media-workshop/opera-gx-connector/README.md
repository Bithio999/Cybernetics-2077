# Opera GX live connector

This bundled, unpacked extension connects your signed-in YouTube/Twitch session to Media Workshop without copying or decrypting GX's cookie database. GX stays open. It uses the standard browser cookies API; no administrator rights, browser termination or changes to Windows/browser security are needed.

1. Start Media Workshop. In **Opera GX**, open `opera://extensions`.
2. Enable **Developer mode**, click **Load unpacked**, and select this **opera-gx-connector** folder (the one containing `manifest.json`). Allow the listed site access. No packing or store upload is needed.
3. Open/reload Media Workshop in GX. Under **Account access**, choose **Opera GX → Live connector**.
4. Sign into Twitch/YouTube in the same GX profile, then click **Connect Opera GX** in the app. Queue your links or Retry the failed download.

The toolbar button opens the app's default address `http://127.0.0.1:8766/`; for a custom port, open the address printed in the app terminal instead. Connect works with any local port. Reconnect after reloading the app, restarting it, changing your site login, or switching GX profiles. The extension must be enabled in that profile. GX can remain open throughout downloading.

Only cookies for YouTube and Twitch are read, and only after an actual user click on Connect in the local app. Session data goes directly to that app's protected loopback endpoint. The extension does not export a file, log cookies, store credentials, contact an external server or collect other sites' cookies. Imported cookies use the app's existing in-memory/per-job cleanup workflow; the extension itself has no persistent storage permission. It does not read private/incognito sessions by default.

This provides your existing account access, not a subscription or DRM bypass. Native Opera GX on Windows/macOS and real subscriber-account access still require testing on your machine. The included browser review supports a fresh Chromium profile with synthetic cookies. In this cloud environment, extension installation is blocked by browser policy: the successful review uses `--fixture` with a simulated runtime/cookies API. It checks the actual content script and local import/UI, not native extension loading or GX cookie extraction.

References: [Opera: loading/testing unpacked extensions](https://dev.opera.com/extensions/testing/) · [Chromium cookies API and host permissions](https://developer.chrome.com/docs/extensions/reference/api/cookies) · [Content-script isolation](https://developer.chrome.com/docs/extensions/develop/concepts/content-scripts).
