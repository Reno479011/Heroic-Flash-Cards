from pathlib import Path
import re

SOURCE = Path("app/src/main/java/org/asiaflix/clean/MainActivity.java")
GRADLE = Path("app/build.gradle")
text = SOURCE.read_text(encoding="utf-8")


def sub_once(pattern: str, replacement: str, label: str, flags: int = re.S) -> None:
    global text
    updated, count = re.subn(pattern, replacement, text, count=1, flags=flags)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    text = updated


# Long-press timing and accelerated seek tiers.
sub_once(
    r"    private static final long DOUBLE_TAP_WINDOW_MS = 360L;\n",
    """    private static final long DOUBLE_TAP_WINDOW_MS = 360L;
    private static final long HOLD_START_MS = 560L;
    private static final long HOLD_REPEAT_MS = 340L;
""",
    "hold timing constants",
    flags=0,
)

# Preserve the player's bottom control/timeline region, expose native HTML5 controls,
# and add long-press seeking on accessible video surfaces. The bottom 28 percent is
# never consumed by the app so tapping or dragging a seek bar reaches the player.
sub_once(
    r"              const mediaSurface = function\(target\)\{.*?\n\n              const cleanPage = function\(\)\{",
    """              const activeVideo = function(){
                const videos = allVideos();
                return videos.find(function(v){ return !v.paused && !v.ended; }) || videos[0] || null;
              };
              window.__asiaSeekBy = function(deltaSeconds){
                const video = activeVideo();
                if (!video || !Number.isFinite(video.duration) || video.duration <= 0) return false;
                try {
                  const delta = Number(deltaSeconds || 0);
                  const destination = Math.max(0,Math.min(video.duration,Number(video.currentTime || 0) + delta));
                  video.currentTime = destination;
                  const amount = Math.abs(Math.round(delta));
                  const label = amount >= 60 && amount % 60 === 0 ? (amount / 60) + (amount === 60 ? ' minute' : ' minutes') : amount + ' seconds';
                  showBadge((delta < 0 ? '↶ ' : '↷ ') + label);
                  return true;
                } catch(e) { return false; }
              };
              const ensureNativeControls = function(){
                document.querySelectorAll('video').forEach(function(video){
                  try {
                    video.controls = true;
                    video.setAttribute('controls','controls');
                    video.setAttribute('playsinline','playsinline');
                    video.setAttribute('webkit-playsinline','true');
                    video.style.setProperty('pointer-events','auto','important');
                    video.style.setProperty('touch-action','manipulation','important');
                  } catch(e) {}
                });
              };
              const mediaSurface = function(target){
                if (!target || !target.closest) return null;
                return target.closest('video,iframe,.player,.video-player,.jwplayer,[class*=\"video-player\"],[class*=\"player-container\"],[class*=\"plyr\"],[class*=\"video-js\"]');
              };
              const timelineTarget = function(target){
                if (!target || !target.closest) return false;
                return !!target.closest('input[type=\"range\"],progress,[role=\"slider\"],.progress,.progress-bar,.seek,.seekbar,.timeline,.vjs-progress-control,.vjs-progress-holder,.jw-slider-time,.plyr__progress,.mejs__time-rail,[class*=\"progress-control\"],[class*=\"seek-bar\"],[class*=\"timeline\"]');
              };
              const inControlZone = function(surface,clientY,target){
                if (!surface) return true;
                const r = surface.getBoundingClientRect();
                return timelineTarget(target) || clientY >= r.top + r.height * .72;
              };
              const seekSide = function(surface,clientX){
                const r = surface.getBoundingClientRect();
                if (clientX <= r.left + r.width * .42) return -1;
                if (clientX >= r.left + r.width * .58) return 1;
                return 0;
              };
              const seekFromScreenSide = function(clientX,clientY,target){
                const surface = mediaSurface(target);
                if (!surface || !visible(surface) || inControlZone(surface,clientY,target)) return false;
                const side = seekSide(surface,clientX);
                return side ? window.__asiaSeekBy(side * SEEK) : false;
              };
              document.addEventListener('dblclick',function(event){
                if (seekFromScreenSide(event.clientX,event.clientY,event.target)) {
                  event.preventDefault();
                  event.stopImmediatePropagation();
                }
              },true);

              let lastTouchAt = 0;
              let lastTouchSide = 0;
              let webHoldStarter = 0;
              let webHoldRepeater = 0;
              let webHoldStartedAt = 0;
              let webHoldSide = 0;
              let webHoldSurface = null;
              let webHoldStartX = 0;
              let webHoldStartY = 0;
              let webHoldFired = false;
              const holdSeconds = function(elapsed){
                if (elapsed < 1200) return 10;
                if (elapsed < 3000) return 30;
                if (elapsed < 6000) return 60;
                return 120;
              };
              const stopWebHold = function(){
                clearTimeout(webHoldStarter);
                clearTimeout(webHoldRepeater);
                webHoldStarter = 0;
                webHoldRepeater = 0;
                webHoldSide = 0;
                webHoldSurface = null;
              };
              const repeatWebHold = function(){
                if (!webHoldSide || !webHoldSurface || !visible(webHoldSurface)) return stopWebHold();
                webHoldFired = true;
                window.__asiaSeekBy(webHoldSide * holdSeconds(Date.now() - webHoldStartedAt));
                webHoldRepeater = setTimeout(repeatWebHold,340);
              };
              document.addEventListener('touchstart',function(event){
                const touch = event.touches && event.touches[0];
                const surface = mediaSurface(event.target);
                stopWebHold();
                webHoldFired = false;
                if (!touch || !surface || !visible(surface) || inControlZone(surface,touch.clientY,event.target)) return;
                const side = seekSide(surface,touch.clientX);
                if (!side) return;
                webHoldSide = side;
                webHoldSurface = surface;
                webHoldStartX = touch.clientX;
                webHoldStartY = touch.clientY;
                webHoldStartedAt = Date.now();
                webHoldStarter = setTimeout(repeatWebHold,560);
              },{capture:true,passive:true});
              document.addEventListener('touchmove',function(event){
                const touch = event.touches && event.touches[0];
                if (!touch || !webHoldSide) return;
                if (Math.abs(touch.clientX - webHoldStartX) > 24 || Math.abs(touch.clientY - webHoldStartY) > 24) stopWebHold();
              },{capture:true,passive:true});
              document.addEventListener('touchcancel',stopWebHold,true);
              document.addEventListener('touchend',function(event){
                const touch = event.changedTouches && event.changedTouches[0];
                const surface = mediaSurface(event.target);
                const held = webHoldFired;
                stopWebHold();
                webHoldFired = false;
                if (held) {
                  event.preventDefault();
                  event.stopImmediatePropagation();
                  lastTouchAt = 0;
                  lastTouchSide = 0;
                  return;
                }
                if (!touch || !surface || !visible(surface) || inControlZone(surface,touch.clientY,event.target)) return;
                const side = seekSide(surface,touch.clientX);
                const now = Date.now();
                if (side && side === lastTouchSide && now - lastTouchAt <= 360) {
                  if (window.__asiaSeekBy(side * SEEK)) {
                    event.preventDefault();
                    event.stopImmediatePropagation();
                  }
                  lastTouchAt = 0;
                  lastTouchSide = 0;
                } else {
                  lastTouchAt = now;
                  lastTouchSide = side;
                }
              },{capture:true,passive:false});

              const cleanPage = function(){""",
    "mobile timeline and hold gestures",
)

