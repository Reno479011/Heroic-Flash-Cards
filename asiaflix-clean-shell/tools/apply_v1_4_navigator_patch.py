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


# Restore the direct-on-element blue highlight used by the uploaded v1.1 navigator.
# The v1.3 floating overlay is intentionally disabled because it could disappear or
# drift away from the actual movie/show card when the website redrew its layout.
sub_once(
    r"\n\s*\.asia-tv-focus \{.*?\n\s*\}\n\s*#asia-tv-highlight \{.*?\n\s*\}\n",
    """
                a:focus,button:focus,input:focus,select:focus,textarea:focus,
                summary:focus,[role=\"button\"]:focus,[tabindex]:focus,
                .asia-tv-focus,.asiaflix-tv-focus {
                  outline:5px solid #00d9ff !important;
                  outline-offset:5px !important;
                  border-radius:10px !important;
                  box-shadow:0 0 0 4px rgba(0,18,40,.98),0 0 30px 12px rgba(0,217,255,.98) !important;
                  filter:brightness(1.17) contrast(1.07) !important;
                  transform:scale(1.04) !important;
                  transition:transform .10s ease,box-shadow .10s ease,filter .10s ease !important;
                  position:relative !important;
                  z-index:2147483000 !important;
                  scroll-margin:120px !important;
                }
                #asia-tv-highlight { display:none !important; visibility:hidden !important; }
""",
    "v1.1 direct blue focus CSS",
)

# Replace the navigator with the v1.1 geometric focus behavior while retaining the
# current function names expected by MainActivity. It highlights the complete poster
# or card whenever possible, but activates the original clickable control.
sub_once(
    r"              let focused = null;\n.*?              document\.addEventListener\('click',function\(event\)\{",
    """              let focused = null;
              let visualFocused = null;
              const focusSelector = 'a[href],button,input:not([type=\"hidden\"]),select,textarea,summary,[role=\"button\"],[onclick],video,iframe,[tabindex],.movie,.poster,.episode,.play-button,.movie-card,.series-card,.film-card,.post-card,[class*=\"movie-card\"],[class*=\"poster-card\"],[class*=\"episode-card\"]';
              const focusables = function(){
                const list = [];
                document.querySelectorAll(focusSelector).forEach(function(node){
                  if (!visible(node) || node.disabled || node.getAttribute('aria-hidden') === 'true') return;
                  if (node.tagName === 'A' && node.href && !isFirstParty(node.href)) return;
                  if (node.getAttribute('tabindex') === '-1') return;
                  if (node.closest('.adsbygoogle,[data-ad-client],[data-ad-slot],[class*=\"appsgeyser\"],[id*=\"appsgeyser\"]')) return;
                  if (!node.hasAttribute('tabindex')) node.setAttribute('tabindex','0');
                  list.push(node);
                });
                return list;
              };
              const visualNodeFor = function(node){
                if (!node || !node.closest) return node;
                const card = node.closest('.movie,.poster,.episode,.movie-card,.series-card,.film-card,.post-card,[class*=\"movie-card\"],[class*=\"poster-card\"],[class*=\"episode-card\"],article,li');
                if (!card || !visible(card)) return node;
                const r = card.getBoundingClientRect();
                if (r.width < 18 || r.height < 18 || r.width > innerWidth * .96 || r.height > innerHeight * .92) return node;
                return card;
              };
              const clearVisualFocus = function(){
                document.querySelectorAll('.asia-tv-focus,.asiaflix-tv-focus').forEach(function(old){
                  old.classList.remove('asia-tv-focus');
                  old.classList.remove('asiaflix-tv-focus');
                });
                visualFocused = null;
              };
              const syncHighlight = function(){
                const fullscreen = document.fullscreenElement || document.webkitFullscreenElement;
                if (!TV_MODE || fullscreen || !focused || !visible(focused)) {
                  clearVisualFocus();
                  return false;
                }
                const nextVisual = visualNodeFor(focused);
                if (visualFocused !== nextVisual) clearVisualFocus();
                visualFocused = nextVisual;
                if (visualFocused) {
                  visualFocused.classList.add('asia-tv-focus');
                  visualFocused.classList.add('asiaflix-tv-focus');
                }
                return !!visualFocused;
              };
              const markFocus = function(node){
                focused = node;
                clearVisualFocus();
                if (!focused) return false;
                if (!focused.hasAttribute('tabindex')) focused.setAttribute('tabindex','0');
                try { focused.focus({preventScroll:true}); } catch(e) { try { focused.focus(); } catch(ignore) {} }
                visualFocused = visualNodeFor(focused);
                if (visualFocused) {
                  visualFocused.classList.add('asia-tv-focus');
                  visualFocused.classList.add('asiaflix-tv-focus');
                }
                try { visualFocused.scrollIntoView({behavior:'smooth',block:'center',inline:'center'}); } catch(e) {
                  try { focused.scrollIntoView({behavior:'smooth',block:'center',inline:'center'}); } catch(ignore) {}
                }
                setTimeout(syncHighlight,120);
                return true;
              };
              const center = function(node){
                const r = node.getBoundingClientRect();
                return {x:r.left + r.width / 2,y:r.top + r.height / 2,r:r};
              };
              window.__asiaTvInit = function(){
                cleanPage();
                const list = focusables();
                if (!list.length) { focused = null; clearVisualFocus(); return false; }
                const active = document.activeElement;
                if (active && active !== document.body && list.includes(active) && visible(active)) return markFocus(active);
                if (focused && list.includes(focused) && visible(focused)) return markFocus(focused);
                return markFocus(list[0]);
              };
              window.__asiaTvMove = function(direction){
                cleanPage();
                const list = focusables();
                if (!list.length) return false;
                let current = focused;
                if (!current || !list.includes(current) || !visible(current)) {
                  const active = document.activeElement;
                  current = active && list.includes(active) && visible(active) ? active : list[0];
                  return markFocus(current);
                }
                const a = center(current);
                let best = null;
                let bestScore = Number.POSITIVE_INFINITY;
                list.forEach(function(candidate){
                  if (candidate === current) return;
                  const b = center(candidate);
                  const dx = b.x - a.x;
                  const dy = b.y - a.y;
                  let primary = 0;
                  let cross = 0;
                  if (direction === 'left') { if (dx >= -2) return; primary = -dx; cross = Math.abs(dy); }
                  if (direction === 'right') { if (dx <= 2) return; primary = dx; cross = Math.abs(dy); }
                  if (direction === 'up') { if (dy >= -2) return; primary = -dy; cross = Math.abs(dx); }
                  if (direction === 'down') { if (dy <= 2) return; primary = dy; cross = Math.abs(dx); }
                  const overlapBonus = cross < Math.max(a.r.width,a.r.height) * .7 ? -120 : 0;
                  const score = primary + cross * 2.35 + overlapBonus;
                  if (score < bestScore) { bestScore = score; best = candidate; }
                });
                if (best) return markFocus(best);
                const amount = Math.round((direction === 'up' || direction === 'down' ? innerHeight : innerWidth) * .72);
                if (direction === 'up') scrollBy({top:-amount,behavior:'smooth'});
                if (direction === 'down') scrollBy({top:amount,behavior:'smooth'});
                if (direction === 'left') scrollBy({left:-amount,behavior:'smooth'});
                if (direction === 'right') scrollBy({left:amount,behavior:'smooth'});
                setTimeout(function(){ window.__asiaTvInit(); },240);
                return true;
              };
              window.__asiaTvActivate = function(){
                cleanPage();
                if (!focused || !visible(focused)) return window.__asiaTvInit();
                try {
                  if (focused.matches('video')) {
                    window.__asiaMedia('fullscreen');
                    return true;
                  }
                  if (focused.matches('iframe')) {
                    const request = focused.requestFullscreen || focused.webkitRequestFullscreen;
                    if (request) request.call(focused); else focused.click();
                    return true;
                  }
                  let action = focused;
                  if (!action.matches('a,button,input,select,textarea,summary,[role=\"button\"],[onclick]')) {
                    action = action.querySelector('a[href],button,[role=\"button\"],[onclick]') || action;
                  }
                  action.click();
                  return true;
                } catch(e) { return false; }
              };

              document.addEventListener('click',function(event){""",
    "v1.1-style geometric navigator",
)

