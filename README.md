Heroic Flash Cards is an open-source JavaScript-based flashcard + TTS learning tool
# Heroic Flashcards – LibreTranslate backend (GitHub Pages friendly)

GitHub Pages is static-only, so your translation box needs a **separate** server to call.

This repo builds a single Docker service that:
- runs LibreTranslate locally on `127.0.0.1:5000`
- exposes a public HTTP endpoint via a small Python reverse proxy that **adds CORS headers**
  so your site `https://heroicflashcards.js.org` can call it.

## Deploy (Render – easiest)

1) Push this repo to GitHub (new repo recommended, e.g. `heroicflashcards-translate`).

2) On Render:
- New → **Web Service**
- Connect your GitHub repo
- Environment: **Docker**
- Set these environment variables:

```
ALLOWED_ORIGIN=https://heroicflashcards.js.org
LT_LOAD_ONLY=en,zh,ko,vi,th,fr,hi
LT_DISABLE_WEB_UI=true
```

3) Deploy. Your public API will be at:
`https://<your-service>.onrender.com`

Test:
- `GET /languages`
- `POST /translate`

## Wire it into your GitHub Pages app

In your flashcards JS, set:

```
const LT_BASE = "https://<your-service>.onrender.com";
```

and call:
- `${LT_BASE}/translate`
- `${LT_BASE}/languages`

That’s it.

## Notes

- The first boot can take a while because language models download.
  `LT_LOAD_ONLY` keeps it smaller.
