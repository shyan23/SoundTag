// Google Drive uploader — browser port of DriveUploader.kt.
// Uses Google Identity Services token flow + Drive v3 REST API.
// Writes audio + JSON sidecar into SoundTag/{annotator_id}/.

import { GOOGLE_CLIENT_ID, DRIVE_SCOPE, DRIVE_TARGET_FOLDER_ID } from "./config.js";

const DRIVE_FILES = "https://www.googleapis.com/drive/v3/files";
const DRIVE_UPLOAD = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart";

let accessToken = null;
let tokenClient = null;
let gisReady = null;

/** Lazily load Google Identity Services script. */
function loadGis() {
  if (gisReady) return gisReady;
  gisReady = new Promise((resolve, reject) => {
    if (window.google?.accounts?.oauth2) return resolve();
    const s = document.createElement("script");
    s.src = "https://accounts.google.com/gsi/client";
    s.async = true; s.defer = true;
    s.onload = () => resolve();
    s.onerror = () => reject(new Error("Failed to load Google Identity Services"));
    document.head.appendChild(s);
  });
  return gisReady;
}

async function ensureToken() {
  if (!GOOGLE_CLIENT_ID) throw new Error("GOOGLE_CLIENT_ID not configured in config.js");
  await loadGis();
  if (accessToken) return accessToken;
  return new Promise((resolve, reject) => {
    tokenClient = google.accounts.oauth2.initTokenClient({
      client_id: GOOGLE_CLIENT_ID,
      scope: DRIVE_SCOPE,
      callback: resp => {
        if (resp.error) return reject(new Error(resp.error));
        accessToken = resp.access_token;
        resolve(accessToken);
      },
    });
    tokenClient.requestAccessToken({ prompt: "" });
  });
}

export function signOut() {
  if (accessToken) {
    try { google.accounts.oauth2.revoke(accessToken, () => {}); } catch {}
  }
  accessToken = null;
}

async function driveFetch(url, opts = {}) {
  const r = await fetch(url, {
    ...opts,
    headers: { Authorization: `Bearer ${accessToken}`, ...(opts.headers || {}) },
  });
  if (r.status === 401) { accessToken = null; throw new Error("auth expired"); }
  if (!r.ok) throw new Error(`Drive HTTP ${r.status}: ${await r.text()}`);
  return r.json();
}

/** Multipart upload of a blob into a Drive folder. */
async function uploadBlobToFolder(blob, name, folderId, mimeType) {
  const metadata = { name, parents: [folderId] };
  const boundary = "stb_" + Math.random().toString(36).slice(2);
  const pre =
    `--${boundary}\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n` +
    `${JSON.stringify(metadata)}\r\n--${boundary}\r\nContent-Type: ${mimeType}\r\n\r\n`;
  const post = `\r\n--${boundary}--`;
  const body = new Blob([pre, blob, post], { type: `multipart/related; boundary=${boundary}` });
  const r = await fetch(DRIVE_UPLOAD, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": `multipart/related; boundary=${boundary}`,
    },
    body,
  });
  if (!r.ok) throw new Error(`Upload HTTP ${r.status}: ${await r.text()}`);
  return r.json();
}

/**
 * Upload audio blob + JSON sidecar to SoundTag/{annotator_id}/ on Drive.
 * @param {Blob} audioBlob
 * @param {object} metadata - JSON sidecar object
 * @returns {Promise<{ok: boolean, error?: string}>}
 */
export async function uploadRecording(audioBlob, metadata) {
  if (!GOOGLE_CLIENT_ID) {
    return { ok: false, error: "Google Drive not configured (set GOOGLE_CLIENT_ID in config.js)" };
  }
  if (!DRIVE_TARGET_FOLDER_ID) {
    return { ok: false, error: "DRIVE_TARGET_FOLDER_ID not set in config.js" };
  }
  try {
    await ensureToken();
    const folderId = DRIVE_TARGET_FOLDER_ID;

    const baseName = metadata.filename.replace(/\.[^.]+$/, "");
    const audioMime = audioBlob.type || "audio/webm";
    await uploadBlobToFolder(audioBlob, metadata.filename, folderId, audioMime);

    const jsonBlob = new Blob([JSON.stringify(metadata, null, 2)], { type: "application/json" });
    await uploadBlobToFolder(jsonBlob, `${baseName}.json`, folderId, "application/json");

    return { ok: true };
  } catch (e) {
    return { ok: false, error: e.message };
  }
}
