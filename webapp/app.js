// SoundTag Web — multi-view controller mirroring the Android Compose screens.

import { getLocation, formatGps } from "./location.js";
import { Recorder, formatTimer, MAX_DURATION_MS } from "./recorder.js";
import { DbMeter } from "./db_meter.js";
import { buildMetadata, saveLocally } from "./metadata.js";
import { uploadRecording } from "./upload.js";
import {
  LABELS, SEVERITY_LEVELS, SEVERITY_SCORES, ENVIRONMENTS, CONTEXTS,
  defaultSelections, renderChipGroup, renderToggleGroup,
  renderSeverityRow, renderScoreBoxes, autoFilename,
} from "./annotate.js";
import {
  loadRecordings, saveRecordings, addRecording,
  updateRecordingStatus, removeRecording,
  loadProfile, saveProfile, todayCount,
} from "./store.js";
import { putBlob, getBlob, deleteBlob, hasBlob } from "./db.js";
import { GOOGLE_CLIENT_ID } from "./config.js";

const $ = id => document.getElementById(id);

// ---------- View routing ----------
const VIEWS = ["setup", "record", "annotate", "dashboard"];
function show(view) {
  for (const v of VIEWS) $(`view-${v}`).hidden = v !== view;
  if (view === "dashboard") renderDashboard();
  if (view === "record") {
    $("statusId").textContent = profile.annotatorId || "—";
    $("todayCount").textContent = `Today: ${todayCount(loadRecordings())} recordings`;
  }
  window.scrollTo(0, 0);
}

// ---------- State ----------
const recorder = new Recorder();
let timerInterval = null;
let currentLocation = null;
let lastRecording = null;
let dbMeter = null;
let peakDb = 0;
let profile = loadProfile();
const selections = defaultSelections();

// ---------- Snackbar ----------
function toast(msg, kind = "") {
  const s = $("snackbar");
  s.textContent = msg;
  s.className = "snackbar " + kind;
  s.hidden = false;
  clearTimeout(toast._t);
  toast._t = setTimeout(() => (s.hidden = true), 2500);
}

// ============================== SETUP ==============================
function initSetup() {
  $("setupName").value = profile.name || "";
  $("setupId").value = profile.annotatorId || "";
  $("driveStatusText").textContent = GOOGLE_CLIENT_ID
    ? "Google Drive" : "Google Drive (not configured)";
  $("driveStatusBadge").textContent = GOOGLE_CLIENT_ID ? "Ready" : "Offline";
  $("driveStatusBadge").className = "pill " + (GOOGLE_CLIENT_ID ? "pill-accent" : "pill-muted");
  const updateFolder = () => {
    const id = ($("setupId").value || "{id}").trim() || "{id}";
    $("folderText").textContent = `SoundTag/${id}/`;
  };
  $("setupId").addEventListener("input", updateFolder);
  updateFolder();

  $("continueBtn").addEventListener("click", () => {
    const name = $("setupName").value.trim();
    const annotatorId = $("setupId").value.trim().toLowerCase().replace(/\s+/g, "-");
    if (!annotatorId) return toast("Annotator ID is required", "error");
    profile = { name, annotatorId };
    saveProfile(profile);
    show("record");
    refreshGps();
  });
}

// ============================== RECORD ==============================
function initRecord() {
  $("recordBtn").addEventListener("click", () => {
    if (recorder.state === "recording") stopRecording();
    else startRecording();
  });
  $("openDashboardBtn").addEventListener("click", () => show("dashboard"));

  // Build waveform bars once
  const wf = $("waveform");
  wf.replaceChildren();
  for (let i = 0; i < 14; i++) {
    const b = document.createElement("div");
    b.className = "wave-bar";
    wf.appendChild(b);
  }
}

async function refreshGps() {
  $("gpsCoords").textContent = "locating…";
  $("gpsAcc").hidden = true;
  currentLocation = await getLocation();
  if (currentLocation) {
    $("gpsCoords").textContent =
      `${currentLocation.lat.toFixed(4)}, ${currentLocation.lon.toFixed(4)}`;
    $("gpsAcc").textContent = `±${Math.round(currentLocation.acc)}m`;
    $("gpsAcc").hidden = false;
  } else {
    $("gpsCoords").textContent = "GPS unavailable";
  }
}

function updateWaveform(db) {
  const bars = document.querySelectorAll(".wave-bar");
  const norm = Math.min(1, db / 40);
  bars.forEach((b, i) => {
    const variance = 0.4 + 0.6 * Math.abs(Math.sin((Date.now() / 120) + i));
    const h = 8 + norm * 24 * variance;
    b.style.height = `${h}px`;
    b.style.opacity = String(0.3 + norm * 0.7 * variance);
  });
}

