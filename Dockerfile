# Heroic Flashcards – LibreTranslate w/ CORS proxy (single-container)
# Deployable on Render / Railway / Fly / any Docker host.

FROM libretranslate/libretranslate:latest

WORKDIR /app

# Add a tiny Python proxy that injects CORS headers (for GitHub Pages)
COPY proxy/proxy.py /app/proxy/proxy.py
COPY start.sh /app/start.sh

RUN chmod +x /app/start.sh

# Render sets $PORT. We'll listen on it from the proxy.
ENV PORT=8080

CMD ["/app/start.sh"]
