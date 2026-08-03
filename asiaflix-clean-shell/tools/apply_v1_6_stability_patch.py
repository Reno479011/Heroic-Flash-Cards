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


# The v1.5 player patch is intentionally not applied in this build. The website/player
# remains responsible for its own controls, timeline, gestures and buffering state.
# Keep the visual blue selection, but do not scale video surfaces or player frames.
sub_once(
    r"(                #asia-tv-highlight \{ display:none !important; visibility:hidden !important; \}\n)",
    r"\1" + """                video.asia-tv-focus,iframe.asia-tv-focus,
                video.asiaflix-tv-focus,iframe.asiaflix-tv-focus,
                .player.asia-tv-focus,.video-player.asia-tv-focus,.jwplayer.asia-tv-focus {
                  transform:none !important;
                  filter:none !important;
                }
""",
    "player focus transform override",
    flags=0,
)

# Remove focus styling before a player begins or enters fullscreen so the webpage is
# not reflowed, scaled or kept above the native fullscreen surface during playback.
sub_once(
    r"(              window\.__asiaTvActivate = function\(\)\{.*?if \(focused\.matches\('video'\)\) \{)\n",
    r"\1\n                    clearVisualFocus();",
    "clear focus before video activation",
)
sub_once(
    r"(                  if \(focused\.matches\('iframe'\)\) \{)\n",
    r"\1\n                    clearVisualFocus();",
    "clear focus before iframe activation",
    flags=0,
)

# Clear the direct blue focus while an HTML5 video is actually playing. Restore it
# only after playback pauses/ends and the user navigates again.
sub_once(
    r"(              document\.addEventListener\('click',function\(event\)\{)",
    """              document.addEventListener('play',function(event){
                if (event.target && event.target.tagName === 'VIDEO') clearVisualFocus();
              },true);
              document.addEventListener('playing',function(event){
                if (event.target && event.target.tagName === 'VIDEO') clearVisualFocus();
              },true);
              document.addEventListener('webkitbeginfullscreen',function(){ clearVisualFocus(); },true);

\1""",
    "playback focus cleanup",
    flags=0,
)

text = text.replace("AsiaFlixTV/1.4 OnnCompatible", "AsiaFlixTV/1.6 OnnCompatible")
text = text.replace("AsiaFlixClean/1.4", "AsiaFlixClean/1.6")
SOURCE.write_text(text, encoding="utf-8")

gradle = GRADLE.read_text(encoding="utf-8")
gradle = re.sub(r"versionCode\s+4\b", "versionCode 6", gradle, count=1)
gradle = re.sub(r"versionName\s+'1\.4\.0'", "versionName '1.6.0'", gradle, count=1)
GRADLE.write_text(gradle, encoding="utf-8")

print("Applied Asia Flix Clean v1.6 player stability rollback")
