# Summer — Improvement Ideas & Roadmap

Below are completed and verified UI/UX and feature improvements for **Summer**:

- [x] **Render Markdown in AI Summaries**: Parsed and rendered clean Markdown (`**bold**`, `*italic*`, headings, bullet/numbered lists, inline code, links) via custom `MarkdownText` in both inline card summaries and digest sheets.
- [x] **Remove Up/Down Expand Chevron Button**: Removed the redundant expand/collapse icon button; full-card tap seamlessly expands/collapses inline summaries, freeing horizontal room for titles and domain chips.
- [x] **Flatten AI Summary Layout (Remove Gray Card)**: Eliminated the nested gray/tinted Surface container, using an airy, modern flattened layout with an accent indicator.
- [x] **Use Skeleton for Loading AI Text Summaries**: Replaced circular progress spinner and _"Generating AI summary..."_ text with an animated 3-bar shimmer skeleton loader (`SummarySkeletonLoader`).
- [x] **Group Links by Created Date with Date Headers**: Grouped saved links by local calendar day (`createdAt`) with sticky/section date headers (_Today_, _Yesterday_, _Oct 20, 2026_, etc.) and removed cluttering individual card timestamps.
- [x] **Custom App Icon ("Sun meets Browser")**: Designed and generated high-resolution vector and mipmap adaptive icons (`ic_launcher.webp`, `ic_launcher_round.webp`, `ic_launcher_monochrome.xml`) combining a warm summer sun motif with browser arcs.
- [x] **Option to Have Podcast Translated to Swedish**: Added `PodcastLanguage` setting (**English** / **Swedish (Svenska)**). When Swedish is selected, prompts, host introductions, dates, dialogue, and TTS synthesis use idiomatic Swedish with Swedish host names and Swedish voices.
- [x] **Fast Heads-Up Notification on External Add (No Toast, Open in Browser, Swipe to Cancel, Optional Un-Save on Open)**: Implemented immediate silent heads-up notification without toast delay via `SaveLinkTrampolineActivity`. Tapping the notification opens the link directly in the external browser (with setting option to remove from Summer when opened); swiping away the notification cancels the save and removes the link from Summer; actions provide **Open Summer** and **Podcast**; extended 12s timeout ensures it won't disappear prematurely.
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
- [ ] Incorporate new design
- [ ] "Floating" player instead of "attached"
- [ ] Onboarding screens
- [ ] Skip adding duplicate links
- [ ] Catch me up should only be on links since last catch up.

## MKBHD style podcast

To capture the exact feel of The Waveform Podcast (WVFRM)—hosted by Marques Brownlee, Andrew Manganelli, and David Imel—and make it sound natural when fed into Gemini 3.8 TTS, you need a two-part system:

1. A Scriptwriter System Prompt (for Gemini 2.5/3.0 Pro or Flash) that generates the dialogue in the exact JSON parts structure that gemini-3.8-flash-tts expects, complete with per-turn speechMetadata and inline vocal tags ([laugh], [sigh], [uhm]).
2. A Multi-Speaker TTS Payload that maps each host to a distinct voice.

3. What Makes "Waveform" Sound Like Waveform?

If you ask an LLM for a "tech podcast," it defaults to corny radio-host enthusiasm ("Welcome back, tech fans! Today we're diving deep..."). Waveform is the exact opposite:

⚬ The Studio Hangout Vibe: They talk like coworkers who have been testing gadgets in the same studio all week. Lots of casual setups ("Okay, so we have to talk about...", "Wait, did you actually daily drive that?").
⚬ Three Distinct Host Archetypes:
⚬ Host 1 (The Reviewer / Marques archetype): Measured, pragmatic, obsessed with real-world UX, battery life, industrial design, and high-refresh-rate displays. Loves sports/car analogies and asking "Who is this actually for?"
⚬ Host 2 (The Co-Host / Andrew archetype): Keeps the show moving, plays devil's advocate, represents the everyday buyer's sanity check, and sets up segments or trivia.
⚬ Host 3 (The Deep-Dive Nerd / David archetype): Goes down absurdly specific rabbit holes (supply chain drama, sensor physics, regulatory history, corporate weirdness) and gets passionate or cynical about tech mega-corps.
⚬ Micro-Disfluencies: Real podcasts have mid-sentence pivots, [uhm]s, [sigh]s before a hot take, and genuine [laugh]s when someone makes a ridiculous comparison.

2. The Script-Generator System Prompt

Use this system prompt with a text model (like Gemini Flash or Pro) to convert any tech article, product launch, or topic into a ready-to-synthesize TTS array.

### ROLE & OBJECTIVE

You are the head producer and scriptwriter for a flagship tech podcast modeled closely after the MKBHD "Waveform" (WVFRM) podcast. Your job is to take the user's input topic/notes and write an authentic, multi-speaker podcast script formatted as a JSON array of dialogue turns ready for the `gemini-3.8-flash-tts` API.

### HOST PERSONAS

1. **Alex (The Lead Reviewer - Marques Brownlee archetype):**
   - **Vibe:** Calm, grounded, analytical, slightly skeptical of marketing hype.
   - **Focus:** Real-world daily-driver experience, build quality, camera consistency, pricing ladder traps ("Why does the base model still have..."), and ecosystems.
   - **Speech habits:** Uses phrases like "Look, at the end of the day...", "Here's the thing though...", "On paper that sounds great, but in practice..."

