// Lightweight persistence layer — recordings metadata in localStorage.
// Audio blobs are not persisted across reloads; the Android app uses
// MediaStore, but browsers have no equivalent file listing, so the
// dashboard shows a session + localStorage metadata history only.

const KEY_RECORDINGS = "soundtag.recordings";
const KEY_PROFILE = "soundtag.profile";

export function loadRecordings() {
  try { return JSON.parse(localStorage.getItem(KEY_RECORDINGS) || "[]"); }
  catch { return []; }
}

export function saveRecordings(list) {
  localStorage.setItem(KEY_RECORDINGS, JSON.stringify(list));
}

export function updateRecordingStatus(filename, status) {
  const list = loadRecordings();
  const r = list.find(x => x.filename === filename);
  if (r) { r.status = status; saveRecordings(list); }
  return list;
}

export function removeRecording(filename) {
  const list = loadRecordings().filter(r => r.filename !== filename);
  saveRecordings(list);
  return list;
}

export function addRecording(meta, status) {
  const list = loadRecordings();
  list.unshift({
    filename: meta.filename,
    label: meta.label,
    duration_seconds: meta.duration_seconds,
    started_at_local: meta.started_at_local,
    latitude: meta.latitude,
    longitude: meta.longitude,
    annotator_id: meta.annotator_id,
    status, // "Uploaded" | "Local" | "Pending" | "Failed"
  });
  saveRecordings(list);
  return list;
}

export function loadProfile() {
  try { return JSON.parse(localStorage.getItem(KEY_PROFILE) || "{}"); }
  catch { return {}; }
}

export function saveProfile(profile) {
  localStorage.setItem(KEY_PROFILE, JSON.stringify(profile));
}

export function todayCount(list) {
  const today = new Date().toISOString().slice(0, 10);
  return list.filter(r => (r.started_at_local || "").slice(0, 10) === today).length;
}
