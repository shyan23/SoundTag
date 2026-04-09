"use client";

import Link from "next/link";
import { useSoundTag } from "../providers/soundtag-context";

export default function RecordPage() {
  const {
    recording,
    timerText,
    dbReading,
    gpsLabel,
    gpsAcc,
    waveform,
    todayText,
    statusId,
    startOrStop,
    lastStartError,
    runAudioDiagnostics,
    diagnosticsText,
    diagnosticsRunning,
    eventLog,
  } = useSoundTag();

  return (
    <section className={`view ${recording ? "recording" : ""}`} id="view-record">
      <header className="topbar">
        <div className="status-pill">
          <span className={`status-dot ${recording ? "rec" : ""}`}></span>
          <span className="mono">{statusId}</span>
        </div>
        <div className="topbar-title">SoundTag</div>
        <Link className="icon-btn" href="/dashboard" title="Dashboard">
          <svg
            viewBox="0 0 24 24"
            width="18"
            height="18"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
          >
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </Link>
      </header>
      <div className={`state-strip ${recording ? "rec" : ""}`}></div>

      <main className="record-main">
        <div className="mic-ring-outer">
          <div className="mic-ring-inner">
            <button
              className="mic-btn"
              onClick={() => void startOrStop()}
            >
              {!recording ? (
                <div className="mic-content">
                  <svg
                    viewBox="0 0 24 24"
                    width="38"
                    height="38"
                    fill="none"
                    stroke="#00E5A0"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" />
                    <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
                    <line x1="12" y1="19" x2="12" y2="23" />
                    <line x1="8" y1="23" x2="16" y2="23" />
                  </svg>
                </div>
              ) : (
                <div className="mic-content">
                  <svg
                    viewBox="0 0 24 24"
                    width="30"
                    height="30"
                    fill="none"
                    stroke="#EF4444"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" />
                    <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
                  </svg>
                  <div className="mic-timer mono">{timerText}</div>
                  <div className="mic-db">{dbReading}</div>
                </div>
              )}
            </button>
          </div>
        </div>

        {!recording ? (
          <div className="record-sub">Tap to start recording</div>
        ) : (
          <div className="record-active-block">
            <div className="rec-indicator">
              <span className="rec-dot"></span>
              <span>Recording live</span>
            </div>
            <div className="waveform">
              {waveform.map((h, i) => (
                <div key={i} className="wave-bar" style={{ height: `${h}px` }}></div>
              ))}
            </div>
          </div>
        )}
      </main>

      <footer className="record-footer">
        <div className="gps-row">
          <svg
            viewBox="0 0 24 24"
            width="14"
            height="14"
            fill="none"
            stroke="#00E5A0"
            strokeWidth="2.2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
            <circle cx="12" cy="10" r="3" />
          </svg>
          <span className="mono">{gpsLabel}</span>
          {gpsAcc ? <span className="pill pill-accent">{gpsAcc}</span> : null}
        </div>
        <div className="secondary-row">{todayText}</div>
        <button
          className="btn btn-sm"
          style={{ marginTop: 8, maxWidth: 280 }}
          onClick={() => void runAudioDiagnostics()}
          disabled={diagnosticsRunning}
        >
          {diagnosticsRunning ? "Running audio diagnostics..." : "Run audio diagnostics"}
        </button>
        {lastStartError ? (
          <div className="muted small mono" style={{ marginTop: 8, textAlign: "center", maxWidth: 420 }}>
            Last start error: {lastStartError}
          </div>
        ) : null}
        {diagnosticsText ? (
          <pre
            className="mono small"
            style={{
              marginTop: 10,
              width: "100%",
              maxWidth: 460,
              maxHeight: 180,
              overflow: "auto",
              background: "var(--surface)",
              border: "1px solid var(--border)",
              borderRadius: 10,
              padding: 10,
              color: "var(--muted)",
              textAlign: "left",
              whiteSpace: "pre-wrap",
            }}
          >
            {diagnosticsText}
          </pre>
        ) : null}
        {eventLog.length > 0 ? (
          <pre
            className="mono small"
            style={{
              marginTop: 10,
              width: "100%",
              maxWidth: 460,
              maxHeight: 180,
              overflow: "auto",
              background: "var(--surface)",
              border: "1px solid var(--border)",
              borderRadius: 10,
              padding: 10,
              color: "var(--muted)",
              textAlign: "left",
              whiteSpace: "pre-wrap",
            }}
          >
            {eventLog.join("\n")}
          </pre>
        ) : null}
      </footer>
    </section>
  );
}
