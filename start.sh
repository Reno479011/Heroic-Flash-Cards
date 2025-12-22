#!/usr/bin/env bash
set -euo pipefail

# Default CORS origin: your GitHub Pages custom domain
export ALLOWED_ORIGIN="${ALLOWED_ORIGIN:-https://heroicflashcards.js.org}"

# Start LibreTranslate locally (no CORS here; proxy will handle it).
# The Docker image includes gunicorn and the wsgi app. Env vars like LT_LOAD_ONLY work (see docs).
gunicorn -c scripts/gunicorn_conf.py --bind 127.0.0.1:5000 'wsgi:app()' &
LT_PID=$!

# Start the public proxy (adds CORS + forwards everything)
python3 /app/proxy/proxy.py

# If proxy exits, stop LT
kill "$LT_PID" 2>/dev/null || true
wait "$LT_PID" 2>/dev/null || true
