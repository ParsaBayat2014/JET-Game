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
missing_or_mismatched=[]
for name,digest in EXPECTED.items():
    path=FOLDER/name
    actual=hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else 'missing'
    if actual!=digest:missing_or_mismatched.append(f'{name}: expected {digest}, got {actual}')
if missing_or_mismatched:
    raise SystemExit('Soundtrack checksum mismatch:\n'+'\n'.join(missing_or_mismatched))
print('PASS: all 12 soundtrack files match the original build-206 SHA-256 checksums.')
