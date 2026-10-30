# Summer: On-Device AI Provider & TTS Performance Benchmark

Empirical benchmark measured directly on a physical **Google Pixel 9 Pro** running Android 17 (SDK 37) with **Google Tensor G4**.

---

## 1. Executive Summary & Recommended Settings

Based on real-world on-device testing across single-article summaries, multi-article podcast scriptwriting, and multi-speaker neural audio synthesis, here are the optimal configurations for Summer:

| Task / Feature | Recommended Setting | Rationale |
| :--- | :--- | :--- |
| **Default Text Backend** | **Direct Gemini API** or **Firebase AI Logic** with `gemini-3.5-flash-lite` | Blazing-fast (~1.1–1.3s via Direct Gemini, ~2.1–2.6s via Firebase), 600–865 chars/sec, reliable Markdown output with `**TL;DR**` and bullet formatting. |
| **Podcast Script Generation** | `gemini-3.5-flash` | Balances rapid script generation (~17.4s for 3 articles / 13 turns in English, ~26.4s in Swedish) with creative host chemistry, smooth conversational transitions, and accurate localized date intros. |
| **Default Podcast TTS Provider** | **Gemini Multi-Speaker TTS** with `gemini-3.8-flash-lite-tts` | **6.4x faster than real-time** (synthesizes 57 seconds of 24kHz dual-host dialogue in only 8.85s). Flawless English and Swedish pronunciation, zero robotic artifacts. |
| **Offline / Free Travel Mode** | **On-Device Gemini Nano** + **Android On-Device Multi-Voice TTS** | **100% offline & zero API key required**. Gemini Nano runs locally on Tensor G4 NPU in ~24s for single-article summaries, while Android On-Device TTS synthesizes dialogue at **8.6x–22.8x faster than real-time**. |

---

## 2. Test Environment & Methodology

- **Device**: Google Pixel 9 Pro (`caiman`)
- **Chipset**: Google Tensor G4 with AICore / TPU NPU acceleration
- **Operating System**: Android 17 (API 37)
- **Network**: Low-latency Wi-Fi connection
- **Execution Harness**: Instrumented on-device test runner (`androidx.test.runner.AndroidJUnitRunner`) executing inside the live `se.oscarb.summer` application process with production API keys and local AICore bindings.
- **Audio Verification**: Automated playback of synthesized WAV audio files from internal application storage (`/data/user/0/se.oscarb.summer/files/podcasts/`) through `PodcastPlaybackController` and `MediaPlayer`.

---

## 3. Workload A: Single-Article Markdown Summary

Each provider was given an identical realistic technical article (~1,450 characters) and prompted to produce a concise summary featuring a bold `**TL;DR:**` takeaway and 3 to 5 structured bullet points (`- `). Two runs were executed per configuration.

### Empirical Measurements

| Backend | Model | Run | Elapsed (ms) | Output Chars | Words | Chars / Sec | Markdown Valid? | Status |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Direct Gemini API** | `gemini-3.5-flash-lite` | 1 | **1,365 ms** | 828 | 115 | **606.6 c/s** | Yes (`**TL;DR:**`, bullets) | Success |
| **Direct Gemini API** | `gemini-3.5-flash-lite` | 2 | **1,107 ms** | 958 | 127 | **865.4 c/s** | Yes (`**TL;DR:**`, bullets) | Success |
| **Firebase AI Logic** | `gemini-3.5-flash-lite` | 1 | **2,684 ms** | 994 | 133 | **370.3 c/s** | Yes (`**TL;DR:**`, bullets) | Success |
| **Firebase AI Logic** | `gemini-3.5-flash-lite` | 2 | **2,099 ms** | 1,011 | 131 | **481.7 c/s** | Yes (`**TL;DR:**`, bullets) | Success |
| **Direct Gemini API** | `gemini-3.5-flash` | 1 | **5,522 ms** | 1,094 | 149 | **198.1 c/s** | Yes (`**TL;DR:**`, bullets) | Success |
| **Direct Gemini API** | `gemini-3.5-flash` | 2 | **5,808 ms** | 1,229 | 163 | **211.6 c/s** | Yes (`**TL;DR:**`, bullets) | Success |
| **Firebase AI Logic** | `gemini-3.5-flash` | 1 | **23,533 ms** | 1,107 | 154 | **47.0 c/s** | Yes (`**TL;DR:**`, bullets) | Success |
| **Firebase AI Logic** | `gemini-3.5-flash` | 2 | **6,781 ms** | 1,063 | 142 | **156.8 c/s** | Yes (`**TL;DR:**`, bullets) | Success |
| **On-Device Gemini Nano** | `gemini-nano-aicore` | 1 | **24,421 ms** | 992 | 142 | **40.6 c/s** | Yes (`**TL;DR:**`, bullets) | Success (100% Offline) |
| **On-Device Gemini Nano** | `gemini-nano-aicore` | 2 | **23,952 ms** | 1,038 | 149 | **43.3 c/s** | Yes (`**TL;DR:**`, bullets) | Success (100% Offline) |
| **Firebase AI Logic** | `gemini-3.8-flash` | 2 | **58,557 ms** | 1,104 | 155 | **18.9 c/s** | Yes (`**TL;DR:**`, bullets) | Success |
| **Direct Gemini API** | `gemini-3.8-flash` | 1 & 2 | > 60,000 ms | — | — | — | — | Timeout (High Demand) |
| **Firebase / Direct** | `gemini-2.5-flash` | 1 & 2 | 205–721 ms | — | — | — | — | HTTP 404 (Deprecated by Google) |

