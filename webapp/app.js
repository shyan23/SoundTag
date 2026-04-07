import { getLocation, formatGps } from "./location.js";
import { Recorder, formatTimer, MAX_DURATION_MS } from "./recorder.js";
import { buildMetadata, saveLocally } from "./metadata.js";
import {
  LABELS, SEVERITY, ENVIRONMENTS, CONTEXTS,
  defaultSelections, renderChipGroup, autoFilename,
} from "./annotate.js";

const $ = id => document.getElementById(id);
const gpsEl = $("gpsBadge");
const timerEl = $("timer");
const stateEl = $("stateText");
const recordBtn = $("recordBtn");
const stopBtn = $("stopBtn");
const preview = $("preview");
const snackbar = $("snackbar");
const annotateCard = $("annotateCard");
const filenameInput = $("filename");
const annotatorInput = $("annotator");
const notesInput = $("notes");
const isNoiseInput = $("isNoise");

const recorder = new Recorder();
let timerInterval = null;
let currentLocation = null;
let lastRecording = null; // { blob, mimeType, startedAt, durationMs }
const selections = defaultSelections();

function toast(msg, kind = "") {
  snackbar.textContent = msg;
  snackbar.className = "snackbar " + kind;
  snackbar.hidden = false;
  clearTimeout(toast._t);
  toast._t = setTimeout(() => (snackbar.hidden = true), 2500);
}

function renderAllChips() {
  renderChipGroup($("labelChips"), LABELS, selections.label, v => {
    selections.label = v;
    filenameInput.value = autoFilename(v);
    renderAllChips();
  });
  renderChipGroup($("severityChips"), SEVERITY, selections.severity, v => {
    selections.severity = v; renderAllChips();
  });
  renderChipGroup($("envChips"), ENVIRONMENTS, selections.environment, v => {
    selections.environment = v; renderAllChips();
  });
  renderChipGroup($("ctxChips"), CONTEXTS, selections.locationContext, v => {
    selections.locationContext = v; renderAllChips();
  });
}

async function refreshGps() {
  gpsEl.textContent = "GPS: fetching\u2026";
  currentLocation = await getLocation();
  gpsEl.textContent = formatGps(currentLocation);
}

function tick() {
  const ms = recorder.elapsedMs();
  timerEl.textContent = formatTimer(ms);
  if (ms >= MAX_DURATION_MS) stopRecording();
}

async function startRecording() {
  try {
    await recorder.start();
    stateEl.textContent = "Recording";
    recordBtn.disabled = true;
    stopBtn.disabled = false;
    preview.hidden = true;
    annotateCard.hidden = true;
    timerInterval = setInterval(tick, 200);
    refreshGps();
  } catch (e) {
    toast("Microphone permission denied", "error");
  }
}

async function stopRecording() {
  try {
    const result = await recorder.stop();
    clearInterval(timerInterval);
    stateEl.textContent = "Stopped";
    recordBtn.disabled = false;
    stopBtn.disabled = true;
    preview.src = URL.createObjectURL(result.blob);
    preview.hidden = false;
    lastRecording = result;
    annotateCard.hidden = false;
    if (!filenameInput.value) filenameInput.value = autoFilename(selections.label);
  } catch (e) {
    if (e.message === "min-duration") toast("Minimum 5 seconds", "error");
  }
}

function currentMetadata() {
  const base = (filenameInput.value || autoFilename(selections.label)).trim();
  return buildMetadata({
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
      isNoise: isNoiseInput.checked,
      annotatorId: annotatorInput.value.trim(),
      notes: notesInput.value.trim(),
    },
  });
}

function onSaveLocal() {
  if (!lastRecording) return toast("No recording", "error");
  saveLocally(lastRecording.blob, currentMetadata());
  toast("Saved locally", "success");
}

recordBtn.addEventListener("click", startRecording);
stopBtn.addEventListener("click", stopRecording);
$("saveLocalBtn").addEventListener("click", onSaveLocal);

renderAllChips();
refreshGps();
