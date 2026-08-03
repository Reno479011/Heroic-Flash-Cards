#!/usr/bin/env bash
set +e

PROJECT="asiaflix-clean-shell"
EVIDENCE="tv-v22-evidence"
mkdir -p "$EVIDENCE"

TESTS="org.asiaflix.clean.MainActivityInstrumentedTest#harnessRemovesXmAndMovesBlueTvFocus,org.asiaflix.clean.MainActivityInstrumentedTest#tvVideoSelectionImmediatelyUsesFullScreenStageAndRemoteSeek,org.asiaflix.clean.MainActivityInstrumentedTest#fullscreenDpadHoldAcceleratesForOnnRemote,org.asiaflix.clean.AsiaFlixLiveInstrumentedTest#forcedTvModeHasPersistentBlueSelectionOnLivePage,org.asiaflix.clean.AsiaFlixLiveInstrumentedTest#livePageBlocksAdvertisementApiAndSeeksRealJwVideo"

gradle -p "$PROJECT" :app:connectedDebugAndroidTest --no-daemon --stacktrace \
  "-Pandroid.testInstrumentationRunnerArguments.class=$TESTS"
status=$?

adb shell monkey -p org.asiaflix.clean -c android.intent.category.LEANBACK_LAUNCHER 1 >/dev/null 2>&1 || true
sleep 12
adb logcat -d > "$EVIDENCE/tv-logcat.txt" 2>&1 || true
adb exec-out screencap -p > "$EVIDENCE/tv-final.png" 2>/dev/null || true
adb shell dumpsys activity activities > "$EVIDENCE/tv-activities.txt" 2>&1 || true
adb shell dumpsys window > "$EVIDENCE/tv-window.txt" 2>&1 || true
adb pull /sdcard/asiaflix-tv-instant-player-fullscreen.png "$EVIDENCE/asiaflix-tv-instant-player-fullscreen.png" >/dev/null 2>&1 || true

if grep -E 'FATAL EXCEPTION|ANR in org\.asiaflix\.clean' "$EVIDENCE/tv-logcat.txt"; then
  echo "Detected an Asia Flix crash or ANR in Android TV logcat"
  status=1
fi

if [ "$status" -eq 0 ]; then
  echo "Asia Flix v2.2 Android TV instant-fullscreen suite passed"
else
  echo "Asia Flix v2.2 Android TV instant-fullscreen suite failed"
fi

exit "$status"
