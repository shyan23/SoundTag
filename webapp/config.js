// Google Drive upload config — matches the Android DriveUploader.
// Recordings are written to: SoundTag/{annotator_id}/  (created on demand).
//
// GOOGLE_CLIENT_ID is issued by Google Cloud Console (OAuth 2.0 "Web application" client).
// Set it via the NEXT_PUBLIC_GOOGLE_CLIENT_ID environment variable:
//   - Local dev: add to webapp/.env.local
//   - Vercel:    Project → Settings → Environment Variables → NEXT_PUBLIC_GOOGLE_CLIENT_ID
// Next.js inlines NEXT_PUBLIC_* at build time so it reaches the browser bundle.
// The client ID is not a secret (it's visible to browsers by design); the client_secret
// from the JSON is NOT used by the GIS browser token flow and must never be put here.
// When unset, upload buttons are disabled and local save keeps working.
export const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || null;

// Drive scope — same as DriveUploader.kt (DriveScopes.DRIVE).
export const DRIVE_SCOPE = "https://www.googleapis.com/auth/drive";

// Root folder name in Drive. Matches APP_NAME in DriveUploader.kt.
// Used only when DRIVE_TARGET_FOLDER_ID is null (fallback: create SoundTag/ under user's Drive root).
export const DRIVE_ROOT_FOLDER = "SoundTag";

// Shared target folder ID. When set, uploads go into {DRIVE_TARGET_FOLDER_ID}/{annotator_id}/
// instead of creating a SoundTag/ folder under the user's Drive root.
// Folder: https://drive.google.com/drive/folders/1Hv7bnjMLJ1189DgXfLVUhS5m8AA_AnHx
// The signed-in Google account must have write access to this folder.
export const DRIVE_TARGET_FOLDER_ID = "1Hv7bnjMLJ1189DgXfLVUhS5m8AA_AnHx";
