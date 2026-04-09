"use client";

import Link from "next/link";
import { useSoundTag } from "../providers/soundtag-context";

function Row({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="summary-detail-row">
      <span className="summary-detail-label">{label}</span>
      <span className={`summary-detail-value${mono ? " mono" : ""}`}>{value}</span>
    </div>
  );
}

export default function SummaryPage() {
  const { lastSavedMeta, lastSaveStatus, labels, playbackUrl } = useSoundTag();

  if (!lastSavedMeta) {
    return (
      <section className="view" id="view-summary">
        <header className="topbar">
          <div className="icon-btn-spacer"></div>
          <div className="topbar-title">Summary</div>
          <div className="icon-btn-spacer"></div>
        </header>
        <main className="summary-main">
          <div className="empty">No recording to show</div>
          <Link className="btn btn-primary btn-cta" href="/record">
            Back to Record
          </Link>
        </main>
      </section>
    );
  }

  const m = lastSavedMeta;
  const labelObj = labels.find((l: any) => l.id === m.label);
  const labelDisplay = labelObj ? `${labelObj.emoji} ${labelObj.text}` : m.label;
  const statusClass =
    lastSaveStatus === "Uploaded"
      ? "pill-success"
      : lastSaveStatus === "Failed"
        ? "pill-error"
        : lastSaveStatus === "Pending"
          ? "pill-warn"
          : "pill-muted";

  const localTime = m.started_at_local
    ? new Date(m.started_at_local).toLocaleString()
    : "—";

  const gps =
    m.latitude != null && m.longitude != null
      ? `${m.latitude.toFixed(4)}, ${m.longitude.toFixed(4)}`
      : "Not available";
  const gpsAcc =
    m.location_accuracy_m != null ? `\u00b1${Math.round(m.location_accuracy_m)}m` : "";

  return (
    <section className="view" id="view-summary">
      <header className="topbar">
        <Link className="icon-btn" href="/record">
          <svg
            viewBox="0 0 24 24"
            width="20"
            height="20"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
        </Link>
        <div className="topbar-title">Recording Saved</div>
        <div className="icon-btn-spacer"></div>
      </header>

      <main className="summary-main">
        {/* Status banner */}
        <div className="summary-hero-card">
          <div className="summary-hero-icon">
            <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#00E5A0" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
          </div>
          <div className="summary-hero-label mono">{m.filename}</div>
          <span className={`pill ${statusClass}`}>{lastSaveStatus}</span>
        </div>

        {/* Annotation */}
        <div className="summary-card">
          <div className="weight">Annotation</div>
          <Row label="Label" value={labelDisplay} />
          <Row label="Noise pollution" value={m.is_noise ? "Yes" : "No"} />
          <Row label="Severity" value={`${m.severity} (${m.severity_score}/5)`} />
          <Row label="Environment" value={m.environment} />
          <Row label="Location context" value={m.location_context} />
          {m.notes ? <Row label="Notes" value={m.notes} /> : null}
        </div>

        {/* Recording details */}
        <div className="summary-card">
          <div className="weight">Recording</div>
          <Row label="Duration" value={`${m.duration_seconds}s`} />
          {m.peak_db != null ? <Row label="Peak level" value={`${m.peak_db} dB`} /> : null}
          <Row label="Encoding" value={`${m.encoding} / ${m.sample_rate_hz}Hz / ${m.channels}ch`} mono />
          <Row label="Recorded at" value={localTime} />
        </div>

        {/* Location */}
        <div className="summary-card">
          <div className="weight">Location</div>
          <Row label="GPS" value={`${gps}${gpsAcc ? ` ${gpsAcc}` : ""}`} mono />
        </div>

        {/* Device */}
        <div className="summary-card">
          <div className="weight">Device</div>
          <Row label="Platform" value={m.device_model || "web"} />
          <Row label="App version" value={m.app_version} mono />
          <Row label="Annotator" value={m.annotator_id} mono />
        </div>

        {/* Playback */}
        {playbackUrl ? (
          <div className="summary-card">
            <div className="weight">Playback</div>
            <audio src={playbackUrl} controls style={{ width: "100%" }} />
          </div>
        ) : null}

        {/* Actions */}
        <div className="action-stack">
          <Link className="btn btn-primary btn-cta" href="/record">
            Record Another
          </Link>
          <Link className="btn btn-cta" href="/dashboard">
            View Dashboard
          </Link>
        </div>
      </main>
    </section>
  );
}
