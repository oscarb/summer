package se.oscarb.summer.data

import kotlinx.serialization.Serializable
import kotlin.random.Random

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
  val podcastError: String? = null
)

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
  // Podcast & TTS Settings
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
  val androidSpeechPitch: Float = 1.0f
)
