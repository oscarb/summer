# Summer — Improvement Ideas & Roadmap

Below are completed and verified UI/UX and feature improvements for **Summer**:

- [x] **Render Markdown in AI Summaries**: Parsed and rendered clean Markdown (`**bold**`, `*italic*`, headings, bullet/numbered lists, inline code, links) via custom `MarkdownText` in both inline card summaries and digest sheets.
- [x] **Remove Up/Down Expand Chevron Button**: Removed the redundant expand/collapse icon button; full-card tap seamlessly expands/collapses inline summaries, freeing horizontal room for titles and domain chips.
- [x] **Flatten AI Summary Layout (Remove Gray Card)**: Eliminated the nested gray/tinted Surface container, using an airy, modern flattened layout with an accent indicator.
- [x] **Use Skeleton for Loading AI Text Summaries**: Replaced circular progress spinner and _"Generating AI summary..."_ text with an animated 3-bar shimmer skeleton loader (`SummarySkeletonLoader`).
- [x] **Group Links by Created Date with Date Headers**: Grouped saved links by local calendar day (`createdAt`) with sticky/section date headers (_Today_, _Yesterday_, _Oct 20, 2026_, etc.) and removed cluttering individual card timestamps.
- [x] **Custom App Icon ("Sun meets Browser")**: Designed and generated high-resolution vector and mipmap adaptive icons (`ic_launcher.webp`, `ic_launcher_round.webp`, `ic_launcher_monochrome.xml`) combining a warm summer sun motif with browser arcs.
- [x] **Option to Have Podcast Translated to Swedish**: Added `PodcastLanguage` setting (**English** / **Swedish (Svenska)**). When Swedish is selected, prompts, host introductions, dates, dialogue, and TTS synthesis use idiomatic Swedish with Swedish host names and Swedish voices.
- [x] **Snackbar / Action Bar on Add**: Implemented `SaveLinkTrampolineActivity` (translucent overlay with 4.5s auto-dismissing floating pill bar with **Open App**, **Open Link**, and **Podcast**) when sharing URLs from other apps, plus `InAppPostSaveActionBar` when adding links inside Summer.
- [x] **Random Names and Voices for Podcasts**: Added a toggle in Settings (default **On**) to randomize host names and distinct voices every episode, with manual persona/name selectors displayed when toggled off.
- [x] **Start with Presentations & Localized Date**: Guaranteed opening welcome, localized today's date, and host introductions (_"Welcome to another episode of Summer! Today is Friday, October 30... I'm Maya, and with me is Leo..."_) on every episode.
- [x] **Session Quiet-Window / Every Nth URL Auto-Podcast**: Added automatic Catch Me Up generation after a quiet-window debounce timer (configurable 1–10 minutes, default 2 min; min links 2–10, default 2), clustering unread links saved in the same browsing session, generating the podcast silently in the background, and auto-archiving.
- [x] **Silent Progress & Ready-to-Play Notifications**: Progress and quiet-window countdowns run under a completely silent notification channel (`IMPORTANCE_LOW`, no sound/vibration). Once ready, an alert notification (`IMPORTANCE_DEFAULT`) is delivered with a **▶ Play Episode** action and full tap-to-play intent.
- [x] **Performance Benchmark**: Documented empirical on-device performance on Pixel 9 Pro (Tensor G4) across Firebase AI Logic, Direct Gemini API, On-Device Gemini Nano (AICore), and TTS LLMs / on-device TTS in `BENCHMARK.md`.
- [x] **Fix Play Button Audio Bug**: Fixed `MediaPlayer` silent failure on `0600` internal storage files by passing open file descriptors (`FileInputStream.fd`), validated audio WAV sizes, handled error states in UI, and guaranteed `MediaSession` main-thread Looper callbacks.
- [x] **Site Thumbnail & Inline Favicon Layout**: Added 56.dp zero-padding cropped thumbnail (`ContentScale.Crop`) on the left side of cards, with a smaller 15.dp favicon placed inline next to the domain name.
- [x] **Configurable Number of Podcast Hosts (2 or 3)**: Added 2 or 3 host configuration in Settings with prompt support and multi-speaker TTS scene batching (`WavAudioUtils`) for 3 distinct speakers.
- [x] **Keep Viewable Text Summaries to Articles & Podcasts to Digests**: Individual cards focus purely on readable Markdown text summaries; Catch Me Up digests focus purely on the podcast audio player and cited source links.
- [ ] Gitignore TODO and remove TODO from README
- [ ] Smoother animations when archiving/deleting items.
- [ ] Require longer distance traveled before archiving/deleting items
- [ ] Resuming playback not always working
- [ ] Don't limit to 2 lines on titles in items in Saved links.
- [ ] Tap "ripple" is not clipped to rounded corners in Saved links
- [ ] Share digests as files to another apps
- [ ] MKBHD style podcasts
- [ ] Fall through to next model if one isn't working
- [ ] Debug/prod version
- [ ] Simplify app icon/logo
- [ ] Give all epsiodes numbers and unique names
- [ ] Show notification when producing podcast
- [ ] Skip TL:DR in summaries
- [ ] Search feature?
- [ ] Remove the "Time after last saved link" option and "Minimum links" in UI, but keep in code