function tick() {
  const ms = recorder.elapsedMs();
  $("timer").textContent = formatTimer(ms);
  if (dbMeter) {
    const db = dbMeter.sample();
    if (db > peakDb) peakDb = db;
    $("dbReading").textContent = `${db.toFixed(1)} dB`;
    updateWaveform(db);
  }
  if (ms >= MAX_DURATION_MS) stopRecording();
}

async function startRecording() {
  try {
    const stream = await recorder.start();
    $("view-record").classList.add("recording");
    $("stateStrip").classList.add("rec");
    $("statusDot").classList.add("rec");
    $("micIdleContent").hidden = true;
    $("micActiveContent").hidden = false;
    $("recordSub").hidden = true;
    $("activeBlock").hidden = false;
    peakDb = 0;
    dbMeter = new DbMeter(stream);
    timerInterval = setInterval(tick, 100);
    refreshGps();
  } catch (e) {
    toast("Microphone permission denied", "error");
  }
}

async function stopRecording() {
  try {
    const result = await recorder.stop();
    clearInterval(timerInterval);
    $("view-record").classList.remove("recording");
    $("stateStrip").classList.remove("rec");
    $("statusDot").classList.remove("rec");
    $("micIdleContent").hidden = false;
    $("micActiveContent").hidden = true;
    $("recordSub").hidden = false;
    $("activeBlock").hidden = true;
    if (dbMeter) { dbMeter.dispose(); dbMeter = null; }
    lastRecording = { ...result, peakDb };
    openAnnotate();
  } catch (e) {
    if (e.message === "min-duration") toast("Minimum 5 seconds", "error");
  }
}

// ============================== ANNOTATE ==============================
function initAnnotate() {
  $("annotateBackBtn").addEventListener("click", () => show("record"));
  $("saveLocalBtn").addEventListener("click", () => handleSave(true));
  $("saveLocalOnlyBtn").addEventListener("click", () => handleSave(false));

  $("pbPlay").addEventListener("click", () => {
    const a = $("playbackAudio");
    if (a.paused) a.play(); else a.pause();
  });
  const audio = $("playbackAudio");
  audio.addEventListener("timeupdate", () => {
    if (!audio.duration) return;
    const pct = (audio.currentTime / audio.duration) * 100;
    $("pbFill").style.width = pct + "%";
    $("pbTime").textContent =
      `${formatTimer(audio.currentTime * 1000)} / ${formatTimer(audio.duration * 1000)}`;
  });
}

function renderAnnotateChips() {
  renderChipGroup($("labelChips"), LABELS, selections.label, v => {
    selections.label = v;
    $("filename").value = autoFilename(v);
    renderAnnotateChips();
  });
  renderToggleGroup($("noiseToggle"), ["noise", "not noise"],
    selections.isNoise ? "noise" : "not noise",
    v => { selections.isNoise = v === "noise"; renderAnnotateChips(); });
  renderSeverityRow($("severityRow"), selections.severity, v => {
    selections.severity = v;
    selections.score = SEVERITY_SCORES[v];
    $("scoreValue").textContent = selections.score;
    renderAnnotateChips();
  });
  renderScoreBoxes($("scoreBoxes"), selections.score, v => {
    selections.score = v; renderAnnotateChips();
  });
  $("scoreValue").textContent = selections.score;
  renderToggleGroup($("envToggle"), ENVIRONMENTS, selections.environment,
    v => { selections.environment = v; renderAnnotateChips(); });
  renderChipGroup($("ctxChips"), CONTEXTS, selections.locationContext,
    v => { selections.locationContext = v; renderAnnotateChips(); });
}

function openAnnotate() {
  if (!lastRecording) return;
  const dur = Math.round(lastRecording.durationMs / 1000);
  const ts = new Date(lastRecording.startedAt).toLocaleTimeString();
  $("recInfo").textContent = `${dur}s · ${ts}`;
  $("recGps").textContent = currentLocation
    ? `${currentLocation.lat.toFixed(4)}, ${currentLocation.lon.toFixed(4)} ±${Math.round(currentLocation.acc)}m`
    : "No GPS";

  $("playbackAudio").src = URL.createObjectURL(lastRecording.blob);
  $("filename").value = autoFilename(selections.label);
  $("notes").value = "";
  $("saveFootnote").textContent = GOOGLE_CLIENT_ID
    ? `Saves to Drive under SoundTag/${profile.annotatorId}/`
    : "Drive not configured — save locally";
  renderAnnotateChips();
  show("annotate");
}

function currentMetadata() {
  const base = ($("filename").value || autoFilename(selections.label)).trim();
  const meta = buildMetadata({
    filenameBase: base,
    mimeType: lastRecording.mimeType,
    startedAt: lastRecording.startedAt,
    durationMs: lastRecording.durationMs,
    location: currentLocation,
    annotation: {
      label: selections.label,
      severity: selections.severity,
      environment: selections.environment,
      locationContext: selections.locationContext,
      isNoise: selections.isNoise,
      annotatorId: profile.annotatorId,
      notes: $("notes").value.trim(),
    },
  });
  meta.severity_score = selections.score;
  meta.peak_db = Number(lastRecording.peakDb.toFixed(1));
  return meta;
}

