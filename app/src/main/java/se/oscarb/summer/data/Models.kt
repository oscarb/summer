package se.oscarb.summer.data

import kotlinx.serialization.Serializable

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
    subtitle = "Native 2-host dialogue in one pass via Google AI Studio (gemini-3.8-flash-tts)"
  ),
  ANDROID_ON_DEVICE(
    displayName = "Android On-Device Multi-Voice TTS",
    subtitle = "100% free & offline system voices alternating between two hosts (no API key)"
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

data class GeminiVoiceOption(
  val name: String,
  val style: String
) {
  val displayLabel: String get() = "$name — $style"
}

object GeminiCatalog {
  val PREBUILT_VOICES: List<GeminiVoiceOption> = listOf(
    GeminiVoiceOption("Puck", "Upbeat"),
    GeminiVoiceOption("Kore", "Firm"),
    GeminiVoiceOption("Zephyr", "Bright"),
    GeminiVoiceOption("Charon", "Informative"),
    GeminiVoiceOption("Aoede", "Breezy"),
    GeminiVoiceOption("Sulafat", "Warm"),
    GeminiVoiceOption("Achird", "Friendly"),
    GeminiVoiceOption("Fenrir", "Excitable"),
    GeminiVoiceOption("Leda", "Youthful"),
    GeminiVoiceOption("Orus", "Firm"),
    GeminiVoiceOption("Callirrhoe", "Easy-going"),
    GeminiVoiceOption("Autonoe", "Bright"),
    GeminiVoiceOption("Enceladus", "Breathy"),
    GeminiVoiceOption("Iapetus", "Clear"),
    GeminiVoiceOption("Umbriel", "Easy-going"),
    GeminiVoiceOption("Algieba", "Smooth"),
    GeminiVoiceOption("Despina", "Smooth"),
    GeminiVoiceOption("Erinome", "Clear"),
    GeminiVoiceOption("Algenib", "Gravelly"),
    GeminiVoiceOption("Rasalgethi", "Informative"),
    GeminiVoiceOption("Laomedeia", "Upbeat"),
    GeminiVoiceOption("Achernar", "Soft"),
    GeminiVoiceOption("Alnilam", "Firm"),
    GeminiVoiceOption("Schedar", "Even"),
    GeminiVoiceOption("Gacrux", "Mature"),
    GeminiVoiceOption("Pulcherrima", "Forward"),
    GeminiVoiceOption("Zubenelgenubi", "Casual"),
    GeminiVoiceOption("Vindemiatrix", "Gentle"),
    GeminiVoiceOption("Sadachbia", "Lively"),
    GeminiVoiceOption("Sadaltager", "Knowledgeable")
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

  /**
   * Ensures a configured voice string matches one of the 30 valid prebuilt Gemini TTS voices
   * (case-insensitive), falling back to [defaultVoice] if invalid (e.g. if a speaker name like
   * "Joe" or "Jane" was previously entered).
   */
  fun resolveValidVoice(configured: String, defaultVoice: String): String {
    val trimmed = configured.trim()
    return VALID_VOICE_NAMES_LOWER[trimmed.lowercase()] ?: defaultVoice
  }
}

@Serializable
data class DialogueTurn(
  val speaker: String,
  val isHostB: Boolean,
  val text: String
)

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
  val summaryMarkdown: String,
  val providerName: String,
  val sourceWebsites: List<DigestSourceWebsite>,
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
  val fallbackToOnDeviceTts: Boolean = true,
  val autoGeneratePodcastOnDigest: Boolean = true,
  val autoGenerateArticlePodcastOnSave: Boolean = false,
  val hostAName: String = "Alex",
  val hostBName: String = "Sam",
  val geminiTtsModel: String = "gemini-3.8-flash-tts",
  val geminiHostAVoice: String = "Puck",
  val geminiHostBVoice: String = "Kore",
  val elevenLabsApiKey: String = "",
  val elevenLabsModel: String = "eleven_flash_v2_5",
  val elevenLabsHostAVoiceId: String = "pNInz6obpgDQGcFmaJgB",
  val elevenLabsHostBVoiceId: String = "21m00Tcm4TlvDq8ikWAM",
  val azureSpeechKey: String = "",
  val azureSpeechRegion: String = "eastus",
  val azureHostAVoice: String = "en-US-AndrewMultilingualNeural",
  val azureHostBVoice: String = "en-US-AvaMultilingualNeural",
  val openAiTtsBaseUrl: String = "https://api.openai.com/v1",
  val openAiTtsApiKey: String = "",
  val openAiTtsModel: String = "gpt-4o-mini-tts",
  val openAiHostAVoice: String = "onyx",
  val openAiHostBVoice: String = "nova",
  val androidTtsEngine: String = "", // empty = system default TextToSpeech engine
  val androidHostAVoice: String = "", // empty = auto-select distinct voice 1
  val androidHostBVoice: String = "", // empty = auto-select distinct voice 2
  val androidSpeechRate: Float = 1.0f,
  val androidSpeechPitch: Float = 1.0f
)
