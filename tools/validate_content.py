"""Validate original assets and exact inclusion in a requested 250MB APK.

python tools/validate_content.py --apk releases/<preview>.apk
The optional APK argument avoids accidentally checking an older release.
"""
from pathlib import Path
import argparse, hashlib, json, wave, zipfile
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"

def digest_file(path):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for data in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(data)
    return digest.digest()

def digest_entry(archive, name):
    digest = hashlib.sha256()
    with archive.open(name) as stream:
        for data in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(data)
    return digest.digest()

def validate(apk=None):
    catalog = json.loads((ASSETS / "content/catalog.json").read_text(encoding="utf-8"))
    assert len(catalog["worlds"]) == 16
    assert len(catalog["tracks"]) == 12
    backgrounds, soundtrack = set(), set()
    for scene in catalog["worlds"]:
        path = ASSETS / scene["background"]
        with Image.open(path) as artwork:
            assert artwork.size == (1080, 1920)
            artwork.verify()
        backgrounds.add(digest_file(path))
    assert len(backgrounds) == 16, "World artwork must be distinct"
    music = sorted((ASSETS / "content/music").glob("*.wav"))
    assert len(music) == 12
    seconds = 0
    tracks = {item["path"]: item for item in catalog["tracks"]}
    for path in music:
        item = tracks[path.relative_to(ASSETS).as_posix()]
        with wave.open(str(path)) as audio:
            assert audio.getnchannels() == 2 and audio.getsampwidth() == 2
            assert audio.getframerate() == 44100
            duration = audio.getnframes() / audio.getframerate()
            assert 110 <= duration <= 120
            assert abs(duration - item["duration_seconds"]) < 1 / 44100
            seconds += duration
        soundtrack.add(digest_file(path))
    assert len(soundtrack) == 12, "Soundtrack compositions must be distinct"
    assert 22 * 60 <= seconds <= 24 * 60
    payload = [path for path in ASSETS.rglob("*") if path.is_file()]
    print("Useful asset bytes:", sum(path.stat().st_size for path in payload))
    print("Original soundtrack seconds:", round(seconds, 3))
    if apk is not None:
        assert apk.is_file(), apk
        with zipfile.ZipFile(apk) as archive:
            names = set(archive.namelist())
            for path in payload:
                name = "assets/" + path.relative_to(ASSETS).as_posix()
                assert name in names, name
                assert digest_entry(archive, name) == digest_file(path), name
                if path.suffix == ".wav":
                    assert archive.getinfo(name).compress_type == zipfile.ZIP_STORED, "Music must stream through AssetFileDescriptor"
            assert not any(name.endswith((".jks", ".keystore", "release.properties", "myket.properties")) for name in names)
        print("APK bytes:", apk.stat().st_size)
        assert 247_000_000 <= apk.stat().st_size <= 253_000_000, "Expected approximately 250MB of useful media and game content"
    print("PASS: 16 distinct environments, 12 distinct full arrangements, exact checked APK assets" if apk else "PASS: 16 distinct environments and 12 full original arrangements")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", type=Path)
    args = parser.parse_args()
    validate(args.apk.resolve() if args.apk else None)
