"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { useSoundTag } from "../providers/soundtag-context";

export default function DashboardPage() {
  const uploadAudioRef = useRef<HTMLInputElement | null>(null);
  const uploadMetaRef = useRef<HTMLInputElement | null>(null);
  const [uploadStatus, setUploadStatus] = useState("");
  const {
    dashboardTab,
    setDashboardTab,
    recordings,
    todayText,
    syncAllPending,
    retryUpload,
    removeOne,
    downloadOne,
    uploadExisting,
    refreshDashboard,
    statsOverview,
    balanceWarning,
    barData,
  } = useSoundTag();

  useEffect(() => {
    refreshDashboard();
  }, [refreshDashboard]);

  return (
    <section className="view" id="view-dashboard">
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
        <div className="topbar-title">Dashboard</div>
        <div className="icon-btn-spacer"></div>
      </header>

      <div className="tabs">
        <button
          className={`tab ${dashboardTab === "recordings" ? "active" : ""}`}
          onClick={() => setDashboardTab("recordings")}
        >
          Recordings
        </button>
        <button
          className={`tab ${dashboardTab === "stats" ? "active" : ""}`}
          onClick={() => setDashboardTab("stats")}
        >
          Dataset Stats
        </button>
      </div>

      <main className="dashboard-main">
        {dashboardTab === "recordings" ? (
          <>
            <div className="summary-card">
              <div className="summary-row">
                <span className="weight">{todayText.replace(" recordings", "")}</span>
                <span className="muted small">Total: {recordings.length}</span>
              </div>
              <div className="summary-row small muted">
                <svg
                  viewBox="0 0 24 24"
                  width="14"
                  height="14"
                  fill="none"
                  stroke="#00E5A0"
                  strokeWidth="2"
                >
                  <path d="M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z" />
                </svg>
                <span>Drive configured</span>
              </div>
              <button className="btn btn-sm" onClick={() => void syncAllPending()}>
                Sync All Pending
              </button>
            </div>

            <h3 className="section-h">Upload Existing</h3>
            <div className="upload-existing">
              <input ref={uploadAudioRef} type="file" accept="audio/*" />
              <input ref={uploadMetaRef} type="file" accept="application/json" />
              <button
                className="btn btn-primary btn-sm"
                onClick={async () => {
                  setUploadStatus("Uploading…");
                  const msg = await uploadExisting(
                    uploadAudioRef.current?.files?.[0] || null,
                    uploadMetaRef.current?.files?.[0] || null,
                  );
                  setUploadStatus(msg);
                }}
              >
                Upload files
              </button>
              <div className="muted small">{uploadStatus}</div>
            </div>

            <h3 className="section-h">Recent</h3>
            <div className="recording-list">
              {recordings.length === 0 ? (
                <div className="empty">No recordings yet</div>
              ) : (
                recordings.slice(0, 50).map((r) => (
                  <div className="rec-row" key={r.filename}>
                    <div className="rec-row-top">
                      <div className="rec-emoji">🎙️</div>
                      <div className="rec-col">
                        <div className="rec-name mono">{r.filename}</div>
                        <div className="rec-meta">
                          {(r.started_at_local || "").slice(11, 16)} · {r.duration_seconds || 0}s
                        </div>
                      </div>
                      <span
                        className={`pill ${
                          r.status === "Uploaded"
                            ? "pill-success"
                            : r.status === "Pending"
                              ? "pill-warn"
                              : r.status === "Failed"
                                ? "pill-error"
                                : "pill-muted"
                        }`}
                      >
                        {r.status}
                      </span>
                    </div>
                    <div className="rec-actions">
                      {(r.status === "Pending" || r.status === "Failed") && (
                        <button className="row-btn row-btn-accent" onClick={() => void retryUpload(r)}>
                          Retry Upload
                        </button>
                      )}
                      <button className="row-btn" onClick={() => void downloadOne(r.filename)}>
                        Download
                      </button>
                      <button className="row-btn row-btn-error" onClick={() => void removeOne(r.filename)}>
                        Delete
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </>
        ) : (
          <>
            <div className="summary-card">
              <div className="weight">Dataset Overview</div>
              <div className="muted small">{statsOverview}</div>
            </div>
            {balanceWarning ? (
              <div className="summary-card">
                <div className="warn-text">⚠ {balanceWarning}</div>
              </div>
            ) : null}
            <div className="summary-card">
              <div className="weight">Count per Label</div>
              <div className="bar-chart">
                {barData.map((item) => (
                  <div className="bar-row" key={item.id}>
                    <div className="bar-lbl">{item.label}</div>
                    <div className="bar-track">
                      <div className="bar-fill" style={{ width: `${item.pct}%` }}></div>
                    </div>
                    <div className="bar-count">{item.count}</div>
                  </div>
                ))}
              </div>
            </div>
          </>
        )}
      </main>
    </section>
  );
}
