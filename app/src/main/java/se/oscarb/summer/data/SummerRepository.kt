package se.oscarb.summer.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class SummerRepository private constructor(private val appContext: Context) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    encodeDefaults = true
  }

  private val websitesFile = File(appContext.filesDir, "saved_websites.json")
  private val digestsFile = File(appContext.filesDir, "saved_digests.json")
  private val podcastsFile = File(appContext.filesDir, "saved_podcasts.json")
  private val settingsFile = File(appContext.filesDir, "app_settings.json")
  private val podcastAudioDir = File(appContext.filesDir, "podcast_audio")

  private val aiService = AiSummaryService(appContext)
  private val synthesizer = PodcastSynthesizer(appContext)

  private val _websites = MutableStateFlow<List<SavedWebsite>>(emptyList())
  val websites: StateFlow<List<SavedWebsite>> = _websites.asStateFlow()

  private val _digests = MutableStateFlow<List<DigestEntry>>(emptyList())
  val digests: StateFlow<List<DigestEntry>> = _digests.asStateFlow()

  private val _podcasts = MutableStateFlow<List<PodcastEpisode>>(emptyList())
  val podcasts: StateFlow<List<PodcastEpisode>> = _podcasts.asStateFlow()

  private val _settings = MutableStateFlow(AppSettings())
  val settings: StateFlow<AppSettings> = _settings.asStateFlow()

  private val _isGeneratingDigest = MutableStateFlow(false)
  val isGeneratingDigest: StateFlow<Boolean> = _isGeneratingDigest.asStateFlow()

  private val _podcastPlayback = MutableStateFlow(PodcastPlaybackState())
  val podcastPlayback: StateFlow<PodcastPlaybackState> = _podcastPlayback.asStateFlow()

  private val player = PodcastPlayer(
    synthesizer = synthesizer,
    onProgress = { episodeId, partIndex, totalParts ->
      _podcastPlayback.update {
        it.copy(episodeId = episodeId, isPlaying = true, partIndex = partIndex, totalParts = totalParts)
      }
    },
    onFinished = { episodeId ->
      _podcastPlayback.update { it.copy(isPlaying = false, episodeId = null, partIndex = 0, totalParts = 0) }
      _podcasts.update { list ->
        list.map { if (it.id == episodeId) it.copy(status = PodcastStatus.COMPLETED) else it }
      }
      persistPodcasts()
    },
    onError = { episodeId, message ->
      _podcastPlayback.update { it.copy(isPlaying = false, episodeId = null) }
      _podcasts.update { list ->
        list.map { if (it.id == episodeId) it.copy(status = PodcastStatus.FAILED, error = message) else it }
      }
      persistPodcasts()
    }
  )

  init {
    loadFromDisk()
  }

  private fun loadFromDisk() {
    runCatching {
      if (settingsFile.exists()) {
        _settings.value = json.decodeFromString<AppSettings>(settingsFile.readText())
      }
    }
    runCatching {
      if (websitesFile.exists()) {
        val loaded = json.decodeFromString<List<SavedWebsite>>(websitesFile.readText())
        _websites.value = loaded
          .map { it.copy(isSummarizing = false, isLoadingMetadata = false) }
          .sortedByDescending { it.updatedAt }
      }
    }
    runCatching {
      if (digestsFile.exists()) {
        val loaded = json.decodeFromString<List<DigestEntry>>(digestsFile.readText())
        _digests.value = loaded.sortedByDescending { it.createdAt }
      }
    }
    runCatching {
      if (podcastsFile.exists()) {
        val loaded = json.decodeFromString<List<PodcastEpisode>>(podcastsFile.readText())
        _podcasts.value = loaded
          // Any episode left mid-generation (app killed) is shown as failed rather than stuck spinning.
          .map {
            if (it.status == PodcastStatus.WRITING_SCRIPT || it.status == PodcastStatus.SYNTHESIZING) {
              it.copy(
                status = PodcastStatus.FAILED,
                error = it.error ?: "Podcast generation was interrupted. Tap retry to try again."
              )
            } else {
              it
            }
          }
          .sortedByDescending { it.createdAt }
      }
    }
  }

  private fun persistWebsites() {
    scope.launch {
      runCatching {
        websitesFile.writeText(json.encodeToString(_websites.value))
      }
    }
  }

  private fun persistDigests() {
    scope.launch {
      runCatching {
        digestsFile.writeText(json.encodeToString(_digests.value))
      }
    }
  }

  private fun persistPodcasts() {
    scope.launch {
      runCatching {
        podcastsFile.writeText(json.encodeToString(_podcasts.value))
      }
    }
  }

  private fun persistSettings() {
    scope.launch {
      runCatching {
        settingsFile.writeText(json.encodeToString(_settings.value))
      }
    }
  }

  fun updateSettings(transform: (AppSettings) -> AppSettings) {
    _settings.update(transform)
    persistSettings()
    scope.launch { runCatching { synthesizer.applySettings(_settings.value) } }
  }

  /**
   * Saves a URL (or updates timestamp + marks unread if already present).
   * Returns true if it was newly added, false if an existing entry was bumped to top.
   */
  fun saveOrBumpUrl(rawUrl: String): SaveUrlOutcome {
    val normalized = normalizeUrl(rawUrl) ?: return SaveUrlOutcome.InvalidUrl
    val now = System.currentTimeMillis()
    val domain = WebPageScraper.extractDomain(normalized)
    val defaultFavicon = WebPageScraper.defaultFaviconUrl(normalized)

    var targetId = ""
    var isDuplicate = false

    _websites.update { currentList ->
      val existing = currentList.firstOrNull { it.url.equals(normalized, ignoreCase = true) }
      if (existing != null) {
        isDuplicate = true
        targetId = existing.id
        val bumped = existing.copy(
          updatedAt = now,
          isRead = false,
          isArchived = false,
          isLoadingMetadata = existing.extractedContent.isBlank()
        )
        (listOf(bumped) + currentList.filterNot { it.id == existing.id })
          .sortedByDescending { it.updatedAt }
      } else {
        val newEntry = SavedWebsite(
          id = UUID.randomUUID().toString(),
          url = normalized,
          domain = domain,
          title = domain,
          faviconUrl = defaultFavicon,
          isLoadingMetadata = true,
          createdAt = now,
          updatedAt = now,
          isRead = false,
          isArchived = false
        )
        targetId = newEntry.id
        (listOf(newEntry) + currentList).sortedByDescending { it.updatedAt }
      }
    }
    persistWebsites()

    // Background scrape title, favicon, and readable text
    scope.launch {
      val scraped = WebPageScraper.scrape(normalized)
      _websites.update { list ->
        list.map { item ->
          if (item.id == targetId) {
            item.copy(
              title = scraped.title.ifBlank { item.title },
              faviconUrl = scraped.faviconUrl.ifBlank { item.faviconUrl },
              domain = scraped.domain.ifBlank { item.domain },
              extractedContent = scraped.readableContent.ifBlank { item.extractedContent },
              isLoadingMetadata = false
            )
          } else {
            item
          }
        }
      }
      persistWebsites()

      // Auto-summarize if enabled in Settings
      if (_settings.value.autoSummarizeOnSave) {
        val currentItem = _websites.value.firstOrNull { it.id == targetId }
        if (currentItem != null && currentItem.summary.isNullOrBlank()) {
          summarizeWebsite(targetId)
        }
      }
    }

    return if (isDuplicate) {
      SaveUrlOutcome.BumpedExisting(domain)
    } else {
      SaveUrlOutcome.SavedNew(domain)
    }
  }

  fun summarizeWebsite(websiteId: String) {
    scope.launch {
      val site = _websites.value.firstOrNull { it.id == websiteId } ?: return@launch
      if (site.isSummarizing) return@launch

      _websites.update { list ->
        list.map {
          if (it.id == websiteId) it.copy(isSummarizing = true, summaryError = null) else it
        }
      }

      // Ensure we have scraped content if it was empty
      val preparedSite = if (site.extractedContent.isBlank()) {
        val scraped = WebPageScraper.scrape(site.url)
        val updated = site.copy(
          title = scraped.title.ifBlank { site.title },
          faviconUrl = scraped.faviconUrl.ifBlank { site.faviconUrl },
          extractedContent = scraped.readableContent
        )
        _websites.update { list ->
          list.map { if (it.id == websiteId) updated.copy(isSummarizing = true) else it }
        }
        updated
      } else {
        site
      }

      val currentSettings = _settings.value
      val result = aiService.summarizeSingleWebsite(preparedSite, currentSettings)

      _websites.update { list ->
        list.map { item ->
          if (item.id == websiteId) {
            result.fold(
              onSuccess = { summaryText ->
                item.copy(
                  summary = summaryText,
                  summaryProvider = currentSettings.selectedBackend.displayName,
                  summaryError = null,
                  isSummarizing = false
                )
              },
              onFailure = { err ->
                item.copy(
                  summaryError = err.message ?: "Failed to summarize website",
                  isSummarizing = false
                )
              }
            )
          } else {
            item
          }
        }
      }
      persistWebsites()
    }
  }

  suspend fun generateCatchMeUpDigestForUnread(): Result<DigestEntry> {
    val unreadItems = _websites.value.filter { !it.isRead && !it.isArchived }
    if (unreadItems.isEmpty()) {
      return Result.failure(IllegalStateException("No unread websites to summarize!"))
    }
    _isGeneratingDigest.value = true
    return try {
      // Ensure content is scraped for items that are still missing content
      val enrichedItems = unreadItems.map { site ->
        if (site.extractedContent.isBlank() && site.summary.isNullOrBlank()) {
          val scraped = WebPageScraper.scrape(site.url)
          site.copy(
            title = scraped.title.ifBlank { site.title },
            faviconUrl = scraped.faviconUrl.ifBlank { site.faviconUrl },
            extractedContent = scraped.readableContent
          )
        } else {
          site
        }
      }

      val currentSettings = _settings.value
      val summaryResult = aiService.generateCatchMeUpDigest(enrichedItems, currentSettings)
      summaryResult.map { markdown ->
        val digest = DigestEntry(
          id = UUID.randomUUID().toString(),
          createdAt = System.currentTimeMillis(),
          summaryMarkdown = markdown,
          providerName = currentSettings.selectedBackend.displayName,
          sourceWebsites = enrichedItems.map {
            DigestSourceWebsite(
              id = it.id,
              title = it.title,
              url = it.url,
              domain = it.domain,
              faviconUrl = it.faviconUrl
            )
          }
        )
        _digests.update { current ->
          (listOf(digest) + current).sortedByDescending { it.createdAt }
        }
        persistDigests()
        digest
      }
    } finally {
      _isGeneratingDigest.value = false
    }
  }

  /**
   * Generates a listenable podcast episode from the given [websites]: the AI writes a spoken-word
   * script, which is then synthesized to cached audio on device.
   *
   * Returns the new episode's id. The episode is published immediately (status [PodcastStatus.WRITING_SCRIPT])
   * and updated in place as generation progresses, so callers can observe [podcasts].
   */
  fun generatePodcast(websites: List<SavedWebsite>): String? {
    if (websites.isEmpty()) return null
    val episodeId = UUID.randomUUID().toString()
    val now = System.currentTimeMillis()
    val currentSettings = _settings.value
    val workingTitle = websites.firstOrNull()?.let { site ->
      if (websites.size == 1) site.title else "Catch Me Up: ${site.domain} + ${websites.size - 1} more"
    } ?: "Summer Catch Me Up"

    val episode = PodcastEpisode(
      id = episodeId,
      title = workingTitle,
      createdAt = now,
      updatedAt = now,
      scriptMarkdown = "",
      providerName = currentSettings.selectedBackend.displayName,
      voiceName = currentSettings.podcastVoiceName.ifBlank { null },
      status = PodcastStatus.WRITING_SCRIPT,
      sourceWebsites = websites.map {
        DigestSourceWebsite(
          id = it.id,
          title = it.title,
          url = it.url,
          domain = it.domain,
          faviconUrl = it.faviconUrl
        )
      }
    )
    _podcasts.update { (listOf(episode) + it).sortedByDescending { e -> e.createdAt } }
    persistPodcasts()

    scope.launch {
      try {
        val enrichedItems = websites.map { site ->
          if (site.extractedContent.isBlank() && site.summary.isNullOrBlank()) {
            val scraped = WebPageScraper.scrape(site.url)
            site.copy(
              title = scraped.title.ifBlank { site.title },
              faviconUrl = scraped.faviconUrl.ifBlank { site.faviconUrl },
              extractedContent = scraped.readableContent
            )
          } else {
            site
          }
        }

        val scriptResult = aiService.generatePodcastScript(enrichedItems, currentSettings)
        val script = scriptResult.getOrElse { error ->
          updatePodcast(episodeId) {
            it.copy(
              status = PodcastStatus.FAILED,
              error = error.message ?: "Failed to write the podcast script.",
              updatedAt = System.currentTimeMillis()
            )
          }
          return@launch
        }

        updatePodcast(episodeId) {
          it.copy(
            scriptMarkdown = script,
            status = PodcastStatus.SYNTHESIZING,
            updatedAt = System.currentTimeMillis()
          )
        }

        try {
          val files = synthesizer.synthesizeToFiles(
            text = script,
            settings = currentSettings,
            outputDir = podcastAudioDir,
            baseName = episodeId
          )
          updatePodcast(episodeId) {
            it.copy(
              status = PodcastStatus.READY,
              audioFilePaths = files.map { file -> file.absolutePath },
              error = null,
              updatedAt = System.currentTimeMillis()
            )
          }
        } catch (ttsError: Exception) {
          // Keep the script so the episode can still be played back through live text-to-speech.
          updatePodcast(episodeId) {
            it.copy(
              status = PodcastStatus.READY,
              audioFilePaths = emptyList(),
              error = "Audio could not be saved (${ttsError.message ?: "text-to-speech error"}). " +
                "Playing live instead.",
              updatedAt = System.currentTimeMillis()
            )
          }
        }
      } catch (e: Exception) {
        updatePodcast(episodeId) {
          it.copy(
            status = PodcastStatus.FAILED,
            error = e.message ?: "Failed to generate podcast.",
            updatedAt = System.currentTimeMillis()
          )
        }
      }
    }

    return episodeId
  }

  private fun updatePodcast(episodeId: String, transform: (PodcastEpisode) -> PodcastEpisode) {
    _podcasts.update { list ->
      list.map { if (it.id == episodeId) transform(it) else it }
    }
    persistPodcasts()
  }

  fun playPodcast(episodeId: String) {
    val episode = _podcasts.value.firstOrNull { it.id == episodeId } ?: return
    if (episode.scriptMarkdown.isBlank()) return
    player.play(episode, _settings.value)
  }

  suspend fun availablePodcastVoices(): List<TtsVoiceOption> = synthesizer.availableVoices(_settings.value)

  fun pausePodcast() {
    player.pause()
    _podcastPlayback.update { it.copy(isPlaying = false) }
  }

  fun resumePodcast() {
    player.resume()
  }

  fun stopPodcast() {
    player.stop()
    _podcastPlayback.update { PodcastPlaybackState() }
  }

  fun deletePodcast(episodeId: String) {
    if (player.currentEpisodeId == episodeId) {
      player.stop()
      _podcastPlayback.update { PodcastPlaybackState() }
    }
    scope.launch {
      runCatching {
        podcastAudioDir.listFiles { f -> f.name.startsWith("${episodeId}_") }?.forEach { it.delete() }
      }
    }
    _podcasts.update { list -> list.filterNot { it.id == episodeId } }
    persistPodcasts()
  }

  fun toggleReadAndArchive(websiteId: String, archive: Boolean) {
    _websites.update { list ->
      list.map {
        if (it.id == websiteId) {
          it.copy(isArchived = archive, isRead = archive)
        } else {
          it
        }
      }
    }
    persistWebsites()
  }

  fun markWebsitesArchived(ids: Set<String>) {
    _websites.update { list ->
      list.map {
        if (it.id in ids) {
          it.copy(isArchived = true, isRead = true)
        } else {
          it
        }
      }
    }
    persistWebsites()
  }

  fun deleteWebsite(websiteId: String) {
    _websites.update { list -> list.filterNot { it.id == websiteId } }
    persistWebsites()
  }

  fun deleteDigest(digestId: String) {
    _digests.update { list -> list.filterNot { it.id == digestId } }
    persistDigests()
  }

  companion object {
    @Volatile
    private var instance: SummerRepository? = null

    fun getInstance(context: Context): SummerRepository {
      return instance ?: synchronized(this) {
        instance ?: SummerRepository(context.applicationContext).also { instance = it }
      }
    }

    private val URL_REGEX = Regex("""https?://[^\s"'<>]+""", RegexOption.IGNORE_CASE)

    fun extractFirstHttpUrl(rawText: String?): String? {
      if (rawText.isNullOrBlank()) return null
      val match = URL_REGEX.find(rawText.trim())?.value ?: return null
      return match.trimEnd('.', ',', ';', ')', ']', '!')
    }

    fun normalizeUrl(input: String): String? {
      val extracted = extractFirstHttpUrl(input) ?: input.trim().let {
        if (it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true)) {
          it
        } else if (it.contains(".") && !it.contains(" ")) {
          "https://$it"
        } else {
          null
        }
      }
      return extracted?.trim()
    }
  }
}

sealed interface SaveUrlOutcome {
  data class SavedNew(val domain: String) : SaveUrlOutcome
  data class BumpedExisting(val domain: String) : SaveUrlOutcome
  data object InvalidUrl : SaveUrlOutcome
}
