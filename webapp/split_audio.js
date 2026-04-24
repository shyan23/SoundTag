// Split an audio blob into fixed-length chunks using Web Audio API.
// Decodes to PCM, slices into segments, re-encodes each as WAV.

const CHUNK_SECONDS = 5;

/**
 * Encode raw PCM float32 samples into a WAV blob.
 * @param {Float32Array} samples - mono PCM data
 * @param {number} sampleRate
 * @returns {Blob}
 */
function encodeWav(samples, sampleRate) {
  const numSamples = samples.length;
  const buffer = new ArrayBuffer(44 + numSamples * 2);
  const view = new DataView(buffer);

  const writeStr = (offset, str) => {
    for (let i = 0; i < str.length; i++) view.setUint8(offset + i, str.charCodeAt(i));
  };

  writeStr(0, "RIFF");
  view.setUint32(4, 36 + numSamples * 2, true);
  writeStr(8, "WAVE");
  writeStr(12, "fmt ");
  view.setUint32(16, 16, true);          // subchunk1 size
  view.setUint16(20, 1, true);           // PCM
  view.setUint16(22, 1, true);           // mono
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * 2, true); // byte rate
  view.setUint16(32, 2, true);           // block align
  view.setUint16(34, 16, true);          // bits per sample

  writeStr(36, "data");
  view.setUint32(40, numSamples * 2, true);

  for (let i = 0; i < numSamples; i++) {
    const s = Math.max(-1, Math.min(1, samples[i]));
    view.setInt16(44 + i * 2, s < 0 ? s * 0x8000 : s * 0x7FFF, true);
  }

  return new Blob([buffer], { type: "audio/wav" });
}

/**
 * Split an audio blob into ≤ CHUNK_SECONDS chunks.
 * @param {Blob} audioBlob - any browser-decodable audio
 * @returns {Promise<Array<{blob: Blob, durationMs: number, index: number}>>}
 */
export async function splitAudio(audioBlob) {
  const ctx = new (window.AudioContext || window.webkitAudioContext)();
  try {
    const arrayBuf = await audioBlob.arrayBuffer();
    const decoded = await ctx.decodeAudioData(arrayBuf);

    const sampleRate = decoded.sampleRate;
    const allSamples = decoded.getChannelData(0); // mono
    const totalSamples = allSamples.length;
    const chunkSamples = CHUNK_SECONDS * sampleRate;
    const numChunks = Math.ceil(totalSamples / chunkSamples);

    const chunks = [];
    for (let i = 0; i < numChunks; i++) {
      const start = i * chunkSamples;
      const end = Math.min(start + chunkSamples, totalSamples);
      const segment = allSamples.slice(start, end);
      const blob = encodeWav(segment, sampleRate);
      const durationMs = Math.round(((end - start) / sampleRate) * 1000);
      chunks.push({ blob, durationMs, index: i });
    }
    return chunks;
  } finally {
    await ctx.close();
  }
}

export { CHUNK_SECONDS };
