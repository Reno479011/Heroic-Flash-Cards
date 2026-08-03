from pathlib import Path
import re

source = Path("app/src/main/java/org/asiaflix/clean/MainActivity.java")
text = source.read_text(encoding="utf-8")

pattern = re.compile(
    r"(    private final Runnable touchHoldStarter = \(\) -> \{.*?\n    \};\n)"
    r"(    private final Runnable touchHoldRepeater = new Runnable\(\) \{.*?\n    \};\n)",
    re.S,
)

updated, count = pattern.subn(r"\2\1", text, count=1)
if count != 1:
    raise RuntimeError(f"Expected one touch hold runnable block, found {count}")

source.write_text(updated, encoding="utf-8")
print("Reordered v1.5 touch hold runnables")