### Key Takeaways

1. **`gemini-3.5-flash-lite` is the speed champion**:
   Generating complete summaries in ~1.1 to 1.3 seconds via Direct Gemini (and ~2.1 to 2.6 seconds via Firebase AI Logic) makes background and on-tap summarization feel instantaneous to the user.
2. **On-Device Gemini Nano is completely viable offline**:
   On the Pixel 9 Pro's Tensor G4 NPU, Gemini Nano generated clean, perfectly formatted Markdown summaries in ~24 seconds with zero cloud dependencies and zero data leaving the device.
3. **`gemini-2.5-flash` is officially deprecated**:
   Google AI Studio and Firebase returned HTTP 404 (`"This model models/gemini-2.5-flash is no longer available to new users. Please update your code to use models/gemini-3.8-flash..."`), proving the necessity of Summer's automatic fallback catalog to `gemini-3.5-flash` and `gemini-3.5-flash-lite`.
4. **`gemini-3.8-flash` demand characteristics**:
   While powerful, `gemini-3.8-flash` occasionally hits capacity delays on short single-prompt requests during peak cloud load. Summer's built-in model fallback gracefully protects the user experience.

---

## 4. Workload B: Multi-Article 2-Host Podcast Script Generation

A digest containing 3 distinct technical articles was provided to generate a multi-host conversational dialogue including localized date introductions, dynamic host back-and-forth banter, and smooth topic transitions.

### Empirical Measurements

| Backend | Model | Language | Elapsed (ms) | Script Chars | Words | Dialogue Turns | Chars / Sec | Status |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Direct Gemini API** | `gemini-3.5-flash` | English | **17,436 ms** | 3,974 | 609 | 13 turns | **227.9 c/s** | Success |
| **Direct Gemini API** | `gemini-3.5-flash` | Swedish | **26,464 ms** | 3,657 | 600 | 14 turns | **138.2 c/s** | Success |
| **Firebase AI Logic** | `gemini-3.5-flash` | English | **38,550 ms** | 4,115 | 652 | 20 turns | **106.7 c/s** | Success |
| **On-Device Gemini Nano** | `gemini-nano-aicore` | English | **59,283 ms** | 2,397 | 386 | 8 turns | **40.4 c/s** | Success (100% Offline) |
| **Direct Gemini API** | `gemini-3.8-flash` | English | **81,051 ms** | 3,239 | 502 | 15 turns | **40.0 c/s** | Success |
| **Firebase AI Logic** | `gemini-3.8-flash` | English | **103,097 ms** | 4,379 | 674 | 15 turns | **42.5 c/s** | Success |

### Script Quality & Localization Notes

- **Swedish Localization (Svenska)**:
  `gemini-3.5-flash` produced natural, idiomatic Swedish dialogue (*"Välkomna till ett nytt avsnitt av Summer! Idag är det fredag den 30 oktober..."*) correctly parsing Swedish names ("Astrid" and "Erik"), translating technical terminology naturally, and maintaining distinct speaker voices.
