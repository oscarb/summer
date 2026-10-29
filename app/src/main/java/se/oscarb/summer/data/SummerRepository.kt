package se.oscarb.summer.data

import android.content.Context
import kotlinx.coroutines.CancellationException
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
import se.oscarb.summer.podcast.PodcastForegroundService
import se.oscarb.summer.podcast.PodcastPlaybackController
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
  private val settingsFile = File(appContext.filesDir, "app_settings.json")
  private val podcastsDir = File(appContext.filesDir, "podcasts").apply { mkdirs() }

  private val aiService = AiSummaryService(appContext)
  private val podcastTtsService = PodcastTtsService(appContext)
  private val playbackController = PodcastPlaybackController.getInstance(appContext)

  private val _websites = MutableStateFlow<List<SavedWebsite>>(emptyList())
  val websites: StateFlow<List<SavedWebsite>> = _websites.asStateFlow()

  private val _digests = MutableStateFlow<List<DigestEntry>>(emptyList())
  val digests: StateFlow<List<DigestEntry>> = _digests.asStateFlow()

  private val _settings = MutableStateFlow(AppSettings())
  val settings: StateFlow<AppSettings> = _settings.asStateFlow()

  private val _isGeneratingDigest = MutableStateFlow(false)
  val isGeneratingDigest: StateFlow<Boolean> = _isGeneratingDigest.asStateFlow()

  init {
    loadFromDisk()
  }

  private fun loadFromDisk() {
    runCatching {
      if (settingsFile.exists()) {
        val loaded = json.decodeFromString<AppSettings>(settingsFile.readText())
        val normalizedVoiceA = GeminiCatalog.resolveValidVoice(loaded.geminiHostAVoice, "Puck")
        val normalizedVoiceB = GeminiCatalog.resolveValidVoice(loaded.geminiHostBVoice, "Kore")
        val sanitized = loaded.copy(
          geminiHostAVoice = normalizedVoiceA,
          geminiHostBVoice = normalizedVoiceB
        )
        _settings.value = sanitized
        if (sanitized != loaded) {
          persistSettings()
        }
      }
    }
    runCatching {
      if (websitesFile.exists()) {
        val loaded = json.decodeFromString<List<SavedWebsite>>(websitesFile.readText())
        _websites.value = loaded
          .map {
            val validAudio = it.podcastAudioPath?.takeIf { path -> File(path).exists() }
            it.copy(
              isSummarizing = false,
              isLoadingMetadata = false,
              isGeneratingPodcast = false,
              podcastStatusText = null,
              podcastAudioPath = validAudio
            )
          }
          .sortedByDescending { it.updatedAt }
      }
    }
    runCatching {
      if (digestsFile.exists()) {
        val loaded = json.decodeFromString<List<DigestEntry>>(digestsFile.readText())
        _digests.value = loaded
          .map {
            val validAudio = it.podcastAudioPath?.takeIf { path -> File(path).exists() }
            it.copy(
              isGeneratingPodcast = false,
              podcastStatusText = null,
              podcastAudioPath = validAudio
            )
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
  }

  suspend fun verifyTextBackendAccess(): Result<String> {
    return aiService.verifyTextBackendAccess(_settings.value)
  }

  suspend fun verifyGeminiTtsAccess(): Result<String> {
    return podcastTtsService.verifyGeminiTtsAccess(_settings.value)
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

      // Auto-generate individual article podcast in background if enabled in Settings
      if (_settings.value.autoGenerateArticlePodcastOnSave) {
        val currentItem = _websites.value.firstOrNull { it.id == targetId }
        if (currentItem != null && currentItem.podcastAudioPath.isNullOrBlank()) {
          generateOrPlayWebsitePodcast(
            websiteId = targetId,
            forceRegenerate = false,
            autoPlayWhenReady = false
          )
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
        val digestId = UUID.randomUUID().toString()
        val digest = DigestEntry(
          id = digestId,
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

        if (currentSettings.autoGeneratePodcastOnDigest) {
          generatePodcastForDigest(
            digestId = digestId,
            forceRegenerate = false,
            autoPlayWhenReady = false
          )
        }

        digest
      }
    } finally {
      _isGeneratingDigest.value = false
    }
  }

  /**
   * Generates (or plays if already cached) a 2-host podcast episode with a narrative "red line"
   * across all articles in the specified [digestId]. Runs in the background so the user can
   * switch apps or turn off the screen while it generates.
   */
  fun generatePodcastForDigest(
    digestId: String,
    forceRegenerate: Boolean = false,
    autoPlayWhenReady: Boolean = false
  ) {
    scope.launch {
      val digest = _digests.value.firstOrNull { it.id == digestId } ?: return@launch
      if (digest.isGeneratingPodcast) return@launch

      val existingFile = digest.podcastAudioPath?.let { File(it) }
      if (!forceRegenerate && existingFile != null && existingFile.exists()) {
        if (autoPlayWhenReady) {
          val settings = _settings.value
          playbackController.playEpisode(
            audioFile = existingFile,
            title = "Catch Me Up (${digest.sourceWebsites.size} articles)",
            subtitle = "${settings.hostAName} & ${settings.hostBName} • ${digest.podcastVoiceProvider ?: "Podcast"}",
            digestId = digest.id
          )
        }
        return@launch
      }

      val episodeTitle = "Catch Me Up (${digest.sourceWebsites.size} articles)"
      updateDigestPodcastStatus(
        digestId = digestId,
        isGenerating = true,
        statusText = "Writing 2-host conversation with a red line across ${digest.sourceWebsites.size} articles...",
        error = null
      )
      playbackController.updateBackgroundGenerationStatus(
        isGenerating = true,
        title = episodeTitle,
        statusText = "Step 1/2: Writing 2-host dialogue script..."
      )

      try {
        val currentSettings = _settings.value
        val sourceIds = digest.sourceWebsites.map { it.id }.toSet()
        val matchedSites = _websites.value.filter { it.id in sourceIds }.ifEmpty {
          digest.sourceWebsites.map {
            SavedWebsite(
              id = it.id,
              url = it.url,
              domain = it.domain,
              title = it.title,
              faviconUrl = it.faviconUrl,
              createdAt = digest.createdAt,
              updatedAt = digest.createdAt
            )
          }
        }

        val script = if (!forceRegenerate && !digest.podcastScript.isNullOrBlank()) {
          digest.podcastScript
        } else {
          aiService.generateCatchMeUpPodcastScript(
            websites = matchedSites,
            existingDigestMarkdown = digest.summaryMarkdown,
            settings = currentSettings
          ).getOrThrow()
        }

        // Save the script immediately so the user can view the 2-host transcript right away
        _digests.update { list ->
          list.map {
            if (it.id == digestId) {
              it.copy(
                podcastScript = script,
                podcastStatusText = "Synthesizing 2-host voices (${currentSettings.hostAName} & ${currentSettings.hostBName})..."
              )
            } else {
              it
            }
          }
        }
        persistDigests()

        val outputFile = File(podcastsDir, "digest_${digestId}.wav")
        val audioResult = podcastTtsService.synthesizePodcastScript(
          rawScript = script,
          outputWavFile = outputFile,
          settings = currentSettings,
          onProgress = { progressMsg ->
            updateDigestPodcastStatus(
              digestId = digestId,
              isGenerating = true,
              statusText = progressMsg,
              error = null
            )
            playbackController.updateBackgroundGenerationStatus(
              isGenerating = true,
              title = episodeTitle,
              statusText = progressMsg
            )
          }
        ).getOrThrow()

        _digests.update { list ->
          list.map {
            if (it.id == digestId) {
              it.copy(
                podcastScript = script,
                podcastAudioPath = audioResult.audioFile.absolutePath,
                podcastVoiceProvider = audioResult.providerUsed,
                isGeneratingPodcast = false,
                podcastStatusText = null,
                podcastError = null
              )
            } else {
              it
            }
          }
        }
        persistDigests()

        val subtitle = "${currentSettings.hostAName} & ${currentSettings.hostBName} • ${audioResult.providerUsed}"
        playbackController.updateBackgroundGenerationStatus(isGenerating = false)

        if (autoPlayWhenReady) {
          playbackController.playEpisode(
            audioFile = audioResult.audioFile,
            title = episodeTitle,
            subtitle = subtitle,
            digestId = digestId
          )
        } else {
          PodcastForegroundService.notifyPodcastReady(
            context = appContext,
            title = episodeTitle,
            subtitle = subtitle,
            audioFile = audioResult.audioFile,
            digestId = digestId
          )
        }
      } catch (ce: CancellationException) {
        updateDigestPodcastStatus(
          digestId = digestId,
          isGenerating = false,
          statusText = null,
          error = null
        )
        playbackController.updateBackgroundGenerationStatus(isGenerating = false)
        throw ce
      } catch (e: Exception) {
        updateDigestPodcastStatus(
          digestId = digestId,
          isGenerating = false,
          statusText = null,
          error = e.message ?: "Failed to generate podcast audio."
        )
        playbackController.updateBackgroundGenerationStatus(isGenerating = false)
      }
    }
  }

  /**
   * Generates (or plays if already cached) a natural spoken 2-host mini-podcast for a single
   * saved website. Runs in the background so the user can lock the screen or leave the app.
   */
  fun generateOrPlayWebsitePodcast(
    websiteId: String,
    forceRegenerate: Boolean = false,
    autoPlayWhenReady: Boolean = true
  ) {
    scope.launch {
      val site = _websites.value.firstOrNull { it.id == websiteId } ?: return@launch
      if (site.isGeneratingPodcast) return@launch

      val existingFile = site.podcastAudioPath?.let { File(it) }
      if (!forceRegenerate && existingFile != null && existingFile.exists()) {
        if (autoPlayWhenReady) {
          val settings = _settings.value
          playbackController.playEpisode(
            audioFile = existingFile,
            title = site.title,
            subtitle = "${settings.hostAName} & ${settings.hostBName} • ${site.domain}",
            websiteId = site.id
          )
        }
        return@launch
      }

      updateWebsitePodcastStatus(
        websiteId = websiteId,
        isGenerating = true,
        statusText = "Writing natural spoken script...",
        error = null
      )
      playbackController.updateBackgroundGenerationStatus(
        isGenerating = true,
        title = site.title,
        statusText = "Writing natural spoken summary..."
      )

      try {
        val preparedSite = if (site.extractedContent.isBlank()) {
          val scraped = WebPageScraper.scrape(site.url)
          val updated = site.copy(
            title = scraped.title.ifBlank { site.title },
            faviconUrl = scraped.faviconUrl.ifBlank { site.faviconUrl },
            extractedContent = scraped.readableContent
          )
          _websites.update { list ->
            list.map { if (it.id == websiteId) updated.copy(isGeneratingPodcast = true) else it }
          }
          updated
        } else {
          site
        }

        val currentSettings = _settings.value
        val script = if (!forceRegenerate && !preparedSite.podcastScript.isNullOrBlank()) {
          preparedSite.podcastScript
        } else {
          aiService.generateSingleArticlePodcastScript(preparedSite, currentSettings).getOrThrow()
        }

        _websites.update { list ->
          list.map {
            if (it.id == websiteId) {
              it.copy(
                podcastScript = script,
                podcastStatusText = "Synthesizing spoken audio..."
              )
            } else {
              it
            }
          }
        }
        persistWebsites()

        val outputFile = File(podcastsDir, "website_${websiteId}.wav")
        val audioResult = podcastTtsService.synthesizePodcastScript(
          rawScript = script,
          outputWavFile = outputFile,
          settings = currentSettings,
          onProgress = { progressMsg ->
            updateWebsitePodcastStatus(
              websiteId = websiteId,
              isGenerating = true,
              statusText = progressMsg,
              error = null
            )
            playbackController.updateBackgroundGenerationStatus(
              isGenerating = true,
              title = preparedSite.title,
              statusText = progressMsg
            )
          }
        ).getOrThrow()

        _websites.update { list ->
          list.map {
            if (it.id == websiteId) {
              it.copy(
                podcastScript = script,
                podcastAudioPath = audioResult.audioFile.absolutePath,
                podcastVoiceProvider = audioResult.providerUsed,
                isGeneratingPodcast = false,
                podcastStatusText = null,
                podcastError = null
              )
            } else {
              it
            }
          }
        }
        persistWebsites()

        val subtitle = "${currentSettings.hostAName} & ${currentSettings.hostBName} • ${preparedSite.domain}"
        playbackController.updateBackgroundGenerationStatus(isGenerating = false)

        if (autoPlayWhenReady) {
          playbackController.playEpisode(
            audioFile = audioResult.audioFile,
            title = preparedSite.title,
            subtitle = subtitle,
            websiteId = websiteId
          )
        } else {
          PodcastForegroundService.notifyPodcastReady(
            context = appContext,
            title = preparedSite.title,
            subtitle = subtitle,
            audioFile = audioResult.audioFile,
            websiteId = websiteId
          )
        }
      } catch (ce: CancellationException) {
        updateWebsitePodcastStatus(
          websiteId = websiteId,
          isGenerating = false,
          statusText = null,
          error = null
        )
        playbackController.updateBackgroundGenerationStatus(isGenerating = false)
        throw ce
      } catch (e: Exception) {
        updateWebsitePodcastStatus(
          websiteId = websiteId,
          isGenerating = false,
          statusText = null,
          error = e.message ?: "Failed to generate spoken audio."
        )
        playbackController.updateBackgroundGenerationStatus(isGenerating = false)
      }
    }
  }

  private fun updateDigestPodcastStatus(
    digestId: String,
    isGenerating: Boolean,
    statusText: String?,
    error: String?
  ) {
    _digests.update { list ->
      list.map {
        if (it.id == digestId) {
          it.copy(
            isGeneratingPodcast = isGenerating,
            podcastStatusText = statusText,
            podcastError = error
          )
        } else {
          it
        }
      }
    }
  }

  private fun updateWebsitePodcastStatus(
    websiteId: String,
    isGenerating: Boolean,
    statusText: String?,
    error: String?
  ) {
    _websites.update { list ->
      list.map {
        if (it.id == websiteId) {
          it.copy(
            isGeneratingPodcast = isGenerating,
            podcastStatusText = statusText,
            podcastError = error
          )
        } else {
          it
        }
      }
    }
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
    val target = _websites.value.firstOrNull { it.id == websiteId }
    target?.podcastAudioPath?.let { path ->
      runCatching { File(path).delete() }
    }
    if (playbackController.state.value.activeWebsiteId == websiteId) {
      playbackController.stop()
    }
    _websites.update { list -> list.filterNot { it.id == websiteId } }
    persistWebsites()
  }

  fun deleteDigest(digestId: String) {
    val target = _digests.value.firstOrNull { it.id == digestId }
    target?.podcastAudioPath?.let { path ->
      runCatching { File(path).delete() }
    }
    if (playbackController.state.value.activeDigestId == digestId) {
      playbackController.stop()
    }
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
