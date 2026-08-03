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


# v1.6 cleared the selector whenever any video emitted play/playing. Embedded players
# and background previews can emit those events before the user enters fullscreen,
# which made the blue selector disappear from the browsing page. Remove those global
# listeners and limit hiding/restoring to real fullscreen transitions only.
sub_once(
    r"              document\.addEventListener\('play',function\(event\)\{\n"
    r"                if \(event\.target && event\.target\.tagName === 'VIDEO'\) clearVisualFocus\(\);\n"
    r"              \},true\);\n"
    r"              document\.addEventListener\('playing',function\(event\)\{\n"
    r"                if \(event\.target && event\.target\.tagName === 'VIDEO'\) clearVisualFocus\(\);\n"
    r"              \},true\);\n"
    r"              document\.addEventListener\('webkitbeginfullscreen',function\(\)\{ clearVisualFocus\(\); \},true\);\n\n",
    """              const updateFocusForFullscreen = function(){
                const fullscreen = document.fullscreenElement || document.webkitFullscreenElement;
                if (fullscreen) {
                  clearVisualFocus();
                  return;
                }
                setTimeout(function(){
                  if (TV_MODE) {
                    if (focused && visible(focused)) markFocus(focused);
                    else window.__asiaTvInit();
                  }
                },100);
              };
              document.addEventListener('fullscreenchange',updateFocusForFullscreen,true);
              document.addEventListener('webkitfullscreenchange',updateFocusForFullscreen,true);
              document.addEventListener('webkitbeginfullscreen',function(){ clearVisualFocus(); },true);
              document.addEventListener('webkitendfullscreen',updateFocusForFullscreen,true);

""",
    "fullscreen-only focus handling",
    flags=0,
)

# Do not clear the browsing selector merely because the selected item is a video or
# iframe. syncHighlight already hides it once a real fullscreen element exists.
sub_once(
    r"(                  if \(focused\.matches\('video'\)\) \{)\n                    clearVisualFocus\(\);",
    r"\1",
    "retain video selection highlight",
    flags=0,
)
sub_once(
    r"(                  if \(focused\.matches\('iframe'\)\) \{)\n                    clearVisualFocus\(\);",
    r"\1",
    "retain iframe selection highlight",
    flags=0,
)

# Reinforce the large blue direct-on-element selector and make it resilient to site
# styles that use overflow, opacity, or their own outlines on cards.
sub_once(
    r"(                #asia-tv-highlight \{ display:none !important; visibility:hidden !important; \}\n)",
    r"\1" + """                .asia-tv-focus,.asiaflix-tv-focus {
                  outline:6px solid #00d9ff !important;
                  outline-offset:6px !important;
                  border-radius:12px !important;
                  box-shadow:0 0 0 4px rgba(0,16,38,.98),0 0 34px 14px rgba(0,217,255,.98) !important;
                  opacity:1 !important;
                  visibility:visible !important;
                }
""",
    "persistent blue focus styling",
    flags=0,
)

# Re-assert the selector after common page redraws without touching video controls or
# intercepting gestures. This only reapplies the CSS class to the already selected item.
sub_once(
    r"(              document\.addEventListener\('click',function\(event\)\{)",
    """              window.addEventListener('pageshow',function(){ if (TV_MODE) setTimeout(window.__asiaTvInit,120); },true);
              window.addEventListener('resize',function(){ if (TV_MODE) setTimeout(syncHighlight,80); },true);
              document.addEventListener('visibilitychange',function(){
                if (TV_MODE && !document.hidden) setTimeout(function(){
                  if (focused && visible(focused)) markFocus(focused); else window.__asiaTvInit();
                },120);
              },true);
              document.addEventListener('focusin',function(event){
                if (!TV_MODE) return;
                const list = focusables();
                if (event.target && list.includes(event.target)) markFocus(event.target);
              },true);

\1""",
    "focus restoration hooks",
    flags=0,
)

text = text.replace("AsiaFlixTV/1.6 OnnCompatible", "AsiaFlixTV/1.7 OnnCompatible")
text = text.replace("AsiaFlixClean/1.6", "AsiaFlixClean/1.7")
SOURCE.write_text(text, encoding="utf-8")

gradle = GRADLE.read_text(encoding="utf-8")
gradle = re.sub(r"versionCode\s+6\b", "versionCode 7", gradle, count=1)
gradle = re.sub(r"versionName\s+'1\.6\.0'", "versionName '1.7.0'", gradle, count=1)
GRADLE.write_text(gradle, encoding="utf-8")

print("Applied Asia Flix Clean v1.7 persistent focus selector restore")