# Broaden Android TV detection for Onn devices and automatically enable TV mode when
# a hardware D-pad is used, even if a firmware build reports a non-TV UI mode.
if "import android.content.pm.PackageManager;" not in text:
    text = text.replace(
        "import android.content.pm.ActivityInfo;\n",
        "import android.content.pm.ActivityInfo;\nimport android.content.pm.PackageManager;\n",
        1,
    )

sub_once(
    r"        tvMode = uiMode != null && uiMode\.getCurrentModeType\(\) == Configuration\.UI_MODE_TYPE_TELEVISION;",
    """        tvMode = (uiMode != null && uiMode.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION)
                || getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK)
                || getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION);""",
    "Onn TV detection",
    flags=0,
)

sub_once(
    r"(        if \(keyCode == KeyEvent\.KEYCODE_BACK\) \{\n            handleBack\(\);\n            return true;\n        \}\n)",
    r"\1\n        if (!tvMode && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT\n                || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN\n                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER\n                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {\n            tvMode = true;\n            injectPageScript();\n        }\n",
    "hardware D-pad TV fallback",
    flags=0,
)

text = text.replace("AsiaFlixTV/1.3 OnnCompatible", "AsiaFlixTV/1.4 OnnCompatible")
text = text.replace("AsiaFlixClean/1.3", "AsiaFlixClean/1.4")
SOURCE.write_text(text, encoding="utf-8")

gradle = GRADLE.read_text(encoding="utf-8")
gradle = re.sub(r"versionCode\s+3\b", "versionCode 4", gradle, count=1)
gradle = re.sub(r"versionName\s+'1\.2\.0'", "versionName '1.4.0'", gradle, count=1)
GRADLE.write_text(gradle, encoding="utf-8")

print("Applied Asia Flix Clean v1.4 navigator patch")
