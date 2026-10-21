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
  val settings: AppSettings = AppSettings()
)

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = SummerRepository.getInstance(application)

  private val _activeFilter = MutableStateFlow(LinkFilter.UNREAD)
  private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
  private val _activeDigestSheet = MutableStateFlow<DigestEntry?>(null)
  private val _digestError = MutableStateFlow<String?>(null)

  val uiState: StateFlow<SummerUiState> = combine(
    repository.websites,
    repository.digests,
    repository.settings,
    repository.isGeneratingDigest,
    combine(_activeFilter, _expandedIds, _activeDigestSheet, _digestError) { filter, expanded, sheet, err ->
      Quad(filter, expanded, sheet, err)
    }
  ) { websites, digests, settings, isGeneratingDigest, (filter, expanded, sheet, err) ->
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
      expandedWebsiteIds = expanded,
      digests = digests,
      activeDigestSheet = sheet,
      isGeneratingDigest = isGeneratingDigest,
      digestError = err,
      settings = settings
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

  private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
