from pathlib import Path
import base64, json, zlib

# Deterministic full-file installer for the canonical Asia Flix v2 source tree.
# It does not patch legacy v1.x files. It replaces each release file exactly.
ROOT = Path(__file__).resolve().parents[1]
PARTS = ROOT / "tools" / "canonical_payload"
payload = "".join(path.read_text(encoding="utf-8") for path in sorted(PARTS.glob("part*.txt")))
files = json.loads(zlib.decompress(base64.b85decode(payload)).decode())
for relative, content in files.items():
    path = ROOT / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    print(f"WROTE {relative} ({len(content.encode('utf-8'))} bytes)")
print(f"Installed {len(files)} canonical Asia Flix v2 release files")
