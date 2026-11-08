package se.oscarb.summer.data

import kotlinx.serialization.Serializable
import kotlin.random.Random
import java.util.Locale

@Serializable
enum class AiBackend(val displayName: String, val subtitle: String) {
  FIREBASE_AI(
    displayName = "Firebase AI Logic",
    subtitle = "Gemini via your provisioned Firebase project"
  ),
  DIRECT_GEMINI(
    displayName = "Direct Gemini API",
    subtitle = "Google AI Studio API key directly from device"
  ),
  GEMINI_NANO_ON_DEVICE(
    displayName = "On-Device Gemini Nano",
    subtitle = "Local inference via Android AICore (no API key, supported flagship hardware)"
  ),
  OPENAI_COMPATIBLE(
    displayName = "OpenAI / Anthropic / Custom API",
    subtitle = "Any OpenAI-compatible chat completions endpoint or custom provider"
  )
}

@Serializable
enum class PodcastTtsProvider(val displayName: String, val subtitle: String) {
  GEMINI_MULTISPEAKER(
    displayName = "Gemini Multi-Speaker TTS (Free Tier)",
    subtitle = "Native multi-host dialogue via Google AI Studio (gemini-3.8-flash-tts)"
  ),
  NEKOSPEAK(
    displayName = "NekoSpeak On-Device AI TTS",
    subtitle = "Private offline AI voices with Pocket-TTS, Kokoro, KittenTTS & Piper (com.nekospeak.tts)"
  ),
  ANDROID_ON_DEVICE(
    displayName = "Android On-Device Multi-Voice TTS",
    subtitle = "100% free & offline system voices alternating across hosts (no API key)"
  ),
  AZURE_SPEECH(
    displayName = "Microsoft Azure AI Speech (500k Free/mo)",
    subtitle = "Perpetual free tier (500k neural chars/month) using multi-voice SSML"
  ),
  ELEVENLABS(
    displayName = "ElevenLabs Neural TTS",
    subtitle = "Ultra-realistic voices (~10k free credits/month with ElevenLabs account)"
  ),
  OPENAI_COMPATIBLE_TTS(
    displayName = "OpenAI / Kokoro / Custom TTS",
    subtitle = "OpenAI (gpt-4o-mini-tts / tts-1) or free Kokoro / Groq /v1/audio/speech endpoint"
  )
}

@Serializable
enum class PodcastStyle(val displayName: String, val subtitle: String) {
  STANDARD(
    displayName = "Standard Catch-Up Briefing",
    subtitle = "Structured overview introducing topics and key takeaways."
  ),
  MKBHD_WAVEFORM(
    displayName = "Waveform Studio Hangout (MKBHD)",
    subtitle = "Relaxed tech-studio debate with micro-disfluencies, honest opinions, and casual banter."
  )
}

@Serializable
enum class PodcastLanguage(
  val displayName: String,
  val shortBadge: String,
  val bcp47Tag: String
) {
  ENGLISH(
    displayName = "English",
    shortBadge = "EN",
    bcp47Tag = "en-US"
  ),
  SWEDISH(
    displayName = "Swedish (Svenska)",
    shortBadge = "SV",
    bcp47Tag = "sv-SE"
  )
}

enum class VoicePersona {
  FEMININE,
  MASCULINE
}

data class GeminiVoiceOption(
  val name: String,
  val style: String,
  val persona: VoicePersona
) {
  val displayLabel: String get() = "$name — $style"
}

data class AssignedHostVoice(
  val hostIndex: Int,
  val name: String,
  val voiceId: String,
  val persona: VoicePersona = VoicePersona.FEMININE
)

