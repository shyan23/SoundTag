# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

Browser companion to the SoundTag Android app. Records audio via MediaRecorder, captures GPS + metadata, lets users annotate noise type/severity, and uploads `.webm` + `.json` sidecar pairs to Google Drive. Mirrors the Android app's data pipeline for web-based data collection.

## Commands

```bash
npm run dev      # Start dev server (Next.js, http://localhost:3000)
npm run build    # Production build
npm run lint     # ESLint via next lint
```

No test framework is configured.

## Architecture

**Next.js 15 App Router** with React 19. All pages are client-side (`"use client"`) — no SSR data fetching.

### Flow

`/` redirects to `/setup` -> `/record` -> `/annotate` -> back to `/record`; `/dashboard` accessible from record page.

### State Management

Single React context (`SoundTagProvider` in `app/providers/soundtag-context.tsx`) holds ALL app state — recording, annotations, dashboard data, toast messages. Every page consumes it via `useSoundTag()`. There is no other state management.

### Module Split (root-level `.js` files)

These are plain JS modules (not TypeScript, not React) imported by the context provider:

| File | Purpose |
|------|---------|
| `recorder.js` | `Recorder` class wrapping MediaRecorder. 5s min / 5min max duration. |
| `db_meter.js` | `DbMeter` class using Web Audio AnalyserNode. Relative dBFS, not absolute SPL. |
| `location.js` | `getLocation()` with 5s timeout, returns `{lat, lon, acc}` or null. |
| `metadata.js` | `buildMetadata()` produces JSON sidecar matching Android schema. |
| `annotate.js` | Label/severity/environment constants + DOM-based render helpers (legacy, only constants used by React). |
| `store.js` | localStorage CRUD for recording metadata + user profile. |
| `db.js` | IndexedDB wrapper for audio blob persistence across reloads. |
| `upload.js` | Google Drive upload via GIS token flow + Drive v3 REST API. |
| `config.js` | `GOOGLE_CLIENT_ID` (set to `null` to disable Drive upload). |

### Storage

- **localStorage**: Recording metadata list (`soundtag.recordings`), user profile (`soundtag.profile`)
- **IndexedDB** (`soundtag` db, `blobs` store): Audio blobs keyed by filename, survives page reloads
- **Google Drive**: `SoundTag/{annotator_id}/` folder with audio + JSON sidecar pairs

### Key Patterns

- Recording state is NOT persisted — refreshing during recording loses the clip
- Label is extracted from filename prefix: `substringBefore('_')` convention
- `annotate.js` has DOM manipulation helpers (`renderChipGroup`, etc.) from a pre-React version; only the data constants (`LABELS`, `SEVERITY_SCORES`, etc.) are used now
- The recorder tries strict audio constraints first (mono, 44100Hz, no echo cancellation), falls back to `{audio: true}`
- MIME preference order: `audio/webm;codecs=opus` > `audio/webm` > `audio/mp4`

## Styling

All styles in `styles.css` (root level), imported via `app/globals.css`. No CSS modules or Tailwind. Dark theme with CSS custom properties matching the design system colors in the parent `CLAUDE.md`.

## Google Drive Setup

Set `GOOGLE_CLIENT_ID` in `config.js` to an OAuth 2.0 Web client ID from Google Cloud Console. Add your origin to "Authorized JavaScript origins". Leave as `null` for local-only mode.
