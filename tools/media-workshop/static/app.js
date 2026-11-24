"use strict";
const $ = (id) => document.getElementById(id);
let state = {jobs: [], assets: []}, token = "", currentId = "", selectionEnd = null;
let previousAssets = "", selected = new Set(), messageTimer;

function element(tag, cls, text) {
  const node = document.createElement(tag);
  if (cls) node.className = cls;
  if (text !== undefined) node.textContent = text;
  return node;
}
function notify(text, error = false) {
  clearTimeout(messageTimer); const box = $("message"); box.textContent = text;
  box.className = error ? "error" : ""; box.hidden = false;
  messageTimer = setTimeout(() => { box.hidden = true; }, error ? 12000 : 4500);
}
function seconds(text) {
  const parts = String(text).trim().split(":");
  if (parts.length > 3 || parts.some(p => !/^(?:\d+\.?\d*|\.\d+)$/.test(p))) return NaN;
  if (parts.length > 1 && (parts.slice(0, -1).some(p => !/^\d+$/.test(p)) || parts.slice(1).some(p => Number(p) >= 60))) return NaN;
  return parts.reduce((a, p) => a * 60 + Number(p), 0);
}
function clock(value) {
  const ms = Math.max(0, Math.round(Number(value || 0) * 1000));
  const hours = Math.floor(ms / 3600000), minutes = Math.floor(ms / 60000) % 60;
  return (hours ? String(hours).padStart(2, "0") + ":" : "") + String(minutes).padStart(2, "0") + ":" + String(Math.floor(ms / 1000) % 60).padStart(2, "0") + "." + String(ms % 1000).padStart(3, "0");
}
function bytes(n) { return n >= 1073741824 ? (n / 1073741824).toFixed(2) + " GB" : (n / 1048576).toFixed(1) + " MB"; }
function asset(id) { return state.assets.find(a => a.id === id); }
function urls() { return [...new Set($("urls").value.split(/\r?\n/).map(s => s.trim()).filter(Boolean))]; }
async function request(path, data) {
  const response = await fetch(path, {method: "POST", headers: {"Content-Type": "application/json", "X-Workshop-Token": token}, body: JSON.stringify(data)});
  const result = await response.json(); if (!response.ok) throw new Error(result.error || "Request failed.");
  await refresh(); return result;
}
async function action(fn) { try { await fn(); } catch (error) { notify(error.message, true); } }
function button(text, fn, cls = "quiet") {
  const node = element("button", cls, text); node.type = "button";
  node.addEventListener("click", () => action(fn)); return node;
}
function saveLink(a) {
  const link = element("a", "small-link", "Save file ↓"); link.href = "/media/" + a.id + "?download=1"; link.download = a.title + "." + a.format;
  return link;
}
const jobRows = new Map();
function renderJobDetails(row, job) {
  const reason = job.error || job.fallback_reason;
  if (!reason) return;
  let details = row.querySelector("details");
  if (!details) {
    details = element("details");
    details.append(element("summary"), element("p", "hint"), element("pre"));
    details.append(button("Copy details", async () => {
      await navigator.clipboard.writeText(details.querySelector("pre").textContent);
      notify("Processing details copied.");
    }));
    row.querySelector(".job-main").append(details);
  }
  details.querySelector("summary").textContent = job.error ? "Show error details" : "Why this cut is encoding";
  details.querySelector("p").textContent = job.error ? "The job could not finish." : "Boundary processing failed. Full encoding preserves your selected timing but takes longer.";
  const info = job.fallback_info;
  const range = job.spec.kind === "trim" ? `\n\nSelected range: ${job.spec.start}–${job.spec.end} s` : "";
  const context = range + (info ? `\nSource: ${info.video_codec || "unknown"} / ${info.pixel_format || "unknown"}, ${info.width} × ${info.height}, ${info.fps} fps\nPython: ${info.python}\nsmartcut: ${info.smartcut || "unavailable"}; PyAV: ${info.av || "unavailable"}` : "");
  const failure = reason.split("\n").filter(line => !line.startsWith("WORKSHOP:")).join("\n").trim() || reason;
  const text = failure + context;
  if (details.querySelector("pre").textContent !== text) details.querySelector("pre").textContent = text;
}
function renderJobs() {
  const jobs = [...state.jobs].reverse();
  const active = jobs.filter(j => ["queued", "running"].includes(j.state));
  $("queue-count").textContent = jobs.length;
  $("queue-summary").textContent = `${active.length} unfinished · ${state.workers} parallel workers · ${jobs.filter(j => j.state === "complete").length} ready`;
  $("cancel-all").disabled = !active.length;
  $("clear").disabled = !jobs.some(j => !["queued", "running"].includes(j.state));
  const queue = $("queue"), present = new Set(jobs.map(j => j.id));
  for (const [id, row] of jobRows) if (!present.has(id)) { row.remove(); jobRows.delete(id); }
  if (!jobs.length) {
    if (!queue.querySelector(".empty-list")) queue.append(element("div", "empty-list", "No jobs yet. Add links or export a cut to get started."));
    return;
  }
  queue.querySelector(".empty-list")?.remove();
  jobs.forEach((job, index) => {
    let row = jobRows.get(job.id);
    if (!row) {
      row = element("div", "job"); row.dataset.id = job.id;
      row.append(element("span", "file-icon", job.spec.format.toUpperCase()));
      const main = element("div", "job-main"); main.append(element("p", "item-title"));
      const meta = element("div", "item-meta"); meta.append(element("span", "state"), element("span", "stage"), element("span", "pct")); main.append(meta);
      const progress = element("progress"); progress.max = 100; main.append(progress);
      row.append(main, element("div", "job-actions")); jobRows.set(job.id, row);
    }
    row.querySelector(".item-title").textContent = job.title;
    const status = row.querySelector(".state"); status.className = "state " + job.state; status.textContent = job.state;
    row.querySelector(".stage").textContent = job.stage + (job.method ? " · " + job.method : ""); row.querySelector(".pct").textContent = job.progress.toFixed(0) + "%";
    const progress = row.querySelector("progress"); progress.value = job.progress; progress.setAttribute("aria-label", job.title + " progress");
    renderJobDetails(row, job);
    const file = asset(job.asset_id), actionKey = job.state + ":" + (file?.id || "");
    if (row.dataset.actionKey !== actionKey) {
      row.dataset.actionKey = actionKey;
      const actions = row.querySelector(".job-actions"); actions.replaceChildren();
      if (["queued", "running"].includes(job.state)) actions.append(button("Cancel", () => request("/api/cancel", {id: job.id})));
      if (["failed", "cancelled", "interrupted"].includes(job.state)) actions.append(button("Retry", () => request("/api/retry", {id: job.id})));
      if (file) actions.append(button("Open in editor", () => openAsset(file.id)), saveLink(file));
    }
    if (queue.children[index] !== row) queue.insertBefore(row, queue.children[index] || null);
  });
}
function updateSelected() {
  $("selected-count").textContent = `(${selected.size} selected)`;
  $("select-all").checked = state.assets.length > 0 && selected.size === state.assets.length;
  $("select-all").indeterminate = selected.size > 0 && selected.size < state.assets.length;
  $("export").textContent = $("batch-trim").checked ? `Export ${selected.size} cuts →` : "Export cut →";
}
function renderAssets() {
  const key = JSON.stringify(state.assets); if (key === previousAssets) return; previousAssets = key;
  selected = new Set([...selected].filter(id => asset(id)));
  const dropdown = $("source"); dropdown.replaceChildren();
  dropdown.append(new Option(state.assets.length ? "Choose a file…" : "Download or import a file to begin", ""));
  const library = $("library"); library.replaceChildren();
  $("library-count").textContent = state.assets.length;
  if (!state.assets.length) library.append(element("div", "empty-list", "Your files will appear here."));
  for (const file of [...state.assets].reverse()) {
    dropdown.append(new Option(file.title + " · " + file.format.toUpperCase(), file.id));
    const row = element("div", "asset");
    const check = element("input"); check.type = "checkbox"; check.checked = selected.has(file.id); check.dataset.asset = file.id; check.setAttribute("aria-label", "Select " + file.title);
    check.addEventListener("change", () => { if (check.checked) selected.add(file.id); else selected.delete(file.id); updateSelected(); });
    const main = element("div", "asset-main"); main.append(element("p", "item-title", file.title));
    main.append(element("div", "item-meta", `${clock(file.duration)} · ${bytes(file.bytes)}${file.video ? " · " + file.width + " × " + file.height : " · Audio"}`));
    const actions = element("div", "asset-actions"); actions.append(button("Edit", () => openAsset(file.id)), saveLink(file), button("Delete", async () => { if (confirm(`Delete “${file.title}” from this app's library? Files saved elsewhere are kept.`)) await request("/api/delete", {id: file.id}); }));
    row.append(check, element("span", "file-icon", file.format.toUpperCase()), main, actions); library.append(row);
  }
  if (currentId && !asset(currentId)) openAsset("");
  dropdown.value = currentId;
  updateSelected();
  if (!currentId && state.assets.length) openAsset(state.assets.at(-1).id, false);
}
function openAsset(id, scroll = true) {
  currentId = id; selectionEnd = null; $("source").value = id;
  const file = asset(id), player = $("player"); player.pause(); player.hidden = !file;
  $("empty-preview").hidden = !!file; $("trim-controls").disabled = !file;
  if (!file) { player.removeAttribute("src"); player.load(); $("source-info").textContent = "NO MEDIA SELECTED"; return; }
  player.src = "/media/" + file.id;
  player.parentElement.classList.toggle("audio", !file.video);
  $("source-info").textContent = `${file.format.toUpperCase()} · ${file.video ? file.width + " × " + file.height : "AUDIO"} · ${clock(file.duration)}`;
  $("source-type").textContent = file.video ? "VIDEO EDITOR" : "AUDIO EDITOR";
  $("in-range").max = $("out-range").max = file.duration;
  $("export-format").value = file.video ? "mp4" : "mp3";
  $("export-format").querySelector('[value="mp4"]').disabled = !file.video;
  $("start").value = clock(0); $("end").value = clock(file.duration);
  syncRange();
  if (scroll) $("editor").scrollIntoView({behavior: "smooth", block: "start"});
}
function syncRange() {
  const file = asset(currentId); if (!file) return;
  const start = seconds($("start").value), end = $("end").value.trim() ? seconds($("end").value) : asset(currentId)?.duration;
  if (!Number.isFinite(start) || !Number.isFinite(end)) return;
  $("in-range").value = Math.min(start, file.duration); $("out-range").value = Math.min(end, file.duration);
  $("range-selection").style.left = `${100 * Math.min(start, file.duration) / file.duration}%`;
  $("range-selection").style.width = `${100 * Math.max(0, Math.min(end, file.duration) - start) / file.duration}%`;
  $("cut-duration").textContent = end > start ? clock(end - start) : "Invalid range";
}
async function refresh() {
  const response = await fetch("/api/state"); if (!response.ok) throw new Error("The local app is unavailable.");
  state = await response.json(); token = state.token;
  const deps = state.dependencies, missing = [];
  if (!deps.ffmpeg || !deps.ffprobe) missing.push("Install FFmpeg (including ffprobe) and restart the app.");
  if (!deps.yt_dlp) missing.push("Run launch.py to install the downloader.");
  if (!deps.javascript) missing.push("For current YouTube support, install Deno (recommended) or Node.js 22+ and restart the app.");
  $("dependencies").hidden = !missing.length; $("dependencies").textContent = missing.join("\n");
  $("footer-status").textContent = `yt-dlp ${deps.yt_dlp || "missing"} · FFmpeg ${deps.ffmpeg && deps.ffprobe ? "ready" : "missing"} · ${deps.javascript || "no JS runtime"}`;
  $("storage").textContent = "Library location: " + state.directory;
  renderAssets(); renderJobs();
}
$("urls").addEventListener("input", () => { const n = urls().length; $("link-count").textContent = `${n} link${n === 1 ? "" : "s"}`; });
$("download-format").addEventListener("change", () => { $("height").disabled = $("download-format").value === "mp3"; });
$("download-form").addEventListener("submit", (event) => { event.preventDefault(); action(async () => {
  const links = urls(); if (!links.length) throw new Error("Paste at least one link.");
  const result = await request("/api/download", {urls: links, format: $("download-format").value, height: Number($("height").value)});
  $("urls").value = ""; $("link-count").textContent = "0 links"; notify(`${result.ids.length} download${result.ids.length === 1 ? "" : "s"} queued.`);
}); });
$("source").addEventListener("change", () => openAsset($("source").value, false));
for (const field of ["start", "end"]) $(field).addEventListener("input", syncRange);
for (const [range, field] of [["in-range", "start"], ["out-range", "end"]]) $(range).addEventListener("input", () => { $(field).value = clock(Number($(range).value)); syncRange(); });
$("mark-in").addEventListener("click", () => { $("start").value = clock($("player").currentTime); syncRange(); });
$("mark-out").addEventListener("click", () => { $("end").value = clock($("player").currentTime); syncRange(); });
$("preview-cut").addEventListener("click", () => action(async () => { const start = seconds($("start").value), end = $("end").value.trim() ? seconds($("end").value) : asset(currentId)?.duration; if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) throw new Error("Choose a valid start and end first."); selectionEnd = end; $("player").currentTime = start; await $("player").play(); }));
$("player").addEventListener("timeupdate", () => { $("play-time").textContent = clock($("player").currentTime); if (selectionEnd !== null && $("player").currentTime >= selectionEnd) { $("player").pause(); selectionEnd = null; } });
$("player").addEventListener("error", () => { if (currentId) notify("This browser cannot preview the source codec. You can still trim it or export the full range as MP4/MP3.", true); });
$("trim-form").addEventListener("submit", (event) => { event.preventDefault(); action(async () => {
  const ids = $("batch-trim").checked ? [...selected] : [currentId];
  if (!ids.length || !ids[0]) throw new Error("Choose media files first.");
  const result = await request("/api/trim", {assets: ids, format: $("export-format").value, start: $("start").value, end: $("end").value});
  notify(`${result.ids.length} export${result.ids.length === 1 ? "" : "s"} queued. Originals are kept.`);
}); });
$("batch-trim").addEventListener("change", updateSelected);
$("select-all").addEventListener("change", () => { selected = $("select-all").checked ? new Set(state.assets.map(a => a.id)) : new Set(); document.querySelectorAll("[data-asset]").forEach(n => { n.checked = selected.has(n.dataset.asset); }); updateSelected(); });
$("clear").addEventListener("click", () => action(() => request("/api/clear", {})));
$("cancel-all").addEventListener("click", () => action(async () => { for (const j of state.jobs.filter(j => ["queued", "running"].includes(j.state))) await request("/api/cancel", {id: j.id}); }));
function upload(file, index, total) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest(); xhr.open("POST", "/api/import");
    xhr.setRequestHeader("X-Workshop-Token", token); xhr.setRequestHeader("X-File-Name", encodeURIComponent(file.name));
    xhr.upload.onprogress = event => { $("import-progress").textContent = `Importing ${index}/${total}: ${file.name} · ${event.lengthComputable ? Math.round(event.loaded / event.total * 100) + "%" : "…"}`; };
    xhr.onload = () => { try { const result = JSON.parse(xhr.responseText); if (xhr.status >= 400) reject(new Error(result.error)); else resolve(result.asset); } catch { reject(new Error("The file could not be imported.")); } };
    xhr.onerror = () => reject(new Error("Import interrupted. Check the app terminal and available disk space.")); xhr.send(file);
  });
}
$("files").addEventListener("change", () => action(async () => {
  const files = [...$("files").files]; $("files").disabled = true;
  try { for (let i = 0; i < files.length; i++) { const file = await upload(files[i], i + 1, files.length); await refresh(); openAsset(file.id, false); } $("import-progress").textContent = `${files.length} file${files.length === 1 ? "" : "s"} imported.`; }
  finally { $("files").disabled = false; $("files").value = ""; }
}));
async function poll() { try { await refresh(); } catch { $("footer-status").textContent = "Disconnected · keep the app terminal open"; } finally { setTimeout(poll, 900); } }
poll();
