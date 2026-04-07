// Google Drive upload config — matches the Android DriveUploader.
// Recordings are written to: SoundTag/{annotator_id}/  (created on demand).
//
// GOOGLE_CLIENT_ID is the only value you must supply yourself — it is
// issued by Google Cloud Console (OAuth 2.0 "Web application" client)
// and is not stored in this repo (Android reads it from the gitignored
// google-services.json). Create one at:
//   https://console.cloud.google.com/apis/credentials
// Add your app origin (e.g. http://localhost:8000) to "Authorized JavaScript origins".
// Leave as null to disable upload buttons and keep local save working.
export const GOOGLE_CLIENT_ID = null;

// Drive scope — same as DriveUploader.kt (DriveScopes.DRIVE).
export const DRIVE_SCOPE = "https://www.googleapis.com/auth/drive";

// Root folder name in Drive. Matches APP_NAME in DriveUploader.kt.
export const DRIVE_ROOT_FOLDER = "SoundTag";