async function handleSave(withUpload) {
  if (!lastRecording) return toast("No recording", "error");
  const meta = currentMetadata();

  // Always persist the blob to IndexedDB so it survives reloads.
  try {
    await putBlob(meta.filename, lastRecording.blob, lastRecording.mimeType);
  } catch (e) {
    toast("Could not persist blob: " + e.message, "error");
  }

  // Also trigger browser downloads (audio + JSON sidecar).
  saveLocally(lastRecording.blob, meta);

  let status = "Local";
  if (withUpload && GOOGLE_CLIENT_ID) {
    toast("Uploading…");
    const res = await uploadRecording(lastRecording.blob, meta);
    status = res.ok ? "Uploaded" : "Failed";
    toast(res.ok ? "Uploaded" : "Upload failed: " + res.error, res.ok ? "success" : "error");
  } else if (withUpload && !GOOGLE_CLIENT_ID) {
    status = "Pending";
    toast("Saved (Drive not configured)", "success");
  } else {
    toast("Saved locally", "success");
  }
  addRecording(meta, status);
  show("record");
}

// ============================== DASHBOARD ==============================
function initDashboard() {
  $("dashBackBtn").addEventListener("click", () => show("record"));
  document.querySelectorAll(".tab").forEach(t => {
    t.addEventListener("click", () => {
      document.querySelectorAll(".tab").forEach(x => x.classList.remove("active"));
      t.classList.add("active");
      $("tabRecordings").hidden = t.dataset.tab !== "recordings";
      $("tabStats").hidden = t.dataset.tab !== "stats";
    });
  });
  $("syncAllBtn").addEventListener("click", syncAllPending);
  $("uploadExistingBtn").addEventListener("click", onUploadExisting);
}

function renderDashboard() {
  const list = loadRecordings();
  $("sumToday").textContent = `Today: ${todayCount(list)}`;
  $("sumTotal").textContent = `Total: ${list.length}`;
  $("driveSummary").textContent = GOOGLE_CLIENT_ID
    ? "Connected to Drive" : "Drive not configured";

  const listEl = $("recordingList");
  listEl.replaceChildren();
  if (list.length === 0) {
    const e = document.createElement("div");
    e.className = "empty";
    e.textContent = "No recordings yet";
    listEl.appendChild(e);
  } else {
    for (const r of list.slice(0, 50)) listEl.appendChild(recordingRow(r));
  }

  // Stats
  const totalSec = list.reduce((s, r) => s + (r.duration_seconds || 0), 0);
  const mins = Math.floor(totalSec / 60), hrs = Math.floor(mins / 60);
  $("statsOverview").textContent =
    `${list.length} recordings · ${hrs}h ${mins % 60}m total`;
  renderBarChart(list);
}

function recordingRow(r) {
  const row = document.createElement("div");
  row.className = "rec-row";

  const top = document.createElement("div");
  top.className = "rec-row-top";
  const emoji = document.createElement("div");
  emoji.className = "rec-emoji";
  emoji.textContent = LABELS.find(l => l.id === r.label)?.emoji || "🎙️";
  top.appendChild(emoji);

  const col = document.createElement("div");
  col.className = "rec-col";
  const n = document.createElement("div");
  n.className = "rec-name mono";
  n.textContent = r.filename;
  const m = document.createElement("div");
  m.className = "rec-meta";
  const time = (r.started_at_local || "").slice(11, 16);
  m.textContent = `${time} · ${r.duration_seconds || 0}s`;
  col.append(n, m);
  top.appendChild(col);

  const badge = document.createElement("span");
  const statusClass = {
    Uploaded: "pill-success", Local: "pill-muted",
    Pending: "pill-warn", Failed: "pill-error",
  }[r.status] || "pill-muted";
  badge.className = "pill " + statusClass;
  badge.textContent = r.status;
  top.appendChild(badge);
  row.appendChild(top);

  // Actions row — retry for Pending/Failed, delete for any.
  const actions = document.createElement("div");
  actions.className = "rec-actions";

  if (r.status === "Pending" || r.status === "Failed") {
    const retry = document.createElement("button");
    retry.className = "row-btn row-btn-accent";
    retry.textContent = "Retry Upload";
    retry.addEventListener("click", e => { e.stopPropagation(); retryUpload(r); });
    actions.appendChild(retry);
  }

  const dl = document.createElement("button");
  dl.className = "row-btn";
  dl.textContent = "Download";
  dl.addEventListener("click", async e => {
    e.stopPropagation();
    const entry = await getBlob(r.filename);
    if (!entry) return toast("Blob not in cache", "error");
    const a = document.createElement("a");
    a.href = URL.createObjectURL(entry.blob);
    a.download = r.filename;
    a.click();
  });
  actions.appendChild(dl);

  const del = document.createElement("button");
  del.className = "row-btn row-btn-error";
  del.textContent = "Delete";
  del.addEventListener("click", async e => {
    e.stopPropagation();
    await deleteBlob(r.filename);
    removeRecording(r.filename);
    renderDashboard();
    toast("Deleted");
  });
  actions.appendChild(del);

  row.appendChild(actions);
  return row;
}

