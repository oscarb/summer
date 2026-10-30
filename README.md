# ☀️ Summer

**Summer** (`se.oscarb.summer`) is a lightweight Android "read-later & AI catch-up" app that registers itself as a system **Browser** and **Share target** — without a traditional browsing UI.

Whenever you come across interesting articles in other apps (such as **Google Feed**, social feeds, or newsletters) and open or share them with **Summer**, the URL is quietly saved in the background with a fast heads-up notification while you stay right where you were. Later, open **Summer** to view your saved websites (with scraped titles and favicons) and generate individual **AI summaries** or a combined **"Catch Me Up" AI briefing**.

---

## ✨ Features

- **Zero-UI Browser & Share Target Trampoline (`SaveLinkTrampolineActivity`)**
  - Registers for `ACTION_VIEW` (`http` / `https` with `CATEGORY_BROWSABLE`) and `ACTION_SEND` (`text/plain`).
  - Saves the link, bumps duplicates to the top, delivers an instant silent heads-up notification with quick actions (tap to open in browser, swipe to cancel save, Open Summer, Podcast), and immediately returns to your previous app.
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
  - Tap a card to expand its AI summary inline and access quick actions (**Open in Browser**, **Listen**, **Archive**, **Share**, **Delete**).
  - Swipe right to archive/unarchive, or swipe left to delete.
  - Tap **Catch Me Up** to generate a unified briefing across all unread links in a modal Bottom Sheet, with a **Digests** tab to revisit past briefings.
- **🎙️ Two-Host Podcast Episodes & Background Audio (`PodcastTtsService` & `PodcastForegroundService`)**
  - **"Catch Me Up" Two-Host Podcast**: Generates a natural, conversational 2-host dialogue (**Alex & Sam**) that weaves a narrative "red line" connecting the themes across your saved articles, complete with an interactive **2-Host Transcript** view alongside the written briefing.
  - **Individual Article Conversational Audio**: Tap **Listen** on any saved article card to generate and play a natural spoken-word dialogue tailored for listening rather than reading bullet points.
  - **Multi-Engine Voice Synthesis**: Choose between **Google Gemini Multi-Speaker TTS** (`gemini-3.8-flash-tts` / `gemini-3.8-flash-lite-tts` / `gemini-2.5-flash-preview-tts` on the free Google AI Studio tier, with dropdown selectors for all 30 official Gemini studio voices), **Android On-Device Multi-Voice TTS** (100% free & offline with engine/voice/rate/pitch selectors, sentence-boundary sub-chunking, and automatic fallback), **Microsoft Azure AI Speech** (500k free neural characters/month via multi-voice SSML), **ElevenLabs Neural TTS**, or **OpenAI / Kokoro / Custom `/v1/audio/speech`**.
  - **Background Generation & Screen-Off Playback**: Generates and plays episodes in a foreground service with lock-screen & headphone `MediaSession` controls (`Play/Pause`, `±15s` skip, seek bar), a persistent in-app mini-player bar, and offline WAV caching on disk.
- **Loop-Free External Browser Launching (`ExternalBrowserLauncher`)**
  - Because Summer registers as a browser itself, tapping **Open in Browser** queries `PackageManager` for other installed browsers (excluding `se.oscarb.summer`) and opens the article directly in your standalone external browser app by default (with an optional **Chrome Custom Tabs** overlay toggle in **Settings**).

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
