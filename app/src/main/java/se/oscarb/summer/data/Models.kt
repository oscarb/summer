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
  val sourceWebsites: List<DigestSourceWebsite>
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
enum class PodcastStatus {
  /** Script is being written by the AI backend. */
  WRITING_SCRIPT,

  /** Script is ready; speech is being synthesized and cached on device. */
  SYNTHESIZING,

  /** Audio is cached and ready to play. */
  READY,

  /** Playback finished. */
  COMPLETED,

  /** Script or speech generation failed; see [PodcastEpisode.error]. */
  FAILED
}

/**
 * A listenable "podcast" episode generated from the user's saved links.
 *
 * [audioFilePaths] point at cached speech files in the app's private storage, split into
 * per-chunk parts that are played back to back. The list is empty while the episode is
 * still being synthesized (or if synthesis failed), in which case playback falls back to
 * live on-device text-to-speech.
 */
@Serializable
data class PodcastEpisode(
  val id: String,
  val title: String,
  val createdAt: Long,
  val updatedAt: Long,
  val scriptMarkdown: String,
  val providerName: String,
  val voiceName: String? = null,
  val status: PodcastStatus = PodcastStatus.WRITING_SCRIPT,
  val audioFilePaths: List<String> = emptyList(),
  val durationMs: Long = 0L,
  val error: String? = null,
  val sourceWebsites: List<DigestSourceWebsite> = emptyList()
)

/** Live playback state for the podcast player, surfaced to the UI. */
data class PodcastPlaybackState(
  val episodeId: String? = null,
  val isPlaying: Boolean = false,
  val partIndex: Int = 0,
  val totalParts: Int = 0
)

@Serializable
data class AppSettings(
  val selectedBackend: AiBackend = AiBackend.FIREBASE_AI,
  val autoSummarizeOnSave: Boolean = false,
  val firebaseModel: String = "gemini-2.5-flash",
  val directGeminiApiKey: String = "",
  val directGeminiModel: String = "gemini-2.5-flash",
  val openAiBaseUrl: String = "https://api.openai.com/v1",
  val openAiApiKey: String = "",
  val openAiModel: String = "gpt-4o-mini",
  val preferredBrowserPackage: String = "", // empty = auto-detect external browser / Custom Tabs
  // Podcast / text-to-speech playback
  val podcastTtsEngine: String = "", // empty = system default TextToSpeech engine
  val podcastVoiceName: String = "", // empty = engine default voice
  val podcastSpeechRate: Float = 1.0f,
  val podcastSpeechPitch: Float = 1.0f
)
