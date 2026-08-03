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
import java.nio.charset.StandardCharsets;
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
            "go2cloud", "offerstrack", "onclick", "adnxs", "adsrvr", "bidswitch",
            "contextweb", "lijit", "smaato", "zedo", "quantserve", "crwdcntrl",
            "imrworldwide", "effectivegatecpm", "highperformanceformat",
            "partners.xm", "xmglobal", "xmza.com", "xm.com", "trading-point"
    };

    private static final String[] BLOCKED_URL_PARTS = {
            "affiliate_tracking", "affid=", "xm-ultra-low", "xm_ultra_low",
            "low-cost-trading", "low_cost_trading", "islamic-account-available",
            "/advert/", "/advertisement/", "/popunder/", "onclickalgo"
    };

    private FrameLayout root;
    private WebView webView;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private String lastGoodUrl = HOME_URL;
    private String pageScript = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        pageScript = readAsset("page_script.js");
        configureWebView();
        enterImmersiveMode();

        String restored = savedInstanceState == null ? null : savedInstanceState.getString("lastGoodUrl");
        if (isFirstParty(restored)) lastGoodUrl = restored;
        webView.loadUrl(lastGoodUrl);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    this::handleBack);
        }
    }

    private String readAsset(String name) {
        try (java.io.InputStream input = getAssets().open(name);
             java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toString(StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
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
        settings.setUserAgentString(settings.getUserAgentString() + " AsiaFlixCleanTV/1.1");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) settings.setSafeBrowsingEnabled(true);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus(View.FOCUS_DOWN);
        webView.setWebViewClient(new CleanWebViewClient());
        webView.setWebChromeClient(new CleanWebChromeClient());
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            // Deliberately contained: never launch another package for downloads.
        });
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
            return isBlockedResource(safeParse(url)) ? emptyResponse() : null;
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
                if (!pageScript.isEmpty()) view.evaluateJavascript(pageScript, null);
            } else {
                restoreLastGoodPage();
            }
            enterImmersiveMode();
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) restoreLastGoodPage();
            super.onReceivedError(view, request, error);
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
            if (request.isForMainFrame() && response.getStatusCode() >= 400) restoreLastGoodPage();
            super.onReceivedHttpError(view, request, response);
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
    }

    private boolean shouldBlockTopLevel(String url) {
        if (isFirstParty(url)) return false;
        restoreLastGoodPage();
        return true;
    }

    private boolean isFirstParty(String url) {
        Uri uri = safeParse(url);
        if (uri == null) return false;
        String scheme = lower(uri.getScheme());
        String host = lower(uri.getHost());
        return ("https".equals(scheme) || "http".equals(scheme)) &&
                (FIRST_PARTY_HOST.equals(host) || host.endsWith("." + FIRST_PARTY_HOST));
    }

    private boolean isBlockedResource(Uri uri) {
        if (uri == null) return true;
        String scheme = lower(uri.getScheme());
        if ("data".equals(scheme) || "blob".equals(scheme) || "about".equals(scheme)) return false;
        if (!"http".equals(scheme) && !"https".equals(scheme)) return true;

        String host = lower(uri.getHost());
        String full = lower(uri.toString());
        for (String token : BLOCKED_HOST_PARTS) {
            if (host.contains(token)) return true;
        }
        for (String token : BLOCKED_URL_PARTS) {
            if (full.contains(token)) return true;
        }
        return false;
    }

    private WebResourceResponse emptyResponse() {
        byte[] bytes = new byte[0];
        return new WebResourceResponse(
                "text/plain",
                StandardCharsets.UTF_8.name(),
                204,
                "No Content",
                java.util.Collections.emptyMap(),
                new ByteArrayInputStream(bytes));
    }

    private Uri safeParse(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Uri.parse(value); } catch (Exception ignored) { return null; }
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US);
    }

    private void restoreLastGoodPage() {
        if (webView == null) return;
        webView.post(() -> {
            if (webView == null) return;
            String current = webView.getUrl();
            if (isFirstParty(current)) return;
            webView.stopLoading();
            webView.loadUrl(isFirstParty(lastGoodUrl) ? lastGoodUrl : HOME_URL);
        });
    }

    private void evaluateTvCommand(String command) {
        if (webView == null) return;
        webView.evaluateJavascript(command, null);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event);

        int code = event.getKeyCode();
        if (code == KeyEvent.KEYCODE_BACK) {
            handleBack();
            return true;
        }
        if (customView != null) return super.dispatchKeyEvent(event);

        switch (code) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                evaluateTvCommand("window.__asiaFlixTvMove&&window.__asiaFlixTvMove('left')");
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                evaluateTvCommand("window.__asiaFlixTvMove&&window.__asiaFlixTvMove('right')");
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                evaluateTvCommand("window.__asiaFlixTvMove&&window.__asiaFlixTvMove('up')");
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                evaluateTvCommand("window.__asiaFlixTvMove&&window.__asiaFlixTvMove('down')");
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                evaluateTvCommand("window.__asiaFlixTvActivate&&window.__asiaFlixTvActivate()");
                return true;
            case KeyEvent.KEYCODE_MOVE_HOME:
                evaluateTvCommand("window.__asiaFlixTvHome&&window.__asiaFlixTvHome()");
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    private void handleBack() {
        if (customView != null) {
            hideCustomView();
            return;
        }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            webView.postDelayed(() -> {
                if (webView != null && !isFirstParty(webView.getUrl())) restoreLastGoodPage();
            }, 180);
            return;
        }
        finish();
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

    private void enterImmersiveMode() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersiveMode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
        enterImmersiveMode();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("lastGoodUrl", lastGoodUrl);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.removeAllViews();
            root.removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
