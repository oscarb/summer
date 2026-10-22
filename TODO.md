# Summer — Improvement Ideas & Roadmap

Below are planned UI/UX and feature improvements for **Summer**:

- [ ] **Render Markdown in AI Summaries**: The "Firebase AI" (and other providers) return Markdown formatting (e.g., `**TL;DR.** Text...`, bullet lists, bold headings) — make sure to parse and render Markdown properly in both the inline card summary and the "Catch Me Up" digest sheet.
- [ ] **Remove Up/Down Expand Chevron Button**: Skip the explicit up/down (`ExpandMore`/`ExpandLess`) icon button on each website card, while keeping tap-on-card to expand/collapse inline, freeing up horizontal space for the website title and metadata.
- [ ] **Group Links by Created Date with Date Headers**: Group saved links by their created date (`createdAt`), display sticky or section date headers (e.g., *Today*, *Yesterday*, *Oct 20*), and skip showing timestamps on the individual cards.
- [ ] **Custom App Icon ("Sun meets Browser")**: Design and add a custom adaptive app icon combining a warm sun motif with a classic browser/globe icon.
- [ ] **Background Podcast Playback with Media Controls**: Keep podcast audio playing when the app is backgrounded, with a media notification and lock-screen/Bluetooth controls via `MediaSessionService` / `Media3`.
- [ ] **Export / Share Podcast Audio**: Let users share or save a generated podcast episode as a single audio file.
