"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useSoundTag } from "../providers/soundtag-context";

export default function SetupPage() {
  const router = useRouter();
  const { setProfileValues } = useSoundTag();
  const [name, setName] = useState("");
  const [annotatorId, setAnnotatorId] = useState("");
  const [error, setError] = useState("");

  const folderPath = useMemo(() => {
    const value = annotatorId.trim() || "{id}";
    return `SoundTag/${value}/`;
  }, [annotatorId]);

  const onContinue = () => {
    const normalizedId = annotatorId.trim().toLowerCase().replace(/\s+/g, "-");
    if (!normalizedId) {
      setError("Annotator ID is required");
      return;
    }
    setProfileValues(name, normalizedId);
    router.push("/record");
  };

  return (
    <section className="view" id="view-setup">
      <div className="setup-hero">
        <div className="app-icon">
          <svg
            viewBox="0 0 24 24"
            width="28"
            height="28"
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
        <h1 className="hero-title">SoundTag</h1>
        <p className="hero-sub">Urban Noise Dataset Collector</p>
      </div>

      <div className="form-block">
        <div className="field">
          <div className="field-label">Your Name</div>
          <input
            type="text"
            placeholder="Hemal"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>
        <div className="field">
          <div className="field-label">Annotator ID</div>
          <input
            type="text"
            className="mono"
            placeholder="hemal"
            value={annotatorId}
            onChange={(e) => {
              setAnnotatorId(e.target.value);
              setError("");
            }}
          />
          <div className="helper">Used to tag recordings. Lowercase, no spaces.</div>
        </div>
      </div>

      <div className="setup-footer">
        <div className="row-card">
          <svg
            viewBox="0 0 24 24"
            width="18"
            height="18"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
          >
            <path d="M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z" />
          </svg>
          <span className="row-card-text">Google Drive</span>
          <span className="pill pill-accent">Ready</span>
        </div>

        <div className="row-card">
          <span className="folder-emoji">📁</span>
          <span className="row-card-text mono">{folderPath}</span>
          <span className="pill pill-muted">Default</span>
        </div>

        <button className="btn btn-primary btn-cta" onClick={onContinue}>
          Continue
        </button>
        <p className="footer-note">{error || "Settings can be changed later"}</p>
      </div>
    </section>
  );
}