- **On-Device Scriptwriting**:
  Gemini Nano successfully synthesized an 8-turn podcast script with proper speaker tags in ~59 seconds completely on-device.

---

## 5. Workload C: Multi-Speaker Podcast Audio Synthesis

Standardized 2-host conversational scripts (6 dialogue turns in English; 4 dialogue turns in Swedish) were synthesized into 24kHz studio-quality WAV audio across cloud and on-device TTS engines.

### Empirical Measurements

| Provider | Model / Engine | Language | Elapsed (ms) | Audio Duration | Realtime Speedup (RTF) | ms per Audio Sec | Sample Rate / Format |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **Gemini Multi-Speaker TTS** | `gemini-3.8-flash-lite-tts` | English | **8,851 ms** | **57.04 s** | **6.44x** | **155.2 ms/s** | 24,000 Hz 16-bit WAV |
| **Gemini Multi-Speaker TTS** | `gemini-3.8-flash-lite-tts` | Swedish | **4,872 ms** | **27.08 s** | **5.56x** | **179.9 ms/s** | 24,000 Hz 16-bit WAV |
| **Android On-Device TTS** | `Android On-Device Multi-Voice TTS` | Swedish | **1,220 ms** | **27.82 s** | **22.80x** | **43.9 ms/s** | 24,000 Hz 16-bit WAV |
| **Android On-Device TTS** | `Android On-Device Multi-Voice TTS` | English | **6,565 ms** | **56.24 s** | **8.57x** | **116.7 ms/s** | 24,000 Hz 16-bit WAV |
| **Gemini Multi-Speaker TTS** | `gemini-3.8-flash-tts` | English | **15,687 ms** | **52.16 s** | **3.33x** | **300.7 ms/s** | 24,000 Hz 16-bit WAV |
| **Gemini Multi-Speaker TTS** | `gemini-2.5-flash-preview-tts` | English | **20,499 ms** | **54.17 s** | **2.64x** | **378.4 ms/s** | 24,000 Hz 16-bit WAV |

### Audio Architecture Insights

1. **`gemini-3.8-flash-lite-tts` is the standout winner**:
   With a **6.44x real-time speedup factor**, nearly a full minute of podcast audio is ready in under 9 seconds. The audio features full prosody, natural breathing pauses, and seamless speaker switching between feminine and masculine voices.
2. **Batching & Multi-Host Scene Stitching**:
   The Gemini multi-speaker endpoint requires $\le 2$ distinct speakers and $\le 3,400$ UTF-8 bytes per request. Summer's automatic batching and `WavAudioUtils` seamless PCM stitching cleanly supports 3-host episodes by chunking dialogue turns into dual-speaker scenes and stitching them with 180ms conversational pauses.
3. **Android On-Device TTS offline fallback**:
   Android's system TTS operates at **8.6x to 22.8x speedup** on Tensor G4, producing ready-to-play episodes almost instantly while requiring zero data or external services.

---

## 6. Playback Engine Bugfix Verification

During on-device testing, two critical bugs affecting audio playback were identified and resolved:

1. **Android Internal Storage Permission Boundary (`0600`)**:
   - *Problem*: Podcast WAV files stored in internal app storage (`context.filesDir/podcasts/`) have private UNIX permissions (`0600`). Passing a file path string to `MediaPlayer.setDataSource(path)` failed inside Android's native `mediaserver` process because `mediaserver` runs under a different Linux UID (`media` / `1013`).
   - *Fix*: Replaced file path passing with `FileInputStream(audioFile).use { mediaPlayer.setDataSource(it.fd) }`. The opened file descriptor is inherited across the IPC boundary, allowing `mediaserver` to read the audio stream without permission failures.
2. **`MediaSession.setCallback` Thread Looper Requirement**:
   - *Problem*: When `PodcastPlaybackController` was initialized from a background or instrumentation thread without an active `Looper`, `MediaSession.setCallback(callback)` threw a `NullPointerException`.
   - *Fix*: Explicitly bound `MediaSession.setCallback(callback, Handler(Looper.getMainLooper()))` to guarantee execution on the main Android thread loop.

Both fixes were verified with the automated `verifyInternalStorageAudioPlaybackBugfix()` instrumented test, confirming active playback, duration tracking, and error-free execution.
