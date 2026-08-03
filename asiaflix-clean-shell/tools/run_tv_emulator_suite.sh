#!/usr/bin/env bash
set +e

PROJECT="asiaflix-clean-shell"
EVIDENCE="tv-evidence"
mkdir -p "$EVIDENCE"

TESTS="org.asiaflix.clean.MainActivityInstrumentedTest#harnessRemovesXmAndMovesBlueTvFocus,org.asiaflix.clean.MainActivityInstrumentedTest#fullscreenDpadHoldAcceleratesForOnnRemote,org.asiaflix.clean.AsiaFlixLiveInstrumentedTest#forcedTvModeHasPersistentBlueSelectionOnLivePage,org.asiaflix.clean.AsiaFlixLiveInstrumentedTest#livePageBlocksAdvertisementApiAndSeeksRealJwVideo"

gradle -p "$PROJECT" :app:connectedDebugAndroidTest --no-daemon --stacktrace \
  "-Pandroid.testInstrumentationRunnerArguments.class=$TESTS"
status=$?

# Also launch through the same Leanback category used by Onn/Android TV home screens.
adb shell monkey -p org.asiaflix.clean -c android.intent.category.LEANBACK_LAUNCHER 1 >/dev/null 2>&1 || true
sleep 12
adb shell input keyevent 22 || true
adb shell input keyevent 20 || true
adb shell input keyevent 23 || true
sleep 3

adb logcat -d > "$EVIDENCE/tv-logcat.txt" 2>&1 || true
adb exec-out screencap -p > "$EVIDENCE/tv-final.png" 2>/dev/null || true
adb shell dumpsys activity activities > "$EVIDENCE/tv-activities.txt" 2>&1 || true
adb shell dumpsys window > "$EVIDENCE/tv-window.txt" 2>&1 || true
adb shell pm list features > "$EVIDENCE/tv-features.txt" 2>&1 || true

for remote in \
  asiaflix-harness-blue-selector.png \
  asiaflix-live-tv-blue-selector.png \
  asiaflix-live-player-no-xm.png; do
  adb pull "/sdcard/$remote" "$EVIDENCE/$remote" >/dev/null 2>&1 || true
done

if grep -E 'FATAL EXCEPTION|ANR in org\.asiaflix\.clean' "$EVIDENCE/tv-logcat.txt"; then
  echo "Detected an Asia Flix crash or ANR in Android TV emulator logcat"
  status=1
fi

if [ "$status" -ne 0 ]; then
  echo "Android TV / Onn-style functional release gate failed"
else
  echo "Android TV / Onn-style functional release gate passed"
fi
exit "$status"
