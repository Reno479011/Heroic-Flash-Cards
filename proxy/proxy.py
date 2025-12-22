#!/usr/bin/env python3
import os
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urljoin, urlsplit
import urllib.request

UPSTREAM = os.environ.get("UPSTREAM", "http://127.0.0.1:5000")
PORT = int(os.environ.get("PORT", "8080"))
ALLOWED_ORIGIN = os.environ.get("ALLOWED_ORIGIN", "*")

# For safety, only allow a single origin string or "*"
# (If you want multiple origins, set "*" or put a reverse proxy in front.)
def _cors_origin(origin_header: str | None) -> str:
    if ALLOWED_ORIGIN.strip() == "*":
        return "*"
    return ALLOWED_ORIGIN.strip()

CORS_HEADERS = [
    ("Access-Control-Allow-Origin", None),  # filled per-request
    ("Vary", "Origin"),
    ("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS"),
    ("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With"),
    ("Access-Control-Max-Age", "86400"),
]

HOP_BY_HOP = {
    "connection",
    "keep-alive",
    "proxy-authenticate",
    "proxy-authorization",
    "te",
    "trailers",
    "transfer-encoding",
    "upgrade",
    "host",
}

class ProxyHandler(BaseHTTPRequestHandler):
    server_version = "HeroicFlashcardsLTProxy/1.0"

    def _send_cors(self):
        origin = self.headers.get("Origin")
        allow = _cors_origin(origin)
        for k, v in CORS_HEADERS:
            if v is None and k.lower() == "access-control-allow-origin":
                self.send_header(k, allow)
            else:
                self.send_header(k, v)

    def _proxy(self):
        # Build upstream URL
        # Keep path + query intact
        upstream_base = UPSTREAM if UPSTREAM.endswith("/") else UPSTREAM + "/"
        # self.path includes query string
        target = urljoin(upstream_base, self.path.lstrip("/"))

        # Read body if present
        body = None
        length = self.headers.get("Content-Length")
        if length:
            try:
                body = self.rfile.read(int(length))
            except Exception:
                body = None

        # Copy headers, excluding hop-by-hop
        headers = {}
        for k, v in self.headers.items():
            if k.lower() in HOP_BY_HOP:
                continue
            # Let urllib compute content-length for us if body is None; otherwise keep it
            if k.lower() == "content-length":
                continue
            headers[k] = v

        req = urllib.request.Request(target, data=body, headers=headers, method=self.command)

        try:
            with urllib.request.urlopen(req, timeout=120) as resp:
                data = resp.read()
                status = resp.getcode()

                self.send_response(status)

                # Copy response headers (excluding hop-by-hop), then add CORS
                for k, v in resp.headers.items():
                    if k.lower() in HOP_BY_HOP:
                        continue
                    if k.lower() == "content-length":
                        continue
                    # Some servers send their own CORS; we overwrite with ours
                    if k.lower().startswith("access-control-"):
                        continue
                    self.send_header(k, v)

                self._send_cors()
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)
        except urllib.error.HTTPError as e:
            # Upstream error still returns a body sometimes
            data = e.read() if hasattr(e, "read") else b""
            self.send_response(e.code)
            self._send_cors()
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data or str(e).encode("utf-8"))
        except Exception as e:
            msg = f"Proxy error: {e}"
            self.send_response(502)
            self._send_cors()
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(msg)))
            self.end_headers()
            self.wfile.write(msg.encode("utf-8"))

    # CORS preflight
    def do_OPTIONS(self):
        self.send_response(204)
        self._send_cors()
        self.end_headers()

    def do_GET(self): self._proxy()
    def do_POST(self): self._proxy()
    def do_PUT(self): self._proxy()
    def do_PATCH(self): self._proxy()
    def do_DELETE(self): self._proxy()

    # quieter logs
    def log_message(self, fmt, *args):
        sys.stdout.write("%s - %s\n" % (self.address_string(), fmt % args))

def main():
    httpd = HTTPServer(("0.0.0.0", PORT), ProxyHandler)
    print(f"LT proxy listening on 0.0.0.0:{PORT} → {UPSTREAM} | CORS origin: {ALLOWED_ORIGIN}", flush=True)
    httpd.serve_forever()

if __name__ == "__main__":
    main()
