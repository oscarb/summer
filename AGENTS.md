# Summer (se.oscarb.summer)

Kotlin + Jetpack Compose read-later app. Saves shared URLs, generates AI digests ("Catch Me Up"),
and synthesizes those digests into listenable podcasts with on-device text-to-speech.

## Layout

- `app/src/main/java/se/oscarb/summer/data/` — models, repository, AI backends, scraping, TTS/playback
- `app/src/main/java/se/oscarb/summer/ui/main/` — single-Activity Compose UI (`MainScreen.kt` + ViewModel)
- `app/src/test/java/se/oscarb/summer/` — JVM unit tests

## Architecture notes

- `SummerRepository` is a singleton (`getInstance(context)`) holding all `StateFlow`s and persisting
  to JSON files in `filesDir`. `MainScreenViewModel` only combines those flows into `SummerUiState`.
- Podcast pipeline: `AiSummaryService.generatePodcastScript` (plain narration, no Markdown) →
  `PodcastSynthesizer.synthesizeToFiles` (per-chunk `.wav` in `filesDir/podcast_audio`) →
  `PodcastPlayer` (sequential `MediaPlayer`). If an engine cannot write files, playback falls back
  to live `TextToSpeech.speak`.
- `PodcastScriptChunker` is deliberately Android-free so it can be unit tested on the JVM.

## Build environment

The sandbox has no JDK or Android SDK by default. Set up once per session:

```bash
sudo apt-get install -y openjdk-21-jdk-headless
# Android cmdline-tools + SDK go to /workspace/android-sdk (see below)
printf 'sdk.dir=/workspace/android-sdk\n' > local.properties
```

Then build with:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/workspace/android-sdk
export PATH=$JAVA_HOME/bin:$PATH
./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon
```

Notes:
- Gradle 9.1 + AGP 9.0.1 + Kotlin 2.3.20; compileSdk 36. First run downloads Gradle and deps
  (takes a couple of minutes); run long Gradle invocations in the background and tail the log.
- `unzip` is missing in the sandbox — use `python3 -c "import zipfile; ..."` to extract archives.
- `local.properties` is gitignored; do not commit it.

## Conventions

- One `MainScreen.kt` file holds all tabs and sheets; keep new composables `private` and colocate
  them near related UI.
- `kotlinx.coroutines.combine` only has typed overloads up to five flows; nest with small private
  data classes (see `CoreState`/`Sheets` in `MainScreenViewModel`) instead of casting from arrays.
- Never swallow `CancellationException` as an error.
