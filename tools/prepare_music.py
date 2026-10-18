"""Recreate the exact version-2 soundtrack omitted from Git; no downloads."""
from pathlib import Path
import hashlib, json, importlib.util
ROOT = Path(__file__).resolve().parents[1]
EXPECTED = json.loads((ROOT / 'tools/music-sha256.json').read_text())
FOLDER = ROOT / 'app/src/main/assets/content/music'
def matches(name, digest):
    path = FOLDER / name
    return path.is_file() and hashlib.sha256(path.read_bytes()).hexdigest() == digest
if not all(matches(name, digest) for name, digest in EXPECTED.items()):
    try:
        spec = importlib.util.spec_from_file_location('jet_content', ROOT / 'tools/generate_expansion_content.py')
        content = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(content)
    except ModuleNotFoundError:
        raise SystemExit('Install generation dependencies: python -m pip install -r tools/requirements-content.txt')
    content.generate_music()
if not all(matches(name, digest) for name, digest in EXPECTED.items()):
    raise SystemExit('Soundtrack checksum mismatch; use the pinned content dependencies with Python 3.11.')
print('PASS: all 12 soundtrack files match the original build-206 SHA-256 checksums.')
