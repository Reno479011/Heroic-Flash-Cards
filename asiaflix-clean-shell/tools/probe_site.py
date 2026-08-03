from __future__ import annotations

import json
import os
import re
import time
from pathlib import Path
from typing import Any

from selenium import webdriver
from selenium.common.exceptions import WebDriverException
from selenium.webdriver.chrome.options import Options

OUT = Path(os.environ.get("PROBE_OUT", "probe"))
OUT.mkdir(parents=True, exist_ok=True)

HOME = "https://asiaflix.org/"
DRAMA = "https://asiaflix.org/drama/the-vacation-principle"


def build_driver(width: int, height: int, mobile: bool = False) -> webdriver.Chrome:
    options = Options()
    options.add_argument("--headless=new")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--disable-gpu")
    options.add_argument("--autoplay-policy=no-user-gesture-required")
    options.add_argument("--disable-notifications")
    options.add_argument(f"--window-size={width},{height}")
    options.set_capability("goog:loggingPrefs", {"browser": "ALL", "performance": "ALL"})
    if mobile:
        options.add_experimental_option(
            "mobileEmulation",
            {
                "deviceMetrics": {"width": width, "height": height, "pixelRatio": 3.0},
                "userAgent": (
                    "Mozilla/5.0 (Linux; Android 14; SM-S911U) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Mobile Safari/537.36"
                ),
            },
        )
    return webdriver.Chrome(options=options)


def js(driver: webdriver.Chrome, script: str) -> Any:
    return driver.execute_script(script)


def capture(driver: webdriver.Chrome, name: str) -> dict[str, Any]:
    time.sleep(5)
    driver.save_screenshot(str(OUT / f"{name}.png"))
    (OUT / f"{name}.html").write_text(driver.page_source, encoding="utf-8")

    dom = js(
        driver,
        r"""
        const clean = value => String(value || '').replace(/\s+/g,' ').trim();
        const rect = node => {
          const r = node.getBoundingClientRect();
          return {x:r.x,y:r.y,width:r.width,height:r.height,display:getComputedStyle(node).display,
                  visibility:getComputedStyle(node).visibility,position:getComputedStyle(node).position};
        };
        const fingerprint = node => {
          let text = clean(node.innerText || node.textContent).slice(0,800);
          for (const key of ['id','class','title','aria-label','href','src','data-src','alt']) {
            try { text += ' ' + clean(node.getAttribute(key)); } catch(e) {}
          }
          return clean(text);
        };
        const adRe = /(xm\s*ultra\s*low|low[\s-]*cost\s*trading|islamic\s*account|capital\s*is\s*at\s*risk|xmglobal|xmtrading|xm-partners|\bxm\b)/i;
        const suspicious = Array.from(document.querySelectorAll('body *'))
          .map(node => ({node, mark:fingerprint(node)}))
          .filter(item => adRe.test(item.mark))
          .slice(0,80)
          .map(item => ({tag:item.node.tagName, mark:item.mark, rect:rect(item.node), outer:item.node.outerHTML.slice(0,5000)}));
        const iframes = Array.from(document.querySelectorAll('iframe')).map(node => ({
          src:node.src || node.getAttribute('src') || '', title:node.title || '', name:node.name || '',
          sandbox:node.getAttribute('sandbox') || '', allow:node.getAttribute('allow') || '', rect:rect(node),
          outer:node.outerHTML.slice(0,3000)
        }));
        const images = Array.from(document.images).map(node => ({
          src:node.currentSrc || node.src || '', alt:node.alt || '', title:node.title || '', rect:rect(node),
          parent:(node.parentElement && node.parentElement.outerHTML || '').slice(0,2200)
        })).filter(item => item.rect.width > 250 || adRe.test(item.src + ' ' + item.alt + ' ' + item.title));
        const videos = Array.from(document.querySelectorAll('video')).map(node => ({
          src:node.currentSrc || node.src || '', poster:node.poster || '', controls:node.controls,
          paused:node.paused, duration:Number.isFinite(node.duration) ? node.duration : null, rect:rect(node),
          outer:node.outerHTML.slice(0,3000)
        }));
        const links = Array.from(document.querySelectorAll('a[href]')).map(node => ({
          href:node.href, text:clean(node.innerText || node.textContent).slice(0,180), target:node.target || ''
        })).filter(item => /episode|watch|player|stream|server|xm|trade|advert|banner/i.test(item.href + ' ' + item.text)).slice(0,300);
        return {url:location.href,title:document.title,suspicious,iframes,images,videos,links};
        """,
    )

    requests: list[dict[str, Any]] = []
    for entry in driver.get_log("performance"):
        try:
            message = json.loads(entry["message"])["message"]
            if message.get("method") != "Network.requestWillBeSent":
                continue
            req = message["params"]["request"]
            url = req.get("url", "")
            if re.search(r"xm|banner|advert|doubleclick|googlead|adsterra|monetag|pop|click|player|stream|\.m3u8|\.mp4", url, re.I):
                requests.append({"url": url, "method": req.get("method"), "type": message["params"].get("type")})
        except Exception:
            continue

    console = []
    try:
        console = driver.get_log("browser")
    except WebDriverException:
        pass

    result = {"dom": dom, "requests": requests[-1000:], "console": console[-300:]}
    (OUT / f"{name}.json").write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8")
    return result


def probe(label: str, width: int, height: int, mobile: bool) -> None:
    driver = build_driver(width, height, mobile)
    try:
        driver.get(HOME)
        capture(driver, f"{label}_home")

        driver.get(DRAMA)
        drama = capture(driver, f"{label}_drama")

        candidates = drama["dom"].get("links", [])
        watch = next(
            (
                item["href"]
                for item in candidates
                if re.search(r"/watch/|/episode/|episode-|[?&](ep|episode)=", item.get("href", ""), re.I)
            ),
            None,
        )
        if watch:
            driver.get(watch)
            capture(driver, f"{label}_player")
        else:
            # Click the first visible episode-like control when the page uses JavaScript routing.
            clicked = js(
                driver,
                r"""
                const nodes = Array.from(document.querySelectorAll('a,button,[role="button"],[tabindex]'));
                const target = nodes.find(n => {
                  const text = String(n.innerText || n.textContent || n.getAttribute('aria-label') || '').trim();
                  const r = n.getBoundingClientRect();
                  return r.width > 2 && r.height > 2 && /^(ep(isode)?\s*)?1(\b|\s)/i.test(text);
                });
                if (!target) return false;
                target.click();
                return true;
                """,
            )
            if clicked:
                time.sleep(8)
                capture(driver, f"{label}_player")
    finally:
        driver.quit()


if __name__ == "__main__":
    probe("mobile", 412, 915, True)
    probe("tv", 1920, 1080, False)
    print(f"Wrote probe artifacts to {OUT.resolve()}")
