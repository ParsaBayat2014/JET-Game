"""Package source and hash artifacts without signing keys, credentials or local caches."""
from pathlib import Path
import hashlib
import zipfile
import argparse

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--version-code', type=int, default=205)
parser.add_argument('--variant', choices=('preview','release'), default='preview')
args = parser.parse_args()
if args.version_code < 1:
    parser.error('version-code must be positive')

root = Path(__file__).resolve().parents[1]
files = []
def source_file(p):
    if not p.is_file() or "__pycache__" in p.parts or p.suffix in (".class", ".pyc"):
        return False
    # Raw device dumps remain local; the QA report and game screenshots are reproducible artifacts.
    return not (p.parent.name == "previews" and p.name.startswith("android-") and p.suffix in (".txt", ".xml", ".log"))
for folder in ("app/src", "gradle", "tools", "docs"):
    files += [p for p in (root / folder).rglob("*") if source_file(p)]
for name in ("settings.gradle", "build.gradle", "gradle.properties", "gradlew", "gradlew.bat", ".gitignore", "README.md", "local.properties.example", "myket.properties.example", "signing/release.properties.example", "app/build.gradle", "app/proguard-rules.pro"):
    p = root / name
    if p.exists():
        files.append(p)
out = root / f"releases/JET-2.0.0-build{args.version_code}-source.zip"
out.parent.mkdir(exist_ok=True)
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as archive:
    for p in sorted(set(files)):
        relative = p.relative_to(root).as_posix()
        if p.suffix in (".jks", ".keystore") or relative in ("myket.properties", "signing/release.properties"):
            raise RuntimeError("Private file in source package")
        archive.write(p, relative)
for p in (out, root / f"releases/JET-2.0.0-build{args.version_code}-{args.variant}.apk"):
    if p.exists():
        sha = hashlib.sha256(p.read_bytes()).hexdigest()
        (p.parent / (p.name + ".sha256")).write_text(sha + "  " + p.name + "\n", encoding="ascii")
        print(p.name, p.stat().st_size, sha)

