package org.asiaflix.clean;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.SafeBrowsingResponse;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String HOME_URL = "https://asiaflix.org/";
    private static final String FIRST_PARTY_HOST = "asiaflix.org";
    private static final int SEEK_SECONDS = 10;

    private static final String[] BLOCKED_HOST_PARTS = {
            "appsgeyser", "doubleclick", "googlesyndication", "googleadservices",
            "adservice.google", "admob", "appodeal", "applovin", "adcolony",
            "ironsrc", "ironsource", "unityads", "pangle", "byteoversea",
            "amazon-adsystem", "criteo", "taboola", "outbrain", "pubmatic",
            "rubiconproject", "openx", "smartadserver", "casalemedia", "adform",
            "scorecardresearch", "yieldmo", "media.net", "mgid", "revcontent",
            "adskeeper", "bidvertiser", "admaven", "adsterra", "monetag",
            "propellerads", "popads", "popcash", "clickadu", "exoclick",
            "trafficjunky", "hilltopads", "richads", "pushground", "zeropark",
            "go2cloud", "offerstrack", "onclick", "tracking.", "trk.",
            "xm.com", "xmglobal", "xmbroker", "xmtrading", "xm-partners"
    };

    private static final String[] BLOCKED_URL_PARTS = {
            "xm-ultra-low", "xm_ultra_low", "low-cost-trading", "low_cost_trading",
            "islamic-account-available", "capital-is-at-risk", "xm-banner", "xm_banner",
            "googleads", "adsystem", "/advert/", "/advertise/", "/adserver/",
            "popunder", "popup-ad", "interstitial-ad"
    };

    private static final String PAGE_SCRIPT = """
            (function(){
              const TV_MODE = __TV_MODE__;
              const FIRST = 'asiaflix.org';
              const SEEK = 10;
              const blockedTokens = [
                'appsgeyser','doubleclick','googlesyndication','googleadservices','adservice.google',
                'admob','appodeal','applovin','adcolony','ironsrc','ironsource','unityads',
                'pangle','amazon-adsystem','criteo','taboola','outbrain','pubmatic',
                'rubiconproject','openx','smartadserver','casalemedia','adform',
                'scorecardresearch','yieldmo','media.net','mgid','revcontent','adskeeper',
                'bidvertiser','admaven','adsterra','monetag','propellerads','popads',
                'popcash','clickadu','exoclick','trafficjunky','hilltopads','richads',
                'pushground','zeropark','go2cloud','offerstrack','onclick','xmglobal',
                'xmbroker','xmtrading','xm-partners','xm.com'
              ];
              const xmPattern = /(xm\s*ultra\s*low|low[\s-]*cost\s*trading|islamic\s*account\s*available|your\s*capital\s*is\s*at\s*risk|start\s*now|xmglobal|xmtrading|xm-partners|xmbroker|\bxm\b)/i;
              const scamPattern = /(android security|security alert|wiretapping|your android has been hacked|virus detected|run a test|google security support|install random app|buy premium|appsgeyser)/i;
              const adTokenPattern = /(^|[\s_-])(ad|ads|advert|advertisement|sponsor|sponsored|banner)([\s_-]|$)/i;
              const parseUrl = function(value){ try { return new URL(value, location.href); } catch(e) { return null; } };
              const isFirstParty = function(value){
                const u = parseUrl(value);
                return !!u && (u.protocol === 'https:' || u.protocol === 'http:') && (u.hostname === FIRST || u.hostname.endsWith('.' + FIRST));
              };
              const isBlockedUrl = function(value){
                const u = parseUrl(value);
                if (!u) return true;
                if (u.protocol === 'data:' || u.protocol === 'blob:' || u.protocol === 'about:') return false;
                if (u.protocol !== 'https:' && u.protocol !== 'http:') return true;
                const text = (u.hostname + u.pathname + u.search).toLowerCase();
                return blockedTokens.some(function(token){ return text.indexOf(token) !== -1; });
              };
              const visible = function(node){
                if (!node || !node.isConnected) return false;
                const r = node.getBoundingClientRect();
                const s = getComputedStyle(node);
                return r.width > 2 && r.height > 2 && s.display !== 'none' && s.visibility !== 'hidden' && Number(s.opacity || 1) > 0.05;
              };
              const fingerprint = function(node){
                if (!node) return '';
                let text = '';
                try { text += ' ' + (node.innerText || node.textContent || '').slice(0, 1200); } catch(e) {}
                ['id','class','title','aria-label','href','src','data-src','alt'].forEach(function(name){
                  try { text += ' ' + (node.getAttribute(name) || ''); } catch(e) {}
                });
                try {
                  const image = node.matches && node.matches('img') ? node : node.querySelector && node.querySelector('img');
                  if (image) text += ' ' + (image.src || '') + ' ' + (image.alt || '') + ' ' + (image.title || '');
                } catch(e) {}
                return text;
              };
              const pickRemovableContainer = function(node){
                let current = node;
                let best = node;
                for (let i = 0; i < 5 && current && current.parentElement; i++) {
                  const parent = current.parentElement;
                  if (parent.matches('html,body,main,header,nav')) break;
                  const r = parent.getBoundingClientRect();
                  if (r.height > 430 || r.width < innerWidth * 0.35) break;
                  best = parent;
                  current = parent;
                }
                return best;
              };
              const erase = function(node){
                if (!node || !node.isConnected || node.matches('html,body,main,header,nav')) return;
                const target = pickRemovableContainer(node);
                try { target.remove(); } catch(e) {
                  try { target.style.setProperty('display','none','important'); } catch(ignore) {}
                }
              };

              try { window.open = function(){ return null; }; } catch(e) {}
              try { window.showModalDialog = function(){ return null; }; } catch(e) {}
              try {
                if ('Notification' in window) Notification.requestPermission = function(){ return Promise.resolve('denied'); };
              } catch(e) {}

              const style = document.getElementById('asiaflix-clean-style') || document.createElement('style');
              style.id = 'asiaflix-clean-style';
              style.textContent = `
                html,body { margin:0 !important; padding:0 !important; min-width:100% !important; min-height:100% !important; }
                .adsbygoogle,[data-ad-client],[data-ad-slot],[id^="google_ads"],ins.adsbygoogle,
                [class*="appsgeyser"],[id*="appsgeyser"],[class*="xm-banner"],[id*="xm-banner"],
                [class*="xm_ad"],[id*="xm_ad"],[class*="xm-ad"],[id*="xm-ad"] {
                  display:none !important; visibility:hidden !important; width:0 !important; height:0 !important;
                  min-height:0 !important; max-height:0 !important; margin:0 !important; padding:0 !important;
                }
                .asia-tv-focus {
                  outline:5px solid #19e7ff !important;
                  outline-offset:4px !important;
                  box-shadow:0 0 0 3px #001b20,0 0 24px 9px rgba(25,231,255,.95) !important;
                  transform:scale(1.035) !important;
                  transition:transform .11s ease,box-shadow .11s ease !important;
                  position:relative !important;
                  z-index:2147483000 !important;
                }
                video:fullscreen,iframe:fullscreen,video:-webkit-full-screen,iframe:-webkit-full-screen {
                  position:fixed !important; inset:0 !important; width:100vw !important; height:100vh !important;
                  max-width:none !important; max-height:none !important; margin:0 !important; padding:0 !important;
                  object-fit:contain !important; background:#000 !important; z-index:2147483647 !important;
                }
                #asia-media-badge {
                  position:fixed; left:50%; top:50%; transform:translate(-50%,-50%); z-index:2147483647;
                  background:rgba(0,0,0,.82); color:#fff; border:3px solid #19e7ff; border-radius:14px;
                  font:700 28px sans-serif; padding:14px 22px; pointer-events:none; display:none;
                }
              `;
              if (!style.parentNode) (document.head || document.documentElement).appendChild(style);

              const showBadge = function(text){
                let badge = document.getElementById('asia-media-badge');
                if (!badge) {
                  badge = document.createElement('div');
                  badge.id = 'asia-media-badge';
                  document.documentElement.appendChild(badge);
                }
                badge.textContent = text;
                badge.style.display = 'block';
                clearTimeout(window.__asiaBadgeTimer);
                window.__asiaBadgeTimer = setTimeout(function(){ badge.style.display = 'none'; }, 800);
              };

              const allVideos = function(){
                const found = Array.from(document.querySelectorAll('video'));
                document.querySelectorAll('iframe').forEach(function(frame){
                  try { found.push.apply(found, Array.from(frame.contentDocument.querySelectorAll('video'))); } catch(e) {}
                });
                return found.filter(function(v){ return visible(v); });
              };
              window.__asiaMedia = function(action){
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
                const explicitSelectors = [
                  '.adsbygoogle','[data-ad-client]','[data-ad-slot]','[id^="google_ads"]','ins.adsbygoogle',
                  '[class*="appsgeyser"]','[id*="appsgeyser"]',
                  'iframe[src*="doubleclick"]','iframe[src*="googlesyndication"]',
                  'iframe[src*="adsterra"]','iframe[src*="monetag"]','iframe[src*="propellerads"]',
                  'iframe[src*="popads"]','iframe[src*="popcash"]','iframe[src*="clickadu"]',
                  'a[href*="xm.com"]','a[href*="xmglobal"]','a[href*="xmtrading"]','a[href*="xmbroker"]',
                  'img[src*="xm-ultra"]','img[src*="xm_ultra"]','img[alt*="XM Ultra" i]','img[alt*="Low-Cost Trading" i]'
                ];
                explicitSelectors.forEach(function(selector){
                  try { document.querySelectorAll(selector).forEach(erase); } catch(e) {}
                });

                document.querySelectorAll('img,iframe,a,ins,div,section,aside,figure').forEach(function(node){
                  if (!visible(node)) return;
                  const r = node.getBoundingClientRect();
                  const mark = fingerprint(node);
                  const nearTop = r.top < Math.max(720, innerHeight * 0.58) && r.bottom > 0;
                  const wideBanner = r.width > innerWidth * 0.58 && r.height >= 45 && r.height <= Math.min(330, innerHeight * 0.36) && r.width / Math.max(1,r.height) > 4.0;
                  const adClass = adTokenPattern.test((node.id || '') + ' ' + (node.className || ''));
                  const containsMedia = !!(node.querySelector && node.querySelector('img,iframe'));
                  if (xmPattern.test(mark)) { erase(node); return; }
                  if (nearTop && wideBanner && (node.matches('img,iframe,a,ins') || containsMedia || adClass)) { erase(node); return; }
                  if (adClass && wideBanner) { erase(node); return; }
                  if (scamPattern.test(mark) && (r.width * r.height > innerWidth * innerHeight * 0.18 || getComputedStyle(node).position === 'fixed')) erase(node);
                });

                document.querySelectorAll('a,area,form').forEach(function(node){
                  const value = node.href || node.action || node.getAttribute('href') || node.getAttribute('action') || '';
                  const lower = value.trim().toLowerCase();
                  if (!value || lower.startsWith('#') || lower.startsWith('javascript:')) {
                    node.removeAttribute('target');
                    return;
                  }
                  if (!isFirstParty(value)) {
                    node.removeAttribute('target');
                    node.onclick = function(event){ event.preventDefault(); event.stopImmediatePropagation(); return false; };
                  } else {
                    node.removeAttribute('target');
                  }
                });

                document.querySelectorAll('iframe').forEach(function(frame){
                  const src = frame.getAttribute('src') || '';
                  if (src && isBlockedUrl(src)) { erase(frame); return; }
                  if (src && !isFirstParty(src)) {
                    frame.setAttribute('sandbox','allow-scripts allow-same-origin allow-forms allow-presentation');
                    frame.setAttribute('allow','autoplay; fullscreen; encrypted-media; picture-in-picture');
                    frame.setAttribute('allowfullscreen','true');
                  }
                });
              };

              let focused = null;
              const focusables = function(){
                const selector = 'a[href],button,input:not([type="hidden"]),select,textarea,[role="button"],[tabindex],video,iframe,.movie,.poster,.episode,.play-button';
                return Array.from(document.querySelectorAll(selector)).filter(function(node){
                  if (!visible(node) || node.disabled) return false;
                  if (node.closest('.adsbygoogle,[data-ad-client],[data-ad-slot],[class*="appsgeyser"],[id*="appsgeyser"]')) return false;
                  return true;
                });
              };
              const markFocus = function(node){
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
              window.__asiaTvMove = function(direction){
                cleanPage();
                const items = focusables();
                if (!items.length) return false;
                if (!focused || !items.includes(focused)) return markFocus(items[0]);
                const base = focused.getBoundingClientRect();
                const bx = base.left + base.width / 2;
                const by = base.top + base.height / 2;
                let best = null;
                let score = Infinity;
                items.forEach(function(item){
                  if (item === focused) return;
                  const r = item.getBoundingClientRect();
                  const x = r.left + r.width / 2;
                  const y = r.top + r.height / 2;
                  const dx = x - bx;
                  const dy = y - by;
                  if (direction === 'left' && dx >= -4) return;
                  if (direction === 'right' && dx <= 4) return;
                  if (direction === 'up' && dy >= -4) return;
                  if (direction === 'down' && dy <= 4) return;
                  const primary = (direction === 'left' || direction === 'right') ? Math.abs(dx) : Math.abs(dy);
                  const secondary = (direction === 'left' || direction === 'right') ? Math.abs(dy) : Math.abs(dx);
                  const candidate = primary + secondary * 2.25;
                  if (candidate < score) { score = candidate; best = item; }
                });
                return markFocus(best || focused);
              };
              window.__asiaTvActivate = function(){
                if (!focused || !visible(focused)) return window.__asiaTvInit();
                try {
                  if (focused.matches('video')) {
                    window.__asiaMedia('fullscreen');
                    return true;
                  }
                  if (focused.matches('iframe')) {
                    const request = focused.requestFullscreen || focused.webkitRequestFullscreen;
                    if (request) request.call(focused);
                    else focused.click();
                    return true;
                  }
                  focused.click();
                  return true;
                } catch(e) { return false; }
              };

              document.addEventListener('click',function(event){
                const target = event.target && event.target.closest ? event.target.closest('a,area') : null;
                if (!target) return;
                const raw = target.getAttribute('href') || '';
                const lower = raw.trim().toLowerCase();
                if (!raw || lower.startsWith('#') || lower.startsWith('javascript:')) { target.removeAttribute('target'); return; }
                if (!isFirstParty(raw)) { event.preventDefault(); event.stopImmediatePropagation(); return false; }
                target.removeAttribute('target');
              },true);
              document.addEventListener('submit',function(event){
                const form = event.target;
                if (form && form.action && !isFirstParty(form.action)) { event.preventDefault(); event.stopImmediatePropagation(); }
                else if (form) form.removeAttribute('target');
              },true);

              if (!window.__asiaCleanObserver) {
                let timer = 0;
                window.__asiaCleanObserver = new MutationObserver(function(){
                  clearTimeout(timer);
                  timer = setTimeout(function(){ cleanPage(); if (TV_MODE) window.__asiaTvInit(); },160);
                });
                window.__asiaCleanObserver.observe(document.documentElement,{childList:true,subtree:true,attributes:true,attributeFilter:['href','src','target','action','class','id','style','alt','title']});
                let passes = 0;
                const rapidCleaner = setInterval(function(){
                  cleanPage();
                  if (TV_MODE) window.__asiaTvInit();
                  if (++passes >= 40) clearInterval(rapidCleaner);
                },500);
              }
              cleanPage();
              if (TV_MODE) window.__asiaTvInit();
            })();
            """;

    private FrameLayout root;
    private WebView webView;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private TextView mediaBadge;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideBadge = () -> {
        if (mediaBadge != null) mediaBadge.setVisibility(View.GONE);
    };
    private String lastGoodUrl = HOME_URL;
    private boolean tvMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        UiModeManager uiMode = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        tvMode = uiMode != null && uiMode.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(webView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mediaBadge = new TextView(this);
        mediaBadge.setTextColor(Color.WHITE);
        mediaBadge.setTextSize(tvMode ? 26f : 20f);
        mediaBadge.setGravity(Gravity.CENTER);
        mediaBadge.setBackgroundColor(0xDD000000);
        mediaBadge.setPadding(28, 18, 28, 18);
        mediaBadge.setVisibility(View.GONE);
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        root.addView(mediaBadge, badgeParams);

        configureWebView();
        enterImmersiveMode();

        String restored = savedInstanceState == null ? null : savedInstanceState.getString("lastGoodUrl");
        if (isFirstParty(restored)) lastGoodUrl = restored;
        webView.loadUrl(lastGoodUrl);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
        }
    }

    private void configureWebView() {
        WebView.setWebContentsDebuggingEnabled(false);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setGeolocationEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setSaveFormData(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setUserAgentString(settings.getUserAgentString() + (tvMode ? " AsiaFlixTV/1.2 OnnCompatible" : " AsiaFlixClean/1.2"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) settings.setSafeBrowsingEnabled(true);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus(View.FOCUS_DOWN);
        webView.setWebViewClient(new CleanWebViewClient());
        webView.setWebChromeClient(new CleanWebChromeClient());
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> { });
    }

    private void injectPageScript() {
        String script = PAGE_SCRIPT.replace("__TV_MODE__", tvMode ? "true" : "false");
        webView.evaluateJavascript(script, null);
        webView.postDelayed(() -> webView.evaluateJavascript(script, null), 900);
        webView.postDelayed(() -> webView.evaluateJavascript(script, null), 2600);
    }

    private final class CleanWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (!request.isForMainFrame()) return isBlockedResource(uri);
            return shouldBlockTopLevel(uri == null ? null : uri.toString());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return shouldBlockTopLevel(url);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return isBlockedResource(request.getUrl()) ? emptyResponse() : null;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            Uri uri = safeParse(url);
            return isBlockedResource(uri) ? emptyResponse() : null;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (isFirstParty(url)) lastGoodUrl = url;
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (isFirstParty(url)) {
                lastGoodUrl = url;
                injectPageScript();
            } else {
                restoreLastGoodPage();
            }
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) restoreLastGoodPage();
            super.onReceivedError(view, request, error);
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            if (request.isForMainFrame() && errorResponse.getStatusCode() >= 400) restoreLastGoodPage();
            super.onReceivedHttpError(view, request, errorResponse);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.cancel();
            restoreLastGoodPage();
        }

        @Override
        public void onSafeBrowsingHit(WebView view, WebResourceRequest request, int threatType, SafeBrowsingResponse callback) {
            callback.backToSafety(true);
            restoreLastGoodPage();
        }
    }

    private final class CleanWebChromeClient extends WebChromeClient {
        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            return false;
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            request.deny();
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, false, false);
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (customView != null) {
                callback.onCustomViewHidden();
                return;
            }
            customView = view;
            customViewCallback = callback;
            webView.setVisibility(View.GONE);
            root.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mediaBadge.bringToFront();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            enterImmersiveMode();
        }

        @Override
        public void onHideCustomView() {
            hideCustomView();
        }
    }

    private boolean shouldBlockTopLevel(String url) {
        if (isFirstParty(url)) return false;
        restoreLastGoodPage();
        return true;
    }

    private boolean isBlockedResource(Uri uri) {
        if (uri == null) return true;
        String scheme = safeLower(uri.getScheme());
        if ("data".equals(scheme) || "blob".equals(scheme) || "about".equals(scheme)) return false;
        if (!"http".equals(scheme) && !"https".equals(scheme)) return true;
        String host = safeLower(uri.getHost());
        String full = uri.toString().toLowerCase(Locale.US);
        for (String token : BLOCKED_HOST_PARTS) if (host.contains(token)) return true;
        for (String token : BLOCKED_URL_PARTS) if (full.contains(token)) return true;
        return false;
    }

    private boolean isFirstParty(String url) {
        Uri uri = safeParse(url);
        if (uri == null) return false;
        String scheme = safeLower(uri.getScheme());
        String host = safeLower(uri.getHost());
        return ("https".equals(scheme) || "http".equals(scheme)) && (FIRST_PARTY_HOST.equals(host) || host.endsWith("." + FIRST_PARTY_HOST));
    }

    private Uri safeParse(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Uri.parse(value); } catch (RuntimeException ignored) { return null; }
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US);
    }

    private WebResourceResponse emptyResponse() {
        return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
    }

    private void restoreLastGoodPage() {
        webView.post(() -> {
            String current = webView.getUrl();
            if (!isFirstParty(current)) {
                webView.stopLoading();
                webView.loadUrl(isFirstParty(lastGoodUrl) ? lastGoodUrl : HOME_URL);
            }
        });
    }

    private void runTvCommand(String command) {
        webView.evaluateJavascript("window.__asiaTv" + command + " ? window.__asiaTv" + command + "() : false", null);
    }

    private void moveTvFocus(String direction) {
        webView.evaluateJavascript("window.__asiaTvMove ? window.__asiaTvMove('" + direction + "') : false", null);
    }

    private void controlMedia(String action, String badgeText, int fallbackKeyCode) {
        showMediaBadge(badgeText);
        String js = "window.__asiaMedia ? window.__asiaMedia('" + action + "') : false";
        webView.evaluateJavascript(js, result -> {
            if (!"true".equals(result)) dispatchFallbackMediaKey(fallbackKeyCode);
        });
    }

    private void dispatchFallbackMediaKey(int keyCode) {
        long now = android.os.SystemClock.uptimeMillis();
        KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0);
        KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0);
        if (customView != null) {
            customView.dispatchKeyEvent(down);
            customView.dispatchKeyEvent(up);
        }
        webView.dispatchKeyEvent(down);
        webView.dispatchKeyEvent(up);
    }

    private void showMediaBadge(String text) {
        mediaBadge.setText(text);
        mediaBadge.setVisibility(View.VISIBLE);
        mediaBadge.bringToFront();
        handler.removeCallbacks(hideBadge);
        handler.postDelayed(hideBadge, 850);
    }

    private void hideCustomView() {
        if (customView == null) return;
        root.removeView(customView);
        customView = null;
        webView.setVisibility(View.VISIBLE);
        if (customViewCallback != null) customViewCallback.onCustomViewHidden();
        customViewCallback = null;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        enterImmersiveMode();
        webView.requestFocus();
    }

    private void handleBack() {
        if (customView != null) {
            hideCustomView();
            return;
        }
        String current = webView.getUrl();
        if (!isFirstParty(current)) {
            webView.loadUrl(isFirstParty(lastGoodUrl) ? lastGoodUrl : HOME_URL);
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBack();
            return true;
        }

        boolean fullscreen = customView != null;
        if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND || keyCode == KeyEvent.KEYCODE_BUTTON_L1 || (fullscreen && keyCode == KeyEvent.KEYCODE_DPAD_LEFT)) {
            controlMedia("back", "↶ " + SEEK_SECONDS + " seconds", KeyEvent.KEYCODE_MEDIA_REWIND);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD || keyCode == KeyEvent.KEYCODE_BUTTON_R1 || (fullscreen && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            controlMedia("forward", "↷ " + SEEK_SECONDS + " seconds", KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE || (fullscreen && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER))) {
            controlMedia("toggle", "Play / Pause", KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            return true;
        }

        if (tvMode && !fullscreen) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) { moveTvFocus("left"); return true; }
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) { moveTvFocus("right"); return true; }
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) { moveTvFocus("up"); return true; }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) { moveTvFocus("down"); return true; }
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A) {
                runTvCommand("Activate");
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    protected void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersiveMode();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("lastGoodUrl", lastGoodUrl);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        enterImmersiveMode();
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
        }
        super.onDestroy();
    }
}