# Turn controls on again whenever the page or player dynamically redraws.
sub_once(
    r"              const cleanPage = function\(\)\{\n",
    """              const cleanPage = function(){
                ensureNativeControls();
""",
    "native video controls activation",
    flags=0,
)

# Native fullscreen touch/remote state. Native handling covers cross-origin players
# that JavaScript cannot access inside an iframe.
sub_once(
    r"    private int pendingSeekDirection;\n.*?    private String lastGoodUrl = HOME_URL;",
    """    private int pendingSeekDirection;
    private long pendingSeekStartedAt;
    private long lastTouchTapAt;
    private int lastTouchTapSide;
    private float touchDownX;
    private float touchDownY;
    private boolean touchMoved;
    private boolean touchHoldArmed;
    private boolean touchHoldFired;
    private int touchHoldDirection;
    private long touchHoldStartedAt;
    private int remoteHoldDirection;
    private long remoteHoldStartedAt;
    private long lastRemoteHoldSeekAt;
    private final Runnable performPendingSeek = () -> {
        int direction = pendingSeekDirection;
        pendingSeekDirection = 0;
        pendingSeekStartedAt = 0L;
        if (direction != 0) executeSeek(direction);
    };
    private final Runnable touchHoldStarter = () -> {
        if (!touchHoldArmed || touchHoldDirection == 0) return;
        touchHoldFired = true;
        touchHoldStartedAt = android.os.SystemClock.uptimeMillis();
        executeSeekSeconds(touchHoldDirection, acceleratedSeekSeconds(0L));
        handler.postDelayed(touchHoldRepeater, HOLD_REPEAT_MS);
    };
    private final Runnable touchHoldRepeater = new Runnable() {
        @Override
        public void run() {
            if (!touchHoldArmed || touchHoldDirection == 0) return;
            long elapsed = android.os.SystemClock.uptimeMillis() - touchHoldStartedAt;
            executeSeekSeconds(touchHoldDirection, acceleratedSeekSeconds(elapsed));
            handler.postDelayed(this, HOLD_REPEAT_MS);
        }
    };
    private String lastGoodUrl = HOME_URL;""",
    "native hold state",
)

