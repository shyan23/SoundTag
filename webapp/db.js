// IndexedDB wrapper for audio blob persistence.
// Keyed by filename (same key used in store.js metadata list) so the
// dashboard can re-upload a recording after a page reload.

const DB_NAME = "soundtag";
const DB_VERSION = 1;
const STORE = "blobs";

let dbPromise = null;

function openDb() {
  if (dbPromise) return dbPromise;
  dbPromise = new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE)) {
        db.createObjectStore(STORE); // key = filename, value = { blob, mimeType }
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
  return dbPromise;
}

function tx(mode) {
  return openDb().then(db => db.transaction(STORE, mode).objectStore(STORE));
}

export async function putBlob(filename, blob, mimeType) {
  const store = await tx("readwrite");
  return new Promise((resolve, reject) => {
    const r = store.put({ blob, mimeType }, filename);
    r.onsuccess = () => resolve();
    r.onerror = () => reject(r.error);
  });
}

export async function getBlob(filename) {
  const store = await tx("readonly");
  return new Promise((resolve, reject) => {
    const r = store.get(filename);
    r.onsuccess = () => resolve(r.result || null);
    r.onerror = () => reject(r.error);
  });
}

export async function deleteBlob(filename) {
  const store = await tx("readwrite");
  return new Promise((resolve, reject) => {
    const r = store.delete(filename);
    r.onsuccess = () => resolve();
    r.onerror = () => reject(r.error);
  });
}

export async function hasBlob(filename) {
  const v = await getBlob(filename);
  return v !== null;
}
