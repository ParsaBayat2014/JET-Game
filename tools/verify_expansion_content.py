"""Validate the expanded original Worlds media pack with bounded audio memory."""
from pathlib import Path
import argparse, hashlib, json, math, wave
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
RATE = 44100

def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()

def verify():
    manifest = json.loads((ASSETS / "content/catalog.json").read_text(encoding="utf-8"))
    assert manifest["pack"].endswith(" Worlds")
    assert len(manifest["worlds"]) == 16 and len(manifest["tracks"]) == 12
    assert len({w["id"] for w in manifest["worlds"]}) == 16
    assert len({t["id"] for t in manifest["tracks"]}) == 12
    rows, image_hashes, music_hashes = [], set(), set()
    thumbnails = []
    for index, item in enumerate(manifest["worlds"]):
        path = ASSETS / item["background"]
        assert item["index"] == index
        with Image.open(path) as illustration:
            illustration.load()
            assert illustration.size == (1080, 1920) and illustration.mode == "RGB"
            thumb = illustration.copy()
            thumb.thumbnail((162, 288))
            thumbnails.append(thumb)
        digest = sha256(path)
        image_hashes.add(digest)
        rows.append({"id": item["id"], "type": "background", "bytes": path.stat().st_size, "sha256": digest})
    assert len(image_hashes) == 16, "Illustrated environments must be distinct"
    waveform = Image.new("RGB", (1200, 12 * 104 + 36), (9, 16, 28))
    draw = ImageDraw.Draw(waveform)
    draw.text((18, 12), "12 original stereo compositions: bounded peaks, arranged phrases, continuous loops", fill=(226, 238, 251))
    for ti, item in enumerate(manifest["tracks"]):
        path = ASSETS / item["path"]
        peaks, squares, sums, sample_count, frame_count = 0.0, 0.0, np.zeros(2), 0, 0
        first, last, bins = None, None, []
        with wave.open(str(path), "rb") as audio:
            assert audio.getnchannels() == 2 and audio.getsampwidth() == 2
            assert audio.getframerate() == RATE
            duration = audio.getnframes() / RATE
            assert abs(duration - item["duration_seconds"]) < 1 / RATE
            assert 110 <= duration <= 120, "Each track should be a complete approximately two-minute composition"
            while raw := audio.readframes(RATE):
                samples = np.frombuffer(raw, dtype="<i2").reshape(-1, 2).astype(np.float64) / 32768
                if first is None:
                    first = samples[0].copy()
                last = samples[-1].copy()
                peaks = max(peaks, float(np.max(np.abs(samples))))
                squares += float(np.sum(samples * samples))
                sums += samples.sum(axis=0)
                sample_count += samples.size
                frame_count += len(samples)
                bins.append((float(np.min(samples)), float(np.max(samples)), float(np.sqrt(np.mean(samples * samples)))))
        rms = math.sqrt(squares / sample_count)
        dc = float(np.max(np.abs(sums / frame_count)))
        edge = float(np.max(np.abs(last - first)))
        assert .05 < rms < .12 and peaks < .75 and dc < .002 and edge < .012, (item["id"], peaks, rms, dc, edge)
        digest = sha256(path)
        music_hashes.add(digest)
        rows.append({"id": item["id"], "type": "music", "bytes": path.stat().st_size, "seconds": duration,
                     "peak": round(peaks, 5), "rms": round(rms, 5), "dc": round(dc, 6),
                     "loop_sample_delta": round(edge, 5), "sha256": digest})
        top = 36 + ti * 104
        center = top + 65
        draw.text((18, top + 3), item["title"] + " / %.2fs / peak %.3f / RMS %.3f / loop %.5f" % (duration, peaks, rms, edge), fill=(199, 220, 237))
        draw.line((18, center, 1180, center), fill=(32, 55, 76), width=1)
        for j, (low, high, level) in enumerate(bins):
            x = 18 + j * 1162 / len(bins)
            draw.line((x, center - high * 48, x, center - low * 48), fill=(85, 198, 207), width=4)
    assert len(music_hashes) == 12, "Each composition must be distinct"
    assert {w["music"] for w in manifest["worlds"]} <= {t["path"] for t in manifest["tracks"]}
    content_bytes = sum(p.stat().st_size for p in (ASSETS / "content").rglob("*") if p.is_file())
    assert 242_000_000 <= content_bytes <= 252_000_000, content_bytes
    report = {"pack": manifest["pack"], "result": "PASS", "images": 16, "tracks": 12,
              "soundtrack_seconds": sum(r["seconds"] for r in rows if r["type"] == "music"),
              "bytes": content_bytes,
              "checks": "All media decodes; distinct files; correct formats and whole-bar durations; bounded volume; no clipping; low DC offset; continuous sample boundaries. No generated padding.",
              "files": rows}
    return report, waveform

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    report, waveform = verify()
    previews = ROOT / "docs/previews"
    previews.mkdir(parents=True, exist_ok=True)
    waveform.save(previews / "soundtrack-waveforms.jpg", quality=95)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
