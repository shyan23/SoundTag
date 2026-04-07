// Annotation UI — chip groups for label/severity/env/context.

export const LABELS = [
  "traffic", "horn", "construction", "industrial",
  "crowd", "nature", "silence", "mixed",
];
export const SEVERITY = ["low", "medium", "high", "severe"];
export const ENVIRONMENTS = ["outdoor", "indoor", "vehicle"];
export const CONTEXTS = ["roadside", "market", "residential", "park", "industrial"];

export const defaultSelections = () => ({
  label: "traffic",
  severity: "medium",
  environment: "outdoor",
  locationContext: "roadside",
});

/**
 * Render a chip group. Uses DOM APIs (no innerHTML) to avoid XSS.
 * @param {HTMLElement} container
 * @param {string[]} items
 * @param {string} currentValue
 * @param {(value:string) => void} onSelect
 */
export function renderChipGroup(container, items, currentValue, onSelect) {
  container.replaceChildren();
  for (const value of items) {
    const chip = document.createElement("div");
    chip.className = "chip" + (value === currentValue ? " active" : "");
    chip.textContent = value;
    chip.addEventListener("click", () => onSelect(value));
    container.appendChild(chip);
  }
}

export function autoFilename(label) {
  const n = String(Math.floor(Math.random() * 900) + 100);
  return `${label}_${n}`;
}
