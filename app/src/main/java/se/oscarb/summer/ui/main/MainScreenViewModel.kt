package se.oscarb.summer.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import se.oscarb.summer.data.AppSettings
import se.oscarb.summer.data.DigestEntry
import se.oscarb.summer.data.LinkFilter
import se.oscarb.summer.data.SavedWebsite
import se.oscarb.summer.data.SummerRepository
import se.oscarb.summer.podcast.PodcastPlaybackController
import se.oscarb.summer.podcast.PodcastPlaybackState

data class SummerUiState(
  val websites: List<SavedWebsite> = emptyList(),
  val filteredWebsites: List<SavedWebsite> = emptyList(),
  val unreadCount: Int = 0,
  val activeFilter: LinkFilter = LinkFilter.UNREAD,
  val expandedWebsiteIds: Set<String> = emptySet(),
  val digests: List<DigestEntry> = emptyList(),
  val activeDigestSheet: DigestEntry? = null,
  val isGeneratingDigest: Boolean = false,
  val digestError: String? = null,
  val settings: AppSettings = AppSettings(),
  val podcastPlayback: PodcastPlaybackState = PodcastPlaybackState()
)

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = SummerRepository.getInstance(application)
  private val playbackController = PodcastPlaybackController.getInstance(application)

  private val _activeFilter = MutableStateFlow(LinkFilter.UNREAD)
  private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
  private val _activeDigestSheet = MutableStateFlow<DigestEntry?>(null)
  private val _digestError = MutableStateFlow<String?>(null)

  val uiState: StateFlow<SummerUiState> = combine(
    repository.websites,
    repository.digests,
    repository.settings,
    repository.isGeneratingDigest,
    combine(
      _activeFilter,
      _expandedIds,
      _activeDigestSheet,
      _digestError,
      playbackController.state
    ) { filter, expanded, sheet, err, playback ->
      Quint(filter, expanded, sheet, err, playback)
    }
  ) { websites, digests, settings, isGeneratingDigest, (filter, expanded, sheet, err, playback) ->
    val filtered = when (filter) {
      LinkFilter.UNREAD -> websites.filter { !it.isArchived }
      LinkFilter.ALL -> websites
      LinkFilter.ARCHIVED -> websites.filter { it.isArchived }
    }
    val liveSheet = sheet?.let { active ->
      digests.firstOrNull { it.id == active.id } ?: active
    }
    SummerUiState(
      websites = websites,
      filteredWebsites = filtered,
      unreadCount = websites.count { !it.isArchived },
      activeFilter = filter,
      expandedWebsiteIds = expanded,
      digests = digests,
      activeDigestSheet = liveSheet,
      isGeneratingDigest = isGeneratingDigest,
      digestError = err,
      settings = settings,
      podcastPlayback = playback
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = SummerUiState()
  )

  fun setFilter(filter: LinkFilter) {
    _activeFilter.value = filter
  }

  fun onWebsiteCardClicked(website: SavedWebsite) {
    val currentlyExpanded = website.id in _expandedIds.value
    if (currentlyExpanded) {
      _expandedIds.value = _expandedIds.value - website.id
    } else {
      _expandedIds.value = _expandedIds.value + website.id
      if (website.summary.isNullOrBlank() && !website.isSummarizing) {
        repository.summarizeWebsite(website.id)
      }
    }
  }

  fun resummarizeWebsite(websiteId: String) {
    _expandedIds.value = _expandedIds.value + websiteId
    repository.summarizeWebsite(websiteId)
  }

  fun addUrlManually(url: String) {
    repository.saveOrBumpUrl(url)
  }

  fun toggleArchive(website: SavedWebsite) {
    repository.toggleReadAndArchive(website.id, archive = !website.isArchived)
  }

  fun deleteWebsite(websiteId: String) {
    _expandedIds.value = _expandedIds.value - websiteId
    repository.deleteWebsite(websiteId)
  }

  fun triggerCatchMeUpDigest() {
    if (repository.isGeneratingDigest.value) return
    _digestError.value = null
    viewModelScope.launch {
      val result = repository.generateCatchMeUpDigestForUnread()
      result.fold(
        onSuccess = { digest ->
          _activeDigestSheet.value = digest
        },
        onFailure = { error ->
          _digestError.value = error.message ?: "Failed to generate Catch Me Up digest."
        }
      )
    }
  }

  fun openSavedDigest(digest: DigestEntry) {
    _activeDigestSheet.value = digest
  }

  fun dismissDigestSheet() {
    _activeDigestSheet.value = null
  }

  fun clearDigestError() {
    _digestError.value = null
  }

  fun markDigestSourcesArchived(digest: DigestEntry) {
    val ids = digest.sourceWebsites.map { it.id }.toSet()
    repository.markWebsitesArchived(ids)
    _activeDigestSheet.value = null
  }

  fun deleteDigest(digestId: String) {
    if (_activeDigestSheet.value?.id == digestId) {
      _activeDigestSheet.value = null
    }
    repository.deleteDigest(digestId)
  }

  fun updateSettings(transform: (AppSettings) -> AppSettings) {
    repository.updateSettings(transform)
  }

  fun playOrGenerateDigestPodcast(digest: DigestEntry, forceRegenerate: Boolean = false) {
    val pb = playbackController.state.value
    if (!forceRegenerate && pb.isActive && pb.activeDigestId == digest.id) {
      playbackController.togglePlayPause()
      return
    }
    repository.generatePodcastForDigest(
      digestId = digest.id,
      forceRegenerate = forceRegenerate,
      autoPlayWhenReady = true
    )
  }

  fun generateDigestPodcastInBackground(digest: DigestEntry) {
    repository.generatePodcastForDigest(
      digestId = digest.id,
      forceRegenerate = true,
      autoPlayWhenReady = false
    )
  }

  fun playOrGenerateWebsitePodcast(website: SavedWebsite, forceRegenerate: Boolean = false) {
    val pb = playbackController.state.value
    if (!forceRegenerate && pb.isActive && pb.activeWebsiteId == website.id) {
      playbackController.togglePlayPause()
      return
    }
    _expandedIds.value = _expandedIds.value + website.id
    repository.generateOrPlayWebsitePodcast(
      websiteId = website.id,
      forceRegenerate = forceRegenerate,
      autoPlayWhenReady = true
    )
  }

  fun togglePodcastPlayPause() {
    playbackController.togglePlayPause()
  }

  fun seekPodcastTo(positionMs: Long) {
    playbackController.seekTo(positionMs)
  }

  fun skipPodcastBy(deltaMs: Long) {
    playbackController.skipBy(deltaMs)
  }

  fun stopPodcastPlayback() {
    playbackController.stop()
  }

  fun openActivePodcastDetails() {
    val pb = playbackController.state.value
    val digestId = pb.activeDigestId
    if (digestId != null) {
      val digest = repository.digests.value.firstOrNull { it.id == digestId }
      if (digest != null) {
        _activeDigestSheet.value = digest
      }
    } else if (pb.activeWebsiteId != null) {
      _expandedIds.value = _expandedIds.value + pb.activeWebsiteId
    }
  }

  private data class Quint<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
  )
}
