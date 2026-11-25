"use strict";
const assert = require("node:assert/strict"), fs = require("node:fs"), path = require("node:path"), vm = require("node:vm");
const root = path.join(__dirname, "../opera-gx-connector");
const {workshopCookieFile} = require(path.join(root, "cookie-format.js"));
const cookie = {domain: ".twitch.tv", path: "/", name: "auth-token", value: "synthetic-not-real", session: true, secure: true, httpOnly: true, hostOnly: false};
const file = workshopCookieFile([cookie, cookie]);
assert.equal(file.split("\n").filter(row => row.startsWith("#HttpOnly_")).length, 1);
assert.ok(file.includes("#HttpOnly_.twitch.tv\tTRUE\t/\tTRUE\t0\tauth-token\tsynthetic-not-real"));
assert.ok(workshopCookieFile([{...cookie, domain: "www.youtube.com", hostOnly: true, httpOnly: false, session: false, expirationDate: 2000}], 1000).includes("www.youtube.com\tFALSE\t/\tTRUE\t2000"));
for (const change of [
  {domain: ".example.com"}, {domain: ".notyoutube.com"}, {domain: ".youtube.com.attacker.invalid"},
  {value: "bad\tvalue"}, {path: "bad"}, {session: false, expirationDate: 10},
  {session: false, expirationDate: NaN}, {partitionKey: {topLevelSite: "https://example.com"}},
]) assert.throws(() => workshopCookieFile([{...cookie, ...change}], 1000));

const manifest = JSON.parse(fs.readFileSync(path.join(root, "manifest.json")));
assert.equal(manifest.manifest_version, 3);
assert.deepEqual(manifest.permissions, ["cookies"]);
assert.deepEqual(manifest.host_permissions, ["https://*.youtube.com/*", "https://*.twitch.tv/*"]);
assert.ok(!manifest.externally_connectable);
assert.ok(!manifest.web_accessible_resources);
let listener, requested = [];
const chrome = {
  runtime: {id: "fixture-extension-id", onMessage: {addListener(fn) { listener = fn; }}},
  cookies: {async getAll({domain}) { requested.push(domain); return domain === "twitch.tv" ? [cookie] : []; }},
  action: {onClicked: {addListener() {}}}, tabs: {create() {}},
};
vm.runInNewContext(fs.readFileSync(path.join(root, "background.js"), "utf8"), {chrome, URL, importScripts() {}, workshopCookieFile});
const sender = {id: chrome.runtime.id, frameId: 0, url: "http://127.0.0.1:8766/"};
for (const bad of [
  {...sender, url: "https://malicious.invalid/"}, {...sender, url: "http://127.0.0.1.attacker.invalid/"},
  {...sender, frameId: 1}, {...sender, id: "another-extension"}, {...sender, url: "file:///"},
  {...sender, url: "http://127.0.0.1:8766/other"},
]) assert.equal(listener({type: "media-workshop:gx:cookies"}, bad, () => assert.fail("Invalid sender got cookies")), false);
assert.equal(requested.length, 0);
(async () => {
  const response = await new Promise(resolve => assert.equal(listener({type: "media-workshop:gx:cookies"}, sender, resolve), true));
  assert.ok(response.cookies.includes(cookie.value));
  assert.deepEqual(requested.sort(), ["twitch.tv", "youtube.com"]);
  chrome.cookies.getAll = async () => { throw new Error(cookie.value); };
  const failed = await new Promise(resolve => listener({type: "media-workshop:gx:cookies"}, sender, resolve));
  assert.ok(failed.error);
  assert.ok(!failed.error.includes(cookie.value));
  console.log("PASS: cookie normalization, scope, manifest permissions, sender checks and safe errors");
})().catch(error => { console.error(error); process.exitCode = 1; });