object GeminiCatalog {
  val PREBUILT_VOICES: List<GeminiVoiceOption> = listOf(
    GeminiVoiceOption("Puck", "Upbeat", VoicePersona.MASCULINE),
    GeminiVoiceOption("Kore", "Firm", VoicePersona.FEMININE),
    GeminiVoiceOption("Zephyr", "Bright", VoicePersona.FEMININE),
    GeminiVoiceOption("Charon", "Informative", VoicePersona.MASCULINE),
    GeminiVoiceOption("Aoede", "Breezy", VoicePersona.FEMININE),
    GeminiVoiceOption("Sulafat", "Warm", VoicePersona.FEMININE),
    GeminiVoiceOption("Achird", "Friendly", VoicePersona.MASCULINE),
    GeminiVoiceOption("Fenrir", "Excitable", VoicePersona.MASCULINE),
    GeminiVoiceOption("Leda", "Youthful", VoicePersona.FEMININE),
    GeminiVoiceOption("Orus", "Firm", VoicePersona.MASCULINE),
    GeminiVoiceOption("Callirrhoe", "Easy-going", VoicePersona.FEMININE),
    GeminiVoiceOption("Autonoe", "Bright", VoicePersona.FEMININE),
    GeminiVoiceOption("Enceladus", "Breathy", VoicePersona.MASCULINE),
    GeminiVoiceOption("Iapetus", "Clear", VoicePersona.MASCULINE),
    GeminiVoiceOption("Umbriel", "Easy-going", VoicePersona.MASCULINE),
    GeminiVoiceOption("Algieba", "Smooth", VoicePersona.MASCULINE),
    GeminiVoiceOption("Despina", "Smooth", VoicePersona.FEMININE),
    GeminiVoiceOption("Erinome", "Clear", VoicePersona.FEMININE),
    GeminiVoiceOption("Algenib", "Gravelly", VoicePersona.MASCULINE),
    GeminiVoiceOption("Rasalgethi", "Informative", VoicePersona.MASCULINE),
    GeminiVoiceOption("Laomedeia", "Upbeat", VoicePersona.FEMININE),
    GeminiVoiceOption("Achernar", "Soft", VoicePersona.FEMININE),
    GeminiVoiceOption("Alnilam", "Firm", VoicePersona.MASCULINE),
    GeminiVoiceOption("Schedar", "Even", VoicePersona.MASCULINE),
    GeminiVoiceOption("Gacrux", "Mature", VoicePersona.MASCULINE),
    GeminiVoiceOption("Pulcherrima", "Forward", VoicePersona.FEMININE),
    GeminiVoiceOption("Zubenelgenubi", "Casual", VoicePersona.MASCULINE),
    GeminiVoiceOption("Vindemiatrix", "Gentle", VoicePersona.FEMININE),
    GeminiVoiceOption("Sadachbia", "Lively", VoicePersona.MASCULINE),
    GeminiVoiceOption("Sadaltager", "Knowledgeable", VoicePersona.MASCULINE)
  )

  val VALID_VOICE_NAMES_LOWER: Map<String, String> =
    PREBUILT_VOICES.associate { it.name.lowercase() to it.name }

  val TTS_MODELS: List<Pair<String, String>> = listOf(
    "gemini-3.8-flash-tts" to "Gemini 3.8 Flash TTS (Flagship Studio Quality)",
    "gemini-3.8-flash-lite-tts" to "Gemini 3.8 Flash-Lite TTS (Fastest & Lightest)",
    "gemini-3.1-flash-tts-preview" to "Gemini 3.1 Flash TTS Preview",
    "gemini-2.5-flash-preview-tts" to "Gemini 2.5 Flash TTS Preview",
    "gemini-2.5-pro-preview-tts" to "Gemini 2.5 Pro TTS Preview"
  )

  val TEXT_MODELS: List<Pair<String, String>> = listOf(
    "gemini-3.5-flash" to "Gemini 3.5 Flash (Fast & Reliable on Free Tier)",
    "gemini-3.8-flash" to "Gemini 3.8 Flash (Latest Flagship — High Demand)",
    "gemini-3.7-flash" to "Gemini 3.7 Flash",
    "gemini-3.1-pro-preview" to "Gemini 3.1 Pro Preview (Deep Reasoning)",
    "gemini-3.5-flash-lite" to "Gemini 3.5 Flash-Lite (Ultra-Fast & High Quota)",
    "gemini-3-flash-preview" to "Gemini 3 Flash Preview",
    "gemini-2.5-flash" to "Gemini 2.5 Flash (Legacy Stable)",
    "gemini-2.5-pro" to "Gemini 2.5 Pro (Legacy Pro)"
  )

  fun resolveValidVoice(configured: String, defaultVoice: String): String {
    val trimmed = configured.trim()
    return VALID_VOICE_NAMES_LOWER[trimmed.lowercase()] ?: defaultVoice
  }

  fun voiceOptionFor(name: String): GeminiVoiceOption? {
    val resolved = VALID_VOICE_NAMES_LOWER[name.trim().lowercase()] ?: return null
    return PREBUILT_VOICES.firstOrNull { it.name == resolved }
  }
}

data class NekoSpeakVoiceOption(
  val id: String,
  val name: String,
  val engineLabel: String,
  val style: String,
  val persona: VoicePersona
) {
  val displayLabel: String get() = "$name — $engineLabel ($style)"
}