# Replace the seek and fullscreen touch helpers. The lower player-control region is
# deliberately passed through untouched so users can tap or drag the actual timeline.
sub_once(
    r"    private void executeSeek\(int direction\) \{.*?\n    private void controlMedia\(String action, String badgeText, int fallbackKeyCode\) \{",
    """    private int acceleratedSeekSeconds(long elapsedMs) {
        if (elapsedMs < 1200L) return 10;
        if (elapsedMs < 3000L) return 30;
        if (elapsedMs < 6000L) return 60;
        return 120;
    }

    private String seekLabel(int seconds) {
        if (seconds >= 60 && seconds % 60 == 0) {
            int minutes = seconds / 60;
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }
        return seconds + " seconds";
    }

    private void executeSeek(int direction) {
        executeSeekSeconds(direction, SEEK_SECONDS);
    }

    private void executeSeekSeconds(int direction, int seconds) {
        int safeSeconds = Math.max(1, Math.min(seconds, 300));
        int delta = direction < 0 ? -safeSeconds : safeSeconds;
        showMediaBadge((direction < 0 ? "↶ " : "↷ ") + seekLabel(safeSeconds));
        String js = "window.__asiaSeekBy ? window.__asiaSeekBy(" + delta + ") : false";
        webView.evaluateJavascript(js, result -> {
            if (!"true".equals(result)) {
                int keyCode = direction < 0 ? KeyEvent.KEYCODE_MEDIA_REWIND : KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
                dispatchFallbackMediaBurst(keyCode, Math.max(1, (int) Math.ceil(safeSeconds / 10.0)));
            }
        });
    }

    private void dispatchFallbackMediaBurst(int keyCode, int count) {
        int limited = Math.min(30, Math.max(1, count));
        for (int i = 0; i < limited; i++) {
            handler.postDelayed(() -> dispatchFallbackMediaKey(keyCode), i * 28L);
        }
    }

    private boolean queueFullscreenSeek(int direction) {
        long now = android.os.SystemClock.uptimeMillis();
        if (pendingSeekDirection == direction && now - pendingSeekStartedAt <= DOUBLE_TAP_WINDOW_MS) {
            handler.removeCallbacks(performPendingSeek);
            pendingSeekDirection = 0;
            pendingSeekStartedAt = 0L;
            executeSeek(direction);
            return true;
        }
        if (pendingSeekDirection != 0) {
            handler.removeCallbacks(performPendingSeek);
            int previous = pendingSeekDirection;
            pendingSeekDirection = 0;
            pendingSeekStartedAt = 0L;
            executeSeek(previous);
        }
        pendingSeekDirection = direction;
        pendingSeekStartedAt = now;
        handler.postDelayed(performPendingSeek, DOUBLE_TAP_WINDOW_MS - 40L);
        return true;
    }

    private void cancelTouchHold() {
        handler.removeCallbacks(touchHoldStarter);
        handler.removeCallbacks(touchHoldRepeater);
        touchHoldArmed = false;
        touchHoldDirection = 0;
        touchHoldStartedAt = 0L;
    }

    private boolean handleFullscreenTouch(View target, MotionEvent event) {
        if (target.getWidth() <= 0 || target.getHeight() <= 0) return false;
        float x = event.getX();
        float y = event.getY();
        boolean controlZone = y >= target.getHeight() * 0.72f;
        int side = x <= target.getWidth() * 0.42f ? -1 : (x >= target.getWidth() * 0.58f ? 1 : 0);

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            cancelTouchHold();
            touchDownX = x;
            touchDownY = y;
            touchMoved = false;
            touchHoldFired = false;
            if (!controlZone && side != 0) {
                touchHoldArmed = true;
                touchHoldDirection = side;
                handler.postDelayed(touchHoldStarter, HOLD_START_MS);
            }
            return false;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (Math.abs(x - touchDownX) > 24f || Math.abs(y - touchDownY) > 24f || controlZone) {
                touchMoved = true;
                cancelTouchHold();
            }
            return false;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            cancelTouchHold();
            touchHoldFired = false;
            return false;
        }
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return false;

        boolean held = touchHoldFired;
        cancelTouchHold();
        touchHoldFired = false;
        if (held) return true;
        if (controlZone || touchMoved || side == 0) return false;

        long now = android.os.SystemClock.uptimeMillis();
        if (side == lastTouchTapSide && now - lastTouchTapAt <= DOUBLE_TAP_WINDOW_MS) {
            lastTouchTapAt = 0L;
            lastTouchTapSide = 0;
            executeSeek(side);
            return true;
        }
        lastTouchTapAt = now;
        lastTouchTapSide = side;
        return false;
    }

    private void controlMedia(String action, String badgeText, int fallbackKeyCode) {""",
    "accelerated native seeking",
)