async function retryUpload(r) {
  const entry = await getBlob(r.filename);
  if (!entry) {
    toast("Blob not in cache — re-record to upload", "error");
    updateRecordingStatus(r.filename, "Failed");
    renderDashboard();
    return;
  }
  if (!GOOGLE_CLIENT_ID) return toast("Drive not configured", "error");
  toast("Uploading…");
  const meta = { ...r };
  const res = await uploadRecording(entry.blob, meta);
  updateRecordingStatus(r.filename, res.ok ? "Uploaded" : "Failed");
  toast(res.ok ? "Uploaded" : "Upload failed: " + res.error, res.ok ? "success" : "error");
  renderDashboard();
}

function renderBarChart(list) {
  const counts = {};
  LABELS.forEach(l => (counts[l.id] = 0));
  list.forEach(r => { if (counts[r.label] != null) counts[r.label]++; });
  const max = Math.max(1, ...Object.values(counts));
  const chart = $("barChart");
  chart.replaceChildren();
  for (const lbl of LABELS) {
    const row = document.createElement("div");
    row.className = "bar-row";
    const l = document.createElement("div");
    l.className = "bar-lbl";
    l.textContent = `${lbl.emoji} ${lbl.text}`;
    const track = document.createElement("div");
    track.className = "bar-track";
    const fill = document.createElement("div");
    fill.className = "bar-fill";
    fill.style.width = `${(counts[lbl.id] / max) * 100}%`;
    track.appendChild(fill);
    const c = document.createElement("div");
    c.className = "bar-count";
    c.textContent = String(counts[lbl.id]);
    row.append(l, track, c);
    chart.appendChild(row);
  }
  const lowLabels = LABELS.filter(l => counts[l.id] < 5).map(l => l.text);
  if (lowLabels.length > 0 && list.length > 0) {
    $("balanceWarningCard").hidden = false;
    $("balanceWarning").textContent =
      `⚠ Collect more: ${lowLabels.join(", ")}. Each label needs at least 5 clips.`;
  } else {
    $("balanceWarningCard").hidden = true;
  }
}

async function syncAllPending() {
  if (!GOOGLE_CLIENT_ID) return toast("Drive not configured", "error");
  const list = loadRecordings();
  const pending = list.filter(r => r.status === "Pending" || r.status === "Failed");
  if (pending.length === 0) return toast("Nothing to sync");

  toast(`Syncing ${pending.length}…`);
  let ok = 0, skipped = 0, failed = 0;
  for (const r of pending) {
    const entry = await getBlob(r.filename);
    if (!entry) { skipped++; updateRecordingStatus(r.filename, "Failed"); continue; }
    const res = await uploadRecording(entry.blob, r);
    if (res.ok) { ok++; updateRecordingStatus(r.filename, "Uploaded"); }
    else { failed++; updateRecordingStatus(r.filename, "Failed"); }
  }
  renderDashboard();
  toast(`Synced ${ok}/${pending.length}` + (skipped ? ` · ${skipped} missing blob` : ""),
    failed ? "error" : "success");
}

async function onUploadExisting() {
  const audio = $("uploadAudio").files[0];
  if (!audio) return toast("Pick an audio file", "error");
  let metadata;
  const metaFile = $("uploadMeta").files[0];
  if (metaFile) {
    try { metadata = JSON.parse(await metaFile.text()); }
    catch { return toast("Invalid JSON sidecar", "error"); }
  } else {
    metadata = {
      filename: audio.name,
      annotator_id: profile.annotatorId || "anonymous",
      label: "mixed",
    };
  }
  $("uploadStatus").textContent = "Uploading…";
  const res = await uploadRecording(audio, metadata);
  $("uploadStatus").textContent = res.ok ? "Done." : `Failed: ${res.error}`;
  toast(res.ok ? "Uploaded" : "Upload failed", res.ok ? "success" : "error");
  if (res.ok) addRecording(metadata, "Uploaded");
}

// ============================== BOOT ==============================
initSetup();
initRecord();
initAnnotate();
initDashboard();
show(profile.annotatorId ? "record" : "setup");
if (profile.annotatorId) refreshGps();
