from pathlib import Path
import base64
import hashlib
import json
import zlib

# Deterministic full-file installer for the canonical Asia Flix release source tree.
# It never stacks text patches over an older build. Every release file is replaced exactly.
ROOT = Path(__file__).resolve().parents[1]
V21_PARTS = ROOT / "tools" / "canonical_payload_v21"
LEGACY_PARTS = ROOT / "tools" / "canonical_payload"
PARTS = V21_PARTS if V21_PARTS.exists() else LEGACY_PARTS
payload = "".join(path.read_text(encoding="utf-8") for path in sorted(PARTS.glob("part*.txt")))

if PARTS == V21_PARTS:
    expected = "f1e71214232b9bcfada915f3a800cbf657237bac300c21960f8576df15cb6248"
    actual = hashlib.sha256(payload.encode("utf-8")).hexdigest()
    if actual != expected:
        raise RuntimeError(f"Canonical v2.1 payload checksum mismatch: expected {expected}, got {actual}")

files = json.loads(zlib.decompress(base64.b85decode(payload)).decode("utf-8"))
for relative, content in files.items():
    path = ROOT / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    print(f"WROTE {relative} ({len(content.encode('utf-8'))} bytes)")
print(f"Installed {len(files)} canonical Asia Flix release files from {PARTS.name}")
