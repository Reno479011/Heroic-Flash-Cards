# Asia Flix Clean Android Shell

A minimal Android WebView application for `https://asiaflix.org/`.

This project intentionally contains no AppsGeyser code, ad SDKs, analytics SDKs, notification permission, Play Store launcher, browser launcher, or external-intent handling.

Security and playback behavior:

- Main-frame navigation is restricted to `asiaflix.org` and its subdomains.
- New windows, external schemes, Play Store links, browser links, geolocation, notifications, and WebView permission requests are denied.
- Known advertising and tracking hosts are intercepted before loading.
- Third-party player frames are sandboxed without popup or top-navigation privileges.
- Native WebView custom-view fullscreen and immersive system-bar hiding are enabled.
- Android phone and Android TV launchers are included.
