#!/usr/bin/env bash
set +e

PROJECT="asiaflix-clean-shell"
EVIDENCE="mobile-evidence"
mkdir -p "$EVIDENCE"

# Run the complete deterministic harness plus live Asia Flix player tests.
gradle -p "$PROJECT" :app:connectedDebugAndroidTest --no-daemon --stacktrace
status=$?

adb logcat -d > "$EVIDENCE/mobile-logcat.txt" 2>&1 || true
adb exec-out screencap -p > "$EVIDENCE/mobile-final.png" 2>/dev/null || true
adb shell dumpsys activity activities > "$EVIDENCE/mobile-activities.txt" 2>&1 || true
adb shell dumpsys window > "$EVIDENCE/mobile-window.txt" 2>&1 || true

for remote in \
  asiaflix-harness-blue-selector.png \
  asiaflix-live-player-no-xm.png \
  asiaflix-live-mobile-fullscreen.png \
  asiaflix-live-mobile-restored.png \
  asiaflix-live-tv-blue-selector.png; do
  adb pull "/sdcard/$remote" "$EVIDENCE/$remote" >/dev/null 2>&1 || true
done

if grep -E 'FATAL EXCEPTION|ANR in org\.asiaflix\.clean' "$EVIDENCE/mobile-logcat.txt"; then
  echo "Detected an Asia Flix crash or ANR in mobile emulator logcat"
  status=1
fi

if [ "$status" -ne 0 ]; then
  echo "Mobile functional release gate failed"
else
  echo "Mobile functional release gate passed"
fi
exit "$status"
