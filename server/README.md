# LibreTranslate server for GitHub Pages

GitHub Pages is static. LibreTranslate must run as a separate HTTPS API server.

## Quick start (requires a domain)
1) Point a domain at your server (DNS A/AAAA), e.g. translate.yourdomain.com
2) Edit Caddyfile: replace translate.yourdomain.com with your domain
3) Run:
   docker compose up -d

## Use it in the app
In the page:
Translate box -> Provider -> Endpoint:
https://translate.yourdomain.com/translate

## Local testing
Run only LibreTranslate:
  docker compose up -d libretranslate

Then open the site from http://localhost (not file://) and set endpoint:
http://localhost:5000/translate
