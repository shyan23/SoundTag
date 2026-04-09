"use client";

import Link from "next/link";
import { useMemo, useRef, useState } from "react";
import { useSoundTag } from "../providers/soundtag-context";

export default function AnnotatePage() {
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const [playing, setPlaying] = useState(false);
  const [current, setCurrent] = useState(0);
  const {
    labels,
    contexts,
    environments,
    selections,
    setSelection,
    filename,
    setFilename,
    notes,
    setNotes,
    saveFootnote,
    onSave,
    lastInfoText,
    lastGpsText,
    playbackUrl,
    playbackDuration,
  } = useSoundTag();

  const progress = useMemo(() => {
    if (!playbackDuration) return 0;
    return Math.min(100, (current / playbackDuration) * 100);
  }, [current, playbackDuration]);

  const timeText = useMemo(() => {
    const fmt = (sec: number) => {
      const m = Math.floor(sec / 60);
      const s = Math.floor(sec % 60);
      return `${m}:${String(s).padStart(2, "0")}`;
    };
    return `${fmt(current)} / ${fmt(playbackDuration || 0)}`;
  }, [current, playbackDuration]);

  return (
    <section className="view" id="view-annotate">
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
        <div className="topbar-title">Annotate Recording</div>
        <div className="icon-btn-spacer"></div>
      </header>

      <main className="annotate-main">
        <div className="info-line">{lastInfoText || "No recording loaded"}</div>
        <div className="info-line mono muted small">{lastGpsText}</div>

        <div className="playback-bar">
          <button
            className="pb-play"
            type="button"
            onClick={() => {
              const a = audioRef.current;
              if (!a) return;
              if (a.paused) {
                void a.play();
                setPlaying(true);
              } else {
                a.pause();
                setPlaying(false);
              }
            }}
          >
            <svg viewBox="0 0 24 24" width="14" height="14" fill="#00261A">
              {playing ? (
                <path d="M6 5h4v14H6zm8 0h4v14h-4z" />
              ) : (
                <polygon points="6 4 20 12 6 20 6 4" />
              )}
            </svg>
          </button>
          <div className="pb-progress">
            <div className="pb-fill" style={{ width: `${progress}%` }}></div>
          </div>
          <div className="pb-time mono">{timeText}</div>
        </div>
        <audio
          ref={audioRef}
          src={playbackUrl}
          hidden
          onTimeUpdate={(e) => setCurrent((e.target as HTMLAudioElement).currentTime)}
          onEnded={() => setPlaying(false)}
        ></audio>

        <h3 className="section-h">What did you record?</h3>
        <div className="chips">
          {labels.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`chip ${selections.label === item.id ? "active" : ""}`}
              onClick={() => {
                setSelection("label", item.id);
                setFilename(`${item.id}_${Math.floor(Math.random() * 900 + 100)}`);
              }}
            >
              <span>{item.emoji}</span>
              <span>{item.text}</span>
            </button>
          ))}
        </div>

        <h3 className="section-h">Is this noise pollution?</h3>
        <div className="toggle-group">
          {(["noise", "not noise"] as const).map((item) => (
            <button
              key={item}
              type="button"
              className={`toggle-opt ${selections.isNoise === (item === "noise") ? "active" : ""}`}
              onClick={() => setSelection("isNoise", item === "noise")}
            >
              {item}
            </button>
          ))}
        </div>

        <div className="sub-label">Severity</div>
        <div className="severity-row">
          {(["low", "medium", "high"] as const).map((item) => (
            <button
              key={item}
              type="button"
              className={`sev-btn ${selections.severity === item ? "active" : ""}`}
              onClick={() => setSelection("severity", item)}
            >
              {item}
            </button>
          ))}
        </div>
        <div className="score-row">
          <span className="score-label">
            Score: <span>{selections.score}</span>
          </span>
          <div className="score-boxes">
            {[1, 2, 3, 4, 5].map((item) => (
              <button
                key={item}
                type="button"
                className={`score-box ${selections.score === item ? "active" : ""}`}
                onClick={() => setSelection("score", item)}
              >
                {item}
              </button>
            ))}
          </div>
        </div>

        <h3 className="section-h">Where were you?</h3>
        <div className="toggle-group">
          {environments.map((item) => (
            <button
              key={item}
              type="button"
              className={`toggle-opt ${selections.environment === item ? "active" : ""}`}
              onClick={() => setSelection("environment", item)}
            >
              {item}
            </button>
          ))}
        </div>
        <div className="sub-label">Location context</div>
        <div className="chips">
          {contexts.map((item) => (
            <button
              key={item}
              type="button"
              className={`chip ${selections.locationContext === item ? "active" : ""}`}
              onClick={() => setSelection("locationContext", item)}
            >
              {item}
            </button>
          ))}
        </div>

        <h3 className="section-h">File Details</h3>
        <div className="field">
          <div className="field-label">Recording Name</div>
          <input
            type="text"
            className="mono"
            value={filename}
            onChange={(e) => setFilename(e.target.value)}
          />
          <div className="helper">Prefix before underscore becomes the ML label</div>
        </div>
        <div className="field">
          <div className="field-label">Notes</div>
          <textarea
            rows={3}
            placeholder="e.g. Near Farmgate intersection"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          ></textarea>
        </div>

        <div className="action-stack">
          <button className="btn btn-primary btn-cta" onClick={() => void onSave("upload")}>
            Save &amp; Upload
          </button>
          <button className="btn btn-cta" onClick={() => void onSave("local")}>
            Save Locally Only
          </button>
          <p className="footer-note">{saveFootnote}</p>
        </div>
      </main>
    </section>
  );
}
