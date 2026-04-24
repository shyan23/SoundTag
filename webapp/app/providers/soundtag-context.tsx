"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useRouter } from "next/navigation";
import { Recorder, formatTimer, MAX_DURATION_MS } from "../../recorder.js";
import { DbMeter } from "../../db_meter.js";
import { getLocation } from "../../location.js";
import { buildMetadata, saveLocally } from "../../metadata.js";
import { uploadRecording } from "../../upload.js";
import { splitAudio, CHUNK_SECONDS } from "../../split_audio.js";
import {
  LABELS,
  SEVERITY_SCORES,
  ENVIRONMENTS,
  CONTEXTS,
  defaultSelections,
  autoFilename,
} from "../../annotate.js";
import {
  loadRecordings,
  addRecording,
  loadProfile,
  saveProfile,
  updateRecordingStatus,
  removeRecording,
  todayCount,
} from "../../store.js";
import { putBlob, getBlob, deleteBlob } from "../../db.js";
import { GOOGLE_CLIENT_ID } from "../../config.js";

type Loc = { lat: number; lon: number; acc: number } | null;
type SaveMode = "upload" | "local";
type TabType = "recordings" | "stats";

type Ctx = {
  profile: { name?: string; annotatorId?: string };
  setProfileValues: (name: string, annotatorId: string) => void;
  recording: boolean;
  timerText: string;
  dbReading: string;
  gpsLabel: string;
  gpsAcc: string;
  waveform: number[];
  recordSub: string;
  todayText: string;
  statusId: string;
  startOrStop: () => Promise<void>;
  lastStartError: string;
  runAudioDiagnostics: () => Promise<void>;
  diagnosticsText: string;
  diagnosticsRunning: boolean;
  eventLog: string[];
  lastSavedMeta: any | null;
  lastSaveStatus: string;
  lastInfoText: string;
  lastGpsText: string;
  playbackUrl: string;
  playbackDuration: number;
  selections: {
    label: string;
    isNoise: boolean;
    severity: "low" | "medium" | "high";
    score: number;
    environment: "outdoor" | "indoor";
    locationContext: string;
  };
  setSelection: (key: string, value: string | number | boolean) => void;
  filename: string;
  setFilename: (v: string) => void;
  notes: string;
  setNotes: (v: string) => void;
  saveFootnote: string;
  onSave: (mode: SaveMode) => Promise<void>;
  recordings: any[];
  refreshDashboard: () => void;
  retryUpload: (r: any) => Promise<void>;
  syncAllPending: () => Promise<void>;
  removeOne: (filename: string) => Promise<void>;
  downloadOne: (filename: string) => Promise<void>;
  uploadExisting: (audio: File | null, meta: File | null) => Promise<string>;
  dashboardTab: TabType;
  setDashboardTab: (t: TabType) => void;
  statsOverview: string;
  balanceWarning: string;
  barData: Array<{ id: string; label: string; count: number; pct: number }>;
  toast: { text: string; kind: "" | "success" | "error" };
  clearToast: () => void;
  labels: any[];
  environments: string[];
  contexts: string[];
};

const SoundTagContext = createContext<Ctx | null>(null);

