# SoundTag - Project Reference

## What This Is
Android app for collecting labeled urban noise audio data for an ML noise pollution classification project (Dhaka-focused). Records audio in background, captures GPS + metadata, lets user annotate noise type/severity, saves `.m4a` + `.json` sidecar pairs.

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3, dark theme
- **Architecture:** ViewModel + StateFlow, Coroutines, LifecycleService
- **Audio:** `MediaRecorder` (AAC/MPEG4, 44100Hz mono)
- **Location:** `play-services-location` (FusedLocationProviderClient)
- **Storage:** MediaStore API (API 29+), legacy File API fallback (API 28-)
- **SDK:** minSdk 26, targetSdk 35, Kotlin DSL build files
- **Package:** `com.soundtag`

## Critical Requirements
- `FOREGROUND_SERVICE_MICROPHONE` declared for Android 14+
- `startForeground()` must be called within 5 seconds of service start
- Location fetch wrapped in `withTimeoutOrNull(5000)` — never blocks recording
- Wake lock during recording (screen can go off)
- Min 5s / max 5min recording duration guard

## Permissions
`RECORD_AUDIO`, `ACCESS_FINE_LOCATION`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`, `WRITE_EXTERNAL_STORAGE` (API <= 28)

## Output Schema
Every `.m4a` gets an identically named `.json` sidecar in `/Music/SoundTag/`:
```json
{
  "label": "traffic",
  "filename": "traffic_001.m4a",
  "is_noise": true,
  "severity": "high",
  "severity_score": 4,
  "environment": "outdoor",
  "location_context": "roadside",
  "latitude": 23.810332,
  "longitude": 90.412521,
  "location_accuracy_m": 4.2,
  "started_at_local": "2025-11-14T14:32:10+06:00",
  "started_at_utc": "2025-11-14T08:32:10Z",
  "duration_seconds": 154,
  "sample_rate_hz": 44100,
  "channels": 1,
  "encoding": "AAC",
  "device_model": "Xiaomi Redmi Note 12",
  "android_version": "13",
  "app_version": "1.0.0",
  "annotator_id": "hemal",
  "notes": ""
}
```

## Noise Classes
| Class | Examples |
|-------|---------|
| traffic | Cars, buses, rickshaws |
| horn | Vehicle honking |
| construction | Drilling, hammering |
| industrial | Factory hum, generators |
| crowd | Market, protest |
| nature | Rain, wind, birds |
| silence | Quiet baseline (non-noise) |
| mixed | Multiple sources |

## Build Phases
1. **Project Setup & Manifest** — permissions, service declaration, notification channel, dependencies
2. **Location Helper** — `LocationHelper.kt`, suspend `getLocation()`, timeout-safe
3. **Recording Service** — `RecordingService.kt`, LifecycleService, sealed RecordingState, foreground notification with live timer, audio focus handling
4. **Metadata Writer & File Saver** — JSON builder, MediaStore save logic
5. **MainActivity & Compose UI** — permissions flow, record/stop button, timer, GPS badge, annotation dialog (noise type, severity, environment, location context, notes), filename editor, snackbar feedback
6. **Google Drive Upload** — service account upload to `SoundTag/{annotator_id}/`, upload status indicator
7. **Offline Queue & Sync** — local fallback, retry when online
8. **Dashboard** — map view, label counts, dataset balance warnings

## Key Architecture Decisions
- `RecordingService` exposes state via companion `MutableStateFlow<RecordingState>` (UI observes without binding)
- `LocationHelper` is pure data layer, no UI code
- Label extracted from filename prefix: `substringBefore('_')`, stripped of digits, lowercased, default "misc"
- Annotation fields (severity, environment, location_context, is_noise) are collected in a post-recording dialog
- Annotator ID set once in settings, persists across sessions

## Design Reference
UI designs are in `../soundtag.pen` (Pencil format). Key screens:
- Splash Screen (implemented)
- Screen 1 - Setup
- Screen 2 - Record / Screen 2b - Recording State
- Screen 3 - Annotate (Full)
- Screen 4 - Dashboard

Design system components: Button, Input, Chip, Chip/Active, Badge/Success, Badge/Pending, Badge/Failed

## Colors (from design)
- Background: `#09090B`
- Brand green: `#00E5A0`
- Surface: `#18181B`
- Border: `#27272A`
- Text primary: `#FAFAFA`
- Text secondary: `#A1A1AA`
- Success: `#22C55E`
- Warning: `#F59E0B`
- Error: `#EF4444`
