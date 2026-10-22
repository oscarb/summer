# ☀️ Summer

**Summer** (`se.oscarb.summer`) is a lightweight Android "read-later & AI catch-up" app that registers itself as a system **Browser** and **Share target** — without a traditional browsing UI.

Whenever you come across interesting articles in other apps (such as **Google Feed**, social feeds, or newsletters) and open or share them with **Summer**, the URL is quietly saved in the background with a brief toast confirmation while you stay right where you were. Later, open **Summer** to view your saved websites (with scraped titles and favicons) and generate individual **AI summaries** or a combined **"Catch Me Up" AI briefing**.

---

## ✨ Features

- **Zero-UI Browser & Share Target Trampoline (`SaveLinkTrampolineActivity`)**
  - Registers for `ACTION_VIEW` (`http` / `https` with `CATEGORY_BROWSABLE`) and `ACTION_SEND` (`text/plain`).
  - Saves the link, bumps duplicates to the top, displays a quick `☀️ Saved to Summer` toast, and immediately returns to your previous app.
- **Automatic Metadata & Content Extraction (`WebPageScraper`)**
  - Uses **Jsoup** to scrape `<title>`, OpenGraph title (`og:title`), high-res favicons (`apple-touch-icon` / `<link rel="icon">` with Google S2 Favicon fallback), and clean readable article text.
- **Multi-Backend AI Summarization (`AiSummaryService`)**
  - Switch between four AI backends in **Settings**:
    1. **Firebase AI Logic** (Gemini via Firebase SDK)
    2. **Direct Gemini API** (Google AI Studio API key)
    3. **On-Device Gemini Nano** (100% local inference via Google AI Edge AICore on supported devices)
    4. **OpenAI / Anthropic / Custom API** (any OpenAI-compatible `/chat/completions` or Anthropic `/v1/messages` endpoint)
  - Supports both **on-demand summarization** (when expanding a card or tapping **Catch Me Up**) and an optional **Auto-summarize in background on save** toggle.
- **Inline Expandable Cards & "Catch Me Up" Digest**
  - Tap a card to expand its AI summary inline and access quick actions (**Open in Browser**, **Archive**, **Share**, **Delete**).
  - Swipe right to archive/unarchive, or swipe left to delete.
  - Tap **Catch Me Up** to generate a unified briefing across all unread links in a modal Bottom Sheet, with a **Digests** tab to revisit past briefings.
- **Listenable "Catch Me Up" Podcasts (`PodcastSynthesizer` / `PodcastPlayer`)**
  - Tap **Listen as Podcast** in any Catch Me Up digest, or **New Podcast** on the **Podcasts** tab, to turn your saved links into a spoken-word episode.
  - The AI writes a plain narration script, which is then read aloud on device with the system **Text-to-Speech** engine — no extra API key required.
  - Speech is cached to private app storage (per-chunk audio parts) so episodes replay instantly; if an engine cannot save audio, playback falls back to live text-to-speech.
  - A mini player keeps playing across tabs, and the **Podcasts** tab shows progress, the full script, and the source links. Voice, engine, rate, and pitch are configurable in **Settings**.
- **Loop-Free External Browser Launching (`ExternalBrowserLauncher`)**
  - Because Summer registers as a browser itself, tapping **Open in Browser** queries `PackageManager` for other installed browsers (excluding `se.oscarb.summer`) and opens the article via **Chrome Custom Tabs**.

---

## 🚀 Setup & Building

### Prerequisites
- **Android Studio** (or Android CLI / Gradle) with **JDK 17+** and **Android SDK 36**.

### Optional: Firebase AI Logic Setup
`app/google-services.json` is excluded from version control so no project credentials or API keys are committed.
- You can build and run the app **without** `google-services.json` and use **Direct Gemini API**, **On-Device Gemini Nano**, or **OpenAI / Custom API** right from the in-app **Settings** tab.
- To use **Firebase AI Logic**, place your own `google-services.json` inside `app/google-services.json` (the Gradle build automatically detects it and applies the Google Services plugin).

### Build & Run
```bash
./gradlew assembleDebug testDebugUnitTest
```

---

## 🗺️ Planned Improvements (see [`TODO.md`](TODO.md))

- [ ] **Render Markdown in AI Summaries**: The "Firebase AI" (and other providers) return Markdown formatting (e.g., `**TL;DR.** Text...`) — render Markdown properly in cards and digests.
- [ ] **Remove Up/Down Expand Chevron Button**: Skip the up/down button on cards while keeping tap-to-expand/collapse and making use of the extra horizontal space.
- [ ] **Group Links by Created Date**: Group links by created date, show date headers, and skip showing timestamps on individual cards.
- [ ] **Custom App Icon**: Add an app icon ("sun meets typical browser icon").
