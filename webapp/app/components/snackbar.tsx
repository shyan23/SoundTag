"use client";

import { useSoundTag } from "../providers/soundtag-context";

export function Snackbar() {
  const { toast } = useSoundTag();
  if (!toast.text) return null;
  return <div className={`snackbar ${toast.kind}`}>{toast.text}</div>;
}
