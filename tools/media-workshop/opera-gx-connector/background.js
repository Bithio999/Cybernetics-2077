"use strict";
importScripts("cookie-format.js");

function localWorkshopSender(sender) {
  try {
    const url = new URL(sender.url);
    return sender.id === chrome.runtime.id && sender.frameId === 0 &&
      url.protocol === "http:" && ["127.0.0.1", "localhost"].includes(url.hostname) &&
      url.pathname === "/" && !url.username && !url.password;
  } catch (_) { return false; }
}

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message?.type !== "media-workshop:gx:cookies" || !localWorkshopSender(sender)) return false;
  (async () => {
    try {
      // Only the two supported sites, only on an explicit Connect click.
      const groups = await Promise.all(["youtube.com", "twitch.tv"].map(domain => chrome.cookies.getAll({domain})));
      sendResponse({cookies: workshopCookieFile(groups.flat())});
    } catch (_) {
      sendResponse({error: "Could not read this browser session. Check the connector's YouTube/Twitch permissions and sign into the site in GX, then connect again."});
    }
  })();
  return true;
});

chrome.action.onClicked.addListener(() => chrome.tabs.create({url: "http://127.0.0.1:8766/"}));