# Remote hold: repeated D-pad events progressively jump 10s, 30s, 1m, then 2m.
sub_once(
    r"        if \(fullscreen && keyCode == KeyEvent\.KEYCODE_DPAD_LEFT\) \{.*?        if \(fullscreen && keyCode == KeyEvent\.KEYCODE_DPAD_RIGHT\) \{.*?            return true;\n        \}\n",
    """        if (fullscreen && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            long now = android.os.SystemClock.uptimeMillis();
            if (event.getRepeatCount() == 0) {
                remoteHoldDirection = -1;
                remoteHoldStartedAt = now;
                lastRemoteHoldSeekAt = 0L;
                queueFullscreenSeek(-1);
            } else {
                handler.removeCallbacks(performPendingSeek);
                pendingSeekDirection = 0;
                pendingSeekStartedAt = 0L;
                if (remoteHoldDirection != -1) {
                    remoteHoldDirection = -1;
                    remoteHoldStartedAt = event.getDownTime();
                }
                if (now - lastRemoteHoldSeekAt >= HOLD_REPEAT_MS) {
                    executeSeekSeconds(-1, acceleratedSeekSeconds(now - remoteHoldStartedAt));
                    lastRemoteHoldSeekAt = now;
                }
            }
            return true;
        }
        if (fullscreen && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            long now = android.os.SystemClock.uptimeMillis();
            if (event.getRepeatCount() == 0) {
                remoteHoldDirection = 1;
                remoteHoldStartedAt = now;
                lastRemoteHoldSeekAt = 0L;
                queueFullscreenSeek(1);
            } else {
                handler.removeCallbacks(performPendingSeek);
                pendingSeekDirection = 0;
                pendingSeekStartedAt = 0L;
                if (remoteHoldDirection != 1) {
                    remoteHoldDirection = 1;
                    remoteHoldStartedAt = event.getDownTime();
                }
                if (now - lastRemoteHoldSeekAt >= HOLD_REPEAT_MS) {
                    executeSeekSeconds(1, acceleratedSeekSeconds(now - remoteHoldStartedAt));
                    lastRemoteHoldSeekAt = now;
                }
            }
            return true;
        }
""",
    "remote accelerated hold",
)

# Reset long-press state when the remote button is released.
sub_once(
    r"\n    private void enterImmersiveMode\(\) \{",
    """
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND || keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            remoteHoldDirection = 0;
            remoteHoldStartedAt = 0L;
            lastRemoteHoldSeekAt = 0L;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void enterImmersiveMode() {""",
    "remote hold cleanup",
    flags=0,
)

# Add touch-hold cleanup to fullscreen exit.
sub_once(
    r"        lastTouchTapSide = 0;\n",
    """        lastTouchTapSide = 0;
        cancelTouchHold();
        remoteHoldDirection = 0;
        remoteHoldStartedAt = 0L;
        lastRemoteHoldSeekAt = 0L;
""",
    "fullscreen hold cleanup",
    flags=0,
)

text = text.replace("AsiaFlixTV/1.4 OnnCompatible", "AsiaFlixTV/1.5 OnnCompatible")
text = text.replace("AsiaFlixClean/1.4", "AsiaFlixClean/1.5")
SOURCE.write_text(text, encoding="utf-8")

gradle = GRADLE.read_text(encoding="utf-8")
gradle = re.sub(r"versionCode\s+4\b", "versionCode 5", gradle, count=1)
gradle = re.sub(r"versionName\s+'1\.4\.0'", "versionName '1.5.0'", gradle, count=1)
GRADLE.write_text(gradle, encoding="utf-8")

print("Applied Asia Flix Clean v1.5 mobile timeline and accelerated seeking patch")
