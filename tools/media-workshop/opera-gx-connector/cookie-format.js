"use strict";

// Shared by the extension worker and synthetic Node tests. No disk/network I/O.
function workshopCookieFile(cookies, now = Date.now() / 1000) {
  const rows = ["# Netscape HTTP Cookie File"], seen = new Set();
  for (const cookie of cookies) {
    if (cookie.partitionKey) continue; // yt-dlp cannot represent partitioned cookies.
    const domain = String(cookie.domain || "").toLowerCase();
    const host = domain.replace(/^\./, "");
    if (!["youtube.com", "twitch.tv"].some(site => host === site || host.endsWith("." + site))) continue;
    const path = cookie.path || "/", name = cookie.name, value = cookie.value;
    if (!path.startsWith("/") || typeof name !== "string" || typeof value !== "string" ||
        [domain, path, name, value].some(part => /[\x00-\x1f\x7f]/.test(part))) continue;
    const expiry = cookie.session ? 0 : Math.floor(cookie.expirationDate);
    if (!cookie.session && (!Number.isFinite(expiry) || expiry <= now)) continue;
    const key = JSON.stringify([domain, path, name]);
    if (seen.has(key)) continue;
    seen.add(key);
    const storedDomain = cookie.hostOnly ? host : "." + host;
    rows.push([
      (cookie.httpOnly ? "#HttpOnly_" : "") + storedDomain,
      cookie.hostOnly ? "FALSE" : "TRUE", path, cookie.secure ? "TRUE" : "FALSE", expiry, name, value,
    ].join("\t"));
  }
  if (rows.length === 1) throw new Error("No usable YouTube/Twitch cookies found. Sign into the site in this GX profile, then connect again.");
  return rows.join("\n") + "\n";
}
if (typeof module !== "undefined") module.exports = {workshopCookieFile};