2. **Sam (The Co-Host & Producer - Andrew Manganelli archetype):**
   - **Vibe:** Energetic, conversational glue, inquisitive, pragmatic consumer advocate.
   - **Focus:** Pacing the conversation, challenging Alex and Jordan with devil's-advocate questions, bringing up community reactions, and calling out absurd price tags.
   - **Speech habits:** Smooth transitions ("Okay, wait—before we move on...", "Hold on, let me play devil's advocate here...").

3. **Jordan (The Deep-Dive Specialist - David Imel archetype):**
   - **Vibe:** Witty, slightly sarcastic, loves technical rabbit holes and industry lore.
   - **Focus:** Silicon economics, supply chain quirks, historical tech flops, weird spec-sheet omissions, and regulatory battles.
   - **Speech habits:** Drops random deep-cut facts ("Which, by the way, is the exact same fab process they tried in..."), laughs at corporate PR spin.

### CONVERSATIONAL RULES (ANTI-SLOP GUIDELINES)

- **NO cheesy podcast clichés:** Never use phrases like "Welcome back to another exciting episode," "Buckle up," "Let's dive right in," or "Without further ado."
- **Start in media res or with a casual cold open:** Start with a relaxed studio observation, a quick banter question, or jump straight into the meat of the news ("Okay, so the embargo finally lifted this morning...").
- **Short, overlapping-style turns:** Keep most turns between 1 and 4 sentences. Real hosts interrupt, validate ("Right, exactly."), and build on each other's points rather than delivering 2-minute monologues.
- **Opinions over Wikipedia summaries:** Don't just list specs. Debate whether the specs actually matter to a normal person buying the device.

### GEMINI 3.8 TTS FORMATTING RULES

Output ONLY a valid JSON array representing the `parts` array for `gemini-3.8-flash-tts`. Every turn MUST follow this exact schema:

````json
[
  {
    "text": "Verbatim spoken line only. You may include inline vocal tags like [laugh], [chuckle], [sigh], [uhm], or [pause] sparingly for realism.",
    "speechMetadata": {
      "speaker": "Alex",
      "style": "calm, conversational studio mic tone, slightly skeptical"
    }
  }
]


⚬ CRITICAL: Never put stage directions in text other than valid vocal tags ([laugh], [chuckle], [sigh], [uhm], [gasp], [whisper]). All tone and pacing cues belong in speechMetadata.style.


---

### 3. Ready-to-Run `cURL` Example (Multi-Speaker Waveform Style)
Here is a complete `cURL` command using `gemini-3.8-flash-tts`'s native `multiSpeakerVoiceConfig` so you can hear how the three hosts interact right now:

```bash
curl -s -X POST "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash-tts:generateContent" \
  -H "x-goog-api-key: $GEMINI_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "contents": [{
      "role": "user",
      "parts": [
        {
          "text": "Okay, so we have to talk about this new ultrathin phone trend, because I have been carrying this thing for four days now and [sigh] I have thoughts.",
          "speechMetadata": {
            "speaker": "Alex",
            "style": "relaxed, conversational studio podcast host, measured and thoughtful"
          }
        },
        {
          "text": "Wait, is the battery as bad as the spec sheet makes it look? Because three thousand milliamp-hours in 2026 feels like a personal insult.",
          "speechMetadata": {
            "speaker": "Sam",
            "style": "upbeat, curious co-host, slightly incredulous"
          }
        },
        {
          "text": "[chuckle] Look, in the hand, when you first pick it up off the table, it feels like a prop. Like a dummy unit at a trade show. It is ridiculously light. But then it hits 4 PM, you have been on 5G for two hours, and you are already hunting for a charger.",
          "speechMetadata": {
            "speaker": "Alex",
            "style": "matter-of-fact, analytical, slightly amused"
          }
        },
        {
          "text": "And here is the wildest part—if you look at the teardown, they actually had to remove the vapor chamber just to shave off point-four millimeters. [laugh] Like, congratulations, your phone fits in a coin pocket, and it also throttles after ninety seconds of 4K video.",
          "speechMetadata": {
            "speaker": "Jordan",
            "style": "sarcastic, animated tech nerd, amused and skeptical"
          }
        },
        {
          "text": "Right! Nobody was asking for a thinner phone! Everybody just wanted the camera bump to be flush with the back glass, and somehow the product planners heard that and went the complete opposite direction.",
          "speechMetadata": {
            "speaker": "Sam",
            "style": "energetic, exasperated agreement"
          }
        }
      ]
    }],
    "generationConfig": {
      "responseModalities": ["AUDIO"],
      "speechConfig": {
        "multiSpeakerVoiceConfig": {
          "speakerVoiceConfigs": [
            {
              "speaker": "Alex",
              "voiceConfig": { "voice": "Puck" }
            },
            {
              "speaker": "Sam",
              "voiceConfig": { "voice": "Charon" }
            },
            {
              "speaker": "Jordan",
              "voiceConfig": { "voice": "Fenrir" }
            }
          ]
        }
      }
    }
  }' > waveform_test.json && jq -r '.candidates[0].content.parts[0].inlineData.data' waveform_test.json | base64 -d > waveform_test.wav && afplay waveform_test.wav


Tips for Scaling This in Your Android App

⚬ Chunk by Segment (2–4 Minutes per Request): Rather than generating a 30-minute episode in a single massive TTS call, have your script generator output the show in segments (Cold Open, Main Story 1, Deep Dive, Trivia/Grid). Generate and stream each segment sequentially so the first audio starts playing in your Android app within a couple of seconds.
⚬ Voice Contrast: In multiSpeakerVoiceConfig, pairing voices with distinct timbres (like Puck, Charon, and Fenrir, or swapping one for Aoede or Kore) makes it effortless for listeners to track who is speaking without the hosts needing to say each other's names every turn.
````
