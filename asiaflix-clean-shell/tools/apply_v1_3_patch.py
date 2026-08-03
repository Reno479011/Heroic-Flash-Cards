from pathlib import Path

SOURCE = Path("app/src/main/java/org/asiaflix/clean/MainActivity.java")
text = SOURCE.read_text(encoding="utf-8")


def replace_once(old: str, new: str, label: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    text = text.replace(old, new, 1)


replace_once(
    "import android.view.KeyEvent;\nimport android.view.View;",
    "import android.view.KeyEvent;\nimport android.view.MotionEvent;\nimport android.view.View;",
    "MotionEvent import",
)

replace_once(
    "    private static final int SEEK_SECONDS = 10;\n",
    "    private static final int SEEK_SECONDS = 10;\n"
    "    private static final long DOUBLE_TAP_WINDOW_MS = 360L;\n",
    "double-tap constant",
)

replace_once(
    """                .asia-tv-focus {
                  outline:5px solid #19e7ff !important;
                  outline-offset:4px !important;
                  box-shadow:0 0 0 3px #001b20,0 0 24px 9px rgba(25,231,255,.95) !important;
                  transform:scale(1.035) !important;
                  transition:transform .11s ease,box-shadow .11s ease !important;
                  position:relative !important;
                  z-index:2147483000 !important;
                }
""",
    """                .asia-tv-focus {
                  outline:6px solid #149dff !important;
                  outline-offset:5px !important;
                  box-shadow:0 0 0 4px #001c3d,0 0 34px 13px rgba(20,157,255,.98) !important;
                  transform:scale(1.045) !important;
                  transition:transform .10s ease,box-shadow .10s ease !important;
                  position:relative !important;
                  z-index:2147483000 !important;
                }
                #asia-tv-highlight {
                  position:fixed !important;
                  display:none;
                  pointer-events:none !important;
                  box-sizing:border-box !important;
                  border:7px solid #149dff !important;
                  border-radius:16px !important;
                  background:rgba(20,157,255,.16) !important;
                  box-shadow:0 0 0 4px rgba(0,27,61,.98),0 0 38px 15px rgba(20,157,255,.98),inset 0 0 20px rgba(132,219,255,.48) !important;
                  z-index:2147483600 !important;
                  opacity:1 !important;
                  transition:left .08s ease,top .08s ease,width .08s ease,height .08s ease,opacity .08s ease !important;
                }
""",
    "blue highlight CSS",
)

replace_once(
    """              window.__asiaMedia = function(action){
                const videos = allVideos();
                const video = videos.find(function(v){ return !v.paused && !v.ended; }) || videos[0];
                if (!video) return false;
                try {
                  if (action === 'back') {
                    video.currentTime = Math.max(0, Number(video.currentTime || 0) - SEEK);
                    showBadge('↶ ' + SEEK + ' seconds');
                  } else if (action === 'forward') {
                    const end = Number.isFinite(video.duration) ? video.duration : Number(video.currentTime || 0) + SEEK;
                    video.currentTime = Math.min(end, Number(video.currentTime || 0) + SEEK);
                    showBadge('↷ ' + SEEK + ' seconds');
                  } else if (action === 'toggle') {
                    if (video.paused) { video.play().catch(function(){}); showBadge('▶ Play'); }
                    else { video.pause(); showBadge('❚❚ Pause'); }
                  } else if (action === 'fullscreen') {
                    const host = video.closest('.player,.video-player,.jwplayer') || video;
                    const request = host.requestFullscreen || host.webkitRequestFullscreen || video.requestFullscreen || video.webkitRequestFullscreen;
                    if (request) request.call(host);
                  }
                  return true;
                } catch(e) { return false; }
              };

              const cleanPage = function(){
""",
    """              window.__asiaMedia = function(action){
                const videos = allVideos();
                const video = videos.find(function(v){ return !v.paused && !v.ended; }) || videos[0];
                if (!video) return false;
                try {
                  if (action === 'back') {
                    video.currentTime = Math.max(0, Number(video.currentTime || 0) - SEEK);
                    showBadge('↶ ' + SEEK + ' seconds');
                  } else if (action === 'forward') {
                    const end = Number.isFinite(video.duration) ? video.duration : Number(video.currentTime || 0) + SEEK;
                    video.currentTime = Math.min(end, Number(video.currentTime || 0) + SEEK);
                    showBadge('↷ ' + SEEK + ' seconds');
                  } else if (action === 'toggle') {
                    if (video.paused) { video.play().catch(function(){}); showBadge('▶ Play'); }
                    else { video.pause(); showBadge('❚❚ Pause'); }
                  } else if (action === 'fullscreen') {
                    const host = video.closest('.player,.video-player,.jwplayer') || video;
                    const request = host.requestFullscreen || host.webkitRequestFullscreen || video.requestFullscreen || video.webkitRequestFullscreen;
                    if (request) request.call(host);
                  }
                  return true;
                } catch(e) { return false; }
              };

              const mediaSurface = function(target){
                if (!target || !target.closest) return null;
                return target.closest('video,iframe,.player,.video-player,.jwplayer,[class*="video-player"],[class*="player-container"]');
              };
              const seekFromScreenSide = function(clientX,target){
                const surface = mediaSurface(target);
                if (!surface || !visible(surface)) return false;
                const r = surface.getBoundingClientRect();
                if (clientX <= r.left + r.width * 0.42) return window.__asiaMedia('back');
                if (clientX >= r.left + r.width * 0.58) return window.__asiaMedia('forward');
                return false;
              };
              document.addEventListener('dblclick',function(event){
                if (seekFromScreenSide(event.clientX,event.target)) {
                  event.preventDefault();
                  event.stopImmediatePropagation();
                }
              },true);
              let lastTouchAt = 0;
              let lastTouchSide = 0;
              document.addEventListener('touchend',function(event){
                const touch = event.changedTouches && event.changedTouches[0];
                const surface = mediaSurface(event.target);
                if (!touch || !surface || !visible(surface)) return;
                const r = surface.getBoundingClientRect();
                const side = touch.clientX <= r.left + r.width * 0.42 ? -1 : (touch.clientX >= r.left + r.width * 0.58 ? 1 : 0);
                const now = Date.now();
                if (side && side === lastTouchSide && now - lastTouchAt <= 360) {
                  if (seekFromScreenSide(touch.clientX,event.target)) {
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

              const cleanPage = function(){
""",
    "double-tap web player gesture",
)

replace_once(
    """              let focused = null;
              const focusables = function(){
""",
    """              let focused = null;
              const highlightBox = function(){
                let box = document.getElementById('asia-tv-highlight');
                if (!box) {
                  box = document.createElement('div');
                  box.id = 'asia-tv-highlight';
                  (document.body || document.documentElement).appendChild(box);
                }
                return box;
              };
              const syncHighlight = function(){
                const box = highlightBox();
                const fullscreen = document.fullscreenElement || document.webkitFullscreenElement;
                if (!TV_MODE || fullscreen || !focused || !visible(focused)) {
                  box.style.display = 'none';
                  return false;
                }
                const r = focused.getBoundingClientRect();
                const pad = 9;
                const left = Math.max(3,r.left - pad);
                const top = Math.max(3,r.top - pad);
                const right = Math.min(innerWidth - 3,r.right + pad);
                const bottom = Math.min(innerHeight - 3,r.bottom + pad);
                box.style.display = 'block';
                box.style.left = left + 'px';
                box.style.top = top + 'px';
                box.style.width = Math.max(24,right - left) + 'px';
                box.style.height = Math.max(24,bottom - top) + 'px';
                return true;
              };
              const focusables = function(){
""",
    "highlight overlay setup",
)

replace_once(
    """              const markFocus = function(node){
                if (focused) focused.classList.remove('asia-tv-focus');
                focused = node;
                if (!focused) return false;
                focused.classList.add('asia-tv-focus');
                if (!focused.hasAttribute('tabindex')) focused.setAttribute('tabindex','0');
                try { focused.focus({preventScroll:true}); } catch(e) { try { focused.focus(); } catch(ignore) {} }
                try { focused.scrollIntoView({behavior:'smooth',block:'center',inline:'center'}); } catch(e) {}
                return true;
              };
              window.__asiaTvInit = function(){
                cleanPage();
                const items = focusables();
                if (!focused || !visible(focused)) markFocus(items[0] || null);
                return !!focused;
              };
""",
    """              const markFocus = function(node){
                if (focused && focused !== node) focused.classList.remove('asia-tv-focus');
                focused = node;
                if (!focused) { syncHighlight(); return false; }
                focused.classList.add('asia-tv-focus');
                if (!focused.hasAttribute('tabindex')) focused.setAttribute('tabindex','0');
                try { focused.focus({preventScroll:true}); } catch(e) { try { focused.focus(); } catch(ignore) {} }
                try { focused.scrollIntoView({behavior:'smooth',block:'center',inline:'center'}); } catch(e) {}
                requestAnimationFrame(function(){ syncHighlight(); requestAnimationFrame(syncHighlight); });
                setTimeout(syncHighlight,180);
                return true;
              };
              window.__asiaTvInit = function(){
                cleanPage();
                const items = focusables();
                if (!focused || !visible(focused)) markFocus(items[0] || null);
                else { focused.classList.add('asia-tv-focus'); syncHighlight(); }
                return !!focused;
              };
""",
    "persistent focus synchronization",
)

replace_once(
    """              document.addEventListener('submit',function(event){
                const form = event.target;
                if (form && form.action && !isFirstParty(form.action)) { event.preventDefault(); event.stopImmediatePropagation(); }
                else if (form) form.removeAttribute('target');
              },true);

              if (!window.__asiaCleanObserver) {
""",
    """              document.addEventListener('submit',function(event){
                const form = event.target;
                if (form && form.action && !isFirstParty(form.action)) { event.preventDefault(); event.stopImmediatePropagation(); }
                else if (form) form.removeAttribute('target');
              },true);
              window.addEventListener('resize',syncHighlight,true);
              window.addEventListener('scroll',function(){ requestAnimationFrame(syncHighlight); },true);
              document.addEventListener('focusin',function(event){
                if (!TV_MODE) return;
                const target = event.target;
                if (target === focused) syncHighlight();
                else if (focusables().includes(target)) markFocus(target);
              },true);
              document.addEventListener('fullscreenchange',syncHighlight,true);
              document.addEventListener('webkitfullscreenchange',syncHighlight,true);

              if (!window.__asiaCleanObserver) {
""",
    "focus event synchronization",
)

replace_once(
    "settings.setUserAgentString(settings.getUserAgentString() + (tvMode ? \" AsiaFlixTV/1.2 OnnCompatible\" : \" AsiaFlixClean/1.2\"));",
    "settings.setUserAgentString(settings.getUserAgentString() + (tvMode ? \" AsiaFlixTV/1.3 OnnCompatible\" : \" AsiaFlixClean/1.3\"));",
    "user agent version",
)

replace_once(
    """    private final Runnable hideBadge = () -> {
        if (mediaBadge != null) mediaBadge.setVisibility(View.GONE);
    };
    private String lastGoodUrl = HOME_URL;
""",
    """    private final Runnable hideBadge = () -> {
        if (mediaBadge != null) mediaBadge.setVisibility(View.GONE);
    };
    private int pendingSeekDirection;
    private long pendingSeekStartedAt;
    private long lastTouchTapAt;
    private int lastTouchTapSide;
    private final Runnable performPendingSeek = () -> {
        int direction = pendingSeekDirection;
        pendingSeekDirection = 0;
        pendingSeekStartedAt = 0L;
        if (direction != 0) executeSeek(direction);
    };
    private String lastGoodUrl = HOME_URL;
""",
    "native gesture fields",
)

replace_once(
    """            customView = view;
            customViewCallback = callback;
            webView.setVisibility(View.GONE);
""",
    """            customView = view;
            customViewCallback = callback;
            view.setOnTouchListener((target, event) -> handleFullscreenTouch(target, event));
            webView.setVisibility(View.GONE);
""",
    "fullscreen touch listener",
)

replace_once(
    """    private void controlMedia(String action, String badgeText, int fallbackKeyCode) {
""",
    """    private void executeSeek(int direction) {
        if (direction < 0) {
            controlMedia("back", "↶ " + SEEK_SECONDS + " seconds", KeyEvent.KEYCODE_MEDIA_REWIND);
        } else {
            controlMedia("forward", "↷ " + SEEK_SECONDS + " seconds", KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
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

    private boolean handleFullscreenTouch(View target, MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP || target.getWidth() <= 0) return false;
        float x = event.getX();
        int side = x <= target.getWidth() * 0.42f ? -1 : (x >= target.getWidth() * 0.58f ? 1 : 0);
        long now = android.os.SystemClock.uptimeMillis();
        if (side != 0 && side == lastTouchTapSide && now - lastTouchTapAt <= DOUBLE_TAP_WINDOW_MS) {
            lastTouchTapAt = 0L;
            lastTouchTapSide = 0;
            executeSeek(side);
            return true;
        }
        lastTouchTapAt = now;
        lastTouchTapSide = side;
        return false;
    }

    private void controlMedia(String action, String badgeText, int fallbackKeyCode) {
""",
    "native double-tap helpers",
)

replace_once(
    """        if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND || keyCode == KeyEvent.KEYCODE_BUTTON_L1 || (fullscreen && keyCode == KeyEvent.KEYCODE_DPAD_LEFT)) {
            controlMedia("back", "↶ " + SEEK_SECONDS + " seconds", KeyEvent.KEYCODE_MEDIA_REWIND);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD || keyCode == KeyEvent.KEYCODE_BUTTON_R1 || (fullscreen && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            controlMedia("forward", "↷ " + SEEK_SECONDS + " seconds", KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
            return true;
        }
""",
    """        if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND || keyCode == KeyEvent.KEYCODE_BUTTON_L1) {
            executeSeek(-1);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD || keyCode == KeyEvent.KEYCODE_BUTTON_R1) {
            executeSeek(1);
            return true;
        }
        if (fullscreen && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (event.getRepeatCount() == 0) queueFullscreenSeek(-1);
            return true;
        }
        if (fullscreen && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (event.getRepeatCount() == 0) queueFullscreenSeek(1);
            return true;
        }
""",
    "fullscreen remote double press",
)

replace_once(
    """        root.removeView(customView);
        customView = null;
""",
    """        root.removeView(customView);
        customView.setOnTouchListener(null);
        customView = null;
        handler.removeCallbacks(performPendingSeek);
        pendingSeekDirection = 0;
        pendingSeekStartedAt = 0L;
        lastTouchTapAt = 0L;
        lastTouchTapSide = 0;
""",
    "fullscreen gesture cleanup",
)

SOURCE.write_text(text, encoding="utf-8")
print("Applied Asia Flix Clean v1.3 highlight and double-tap patch")
