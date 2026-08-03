from pathlib import Path
import base64
import hashlib
import json
import zlib

# Deterministic full-file installer for the canonical Asia Flix release source tree.
# It never stacks feature patches over an older APK. Every release file is replaced exactly,
# followed by one source-normalization pass for required Android test imports.
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

# The v2.1 instrumentation suite captures device screenshots through UiAutomator.
# Normalize these compile-time imports deterministically so the source installed in every
# workflow is identical and the tests cannot be silently skipped.
test_path = ROOT / "app/src/androidTest/java/org/asiaflix/clean/MainActivityInstrumentedTest.java"
test_source = test_path.read_text(encoding="utf-8")
anchor = "import androidx.test.ext.junit.runners.AndroidJUnit4;\n"
required = (
    "import androidx.test.ext.junit.runners.AndroidJUnit4;\n"
    "import androidx.test.platform.app.InstrumentationRegistry;\n"
    "import androidx.test.uiautomator.UiDevice;\n"
)
if "import androidx.test.platform.app.InstrumentationRegistry;" not in test_source:
    if anchor not in test_source:
        raise RuntimeError("Cannot normalize instrumentation imports: AndroidJUnit4 import anchor is missing")
    test_source = test_source.replace(anchor, required, 1)
test_path.write_text(test_source, encoding="utf-8")

for required_import in (
    "import androidx.test.platform.app.InstrumentationRegistry;",
    "import androidx.test.uiautomator.UiDevice;",
):
    if required_import not in test_path.read_text(encoding="utf-8"):
        raise RuntimeError(f"Canonical instrumentation import missing after install: {required_import}")

print(f"Installed {len(files)} canonical Asia Flix release files from {PARTS.name}")
print("Normalized Android instrumentation imports for screenshot verification")
