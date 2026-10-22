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
import se.oscarb.summer.data.PodcastEpisode
import se.oscarb.summer.data.PodcastPlaybackState
import se.oscarb.summer.data.SavedWebsite
import se.oscarb.summer.data.SummerRepository

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
  val podcasts: List<PodcastEpisode> = emptyList(),
  val activePodcastSheet: PodcastEpisode? = null,
  val podcastPlayback: PodcastPlaybackState = PodcastPlaybackState(),
  val podcastError: String? = null,
  val settings: AppSettings = AppSettings()
)

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = SummerRepository.getInstance(application)

  private val _activeFilter = MutableStateFlow(LinkFilter.UNREAD)
  private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
  private val _activeDigestSheet = MutableStateFlow<DigestEntry?>(null)
  private val _digestError = MutableStateFlow<String?>(null)
  private val _activePodcastSheet = MutableStateFlow<PodcastEpisode?>(null)
  private val _podcastError = MutableStateFlow<String?>(null)

  // kotlinx.coroutines only provides typed combine overloads up to five flows, so the sheet
  // inputs are folded in two steps.
  private val sheetCoreFlow = combine(
    _activeFilter,
    _expandedIds,
    _activeDigestSheet,
    _digestError
  ) { filter, expanded, digestSheet, digestErr ->
    SheetCore(filter, expanded, digestSheet, digestErr)
  }

  private val sheetsFlow = combine(
    sheetCoreFlow,
    _activePodcastSheet,
    _podcastError
  ) { core, podcastSheet, podcastErr ->
    Sheets(
      filter = core.filter,
      expanded = core.expanded,
      digestSheet = core.digestSheet,
      digestError = core.digestError,
      podcastSheet = podcastSheet,
      podcastError = podcastErr
    )
  }

  private val coreFlow = combine(
    repository.websites,
    repository.digests,
    repository.settings,
    repository.isGeneratingDigest,
    sheetsFlow
  ) { websites, digests, settings, isGeneratingDigest, sheets ->
    CoreState(websites, digests, settings, isGeneratingDigest, sheets)
  }

  val uiState: StateFlow<SummerUiState> = combine(
    coreFlow,
    repository.podcasts,
    repository.podcastPlayback
  ) { core, podcasts, playback ->
    val websites = core.websites
    val sheets = core.sheets
    val filter = sheets.filter
    val filtered = when (filter) {
      LinkFilter.UNREAD -> websites.filter { !it.isArchived }
      LinkFilter.ALL -> websites
      LinkFilter.ARCHIVED -> websites.filter { it.isArchived }
    }
    SummerUiState(
      websites = websites,
      filteredWebsites = filtered,
      unreadCount = websites.count { !it.isArchived },
      activeFilter = filter,
      expandedWebsiteIds = sheets.expanded,
      digests = core.digests,
      activeDigestSheet = sheets.digestSheet,
      isGeneratingDigest = core.isGeneratingDigest,
      digestError = sheets.digestError,
      podcasts = podcasts,
      // Keep the open sheet in sync with the persisted episode as it generates/updates.
      activePodcastSheet = sheets.podcastSheet?.let { open -> podcasts.firstOrNull { it.id == open.id } },
      podcastPlayback = playback,
      podcastError = sheets.podcastError,
      settings = core.settings
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

  fun generatePodcastFromUnread() {
    val unread = repository.websites.value.filter { !it.isRead && !it.isArchived }
    if (unread.isEmpty()) {
      _podcastError.value = "No unread websites to turn into a podcast!"
      return
    }
    val episodeId = repository.generatePodcast(unread)
    if (episodeId == null) {
      _podcastError.value = "Could not start podcast generation."
      return
    }
    // Surface the new episode right away so the user can watch it generate.
    _activePodcastSheet.value = repository.podcasts.value.firstOrNull { it.id == episodeId }
  }

  fun generatePodcastFromWebsites(websites: List<SavedWebsite>) {
    if (websites.isEmpty()) {
      _podcastError.value = "No websites selected for the podcast."
      return
    }
    val episodeId = repository.generatePodcast(websites)
    if (episodeId == null) {
      _podcastError.value = "Could not start podcast generation."
      return
    }
    _activePodcastSheet.value = repository.podcasts.value.firstOrNull { it.id == episodeId }
  }

  fun openPodcastEpisode(episode: PodcastEpisode) {
    _activePodcastSheet.value = episode
  }

  fun dismissPodcastSheet() {
    _activePodcastSheet.value = null
  }

  fun clearPodcastError() {
    _podcastError.value = null
  }

  fun playPodcast(episodeId: String) {
    repository.playPodcast(episodeId)
  }

  fun pausePodcast() {
    repository.pausePodcast()
  }

  fun resumePodcast() {
    repository.resumePodcast()
  }

  fun stopPodcast() {
    repository.stopPodcast()
  }

  fun deletePodcast(episodeId: String) {
    if (_activePodcastSheet.value?.id == episodeId) {
      _activePodcastSheet.value = null
    }
    repository.deletePodcast(episodeId)
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

  private data class CoreState(
    val websites: List<SavedWebsite>,
    val digests: List<DigestEntry>,
    val settings: AppSettings,
    val isGeneratingDigest: Boolean,
    val sheets: Sheets
  )

  private data class SheetCore(
    val filter: LinkFilter,
    val expanded: Set<String>,
    val digestSheet: DigestEntry?,
    val digestError: String?
  )

  private data class Sheets(
    val filter: LinkFilter,
    val expanded: Set<String>,
    val digestSheet: DigestEntry?,
    val digestError: String?,
    val podcastSheet: PodcastEpisode?,
    val podcastError: String?
  )
}
