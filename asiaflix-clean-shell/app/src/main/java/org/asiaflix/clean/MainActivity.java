package org.asiaflix.clean;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
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

import java.io.ByteArrayInputStream;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String HOME_URL = "https://asiaflix.org/";
    private static final String FIRST_PARTY_HOST = "asiaflix.org";

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
            "go2cloud", "offerstrack", "onclick", "trk.", "tracking."
    };

    private static final String CLEANUP_SCRIPT = """
            javascript:(function(){
              if (window.__asiaFlixCleanInstalled) { try { window.__asiaFlixCleanRun(); } catch(e) {} return; }
              window.__asiaFlixCleanInstalled = true;
              const FIRST = 'asiaflix.org';
              const BLOCKED = ['appsgeyser','doubleclick','googlesyndication','googleadservices','adservice.google','admob','appodeal','applovin','adcolony','ironsrc','ironsource','unityads','pangle','amazon-adsystem','criteo','taboola','outbrain','pubmatic','rubiconproject','openx','smartadserver','casalemedia','adform','scorecardresearch','yieldmo','media.net','mgid','revcontent','adskeeper','bidvertiser','admaven','adsterra','monetag','propellerads','popads','popcash','clickadu','exoclick','trafficjunky','hilltopads','richads','pushground','zeropark','go2cloud','offerstrack','onclick'];
              const parse = function(value) { try { return new URL(value, location.href); } catch(e) { return null; } };
              const firstParty = function(value) {
                const u = parse(value);
                return !!u && (u.protocol === 'https:' || u.protocol === 'http:') && (u.hostname === FIRST || u.hostname.endsWith('.' + FIRST));
              };
              const blockedUrl = function(value) {
                const u = parse(value);
                if (!u) return true;
                if (u.protocol === 'data:' || u.protocol === 'blob:' || u.protocol === 'about:') return false;
                if (u.protocol !== 'https:' && u.protocol !== 'http:') return true;
                const h = u.hostname.toLowerCase();
                return BLOCKED.some(function(token){ return h.indexOf(token) !== -1; });
              };

              try { window.open = function(){ return null; }; } catch(e) {}
              try { window.showModalDialog = function(){ return null; }; } catch(e) {}
              try {
                if ('Notification' in window) {
                  Notification.requestPermission = function(){ return Promise.resolve('denied'); };
                }
              } catch(e) {}

              document.addEventListener('click', function(event){
                const target = event.target && event.target.closest ? event.target.closest('a,area') : null;
                if (!target) return;
                const raw = target.getAttribute('href') || '';
                const trimmed = raw.trim().toLowerCase();
                if (!raw || trimmed.startsWith('#') || trimmed.startsWith('javascript:')) {
                  target.removeAttribute('target');
                  return;
                }
                if (!firstParty(raw)) {
                  event.preventDefault();
                  event.stopImmediatePropagation();
                  return false;
                }
                target.removeAttribute('target');
                target.setAttribute('target', '_self');
              }, true);

              document.addEventListener('submit', function(event){
                const form = event.target;
                if (!form || !form.action) return;
                if (!firstParty(form.action)) {
                  event.preventDefault();
                  event.stopImmediatePropagation();
                } else {
                  form.removeAttribute('target');
                }
              }, true);

              document.addEventListener('play', function(event){
                const video = event.target;
                if (!video || video.tagName !== 'VIDEO' || document.fullscreenElement) return;
                try {
                  const result = video.requestFullscreen ? video.requestFullscreen() : null;
                  if (result && result.catch) result.catch(function(){});
                } catch(e) {}
              }, true);

              const style = document.createElement('style');
              style.id = 'asiaflix-clean-style';
              style.textContent = `
                html, body { margin: 0 !important; padding: 0 !important; min-width: 100% !important; min-height: 100% !important; }
                video:fullscreen, iframe:fullscreen, video:-webkit-full-screen, iframe:-webkit-full-screen {
                  position: fixed !important; inset: 0 !important; width: 100vw !important; height: 100vh !important;
                  max-width: none !important; max-height: none !important; margin: 0 !important; padding: 0 !important;
                  object-fit: contain !important; background: #000 !important; z-index: 2147483647 !important;
                }
                .adsbygoogle, [data-ad-client], [data-ad-slot], [id^="google_ads"],
                [class~="ad-banner"], [class~="ad-container"], [class~="ad-wrapper"],
                [id~="ad-banner"], [id~="ad-container"], [id~="ad-wrapper"],
                [class*="appsgeyser"], [id*="appsgeyser"] { display: none !important; visibility: hidden !important; }
              `;
              if (!document.getElementById(style.id)) (document.head || document.documentElement).appendChild(style);

              const scamText = /(android security|security alert|wiretapping|your android has been hacked|virus detected|run a test|google security support|install random app|buy premium|appsgeyser)/i;
              const removeKnownAds = function(){
                const selectors = [
                  '.adsbygoogle','[data-ad-client]','[data-ad-slot]','[id^="google_ads"]',
                  '[class~="ad-banner"]','[class~="ad-container"]','[class~="ad-wrapper"]',
                  '[id~="ad-banner"]','[id~="ad-container"]','[id~="ad-wrapper"]',
                  '[class*="appsgeyser"]','[id*="appsgeyser"]',
                  'iframe[src*="doubleclick"]','iframe[src*="googlesyndication"]',
                  'iframe[src*="adsterra"]','iframe[src*="monetag"]','iframe[src*="propellerads"]',
                  'iframe[src*="popads"]','iframe[src*="popcash"]','iframe[src*="clickadu"]'
                ];
                selectors.forEach(function(selector){
                  document.querySelectorAll(selector).forEach(function(node){ node.remove(); });
                });

                document.querySelectorAll('a,area,form').forEach(function(node){
                  const value = node.href || node.action || node.getAttribute('href') || node.getAttribute('action') || '';
                  if (value && !firstParty(value) && !value.trim().toLowerCase().startsWith('javascript:') && !value.startsWith('#')) {
                    node.removeAttribute('target');
                    node.addEventListener('click', function(e){ e.preventDefault(); e.stopImmediatePropagation(); }, true);
                  }
                });

                document.querySelectorAll('iframe').forEach(function(frame){
                  const src = frame.getAttribute('src') || '';
                  if (src && blockedUrl(src)) {
                    frame.remove();
                    return;
                  }
                  if (src && !firstParty(src)) {
                    frame.setAttribute('sandbox', 'allow-scripts allow-same-origin allow-forms allow-presentation');
                    frame.setAttribute('allow', 'autoplay; fullscreen; encrypted-media; picture-in-picture');
                    frame.setAttribute('allowfullscreen', 'true');
                  }
                });

                document.querySelectorAll('body > div, body > section, body > aside, body > iframe').forEach(function(node){
                  let text = '';
                  try { text = (node.innerText || node.textContent || '').slice(0, 1200); } catch(e) {}
                  if (!scamText.test(text)) return;
                  const rect = node.getBoundingClientRect();
                  const css = getComputedStyle(node);
                  const large = rect.width * rect.height > innerWidth * innerHeight * 0.25;
                  if (large || css.position === 'fixed' || css.position === 'absolute') node.remove();
                });
              };

              let timer = 0;
              window.__asiaFlixCleanRun = removeKnownAds;
              const observer = new MutationObserver(function(){
                clearTimeout(timer);
                timer = setTimeout(removeKnownAds, 180);
              });
              observer.observe(document.documentElement, { childList: true, subtree: true, attributes: true, attributeFilter: ['href','src','target','action','class','id','style'] });
              removeKnownAds();
            })();
            """;

    private FrameLayout root;
    private WebView webView;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private String lastGoodUrl = HOME_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        configureWebView();
        enterImmersiveMode();

        String restored = savedInstanceState == null ? null : savedInstanceState.getString("lastGoodUrl");
        if (isFirstParty(restored)) {
            lastGoodUrl = restored;
        }
        webView.loadUrl(lastGoodUrl);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    this::handleBack);
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
        settings.setUserAgentString(settings.getUserAgentString() + " AsiaFlixClean/1.0");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus(View.FOCUS_DOWN);
        webView.setWebViewClient(new CleanWebViewClient());
        webView.setWebChromeClient(new CleanWebChromeClient());
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            // Downloads never leave the app or launch an external package.
        });
    }

    private final class CleanWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (!request.isForMainFrame()) {
                return isBlockedResource(uri);
            }
            return shouldBlockTopLevel(view, uri == null ? null : uri.toString());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return shouldBlockTopLevel(view, url);
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
            if (isFirstParty(url)) {
                lastGoodUrl = url;
            }
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (isFirstParty(url)) {
                lastGoodUrl = url;
                view.evaluateJavascript(CLEANUP_SCRIPT, null);
            } else {
                restoreLastGoodPage();
            }
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                restoreLastGoodPage();
            }
            super.onReceivedError(view, request, error);
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            if (request.isForMainFrame() && errorResponse.getStatusCode() >= 400) {
                restoreLastGoodPage();
            }
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
            root.addView(view, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            enterImmersiveMode();
        }

        @Override
        public void onHideCustomView() {
            hideCustomView();
        }

        @Override
        public Bitmap getDefaultVideoPoster() {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }
    }

    private boolean shouldBlockTopLevel(WebView view, String url) {
        if (isFirstParty(url)) {
            lastGoodUrl = url;
            return false;
        }
        view.postDelayed(this::restoreLastGoodPage, 40L);
        return true;
    }

    private boolean isFirstParty(String url) {
        Uri uri = safeParse(url);
        if (uri == null) return false;
        String scheme = lower(uri.getScheme());
        String host = lower(uri.getHost());
        return ("https".equals(scheme) || "http".equals(scheme))
                && host != null
                && (FIRST_PARTY_HOST.equals(host) || host.endsWith("." + FIRST_PARTY_HOST));
    }

    private boolean isBlockedResource(Uri uri) {
        if (uri == null) return true;
        String scheme = lower(uri.getScheme());
        if ("data".equals(scheme) || "blob".equals(scheme) || "about".equals(scheme)) {
            return false;
        }
        if (!"https".equals(scheme) && !"http".equals(scheme)) {
            return true;
        }
        String host = lower(uri.getHost());
        if (host == null || FIRST_PARTY_HOST.equals(host) || host.endsWith("." + FIRST_PARTY_HOST)) {
            return false;
        }
        for (String part : BLOCKED_HOST_PARTS) {
            if (host.contains(part)) return true;
        }
        return false;
    }

    private static Uri safeParse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Uri.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.US);
    }

    private static WebResourceResponse emptyResponse() {
        return new WebResourceResponse(
                "text/plain",
                "UTF-8",
                new ByteArrayInputStream(new byte[0]));
    }

    private void restoreLastGoodPage() {
        if (webView == null) return;
        String current = webView.getUrl();
        if (isFirstParty(current)) return;
        webView.stopLoading();
        webView.loadUrl(isFirstParty(lastGoodUrl) ? lastGoodUrl : HOME_URL);
    }

    private void hideCustomView() {
        if (customView == null) return;
        root.removeView(customView);
        customView = null;
        webView.setVisibility(View.VISIBLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
        enterImmersiveMode();
    }

    private void handleBack() {
        if (customView != null) {
            hideCustomView();
            return;
        }
        if (!isFirstParty(webView.getUrl())) {
            restoreLastGoodPage();
            return;
        }
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }

    private void enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    public void onBackPressed() {
        handleBack();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
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
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersiveMode();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("lastGoodUrl", lastGoodUrl);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            root.removeView(webView);
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