export function SoundTagProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const recorderRef = useRef<any>(null);
  const dbMeterRef = useRef<any>(null);
  const timerRef = useRef<number | null>(null);
  const toastTimerRef = useRef<number | null>(null);

  const [profile, setProfile] = useState<any>({});
  const [recordings, setRecordings] = useState<any[]>([]);
  const [recording, setRecording] = useState(false);
  const [timerText, setTimerText] = useState("00:00");
  const [dbReading, setDbReading] = useState("— dB");
  const [waveform, setWaveform] = useState<number[]>(Array.from({ length: 14 }, () => 8));
  const [loc, setLoc] = useState<Loc>(null);
  const [peakDb, setPeakDb] = useState(0);
  const [lastRecording, setLastRecording] = useState<any>(null);
  const [lastInfoText, setLastInfoText] = useState("");
  const [lastGpsText, setLastGpsText] = useState("No GPS");
  const [playbackUrl, setPlaybackUrl] = useState("");
  const [playbackDuration, setPlaybackDuration] = useState(0);
  const [filename, setFilename] = useState("");
  const [notes, setNotes] = useState("");
  const [dashboardTab, setDashboardTab] = useState<TabType>("recordings");
  const [toast, setToast] = useState<{ text: string; kind: "" | "success" | "error" }>({
    text: "",
    kind: "",
  });
  const [lastStartError, setLastStartError] = useState("");
  const [diagnosticsText, setDiagnosticsText] = useState("");
  const [diagnosticsRunning, setDiagnosticsRunning] = useState(false);
  const [eventLog, setEventLog] = useState<string[]>([]);
  const [starting, setStarting] = useState(false);
  const [lastSavedMeta, setLastSavedMeta] = useState<any | null>(null);
  const [lastSaveStatus, setLastSaveStatus] = useState("");
  const [selections, setSelections] = useState<any>(defaultSelections());

  const logEvent = useCallback((message: string) => {
    const ts = new Date().toLocaleTimeString();
    setEventLog((prev) => [`[${ts}] ${message}`, ...prev].slice(0, 40));
  }, []);

  const setToastMessage = useCallback((text: string, kind: "" | "success" | "error" = "") => {
    setToast({ text, kind });
    if (toastTimerRef.current) window.clearTimeout(toastTimerRef.current);
    toastTimerRef.current = window.setTimeout(() => setToast({ text: "", kind: "" }), 2500);
  }, []);

  const refreshGps = useCallback(async () => {
    const value = await getLocation();
    setLoc(value);
  }, []);

  const refreshDashboard = useCallback(() => {
    setRecordings(loadRecordings());
  }, []);

  useEffect(() => {
    recorderRef.current = new Recorder();
    setProfile(loadProfile());
    setRecordings(loadRecordings());
    refreshGps();
    return () => {
      if (timerRef.current) window.clearInterval(timerRef.current);
      if (dbMeterRef.current) dbMeterRef.current.dispose();
      if (playbackUrl) URL.revokeObjectURL(playbackUrl);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    setFilename((prev) => (prev ? prev : autoFilename(selections.label)));
  }, [selections.label]);

  const startOrStop = useCallback(async () => {
    if (!recorderRef.current) return;
    if (starting) {
      logEvent("Ignored click: start already in progress");
      return;
    }
    logEvent(`Record button clicked (recording=${recording})`);
    if (!recording) {
      try {
        setStarting(true);
        logEvent("Attempting to start recorder");
        const stream = await recorderRef.current.start();
        logEvent("Recorder start succeeded");
        setLastStartError("");
        setRecording(true);
        setPeakDb(0);
        setTimerText("00:00");
        setDbReading("0.0 dB");
        dbMeterRef.current = new DbMeter(stream);
        timerRef.current = window.setInterval(() => {
          const ms = recorderRef.current.elapsedMs();
          setTimerText(formatTimer(ms));
          if (dbMeterRef.current) {
            const db = dbMeterRef.current.sample();
            setPeakDb((p) => (db > p ? db : p));
            setDbReading(`${db.toFixed(1)} dB`);
            setWaveform((old) =>
              old.map((_, i) => {
                const variance = 0.4 + 0.6 * Math.abs(Math.sin(Date.now() / 120 + i));
                return 8 + Math.min(1, db / 40) * 24 * variance;
              }),
            );
          }
          if (ms >= MAX_DURATION_MS) void startOrStop();
        }, 100);
        void refreshGps();
        setToastMessage("Recording started", "success");
      } catch (e: any) {
        logEvent(`Recorder start failed: ${e?.name || "Error"} ${e?.message || ""}`.trim());
        setLastStartError(e?.name ? `${e.name}: ${e?.message || ""}` : e?.message || "unknown error");
        if (e?.message === "media-devices-unavailable") {
          setToastMessage("Recording not supported in this browser/session", "error");
        } else if (e?.name === "NotAllowedError") {
          setToastMessage("Microphone permission denied", "error");
        } else {
          setToastMessage(`Could not start recording: ${e?.message || "unknown error"}`, "error");
        }
      } finally {
        setStarting(false);
      }
      return;
    }

    try {
      logEvent("Attempting to stop recorder");
      const result = await recorderRef.current.stop();
      logEvent("Recorder stop succeeded");
      if (timerRef.current) window.clearInterval(timerRef.current);
      timerRef.current = null;
      if (dbMeterRef.current) {
        dbMeterRef.current.dispose();
        dbMeterRef.current = null;
      }
      setRecording(false);
      const record = { ...result, peakDb };
      setLastRecording(record);
      const dur = Math.round(record.durationMs / 1000);
      const ts = new Date(record.startedAt).toLocaleTimeString();
      setLastInfoText(`${dur}s · ${ts}`);
      const gps = loc
        ? `${loc.lat.toFixed(4)}, ${loc.lon.toFixed(4)} ±${Math.round(loc.acc)}m`
        : "No GPS";
      setLastGpsText(gps);
      const url = URL.createObjectURL(record.blob);
      setPlaybackUrl((old) => {
        if (old) URL.revokeObjectURL(old);
        return url;
      });
      setPlaybackDuration(record.durationMs / 1000);
      setFilename(autoFilename(selections.label));
      setNotes("");
      router.push("/annotate");
    } catch (e: any) {
      logEvent(`Recorder stop failed: ${e?.name || "Error"} ${e?.message || ""}`.trim());
      if (e?.message === "min-duration") setToastMessage("Minimum 5 seconds", "error");
    }
  }, [loc, logEvent, peakDb, recording, refreshGps, router, selections.label, setToastMessage, starting]);

  const runAudioDiagnostics = useCallback(async () => {
    setDiagnosticsRunning(true);
    const lines: string[] = [];
    const push = (k: string, v: string) => lines.push(`${k}: ${v}`);
    try {
      logEvent("Running audio diagnostics");
      push("timestamp", new Date().toISOString());
      push("userAgent", navigator.userAgent);
      push("secureContext", String(window.isSecureContext));
      push(
        "mediaDevices.getUserMedia",
        String(Boolean(navigator?.mediaDevices?.getUserMedia)),
      );
      push(
        "mediaDevices.enumerateDevices",
        String(Boolean(navigator?.mediaDevices?.enumerateDevices)),
      );

      if ((navigator as any).permissions?.query) {
        try {
          const perm = await (navigator as any).permissions.query({ name: "microphone" });
          push("permission.microphone", perm.state);
        } catch (e: any) {
          push("permission.microphone", `query-failed (${e?.message || "unknown"})`);
        }
      } else {
        push("permission.microphone", "permissions API unsupported");
      }

      const devicesBefore = await navigator.mediaDevices.enumerateDevices();
      const inputsBefore = devicesBefore.filter((d) => d.kind === "audioinput");
      push("audioinput.count.beforePermission", String(inputsBefore.length));
      push(
        "audioinput.beforePermission",
        inputsBefore
          .map((d, i) => `${i + 1}) ${d.label || "(label hidden)"} [id=${d.deviceId || "n/a"}]`)
          .join(" | ") || "none",
      );

      let testStream: MediaStream | null = null;
      try {
        testStream = await navigator.mediaDevices.getUserMedia({ audio: true });
        const track = testStream.getAudioTracks()[0];
        const settings = track?.getSettings?.() || {};
        push("testCapture", "success");
        push("activeTrack.label", track?.label || "unknown");
        push("activeTrack.readyState", track?.readyState || "unknown");
        push("activeTrack.deviceId", String((settings as any).deviceId || "n/a"));
        push("activeTrack.sampleRate", String((settings as any).sampleRate || "n/a"));
        push("activeTrack.channelCount", String((settings as any).channelCount || "n/a"));
      } catch (e: any) {
        push("testCapture", `failed (${e?.name || "Error"}: ${e?.message || "unknown"})`);
      } finally {
        testStream?.getTracks().forEach((t) => t.stop());
      }

      const devicesAfter = await navigator.mediaDevices.enumerateDevices();
      const inputsAfter = devicesAfter.filter((d) => d.kind === "audioinput");
      push("audioinput.count.afterPermission", String(inputsAfter.length));
      push(
        "audioinput.afterPermission",
        inputsAfter
          .map((d, i) => `${i + 1}) ${d.label || "(label hidden)"} [id=${d.deviceId || "n/a"}]`)
          .join(" | ") || "none",
      );

      if (lastStartError) {
        push("lastStartError", lastStartError);
      }
    } catch (e: any) {
      push("diagnostics.error", `${e?.name || "Error"}: ${e?.message || "unknown"}`);
    } finally {
      setDiagnosticsText(lines.join("\n"));
      setDiagnosticsRunning(false);
      logEvent("Audio diagnostics completed");
    }
  }, [lastStartError, logEvent]);

  const setSelection = useCallback((key: string, value: string | number | boolean) => {
    setSelections((prev: any) => {
      const next = { ...prev, [key]: value };
      if (key === "severity") {
        const sevKey = value as keyof typeof SEVERITY_SCORES;
        next.score = SEVERITY_SCORES[sevKey];
      }
      return next;
    });
  }, []);

  const setProfileValues = useCallback((name: string, annotatorId: string) => {
    const value = { name: name.trim(), annotatorId: annotatorId.trim().toLowerCase().replace(/\s+/g, "-") };
    setProfile(value);
    saveProfile(value);
  }, []);

  const currentMetadata = useCallback(() => {
    if (!lastRecording) return null;
    const base = (filename || autoFilename(selections.label)).trim();
    const meta: any = buildMetadata({
      filenameBase: base,
      mimeType: lastRecording.mimeType,
      startedAt: lastRecording.startedAt,
      durationMs: lastRecording.durationMs,
      location: loc,
      annotation: {
        label: selections.label,
        severity: selections.severity,
        environment: selections.environment,
        locationContext: selections.locationContext,
        isNoise: selections.isNoise,
        annotatorId: profile.annotatorId,
        notes: notes.trim(),
      },
    });
    meta.severity_score = selections.score;
    meta.peak_db = Number(lastRecording.peakDb.toFixed(1));
    return meta;
  }, [filename, lastRecording, loc, notes, profile.annotatorId, selections]);

  const onSave = useCallback(
    async (mode: SaveMode) => {
      if (!lastRecording) {
        setToastMessage("No recording", "error");
        return;
      }
      const meta = currentMetadata();
      if (!meta) return;

      const durationSec = Math.round(lastRecording.durationMs / 1000);
      const needsSplit = durationSec > CHUNK_SECONDS;

      try {
        await putBlob(meta.filename, lastRecording.blob, lastRecording.mimeType);
      } catch (e: any) {
        setToastMessage(`Could not persist blob: ${e.message}`, "error");
      }

      saveLocally(lastRecording.blob, meta);
      let status = "Local";

      if (mode === "upload" && GOOGLE_CLIENT_ID) {
        if (!needsSplit) {
          setToastMessage("Uploading…");
          const res = await uploadRecording(lastRecording.blob, meta);
          status = res.ok ? "Uploaded" : "Failed";
          setToastMessage(res.ok ? "Uploaded" : `Upload failed: ${res.error}`, res.ok ? "success" : "error");
        } else {
          setToastMessage("Splitting & uploading…");
          try {
            const chunks = await splitAudio(lastRecording.blob);
            const baseName = meta.filename.replace(/\.[^.]+$/, "");
            let uploaded = 0;
            let failed = 0;
            for (const chunk of chunks) {
              const chunkFilename = `${baseName}_chunk${String(chunk.index + 1).padStart(3, "0")}.wav`;
              const chunkMeta = {
                ...meta,
                filename: chunkFilename,
                duration_seconds: Math.round(chunk.durationMs / 1000),
                encoding: "PCM",
                chunk_index: chunk.index,
                chunk_total: chunks.length,
                original_filename: meta.filename,
                original_duration_seconds: durationSec,
              };
              const res = await uploadRecording(chunk.blob, chunkMeta);
              if (res.ok) uploaded++;
              else failed++;
            }
            status = failed === 0 ? "Uploaded" : "Failed";
            setToastMessage(
              failed === 0
                ? `Uploaded ${uploaded} chunks`
                : `${uploaded}/${chunks.length} chunks uploaded, ${failed} failed`,
              failed === 0 ? "success" : "error",
            );
          } catch (e: any) {
            status = "Failed";
            setToastMessage(`Split failed: ${e.message}`, "error");
          }
        }
      } else if (mode === "upload" && !GOOGLE_CLIENT_ID) {
        status = "Pending";
        setToastMessage("Saved (Drive not configured)", "success");
      } else {
        setToastMessage("Saved locally", "success");
      }

      addRecording(meta, status);
      refreshDashboard();
      setLastSavedMeta(meta);
      setLastSaveStatus(status);
      router.push("/summary");
    },
    [currentMetadata, lastRecording, refreshDashboard, router, setToastMessage],
  );

  const retryUpload = useCallback(
    async (r: any) => {
      const entry = await getBlob(r.filename);
      if (!entry) {
        setToastMessage("Blob not in cache — re-record to upload", "error");
        updateRecordingStatus(r.filename, "Failed");
        refreshDashboard();
        return;
      }
      if (!GOOGLE_CLIENT_ID) {
        setToastMessage("Drive not configured", "error");
        return;
      }
      setToastMessage("Uploading…");
      const res = await uploadRecording(entry.blob, r);
      updateRecordingStatus(r.filename, res.ok ? "Uploaded" : "Failed");
      setToastMessage(res.ok ? "Uploaded" : `Upload failed: ${res.error}`, res.ok ? "success" : "error");
      refreshDashboard();
    },
    [refreshDashboard, setToastMessage],
  );

  const syncAllPending = useCallback(async () => {
    if (!GOOGLE_CLIENT_ID) return setToastMessage("Drive not configured", "error");
    const list = loadRecordings();
    const pending = list.filter((r: any) => r.status === "Pending" || r.status === "Failed");
    if (pending.length === 0) return setToastMessage("Nothing to sync");
    setToastMessage(`Syncing ${pending.length}…`);
    let ok = 0;
    let skipped = 0;
    let failed = 0;
    for (const r of pending) {
      const entry = await getBlob(r.filename);
      if (!entry) {
        skipped++;
        updateRecordingStatus(r.filename, "Failed");
        continue;
      }
      const res = await uploadRecording(entry.blob, r);
      if (res.ok) {
        ok++;
        updateRecordingStatus(r.filename, "Uploaded");
      } else {
        failed++;
        updateRecordingStatus(r.filename, "Failed");
      }
    }
    refreshDashboard();
    setToastMessage(
      `Synced ${ok}/${pending.length}${skipped ? ` · ${skipped} missing blob` : ""}`,
      failed ? "error" : "success",
    );
  }, [refreshDashboard, setToastMessage]);

  const removeOne = useCallback(
    async (filenameToDelete: string) => {
      await deleteBlob(filenameToDelete);
      removeRecording(filenameToDelete);
      refreshDashboard();
      setToastMessage("Deleted");
    },
    [refreshDashboard, setToastMessage],
  );

  const downloadOne = useCallback(
    async (filenameToDownload: string) => {
      const entry = await getBlob(filenameToDownload);
      if (!entry) {
        setToastMessage("Blob not in cache", "error");
        return;
      }
      const a = document.createElement("a");
      a.href = URL.createObjectURL(entry.blob);
      a.download = filenameToDownload;
      a.click();
    },
    [setToastMessage],
  );

  const uploadExisting = useCallback(
    async (audio: File | null, meta: File | null) => {
      if (!audio) return "Pick an audio file";
      let metadata: any;
      if (meta) {
        try {
          metadata = JSON.parse(await meta.text());
        } catch {
          return "Invalid JSON sidecar";
        }
      } else {
        metadata = {
          filename: audio.name,
          annotator_id: profile.annotatorId || "anonymous",
          label: "mixed",
        };
      }
      const res = await uploadRecording(audio, metadata);
      if (res.ok) addRecording(metadata, "Uploaded");
      refreshDashboard();
      setToastMessage(res.ok ? "Uploaded" : "Upload failed", res.ok ? "success" : "error");
      return res.ok ? "Done." : `Failed: ${res.error}`;
    },
    [profile.annotatorId, refreshDashboard, setToastMessage],
  );

  const statsOverview = useMemo(() => {
    const totalSec = recordings.reduce((s, r) => s + (r.duration_seconds || 0), 0);
    const mins = Math.floor(totalSec / 60);
    const hrs = Math.floor(mins / 60);
    return `${recordings.length} recordings · ${hrs}h ${mins % 60}m total`;
  }, [recordings]);

  const barData = useMemo(() => {
    const counts: Record<string, number> = {};
    LABELS.forEach((l: any) => (counts[l.id] = 0));
    recordings.forEach((r: any) => {
      if (counts[r.label] != null) counts[r.label]++;
    });
    const max = Math.max(1, ...Object.values(counts));
    return LABELS.map((l: any) => ({
      id: l.id,
      label: `${l.emoji} ${l.text}`,
      count: counts[l.id] || 0,
      pct: ((counts[l.id] || 0) / max) * 100,
    }));
  }, [recordings]);

  const balanceWarning = useMemo(() => {
    const low = barData.filter((x) => x.count < 5).map((x) => x.label);
    if (low.length === 0 || recordings.length === 0) return "";
    return `Collect more: ${low.join(", ")}. Each label needs at least 5 clips.`;
  }, [barData, recordings.length]);

  const value: Ctx = {
    profile,
    setProfileValues,
    recording,
    timerText,
    dbReading,
    gpsLabel: loc ? `${loc.lat.toFixed(4)}, ${loc.lon.toFixed(4)}` : "GPS unavailable",
    gpsAcc: loc ? `±${Math.round(loc.acc)}m` : "",
    waveform,
    recordSub: recording ? "Recording live" : "Tap to start recording",
    todayText: `Today: ${todayCount(recordings)} recordings`,
    statusId: profile.annotatorId || "—",
    startOrStop,
    lastStartError,
    runAudioDiagnostics,
    diagnosticsText,
    diagnosticsRunning,
    eventLog,
    lastSavedMeta,
    lastSaveStatus,
    lastInfoText,
    lastGpsText,
    playbackUrl,
    playbackDuration,
    selections,
    setSelection,
    filename,
    setFilename,
    notes,
    setNotes,
    saveFootnote: GOOGLE_CLIENT_ID
      ? "Saves to the shared SoundTag Drive folder"
      : "Drive not configured — save locally",
    onSave,
    recordings,
    refreshDashboard,
    retryUpload,
    syncAllPending,
    removeOne,
    downloadOne,
    uploadExisting,
    dashboardTab,
    setDashboardTab,
    statsOverview,
    balanceWarning,
    barData,
    toast,
    clearToast: () => setToast({ text: "", kind: "" }),
    labels: LABELS,
    environments: ENVIRONMENTS,
    contexts: CONTEXTS,
  };

  return <SoundTagContext.Provider value={value}>{children}</SoundTagContext.Provider>;
}

export function useSoundTag() {
  const ctx = useContext(SoundTagContext);
  if (!ctx) throw new Error("useSoundTag must be used inside SoundTagProvider");
  return ctx;
}
