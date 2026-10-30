package se.oscarb.summer.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import se.oscarb.summer.podcast.PodcastForegroundService
import se.oscarb.summer.podcast.PodcastPlaybackController
import se.oscarb.summer.util.WavAudioUtils
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

  private val websitesPersistMutex = Mutex()
  private val digestsPersistMutex = Mutex()
  private val settingsPersistMutex = Mutex()

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

  private val _openAndPlayDigestEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
  val openAndPlayDigestEvents: SharedFlow<String> = _openAndPlayDigestEvents.asSharedFlow()

  private var quietWindowJob: Job? = null
  private val quietWindowLock = Any()

  init {
    loadFromDisk()
    backfillMissingThumbnailsForUnread()
  }

  private fun loadFromDisk() {
    runCatching {
      if (settingsFile.exists()) {
        val loaded = json.decodeFromString<AppSettings>(settingsFile.readText())
        val normalizedVoiceA = GeminiCatalog.resolveValidVoice(loaded.geminiHostAVoice, "Puck")
        val normalizedVoiceB = GeminiCatalog.resolveValidVoice(loaded.geminiHostBVoice, "Kore")
        val normalizedVoiceC = GeminiCatalog.resolveValidVoice(loaded.geminiHostCVoice, "Aoede")
        val sanitized = loaded.copy(
          podcastHostCount = loaded.podcastHostCount.coerceIn(2, 3),
          autoPodcastQuietMinutes = loaded.autoPodcastQuietMinutes.coerceIn(1, 10),
          autoPodcastMinLinks = loaded.autoPodcastMinLinks.coerceIn(2, 10),
          geminiHostAVoice = normalizedVoiceA,
          geminiHostBVoice = normalizedVoiceB,
          geminiHostCVoice = normalizedVoiceC
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
          .sortedByDescending { it.createdAt }
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

  private fun backfillMissingThumbnailsForUnread() {
    scope.launch {
      val missing = _websites.value
        .filter { !it.isArchived && it.thumbnailUrl.isBlank() }
        .take(15)
      if (missing.isEmpty()) return@launch

      var anyUpdated = false
      for (site in missing) {
        val scraped = WebPageScraper.scrape(site.url)
        if (scraped.thumbnailUrl.isNotBlank() || (site.extractedContent.isBlank() && scraped.readableContent.isNotBlank())) {
          anyUpdated = true
          _websites.update { list ->
            list.map { item ->
              if (item.id == site.id) {
                item.copy(
                  title = scraped.title.ifBlank { item.title },
                  faviconUrl = scraped.faviconUrl.ifBlank { item.faviconUrl },
                  thumbnailUrl = scraped.thumbnailUrl.ifBlank { item.thumbnailUrl },
                  extractedContent = item.extractedContent.ifBlank { scraped.readableContent }
                )
              } else {
                item
              }
            }
          }
        }
      }
      if (anyUpdated) {
        persistWebsites()
      }
    }
  }

  private fun persistWebsites() {
    scope.launch {
      websitesPersistMutex.withLock {
        runCatching {
          val encoded = json.encodeToString(_websites.value).toByteArray(Charsets.UTF_8)
          WavAudioUtils.writeBytesAtomically(websitesFile, encoded)
        }
      }
    }
  }

  private fun persistDigests() {
    scope.launch {
      digestsPersistMutex.withLock {
        runCatching {
          val encoded = json.encodeToString(_digests.value).toByteArray(Charsets.UTF_8)
          WavAudioUtils.writeBytesAtomically(digestsFile, encoded)
        }
      }
    }
  }

  private fun persistSettings() {
    scope.launch {
      settingsPersistMutex.withLock {
        runCatching {
          val encoded = json.encodeToString(_settings.value).toByteArray(Charsets.UTF_8)
          WavAudioUtils.writeBytesAtomically(settingsFile, encoded)
        }
      }
    }
  }

  fun updateSettings(transform: (AppSettings) -> AppSettings) {
    _settings.update(transform)
    persistSettings()
    if (!_settings.value.autoPodcastAfterQuietWindow) {
      cancelQuietWindowCountdown()
    }
  }

  suspend fun verifyTextBackendAccess(): Result<String> {
    return aiService.verifyTextBackendAccess(_settings.value)
  }

  suspend fun verifyGeminiTtsAccess(): Result<String> {
    return podcastTtsService.verifyGeminiTtsAccess(_settings.value)
  }

  fun requestOpenAndPlayDigest(digestId: String) {
    val digest = _digests.value.firstOrNull { it.id == digestId }
    if (digest != null) {
      val audioFile = digest.podcastAudioPath?.let { File(it) }
      if (audioFile != null && audioFile.exists()) {
        val hostsLabel = PodcastCastCatalog.formatHostList(digest.hostNames, digest.language)
        playbackController.playEpisode(
          audioFile = audioFile,
          title = "Catch Me Up (${digest.sourceWebsites.size} articles)",
          subtitle = "$hostsLabel • ${digest.podcastVoiceProvider ?: "Podcast"}",
          digestId = digest.id
        )
      }
      _openAndPlayDigestEvents.tryEmit(digestId)
    }
  }

  /**
   * Saves a URL (or unarchives + updates session timestamp if already present).
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
          isLoadingMetadata = existing.extractedContent.isBlank() || existing.thumbnailUrl.isBlank()
        )
        (listOf(bumped) + currentList.filterNot { it.id == existing.id })
          .sortedByDescending { it.createdAt }
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
        (listOf(newEntry) + currentList).sortedByDescending { it.createdAt }
      }
    }
    persistWebsites()

    // Schedule / reset quiet-window auto-podcast countdown synchronously while caller Activity
    // (such as SaveLinkTrampolineActivity or MainActivity) is in the foreground!
    scheduleOrResetQuietWindowIfEligible()

    // Background scrape title, favicon, thumbnail, and readable text
    scope.launch {
      val scraped = WebPageScraper.scrape(normalized)
      _websites.update { list ->
        list.map { item ->
          if (item.id == targetId) {
            item.copy(
              title = scraped.title.ifBlank { item.title },
              faviconUrl = scraped.faviconUrl.ifBlank { item.faviconUrl },
              thumbnailUrl = scraped.thumbnailUrl.ifBlank { item.thumbnailUrl },
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

      // Auto-summarize text summary if enabled in Settings
      if (_settings.value.autoSummarizeOnSave) {
        val currentItem = _websites.value.firstOrNull { it.id == targetId }
        if (currentItem != null && currentItem.summary.isNullOrBlank()) {
          summarizeWebsite(targetId)
        }
      }
    }

    return if (isDuplicate) {
      SaveUrlOutcome.BumpedExisting(domain, normalized)
    } else {
      SaveUrlOutcome.SavedNew(domain, normalized)
    }
  }

  private fun scheduleOrResetQuietWindowIfEligible() {
    val currentSettings = _settings.value
    if (!currentSettings.autoPodcastAfterQuietWindow) return

    val unread = _websites.value.filter { !it.isRead && !it.isArchived }
    val sessionCluster = selectRecentSessionCluster(
      unreadItems = unread,
      maxGapMinutes = currentSettings.autoPodcastSessionGapMinutes
    )
    if (sessionCluster.size < currentSettings.autoPodcastMinLinks.coerceIn(2, 10)) {
      return
    }

    val quietDurationMs = currentSettings.autoPodcastQuietMinutes.coerceIn(1, 10) * 60_000L
    val deadlineMs = System.currentTimeMillis() + quietDurationMs

    synchronized(quietWindowLock) {
      quietWindowJob?.cancel()
      // Update foreground state immediately while caller Activity is foregrounded (Android 14+ safe)
      playbackController.setQuietWindowCountdown(
        isWaiting = true,
        deadlineMs = deadlineMs,
        linkCount = sessionCluster.size
      )

      quietWindowJob = scope.launch {
        try {
          delay(quietDurationMs)
          playbackController.setQuietWindowCountdown(isWaiting = false)
          runQuietWindowSessionPodcast()
        } catch (ce: CancellationException) {
          throw ce
        }
      }
    }
  }

  fun triggerQuietWindowPodcastNow() {
    synchronized(quietWindowLock) {
      quietWindowJob?.cancel()
      quietWindowJob = null
    }
    playbackController.setQuietWindowCountdown(isWaiting = false)
    scope.launch {
      runQuietWindowSessionPodcast()
    }
  }

  fun cancelQuietWindowCountdown() {
    synchronized(quietWindowLock) {
      quietWindowJob?.cancel()
      quietWindowJob = null
    }
    playbackController.setQuietWindowCountdown(isWaiting = false)
  }

  private suspend fun runQuietWindowSessionPodcast() {
    val currentSettings = _settings.value
    val unread = _websites.value.filter { !it.isRead && !it.isArchived }
    val cluster = selectRecentSessionCluster(
      unreadItems = unread,
      maxGapMinutes = currentSettings.autoPodcastSessionGapMinutes
    )
    if (cluster.isEmpty()) return

    generateCatchMeUpDigestForWebsites(
      targetWebsites = cluster,
      autoPlayWhenReady = false,
      autoArchiveOnPodcastSuccess = true
    )
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

      val preparedSite = if (site.extractedContent.isBlank()) {
        val scraped = WebPageScraper.scrape(site.url)
        val updated = site.copy(
          title = scraped.title.ifBlank { site.title },
          faviconUrl = scraped.faviconUrl.ifBlank { site.faviconUrl },
          thumbnailUrl = scraped.thumbnailUrl.ifBlank { site.thumbnailUrl },
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

  /**
   * Creates a podcast-only "Catch Me Up" digest across all unread websites (skipping any
   * separate written-briefing LLM call) and immediately starts podcast script + audio generation.
   */
  suspend fun generateCatchMeUpDigestForUnread(): Result<DigestEntry> {
    cancelQuietWindowCountdown()
    val unreadItems = _websites.value.filter { !it.isRead && !it.isArchived }
    return generateCatchMeUpDigestForWebsites(
      targetWebsites = unreadItems,
      autoPlayWhenReady = false,
      autoArchiveOnPodcastSuccess = false
    )
  }

  private suspend fun generateCatchMeUpDigestForWebsites(
    targetWebsites: List<SavedWebsite>,
    autoPlayWhenReady: Boolean,
    autoArchiveOnPodcastSuccess: Boolean
  ): Result<DigestEntry> {
    if (targetWebsites.isEmpty()) {
      return Result.failure(IllegalStateException("No unread websites to create a podcast from!"))
    }
    _isGeneratingDigest.value = true
    return try {
      val currentSettings = _settings.value
      val episodeCast = PodcastCastCatalog.selectEpisodeCast(currentSettings)
      val hostNames = episodeCast.map { it.name }
      val digestId = UUID.randomUUID().toString()

      val digest = DigestEntry(
        id = digestId,
        createdAt = System.currentTimeMillis(),
        summaryMarkdown = "",
        providerName = currentSettings.selectedBackend.displayName,
        hostNames = hostNames,
        language = currentSettings.podcastLanguage,
        isGeneratingPodcast = true,
        podcastStatusText = "Preparing ${hostNames.size}-host podcast (${PodcastCastCatalog.formatHostList(hostNames, currentSettings.podcastLanguage)})...",
        sourceWebsites = targetWebsites.map {
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

      generatePodcastForDigestInternal(
        digestId = digestId,
        preselectedCast = episodeCast,
        forceRegenerate = true,
        autoPlayWhenReady = autoPlayWhenReady,
        autoArchiveOnSuccess = autoArchiveOnPodcastSuccess
      )

      Result.success(digest)
    } finally {
      _isGeneratingDigest.value = false
    }
  }

  /**
   * Generates (or plays if already cached) a 2-host or 3-host podcast episode with a narrative
   * "red line" across all articles in [digestId].
   */
  fun generatePodcastForDigest(
    digestId: String,
    forceRegenerate: Boolean = false,
    autoPlayWhenReady: Boolean = false
  ) {
    generatePodcastForDigestInternal(
      digestId = digestId,
      preselectedCast = null,
      forceRegenerate = forceRegenerate,
      autoPlayWhenReady = autoPlayWhenReady,
      autoArchiveOnSuccess = false
    )
  }

  private fun generatePodcastForDigestInternal(
    digestId: String,
    preselectedCast: List<AssignedHostVoice>?,
    forceRegenerate: Boolean,
    autoPlayWhenReady: Boolean,
    autoArchiveOnSuccess: Boolean
  ) {
    scope.launch {
      val digest = _digests.value.firstOrNull { it.id == digestId } ?: return@launch
      if (digest.isGeneratingPodcast && preselectedCast == null) return@launch

      val existingFile = digest.podcastAudioPath?.let { File(it) }
      if (!forceRegenerate && existingFile != null && existingFile.exists()) {
        if (autoPlayWhenReady) {
          val hostsLabel = PodcastCastCatalog.formatHostList(
            digest.hostNames.ifEmpty { listOf(_settings.value.hostAName, _settings.value.hostBName) },
            digest.language
          )
          playbackController.playEpisode(
            audioFile = existingFile,
            title = "Catch Me Up (${digest.sourceWebsites.size} articles)",
            subtitle = "$hostsLabel • ${digest.podcastVoiceProvider ?: "Podcast"}",
            digestId = digest.id
          )
        }
        return@launch
      }

      if (forceRegenerate && playbackController.state.value.activeDigestId == digestId) {
        playbackController.stop()
      }

      val currentSettings = _settings.value
      val episodeCast = preselectedCast ?: PodcastCastCatalog.selectEpisodeCast(currentSettings)
      val episodeLanguage = currentSettings.podcastLanguage
      val hostNames = episodeCast.map { it.name }
      val hostsDisplay = PodcastCastCatalog.formatHostList(hostNames, episodeLanguage)

      val jobKey = "digest:$digestId"
      val episodeTitle = "Catch Me Up (${digest.sourceWebsites.size} articles)"
      updateDigestPodcastStatus(
        digestId = digestId,
        isGenerating = true,
        statusText = "Writing ${hostNames.size}-host dialogue ($hostsDisplay)...",
        error = null,
        hostNames = hostNames,
        language = episodeLanguage
      )
      playbackController.setGenerationProgress(
        jobKey = jobKey,
        title = episodeTitle,
        statusText = "Step 1/2: Writing ${hostNames.size}-host script ($hostsDisplay)..."
      )

      try {
        val sourceIds = digest.sourceWebsites.map { it.id }.toSet()
        val rawSites = _websites.value.filter { it.id in sourceIds }.ifEmpty {
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

        // Ensure content is scraped for items still missing body text
        val enrichedSites = rawSites.map { site ->
          if (site.extractedContent.isBlank() && site.summary.isNullOrBlank()) {
            val scraped = WebPageScraper.scrape(site.url)
            site.copy(
              title = scraped.title.ifBlank { site.title },
              faviconUrl = scraped.faviconUrl.ifBlank { site.faviconUrl },
              thumbnailUrl = scraped.thumbnailUrl.ifBlank { site.thumbnailUrl },
              extractedContent = scraped.readableContent
            )
          } else {
            site
          }
        }

        val script = if (!forceRegenerate && !digest.podcastScript.isNullOrBlank()) {
          digest.podcastScript
        } else {
          aiService.generateCatchMeUpPodcastScript(
            websites = enrichedSites,
            settings = currentSettings,
            hostNames = hostNames,
            language = episodeLanguage
          ).getOrThrow()
        }

        _digests.update { list ->
          list.map {
            if (it.id == digestId) {
              it.copy(
                hostNames = hostNames,
                language = episodeLanguage,
                podcastScript = script,
                podcastStatusText = "Synthesizing voices ($hostsDisplay)..."
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
          castOverride = episodeCast,
          languageOverride = episodeLanguage,
          onProgress = { progressMsg ->
            updateDigestPodcastStatus(
              digestId = digestId,
              isGenerating = true,
              statusText = progressMsg,
              error = null
            )
            playbackController.setGenerationProgress(
              jobKey = jobKey,
              title = episodeTitle,
              statusText = progressMsg
            )
          }
        ).getOrThrow()

        _digests.update { list ->
          list.map {
            if (it.id == digestId) {
              it.copy(
                hostNames = hostNames,
                language = episodeLanguage,
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

        if (autoArchiveOnSuccess) {
          markWebsitesArchived(sourceIds)
        }

        val subtitle = "$hostsDisplay • ${audioResult.providerUsed}"
        playbackController.clearGenerationProgress(jobKey)

        if (autoPlayWhenReady) {
          playbackController.playEpisode(
            audioFile = audioResult.audioFile,
            title = episodeTitle,
            subtitle = subtitle,
            digestId = digestId,
            forceReload = true
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
        playbackController.clearGenerationProgress(jobKey)
        throw ce
      } catch (e: Exception) {
        updateDigestPodcastStatus(
          digestId = digestId,
          isGenerating = false,
          statusText = null,
          error = e.message ?: "Failed to generate podcast audio."
        )
        playbackController.clearGenerationProgress(jobKey)
      }
    }
  }

  private fun updateDigestPodcastStatus(
    digestId: String,
    isGenerating: Boolean,
    statusText: String?,
    error: String?,
    hostNames: List<String>? = null,
    language: PodcastLanguage? = null
  ) {
    _digests.update { list ->
      list.map {
        if (it.id == digestId) {
          it.copy(
            hostNames = hostNames ?: it.hostNames,
            language = language ?: it.language,
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

    /**
     * Groups the most recent browsing session burst of unread websites by walking backwards
     * from the latest saved URL (`updatedAt`) as long as consecutive saved links are within
     * [maxGapMinutes] of each other. Returns links ordered oldest-to-newest within that burst.
     */
    internal fun selectRecentSessionCluster(
      unreadItems: List<SavedWebsite>,
      maxGapMinutes: Int = 30
    ): List<SavedWebsite> {
      if (unreadItems.isEmpty()) return emptyList()
      val maxGapMs = maxGapMinutes.coerceAtLeast(1) * 60_000L
      val sortedDesc = unreadItems.sortedByDescending { it.updatedAt }
      val cluster = mutableListOf(sortedDesc[0])
      for (i in 1 until sortedDesc.size) {
        val prevTime = cluster.last().updatedAt
        val candidate = sortedDesc[i]
        if (prevTime - candidate.updatedAt <= maxGapMs) {
          cluster.add(candidate)
        } else {
          break
        }
      }
      return cluster.reversed()
    }
  }
}

sealed interface SaveUrlOutcome {
  data class SavedNew(val domain: String, val url: String = "") : SaveUrlOutcome
  data class BumpedExisting(val domain: String, val url: String = "") : SaveUrlOutcome
  data object InvalidUrl : SaveUrlOutcome
}
