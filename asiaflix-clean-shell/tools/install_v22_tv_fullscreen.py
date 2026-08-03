from pathlib import Path
import base64
import hashlib
import json
import zlib

ROOT = Path(__file__).resolve().parents[1]
PARTS = ROOT / "tools" / "v22_payload"
payload = "".join(path.read_text(encoding="utf-8") for path in sorted(PARTS.glob("part*.txt")))

expected = "ff08439bd01dafaeff1c1d5842f2bd6137e31c437e62a9675a6f2c2207fe2c32"
actual = hashlib.sha256(payload.encode("utf-8")).hexdigest()
if actual != expected:
    raise RuntimeError(f"Asia Flix v2.2 payload checksum mismatch: expected {expected}, got {actual}")

files = json.loads(zlib.decompress(base64.b85decode(payload)).decode("utf-8"))
for relative, content in files.items():
    path = ROOT / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    print(f"WROTE {relative} ({len(content.encode('utf-8'))} bytes)")

print(f"Installed {len(files)} Asia Flix v2.2 instant-TV-fullscreen files")