object NekoSpeakCatalog {
  const val PACKAGE_NAME = "com.nekospeak.tts"
  const val GITHUB_URL = "https://github.com/siva-sub/NekoSpeak"
  const val RELEASES_URL = "https://github.com/siva-sub/NekoSpeak/releases"

  val PREBUILT_VOICES: List<NekoSpeakVoiceOption> = listOf(
    // Kokoro v1.0 (Flagship 82M StyleTTS2 ONNX — 24kHz)
    NekoSpeakVoiceOption("af_heart", "Heart (US)", "Kokoro v1.0", "Warm & Expressive", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("am_adam", "Adam (US)", "Kokoro v1.0", "Clear & Natural", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("af_bella", "Bella (US)", "Kokoro v1.0", "Bright & Engaging", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("am_puck", "Puck (US)", "Kokoro v1.0", "Lively & Upbeat", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("af_kore", "Kore (US)", "Kokoro v1.0", "Firm & Articulate", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("am_fenrir", "Fenrir (US)", "Kokoro v1.0", "Dynamic & Bold", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("af_nova", "Nova (US)", "Kokoro v1.0", "Crisp & Modern", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("am_onyx", "Onyx (US)", "Kokoro v1.0", "Deep & Resonant", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("af_aoede", "Aoede (US)", "Kokoro v1.0", "Breezy & Conversational", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("am_michael", "Michael (US)", "Kokoro v1.0", "Measured & Informative", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("af_sarah", "Sarah (US)", "Kokoro v1.0", "Smooth & Friendly", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("am_liam", "Liam (US)", "Kokoro v1.0", "Warm & Casual", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("af_nicole", "Nicole (US)", "Kokoro v1.0", "Soft & Calm", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("am_echo", "Echo (US)", "Kokoro v1.0", "Steady & Clear", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("af_sky", "Sky (US)", "Kokoro v1.0", "Airy & Light", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("am_eric", "Eric (US)", "Kokoro v1.0", "Direct & Grounded", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("af_alloy", "Alloy (US)", "Kokoro v1.0", "Balanced Studio", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("af_jessica", "Jessica (US)", "Kokoro v1.0", "Expressive Host", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("af_river", "River (US)", "Kokoro v1.0", "Relaxed & Flowing", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("am_santa", "Santa (US)", "Kokoro v1.0", "Jolly & Deep", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("bf_emma", "Emma (UK)", "Kokoro v1.0", "British Studio", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("bm_george", "George (UK)", "Kokoro v1.0", "British Broadcast", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("bf_alice", "Alice (UK)", "Kokoro v1.0", "British Clear", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("bm_daniel", "Daniel (UK)", "Kokoro v1.0", "British Narrative", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("bf_isabella", "Isabella (UK)", "Kokoro v1.0", "British Warm", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("bm_fable", "Fable (UK)", "Kokoro v1.0", "British Storyteller", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("bf_lily", "Lily (UK)", "Kokoro v1.0", "British Gentle", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("bm_lewis", "Lewis (UK)", "Kokoro v1.0", "British Conversational", VoicePersona.MASCULINE),
    // Pocket-TTS (Kyutai 100M Streaming Flow-Matching — 24kHz)
    NekoSpeakVoiceOption("alba", "Alba", "Pocket-TTS", "Casual & Natural", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("fantine", "Fantine", "Pocket-TTS", "Expressive & Clear", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("marius", "Marius", "Pocket-TTS", "Young & Energetic", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("cosette", "Cosette", "Pocket-TTS", "Soft & Gentle", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("javert", "Javert", "Pocket-TTS", "Deep & Authoritative", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("eponine", "Eponine", "Pocket-TTS", "Warm & Spirited", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("jean", "Jean", "Pocket-TTS", "Calm & Steady", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("azelma", "Azelma", "Pocket-TTS", "Bright & Lively", VoicePersona.FEMININE),
    // Kitten TTS Nano (Ultra-lightweight 15M — 24kHz)
    NekoSpeakVoiceOption("expr-voice-2-f", "Kitten Voice 2 (F)", "Kitten TTS", "Expressive Feminine", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("expr-voice-2-m", "Kitten Voice 2 (M)", "Kitten TTS", "Expressive Masculine", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("expr-voice-3-f", "Kitten Voice 3 (F)", "Kitten TTS", "Clear Feminine", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("expr-voice-3-m", "Kitten Voice 3 (M)", "Kitten TTS", "Clear Masculine", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("expr-voice-4-f", "Kitten Voice 4 (F)", "Kitten TTS", "Warm Feminine", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("expr-voice-4-m", "Kitten Voice 4 (M)", "Kitten TTS", "Warm Masculine", VoicePersona.MASCULINE),
    NekoSpeakVoiceOption("expr-voice-5-f", "Kitten Voice 5 (F)", "Kitten TTS", "Bright Feminine", VoicePersona.FEMININE),
    NekoSpeakVoiceOption("expr-voice-5-m", "Kitten Voice 5 (M)", "Kitten TTS", "Steady Masculine", VoicePersona.MASCULINE),
    // Piper (Bundled default voice)
    NekoSpeakVoiceOption("en_US-amy-low", "Amy (US)", "Piper", "Fast Bundled Offline", VoicePersona.FEMININE)
  )

  private val VOICES_BY_ID_LOWER: Map<String, NekoSpeakVoiceOption> =
    PREBUILT_VOICES.associateBy { it.id.lowercase() }

  private val CELEBRITY_PERSONAS: Map<String, VoicePersona> = mapOf(
    "celebrity_trump" to VoicePersona.MASCULINE,
    "celebrity_biden" to VoicePersona.MASCULINE,
    "celebrity_obama" to VoicePersona.MASCULINE,
    "celebrity_hillary" to VoicePersona.FEMININE,
    "celebrity_kamala" to VoicePersona.FEMININE,
    "celebrity_musk" to VoicePersona.MASCULINE,
    "celebrity_bill_gates" to VoicePersona.MASCULINE,
    "celebrity_zuckerberg" to VoicePersona.MASCULINE,
    "celebrity_jensen" to VoicePersona.MASCULINE,
    "celebrity_oprah" to VoicePersona.FEMININE,
    "celebrity_jk_rowling" to VoicePersona.FEMININE,
    "celebrity_greta" to VoicePersona.FEMININE,
    "celebrity_tate" to VoicePersona.MASCULINE
  )

  fun voiceOptionFor(voiceId: String): NekoSpeakVoiceOption? {
    val clean = voiceId.trim().lowercase()
    return VOICES_BY_ID_LOWER[clean]
  }

  fun personaForVoice(voiceId: String): VoicePersona? {
    val clean = voiceId.trim().lowercase()
    if (clean.isEmpty()) return null
    VOICES_BY_ID_LOWER[clean]?.let { return it.persona }
    CELEBRITY_PERSONAS[clean]?.let { return it }
    return when {
      clean.startsWith("af_") || clean.startsWith("bf_") || clean.endsWith("-f") -> VoicePersona.FEMININE
      clean.startsWith("am_") || clean.startsWith("bm_") || clean.endsWith("-m") -> VoicePersona.MASCULINE
      else -> null
    }
  }

  /**
   * Resolves concrete NekoSpeak voice IDs for each host in [cast] against the voices exposed by
   * the currently active NekoSpeak model (Kokoro, Pocket-TTS, Kitten TTS, or Piper).
   *
   * If a host's configured `voiceId` belongs to the active engine, it is preserved; otherwise, a
   * distinct voice from [availableVoiceNames] matching the host's [VoicePersona] is selected so
   * multi-host dialogue stays natural and contrasting even when switching engines in NekoSpeak.
   */
  fun resolveVoicesForActiveEngine(
    cast: List<AssignedHostVoice>,
    availableVoiceNames: Collection<String>
  ): List<String> {
    val cleanedAvailable = availableVoiceNames.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    if (cleanedAvailable.isEmpty()) {
      val fallbackDefaults = listOf("af_heart", "am_adam", "af_bella")
      return cast.mapIndexed { index, host ->
        host.voiceId.trim().ifBlank {
          fallbackDefaults.getOrElse(index) { "af_heart" }
        }
      }
    }

    val availableByLower = cleanedAvailable.associateBy { it.lowercase() }
    val reservedVoices = cast.mapNotNull { host ->
      availableByLower[host.voiceId.trim().lowercase()]
    }.toMutableSet()
    val usedVoices = mutableSetOf<String>()

    return cast.map { host ->
      val directMatch = availableByLower[host.voiceId.trim().lowercase()]
      if (directMatch != null) {
        usedVoices.add(directMatch)
        directMatch
      } else {
        val candidatePool = cleanedAvailable.filter { it !in usedVoices && it !in reservedVoices }
          .ifEmpty { cleanedAvailable.filter { it !in usedVoices } }
          .ifEmpty { cleanedAvailable }
        val personaMatched = candidatePool.firstOrNull { personaForVoice(it) == host.persona }
          ?: candidatePool.first()
        usedVoices.add(personaMatched)
        personaMatched
      }
    }
  }
}

object PodcastCastCatalog {
  private val ENGLISH_FEMININE_NAMES = listOf(
    "Maya", "Claire", "Elena", "Nora", "Hannah", "Zoe",
    "Chloe", "Sophie", "Nina", "Tessa", "Aria", "Lucy"
  )
  private val ENGLISH_MASCULINE_NAMES = listOf(
    "Alex", "Leo", "Sam", "Liam", "Marcus", "Noah",
    "Ethan", "Julian", "Owen", "Miles", "Caleb", "Daniel"
  )
  private val SWEDISH_FEMININE_NAMES = listOf(
    "Astrid", "Maja", "Elsa", "Klara", "Freja", "Ebba",
    "Linnea", "Saga", "Alva", "Vera", "Signe", "Wilma"
  )
  private val SWEDISH_MASCULINE_NAMES = listOf(
    "Erik", "Lukas", "Viktor", "Oskar", "Emil", "Filip",
    "Axel", "Nils", "Anton", "Gustav", "Hugo", "matte"
      .replaceFirstChar { it.uppercase() }
  )

  private val AZURE_ENGLISH_VOICES = listOf(
    "en-US-AvaMultilingualNeural" to VoicePersona.FEMININE,
    "en-US-AndrewMultilingualNeural" to VoicePersona.MASCULINE,
    "en-US-EmmaMultilingualNeural" to VoicePersona.FEMININE,
    "en-US-BrianMultilingualNeural" to VoicePersona.MASCULINE
  )
  private val AZURE_SWEDISH_VOICES = listOf(
    "sv-SE-SofieNeural" to VoicePersona.FEMININE,
    "sv-SE-MattiasNeural" to VoicePersona.MASCULINE,
    "sv-SE-HilleviNeural" to VoicePersona.FEMININE
  )
  private val OPENAI_VOICES = listOf(
    "nova" to VoicePersona.FEMININE,
    "onyx" to VoicePersona.MASCULINE,
    "shimmer" to VoicePersona.FEMININE,
    "echo" to VoicePersona.MASCULINE,
    "coral" to VoicePersona.FEMININE,
    "ash" to VoicePersona.MASCULINE
  )
  private val ELEVENLABS_VOICES = listOf(
    "21m00Tcm4TlvDq8ikWAM" to VoicePersona.FEMININE, // Rachel
    "pNInz6obpgDQGcFmaJgB" to VoicePersona.MASCULINE, // Adam
    "EXAVITQu4vr4xnSDxMaL" to VoicePersona.FEMININE, // Sarah
    "ErXwobaYiN019PkySvjV" to VoicePersona.MASCULINE  // Antoni
  )

  /**
   * Builds the 2-host or 3-host cast (names + provider-matched voices) for a podcast episode,
   * randomizing contrasting voices and register-matched names when [AppSettings.randomizePodcastHosts]
   * is enabled, or honoring manual settings when disabled.
   */
  fun selectEpisodeCast(
    settings: AppSettings,
    random: Random = Random.Default
  ): List<AssignedHostVoice> {
    val hostCount = settings.podcastHostCount.coerceIn(2, 3)
    val language = settings.podcastLanguage

    if (!settings.randomizePodcastHosts) {
      val manualNames = listOf(
        settings.hostAName.trim().ifBlank { if (language == PodcastLanguage.SWEDISH) "Astrid" else "Alex" },
        settings.hostBName.trim().ifBlank { if (language == PodcastLanguage.SWEDISH) "Erik" else "Sam" },
        settings.hostCName.trim().ifBlank { if (language == PodcastLanguage.SWEDISH) "Freja" else "Jordan" }
      ).take(hostCount)

      val manualVoices = when (settings.podcastTtsProvider) {
        PodcastTtsProvider.GEMINI_MULTISPEAKER -> listOf(
          GeminiCatalog.resolveValidVoice(settings.geminiHostAVoice, "Puck"),
          GeminiCatalog.resolveValidVoice(settings.geminiHostBVoice, "Kore"),
          GeminiCatalog.resolveValidVoice(settings.geminiHostCVoice, "Aoede")
        )
        PodcastTtsProvider.NEKOSPEAK -> listOf(
          settings.nekoSpeakHostAVoice.trim().ifBlank { "af_heart" },
          settings.nekoSpeakHostBVoice.trim().ifBlank { "am_adam" },
          settings.nekoSpeakHostCVoice.trim().ifBlank { "af_bella" }
        )
        PodcastTtsProvider.AZURE_SPEECH -> {
          if (language == PodcastLanguage.SWEDISH &&
            settings.azureHostAVoice.startsWith("en-", ignoreCase = true)
          ) {
            AZURE_SWEDISH_VOICES.map { it.first }
          } else {
            listOf(
              settings.azureHostAVoice.trim().ifBlank { "en-US-AndrewMultilingualNeural" },
              settings.azureHostBVoice.trim().ifBlank { "en-US-AvaMultilingualNeural" },
              settings.azureHostCVoice.trim().ifBlank { "en-US-EmmaMultilingualNeural" }
            )
          }
        }
        PodcastTtsProvider.ELEVENLABS -> listOf(
          settings.elevenLabsHostAVoiceId.trim().ifBlank { "pNInz6obpgDQGcFmaJgB" },
          settings.elevenLabsHostBVoiceId.trim().ifBlank { "21m00Tcm4TlvDq8ikWAM" },
          settings.elevenLabsHostCVoiceId.trim().ifBlank { "EXAVITQu4vr4xnSDxMaL" }
        )
        PodcastTtsProvider.OPENAI_COMPATIBLE_TTS -> listOf(
          settings.openAiHostAVoice.trim().ifBlank { "onyx" },
          settings.openAiHostBVoice.trim().ifBlank { "nova" },
          settings.openAiHostCVoice.trim().ifBlank { "shimmer" }
        )
        PodcastTtsProvider.ANDROID_ON_DEVICE -> listOf(
          settings.androidHostAVoice.trim(),
          settings.androidHostBVoice.trim(),
          settings.androidHostCVoice.trim()
        )
      }.take(hostCount)

      return manualNames.mapIndexed { index, name ->
        val voice = manualVoices.getOrElse(index) { "" }
        val persona = GeminiCatalog.voiceOptionFor(voice)?.persona
          ?: NekoSpeakCatalog.personaForVoice(voice)
          ?: if (index % 2 == 0) VoicePersona.MASCULINE else VoicePersona.FEMININE
        AssignedHostVoice(
          hostIndex = index,
          name = name,
          voiceId = voice,
          persona = persona
        )
      }
    }

    // Randomized cast: alternate personas (e.g. F/M or M/F, and F/M/F or M/F/M for 3 hosts)
    // so voices are always easy to tell apart!
    val startFeminine = random.nextBoolean()
    val desiredPersonas = List(hostCount) { idx ->
      val isFem = if (idx % 2 == 0) startFeminine else !startFeminine
      if (isFem) VoicePersona.FEMININE else VoicePersona.MASCULINE
    }

    val femNames = (if (language == PodcastLanguage.SWEDISH) SWEDISH_FEMININE_NAMES else ENGLISH_FEMININE_NAMES)
      .shuffled(random)
      .toMutableList()
    val mascNames = (if (language == PodcastLanguage.SWEDISH) SWEDISH_MASCULINE_NAMES else ENGLISH_MASCULINE_NAMES)
      .shuffled(random)
      .toMutableList()

    val usedVoices = mutableSetOf<String>()

    fun pickVoiceForPersona(persona: VoicePersona, index: Int): String {
      return when (settings.podcastTtsProvider) {
        PodcastTtsProvider.GEMINI_MULTISPEAKER -> {
          val matching = GeminiCatalog.PREBUILT_VOICES
            .filter { it.persona == persona && it.name !in usedVoices }
          val chosen = (matching.ifEmpty { GeminiCatalog.PREBUILT_VOICES.filter { it.name !in usedVoices } })
            .random(random)
          usedVoices.add(chosen.name)
          chosen.name
        }
        PodcastTtsProvider.NEKOSPEAK -> {
          val matching = NekoSpeakCatalog.PREBUILT_VOICES
            .filter { it.persona == persona && it.id !in usedVoices }
          val chosen = (matching.ifEmpty { NekoSpeakCatalog.PREBUILT_VOICES.filter { it.id !in usedVoices } })
            .random(random)
          usedVoices.add(chosen.id)
          chosen.id
        }
        PodcastTtsProvider.AZURE_SPEECH -> {
          val pool = if (language == PodcastLanguage.SWEDISH) AZURE_SWEDISH_VOICES else AZURE_ENGLISH_VOICES
          val matching = pool.filter { it.second == persona && it.first !in usedVoices }
          val chosen = (matching.ifEmpty { pool.filter { it.first !in usedVoices } }.ifEmpty { pool })
            .random(random)
          usedVoices.add(chosen.first)
          chosen.first
        }
        PodcastTtsProvider.ELEVENLABS -> {
          val matching = ELEVENLABS_VOICES.filter { it.second == persona && it.first !in usedVoices }
          val chosen = (matching.ifEmpty { ELEVENLABS_VOICES.filter { it.first !in usedVoices } })
            .random(random)
          usedVoices.add(chosen.first)
          chosen.first
        }
        PodcastTtsProvider.OPENAI_COMPATIBLE_TTS -> {
          val matching = OPENAI_VOICES.filter { it.second == persona && it.first !in usedVoices }
          val chosen = (matching.ifEmpty { OPENAI_VOICES.filter { it.first !in usedVoices } })
            .random(random)
          usedVoices.add(chosen.first)
          chosen.first
        }
        PodcastTtsProvider.ANDROID_ON_DEVICE -> "" // On-device selects distinct system voices dynamically
      }
    }

    return desiredPersonas.mapIndexed { index, persona ->
      val chosenName = if (persona == VoicePersona.FEMININE) {
        femNames.removeAt(0)
      } else {
        mascNames.removeAt(0)
      }
      val chosenVoice = pickVoiceForPersona(persona, index)
      AssignedHostVoice(
        hostIndex = index,
        name = chosenName,
        voiceId = chosenVoice,
        persona = persona
      )
    }
  }

  fun formatHostList(names: List<String>, language: PodcastLanguage = PodcastLanguage.ENGLISH): String {
    val clean = names.map { it.trim() }.filter { it.isNotEmpty() }
    return when (clean.size) {
      0 -> if (language == PodcastLanguage.SWEDISH) "Astrid & Erik" else "Alex & Sam"
      1 -> clean[0]
      2 -> "${clean[0]} & ${clean[1]}"
      else -> "${clean.dropLast(1).joinToString(", ")} & ${clean.last()}"
    }
  }
}

@Serializable
data class DialogueTurn(
  val speaker: String,
  val hostIndex: Int = 0,
  val text: String
) {
  val isHostB: Boolean get() = hostIndex == 1
}

@Serializable
enum class LinkFilter(val label: String) {
  UNREAD("Unread"),
  ALL("All"),
  ARCHIVED("Archived")
}

@Serializable
data class SavedWebsite(
  val id: String,
  val url: String,
  val domain: String,
  val title: String,
  val faviconUrl: String,
  val thumbnailUrl: String = "",
  val extractedContent: String = "",
  val summary: String? = null,
  val isSummarizing: Boolean = false,
  val isLoadingMetadata: Boolean = false,
  val summaryError: String? = null,
  val summaryProvider: String? = null,
  val podcastScript: String? = null,
  val podcastAudioPath: String? = null,
  val podcastVoiceProvider: String? = null,
  val isGeneratingPodcast: Boolean = false,
  val podcastStatusText: String? = null,
  val podcastError: String? = null,
  val createdAt: Long,
  val updatedAt: Long,
  val isRead: Boolean = false,
  val isArchived: Boolean = false
)

@Serializable
data class DigestEntry(
  val id: String,
  val createdAt: Long,
  val episodeNumber: Int = 1,
  val title: String = "",
  val summaryMarkdown: String = "",
  val providerName: String,
  val sourceWebsites: List<DigestSourceWebsite>,
  val hostNames: List<String> = emptyList(),
  val language: PodcastLanguage = PodcastLanguage.ENGLISH,
  val podcastScript: String? = null,
  val podcastAudioPath: String? = null,
  val podcastVoiceProvider: String? = null,
  val isGeneratingPodcast: Boolean = false,
  val podcastStatusText: String? = null,
  val podcastError: String? = null,
  val isPlayed: Boolean = false
) {
  val displayTitle: String
    get() = title.ifBlank {
      sourceWebsites.firstOrNull()?.title?.ifBlank { "Daily Catch-Up Briefing" } ?: "Daily Catch-Up Briefing"
    }

  val episodeBadgeText: String
    get() = if (episodeNumber > 0) String.format(Locale.ROOT, "EPISODE %02d", episodeNumber) else "EPISODE BRIEFING"
}

@Serializable
data class DigestSourceWebsite(
  val id: String,
  val title: String,
  val url: String,
  val domain: String,
  val faviconUrl: String
)

@Serializable
data class AppSettings(
  val selectedBackend: AiBackend = AiBackend.FIREBASE_AI,
  val autoSummarizeOnSave: Boolean = false,
  val firebaseModel: String = "gemini-3.8-flash",
  val directGeminiApiKey: String = "",
  val directGeminiModel: String = "gemini-3.8-flash",
  val openAiBaseUrl: String = "https://api.openai.com/v1",
  val openAiApiKey: String = "",
  val openAiModel: String = "gpt-4o-mini",
  val preferredBrowserPackage: String = "", // empty = auto-detect external browser
  val useCustomTabs: Boolean = false, // false = open full browser app; true = open Chrome Custom Tab overlay
  val onlyInterceptGoogleFeed: Boolean = false, // when true, non-Google-Feed links are forwarded to external browser
  val removeWhenOpenedFromNotification: Boolean = false, // when true, opening a link from the quick-save notification deletes it from Summer
  // Podcast & TTS Settings
  val podcastStyle: PodcastStyle = PodcastStyle.STANDARD,
  val podcastTtsProvider: PodcastTtsProvider = PodcastTtsProvider.GEMINI_MULTISPEAKER,
  val podcastLanguage: PodcastLanguage = PodcastLanguage.ENGLISH,
  val podcastHostCount: Int = 2, // 2 or 3 hosts
  val randomizePodcastHosts: Boolean = true,
  val fallbackToOnDeviceTts: Boolean = true,
  val autoGeneratePodcastOnDigest: Boolean = true,
  val autoGenerateArticlePodcastOnSave: Boolean = false,
  // Session Quiet-Window Auto-Podcast Settings
  val autoPodcastAfterQuietWindow: Boolean = false,
  val autoPodcastQuietMinutes: Int = 2, // 1..10 minutes after last saved URL
  val autoPodcastMinLinks: Int = 2, // 2..10 minimum unread links in session burst
  val autoPodcastSessionGapMinutes: Int = 30, // links saved within this gap belong to same session
  // Manual Host Names & Voices (used when randomizePodcastHosts == false)
  val hostAName: String = "Alex",
  val hostBName: String = "Sam",
  val hostCName: String = "Jordan",
  val geminiTtsModel: String = "gemini-3.8-flash-tts",
  val geminiHostAVoice: String = "Puck",
  val geminiHostBVoice: String = "Kore",
  val geminiHostCVoice: String = "Aoede",
  val elevenLabsApiKey: String = "",
  val elevenLabsModel: String = "eleven_flash_v2_5",
  val elevenLabsHostAVoiceId: String = "pNInz6obpgDQGcFmaJgB",
  val elevenLabsHostBVoiceId: String = "21m00Tcm4TlvDq8ikWAM",
  val elevenLabsHostCVoiceId: String = "EXAVITQu4vr4xnSDxMaL",
  val azureSpeechKey: String = "",
  val azureSpeechRegion: String = "eastus",
  val azureHostAVoice: String = "en-US-AndrewMultilingualNeural",
  val azureHostBVoice: String = "en-US-AvaMultilingualNeural",
  val azureHostCVoice: String = "en-US-EmmaMultilingualNeural",
  val openAiTtsBaseUrl: String = "https://api.openai.com/v1",
  val openAiTtsApiKey: String = "",
  val openAiTtsModel: String = "gpt-4o-mini-tts",
  val openAiHostAVoice: String = "onyx",
  val openAiHostBVoice: String = "nova",
  val openAiHostCVoice: String = "shimmer",
  val androidTtsEngine: String = "", // empty = system default TextToSpeech engine
  val androidHostAVoice: String = "", // empty = auto-select distinct voice 1
  val androidHostBVoice: String = "", // empty = auto-select distinct voice 2
  val androidHostCVoice: String = "", // empty = auto-select distinct voice 3
  val androidSpeechRate: Float = 1.0f,
  val androidSpeechPitch: Float = 1.0f,
  val nekoSpeakHostAVoice: String = "af_heart",
  val nekoSpeakHostBVoice: String = "am_adam",
  val nekoSpeakHostCVoice: String = "af_bella",
  val nekoSpeakSpeechRate: Float = 1.0f
)
