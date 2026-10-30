package se.oscarb.summer.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import se.oscarb.summer.data.AiBackend
import se.oscarb.summer.data.AppSettings
import se.oscarb.summer.data.DigestEntry
import se.oscarb.summer.data.GeminiCatalog
import se.oscarb.summer.data.LinkFilter
import se.oscarb.summer.data.OnDeviceTtsCatalog
import se.oscarb.summer.data.PodcastCastCatalog
import se.oscarb.summer.data.PodcastLanguage
import se.oscarb.summer.data.PodcastTtsProvider
import se.oscarb.summer.data.SavedWebsite
import se.oscarb.summer.data.TtsVoiceOption
import se.oscarb.summer.podcast.PodcastPlaybackState
import se.oscarb.summer.ui.components.MarkdownText
import se.oscarb.summer.util.ExternalBrowserLauncher
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit = {},
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel()
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  var showAddLinkDialog by rememberSaveable { mutableStateOf(false) }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { }
  )

  fun ensureNotificationPermissionForPodcast() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        runCatching {
          notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
      }
    }
  }

  LaunchedEffect(state.digestError) {
    state.digestError?.let { errorMsg ->
      snackbarHostState.showSnackbar(errorMsg)
      viewModel.clearDigestError()
    }
  }

  LaunchedEffect(state.podcastPlayback.playbackError) {
    state.podcastPlayback.playbackError?.let { errorMsg ->
      snackbarHostState.showSnackbar(errorMsg)
      viewModel.clearPlaybackError()
    }
  }

  LaunchedEffect(state.postSaveEvent) {
    if (state.postSaveEvent != null) {
      delay(6_500L)
      viewModel.clearPostSaveEvent()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "☀️ Summer",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = when (selectedTab) {
                0 -> "${state.unreadCount} unread website(s) • ${state.settings.selectedBackend.displayName}"
                1 -> "${state.digests.size} saved podcast digest(s)"
                else -> "AI Providers, Podcast Voices & Browser Setup"
              },
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        actions = {
          if (selectedTab == 0) {
            IconButton(onClick = { showAddLinkDialog = true }) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add URL manually"
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    floatingActionButton = {
      if (selectedTab == 0 && state.unreadCount > 0) {
        ExtendedFloatingActionButton(
          onClick = {
            ensureNotificationPermissionForPodcast()
            viewModel.triggerCatchMeUpDigest()
          },
          icon = {
            if (state.isGeneratingDigest) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
              )
            } else {
              Icon(Icons.Default.Podcasts, contentDescription = null)
            }
          },
          text = {
            Text(
              if (state.isGeneratingDigest) {
                "Creating Podcast..."
              } else {
                "Catch Me Up (${state.unreadCount})"
              }
            )
          }
        )
      }
    },
    bottomBar = {
      Column {
        AnimatedVisibility(
          visible = state.postSaveEvent != null,
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut()
        ) {
          state.postSaveEvent?.let { postSave ->
            InAppPostSaveActionBar(
              event = postSave,
              onOpenLink = {
                viewModel.clearPostSaveEvent()
                ExternalBrowserLauncher.openInExternalBrowser(
                  context = context,
                  url = postSave.url,
                  preferredBrowserPackage = state.settings.preferredBrowserPackage,
                  useCustomTabs = state.settings.useCustomTabs
                )
              },
              onGeneratePodcast = {
                ensureNotificationPermissionForPodcast()
                viewModel.triggerBackgroundPodcastFromBanner()
              },
              onDismiss = viewModel::clearPostSaveEvent
            )
          }
        }
        if (
          state.podcastPlayback.isActive ||
          state.podcastPlayback.isGeneratingInBackground ||
          state.podcastPlayback.isQuietWindowWaiting
        ) {
          PodcastMiniPlayerBar(
            playback = state.podcastPlayback,
            onTogglePlayPause = viewModel::togglePodcastPlayPause,
            onSkipBack15 = { viewModel.skipPodcastBy(-15_000L) },
            onSkipForward15 = { viewModel.skipPodcastBy(15_000L) },
            onSeekTo = viewModel::seekPodcastTo,
            onClose = viewModel::stopPodcastPlayback,
            onOpenDetails = viewModel::openActivePodcastDetails,
            onTriggerQuietWindowNow = {
              ensureNotificationPermissionForPodcast()
              viewModel.triggerQuietWindowPodcastNow()
            },
            onCancelQuietWindow = viewModel::cancelQuietWindowCountdown
          )
        }
        NavigationBar {
          NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            icon = { Icon(Icons.Default.Link, contentDescription = null) },
            label = { Text("Saved Links") }
          )
          NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            label = { Text("Digests") }
          )
          NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") }
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (selectedTab) {
        0 -> SavedLinksTab(
          state = state,
          onFilterSelect = viewModel::setFilter,
          onCardClick = viewModel::onWebsiteCardClicked,
          onSummarizeClick = viewModel::resummarizeWebsite,
          onToggleArchive = viewModel::toggleArchive,
          onDelete = viewModel::deleteWebsite,
          onOpenExternal = { url ->
            ExternalBrowserLauncher.openInExternalBrowser(
              context = context,
              url = url,
              preferredBrowserPackage = state.settings.preferredBrowserPackage,
              useCustomTabs = state.settings.useCustomTabs
            )
          },
          onAddSampleOrUrl = { showAddLinkDialog = true }
        )
        1 -> DigestsHistoryTab(
          digests = state.digests,
          playback = state.podcastPlayback,
          onOpenDigest = viewModel::openSavedDigest,
          onPlayOrGeneratePodcast = { digest ->
            ensureNotificationPermissionForPodcast()
            viewModel.playOrGenerateDigestPodcast(digest, forceRegenerate = false)
          },
          onDeleteDigest = viewModel::deleteDigest,
          onGenerateNew = {
            ensureNotificationPermissionForPodcast()
            selectedTab = 0
            viewModel.triggerCatchMeUpDigest()
          }
        )
        2 -> SettingsTab(
          settings = state.settings,
          verification = state.verification,
          onUpdateSettings = viewModel::updateSettings,
          onVerifyGeminiTts = viewModel::verifyGeminiTts,
          onVerifyTextBackend = viewModel::verifyTextBackend
        )
      }
    }
  }

  // Catch Me Up Podcast Digest Modal Bottom Sheet
  state.activeDigestSheet?.let { activeDigest ->
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = viewModel::dismissDigestSheet,
      sheetState = sheetState
    ) {
      CatchMeUpDigestSheetContent(
        digest = activeDigest,
        settings = state.settings,
        playback = state.podcastPlayback,
        onPlayOrGeneratePodcast = { forceRegen ->
          ensureNotificationPermissionForPodcast()
          viewModel.playOrGenerateDigestPodcast(activeDigest, forceRegenerate = forceRegen)
        },
        onOpenSourceUrl = { url ->
          ExternalBrowserLauncher.openInExternalBrowser(
            context = context,
            url = url,
            preferredBrowserPackage = state.settings.preferredBrowserPackage,
            useCustomTabs = state.settings.useCustomTabs
          )
        },
        onMarkAllArchived = {
          viewModel.markDigestSourcesArchived(activeDigest)
        },
        onDismiss = viewModel::dismissDigestSheet
      )
    }
  }

  if (showAddLinkDialog) {
    AddLinkDialog(
      onDismiss = { showAddLinkDialog = false },
      onAddUrl = { url ->
        viewModel.addUrlManually(url)
        showAddLinkDialog = false
      }
    )
  }
}

@Composable
private fun InAppPostSaveActionBar(
  event: PostSaveBannerEvent,
  onOpenLink: () -> Unit,
  onGeneratePodcast: () -> Unit,
  onDismiss: () -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.inverseSurface,
    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    shape = RoundedCornerShape(14.dp),
    shadowElevation = 6.dp,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = if (event.wasBumped) {
          "Moved ${event.domain} to top"
        } else {
          "Saved ${event.domain}"
        },
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )
      TextButton(onClick = onOpenLink) {
        Text(
          text = "Open Link",
          color = MaterialTheme.colorScheme.inversePrimary,
          fontWeight = FontWeight.SemiBold
        )
      }
      TextButton(onClick = onGeneratePodcast) {
        Text(
          text = "Podcast",
          color = MaterialTheme.colorScheme.inversePrimary,
          fontWeight = FontWeight.SemiBold
        )
      }
      IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Dismiss",
          tint = MaterialTheme.colorScheme.inverseOnSurface,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}

@Composable
private fun PodcastMiniPlayerBar(
  playback: PodcastPlaybackState,
  onTogglePlayPause: () -> Unit,
  onSkipBack15: () -> Unit,
  onSkipForward15: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onClose: () -> Unit,
  onOpenDetails: () -> Unit,
  onTriggerQuietWindowNow: () -> Unit,
  onCancelQuietWindow: () -> Unit
) {
  Surface(
    tonalElevation = 6.dp,
    shadowElevation = 6.dp,
    color = MaterialTheme.colorScheme.secondaryContainer,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      if (playback.isGeneratingInBackground) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }

      if (playback.isActive) {
        var scrubPositionMs by remember(playback.audioFilePath) { mutableStateOf<Float?>(null) }
        val displayedPositionMs = scrubPositionMs?.toLong() ?: playback.positionMs

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetails)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = if (playback.isPlaying) Icons.Default.GraphicEq else Icons.Default.Headphones,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = playback.title.ifBlank { "☀️ Summer Podcast" },
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = buildString {
                append(formatDurationMs(displayedPositionMs))
                if (playback.durationMs > 0L) {
                  append(" / ")
                  append(formatDurationMs(playback.durationMs))
                }
                if (playback.subtitle.isNotBlank()) {
                  append(" • ")
                  append(playback.subtitle)
                }
              },
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          IconButton(onClick = onSkipBack15, modifier = Modifier.size(36.dp)) {
            Icon(
              imageVector = Icons.Default.FastRewind,
              contentDescription = "Back 15 seconds",
              modifier = Modifier.size(20.dp)
            )
          }
          IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(40.dp)) {
            Icon(
              imageVector = if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (playback.isPlaying) "Pause" else "Play"
            )
          }
          IconButton(onClick = onSkipForward15, modifier = Modifier.size(36.dp)) {
            Icon(
              imageVector = Icons.Default.FastForward,
              contentDescription = "Forward 15 seconds",
              modifier = Modifier.size(20.dp)
            )
          }
          IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close player",
              modifier = Modifier.size(18.dp)
            )
          }
        }

        if (playback.durationMs > 0L) {
          Slider(
            value = (scrubPositionMs ?: playback.positionMs.toFloat())
              .coerceIn(0f, playback.durationMs.toFloat()),
            onValueChange = { newPos -> scrubPositionMs = newPos },
            onValueChangeFinished = {
              scrubPositionMs?.let { target -> onSeekTo(target.toLong()) }
              scrubPositionMs = null
            },
            valueRange = 0f..playback.durationMs.toFloat(),
            modifier = Modifier
              .fillMaxWidth()
              .height(22.dp)
              .padding(horizontal = 12.dp)
          )
        }
      } else if (playback.isGeneratingInBackground) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "🎙️ Generating: ${playback.generationTitle ?: "Summer Podcast"}",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = playback.generationStatusText
                ?: "Silent in background — you'll get a notification to play when ready",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      } else if (playback.isQuietWindowWaiting) {
        var nowMs by remember(playback.quietWindowDeadlineMs) {
          mutableLongStateOf(System.currentTimeMillis())
        }
        LaunchedEffect(playback.quietWindowDeadlineMs) {
          while (true) {
            nowMs = System.currentTimeMillis()
            if (nowMs >= playback.quietWindowDeadlineMs) break
            delay(1_000L)
          }
        }
        val remainingMs = (playback.quietWindowDeadlineMs - nowMs).coerceAtLeast(0L)

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Auto-podcast in ${formatDurationMs(remainingMs)}",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${playback.quietWindowLinkCount} links in session • resets if you save another",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          TextButton(onClick = onTriggerQuietWindowNow) {
            Text("Start Now", fontWeight = FontWeight.SemiBold)
          }
          IconButton(onClick = onCancelQuietWindow, modifier = Modifier.size(32.dp)) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Cancel auto-podcast timer",
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

private fun formatDurationMs(ms: Long): String {
  val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return "%d:%02d".format(minutes, seconds)
}

internal fun formatDateHeaderLabel(
  date: LocalDate,
  today: LocalDate,
  locale: Locale = Locale.getDefault()
): String {
  return when {
    date == today -> "Today"
    date == today.minusDays(1) -> "Yesterday"
    date.year == today.year -> DateTimeFormatter.ofPattern("MMM d", locale).format(date)
    else -> DateTimeFormatter.ofPattern("MMM d, yyyy", locale).format(date)
  }
}

internal fun groupWebsitesByCreatedDate(
  websites: List<SavedWebsite>,
  zoneId: ZoneId = ZoneId.systemDefault(),
  today: LocalDate = LocalDate.now(zoneId),
  locale: Locale = Locale.getDefault()
): List<Pair<String, List<SavedWebsite>>> {
  if (websites.isEmpty()) return emptyList()
  return websites
    .sortedByDescending { it.createdAt }
    .groupBy { site ->
      Instant.ofEpochMilli(site.createdAt).atZone(zoneId).toLocalDate()
    }
    .entries
    .sortedByDescending { it.key }
    .map { (date, itemsForDay) ->
      formatDateHeaderLabel(date, today, locale) to itemsForDay
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SavedLinksTab(
  state: SummerUiState,
  onFilterSelect: (LinkFilter) -> Unit,
  onCardClick: (SavedWebsite) -> Unit,
  onSummarizeClick: (String) -> Unit,
  onToggleArchive: (SavedWebsite) -> Unit,
  onDelete: (String) -> Unit,
  onOpenExternal: (String) -> Unit,
  onAddSampleOrUrl: () -> Unit
) {
  val dateSections = remember(state.filteredWebsites) {
    groupWebsitesByCreatedDate(state.filteredWebsites)
  }

  Column(modifier = Modifier.fillMaxSize()) {
    // Filter Chips Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      LinkFilter.entries.forEach { filter ->
        val count = when (filter) {
          LinkFilter.UNREAD -> state.websites.count { !it.isArchived }
          LinkFilter.ALL -> state.websites.size
          LinkFilter.ARCHIVED -> state.websites.count { it.isArchived }
        }
        FilterChip(
          selected = state.activeFilter == filter,
          onClick = { onFilterSelect(filter) },
          label = { Text("${filter.label} ($count)") }
        )
      }
    }

    if (state.isGeneratingDigest) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    if (state.filteredWebsites.isEmpty()) {
      EmptySavedLinksView(
        activeFilter = state.activeFilter,
        onAddLinkClick = onAddSampleOrUrl
      )
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
          start = 16.dp,
          end = 16.dp,
          top = 0.dp,
          bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        dateSections.forEach { (dateLabel, itemsForDay) ->
          stickyHeader(key = "date_header_$dateLabel") {
            Surface(
              color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp, end = 4.dp)
              )
            }
          }

          items(
            items = itemsForDay,
            key = { it.id }
          ) { website ->
            val dismissState = rememberSwipeToDismissBoxState(
              confirmValueChange = { value ->
                when (value) {
                  SwipeToDismissBoxValue.StartToEnd -> {
                    onToggleArchive(website)
                    false
                  }
                  SwipeToDismissBoxValue.EndToStart -> {
                    onDelete(website.id)
                    true
                  }
                  SwipeToDismissBoxValue.Settled -> false
                }
              }
            )

            SwipeToDismissBox(
              state = dismissState,
              backgroundContent = {
                val direction = dismissState.dismissDirection
                val bgColor = when (direction) {
                  SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondaryContainer
                  SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                  else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                  contentAlignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                  }
                ) {
                  if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = if (website.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = null
                      )
                      Spacer(Modifier.width(8.dp))
                      Text(if (website.isArchived) "Move to Unread" else "Archive")
                    }
                  } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text("Delete", color = MaterialTheme.colorScheme.onErrorContainer)
                      Spacer(Modifier.width(8.dp))
                      Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                      )
                    }
                  }
                }
              }
            ) {
              WebsiteItemCard(
                website = website,
                isExpanded = website.id in state.expandedWebsiteIds,
                onCardClick = { onCardClick(website) },
                onSummarizeClick = { onSummarizeClick(website.id) },
                onToggleArchive = { onToggleArchive(website) },
                onDelete = { onDelete(website.id) },
                onOpenExternal = { onOpenExternal(website.url) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WebsiteItemCard(
  website: SavedWebsite,
  isExpanded: Boolean,
  onCardClick: () -> Unit,
  onSummarizeClick: () -> Unit,
  onToggleArchive: () -> Unit,
  onDelete: () -> Unit,
  onOpenExternal: () -> Unit
) {
  val context = LocalContext.current

  ElevatedCard(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onCardClick),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Top Row: Square Left Thumbnail (zero padding) + Title & Small Favicon + Domain (no chevron, no timestamp)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
          )
          if (website.thumbnailUrl.isNotBlank()) {
            AsyncImage(
              model = website.thumbnailUrl,
              contentDescription = "${website.domain} thumbnail",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = website.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (website.isArchived) FontWeight.Normal else FontWeight.SemiBold,
            maxLines = if (isExpanded) 4 else 2,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
              model = website.faviconUrl,
              contentDescription = "${website.domain} icon",
              contentScale = ContentScale.Fit,
              modifier = Modifier
                .size(15.dp)
                .clip(RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = website.domain,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false)
            )
            if (website.summary != null) {
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
              ) {
                Text(
                  text = "AI",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
              }
            }
          }
        }
      }

      if (website.isLoadingMetadata) {
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }

      // Inline Expanded Section: Flattened Markdown AI Summary + Action Buttons
      AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
          Spacer(modifier = Modifier.height(10.dp))

          when {
            website.isSummarizing -> {
              SummarySkeletonLoader()
            }
            website.summaryError != null -> {
              Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = website.summaryError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  OutlinedButton(onClick = onSummarizeClick) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Retry Summary")
                  }
                }
              }
            }
            website.summary != null -> {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                  Text(
                    text = website.summaryProvider ?: "AI Summary",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                  )
                }
                IconButton(
                  onClick = onSummarizeClick,
                  modifier = Modifier.size(26.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Regenerate summary",
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              Spacer(modifier = Modifier.height(6.dp))
              MarkdownText(
                markdown = website.summary,
                style = MaterialTheme.typography.bodyMedium
              )
            }
            else -> {
              Button(
                onClick = onSummarizeClick,
                modifier = Modifier.fillMaxWidth()
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Summarize Website")
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Action Buttons Row: Open in Browser, Archive/Read, Share, Delete
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedButton(
              onClick = onOpenExternal,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Open in Browser")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(onClick = onToggleArchive) {
                Icon(
                  imageVector = if (website.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                  contentDescription = if (website.isArchived) "Unarchive" else "Archive"
                )
              }
              IconButton(
                onClick = {
                  val shareText = buildString {
                    appendLine(website.title)
                    appendLine(website.url)
                    if (!website.summary.isNullOrBlank()) {
                      appendLine()
                      appendLine("Summary (via Summer):")
                      appendLine(website.summary)
                    }
                  }
                  val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                  }
                  context.startActivity(Intent.createChooser(sendIntent, "Share website"))
                }
              ) {
                Icon(
                  imageVector = Icons.Default.Share,
                  contentDescription = "Share"
                )
              }
              IconButton(onClick = onDelete) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = "Delete",
                  tint = MaterialTheme.colorScheme.error
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SummarySkeletonLoader() {
  val transition = rememberInfiniteTransition(label = "summary_skeleton")
  val pulseAlpha by transition.animateFloat(
    initialValue = 0.28f,
    targetValue = 0.72f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 850),
      repeatMode = RepeatMode.Reverse
    ),
    label = "skeleton_alpha"
  )
  val barColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = pulseAlpha * 0.45f)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Provider label bar
    Box(
      modifier = Modifier
        .width(108.dp)
        .height(10.dp)
        .clip(RoundedCornerShape(5.dp))
        .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha * 0.45f))
    )

    Spacer(modifier = Modifier.height(2.dp))

    // TL;DR lines
    Box(
      modifier = Modifier
        .fillMaxWidth(0.96f)
        .height(13.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(barColor)
    )
    Box(
      modifier = Modifier
        .fillMaxWidth(0.76f)
        .height(13.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(barColor)
    )

    Spacer(modifier = Modifier.height(2.dp))

    // Bullet point skeleton rows
    listOf(0.91f, 0.84f, 0.68f).forEach { fraction ->
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha * 0.55f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth(fraction)
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(barColor)
        )
      }
    }
  }
}

@Composable
private fun EmptySavedLinksView(
  activeFilter: LinkFilter,
  onAddLinkClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = Icons.Default.AutoAwesome,
      contentDescription = null,
      modifier = Modifier.size(56.dp),
      tint = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = when (activeFilter) {
        LinkFilter.UNREAD -> "All caught up!"
        LinkFilter.ALL -> "No saved websites yet"
        LinkFilter.ARCHIVED -> "No archived websites"
      },
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "When browsing Google Feed or any app, tap a link and choose \"Summer\" (or use Share → Summer). You'll get a quick action bar to open the app, open the link, or start a podcast right away.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(20.dp))
    OutlinedButton(onClick = onAddLinkClick) {
      Icon(Icons.Default.Add, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Text("Add a Link Manually")
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatchMeUpDigestSheetContent(
  digest: DigestEntry,
  settings: AppSettings,
  playback: PodcastPlaybackState,
  onPlayOrGeneratePodcast: (Boolean) -> Unit,
  onOpenSourceUrl: (String) -> Unit,
  onMarkAllArchived: () -> Unit,
  onDismiss: () -> Unit
) {
  val dateStr = remember(digest.createdAt) {
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
      .format(Date(digest.createdAt))
  }
  val isThisDigestPlaying =
    playback.isActive && playback.activeDigestId == digest.id && playback.isPlaying

  val effectiveHostNames = remember(digest.hostNames, digest.language, settings) {
    if (digest.hostNames.isNotEmpty()) {
      digest.hostNames
    } else {
      listOf(settings.hostAName, settings.hostBName, settings.hostCName)
        .take(settings.podcastHostCount.coerceIn(2, 3))
    }
  }
  val hostsLabel = remember(effectiveHostNames, digest.language) {
    PodcastCastCatalog.formatHostList(effectiveHostNames, digest.language)
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 8.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "☀️ Catch Me Up Podcast",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
          )
          if (digest.language == PodcastLanguage.SWEDISH) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              color = MaterialTheme.colorScheme.tertiaryContainer,
              shape = CircleShape
            ) {
              Text(
                text = "🇸🇪 SV",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
        }
        Text(
          text = "${digest.sourceWebsites.size} website(s) • $dateStr",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Podcast Episode Player Card
    Surface(
      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = Icons.Default.Podcasts,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Hosted by $hostsLabel",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = digest.podcastVoiceProvider
                  ?: "Conversational episode weaving all ${digest.sourceWebsites.size} saved articles",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
              )
            }
          }

          if (digest.podcastAudioPath != null && !digest.isGeneratingPodcast) {
            IconButton(onClick = { onPlayOrGeneratePodcast(true) }) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Regenerate podcast episode"
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
          digest.isGeneratingPodcast -> {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = digest.podcastStatusText ?: "Scripting & synthesizing podcast...",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "Silent in background — you'll get a notification with a Play button once ready",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
              }
            }
          }
          digest.podcastError != null -> {
            Text(
              text = digest.podcastError,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { onPlayOrGeneratePodcast(true) }) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Retry Podcast")
            }
          }
          digest.podcastAudioPath != null -> {
            Button(
              onClick = { onPlayOrGeneratePodcast(false) },
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(
                imageVector = if (isThisDigestPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null
              )
              Spacer(Modifier.width(8.dp))
              Text(if (isThisDigestPlaying) "Pause Podcast Episode" else "Play Podcast Episode")
            }
          }
          else -> {
            Button(
              onClick = { onPlayOrGeneratePodcast(false) },
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text("Generate & Play Podcast")
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Text(
      text = "Sources in this Episode (${digest.sourceWebsites.size})",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))

    Column(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      digest.sourceWebsites.forEachIndexed { index, source ->
        OutlinedCard(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenSourceUrl(source.url) },
          shape = RoundedCornerShape(12.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            AsyncImage(
              model = source.faviconUrl,
              contentDescription = null,
              modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = source.title.ifBlank { source.domain },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "[${index + 1}] ${source.domain}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Icon(
              imageVector = Icons.Default.OpenInBrowser,
              contentDescription = "Open source link",
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier.weight(1f)
      ) {
        Text("Close")
      }
      Button(
        onClick = onMarkAllArchived,
        modifier = Modifier.weight(1.4f)
      ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Archive Sources")
      }
    }

    Spacer(modifier = Modifier.height(28.dp))
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DigestsHistoryTab(
  digests: List<DigestEntry>,
  playback: PodcastPlaybackState,
  onOpenDigest: (DigestEntry) -> Unit,
  onPlayOrGeneratePodcast: (DigestEntry) -> Unit,
  onDeleteDigest: (String) -> Unit,
  onGenerateNew: () -> Unit
) {
  if (digests.isEmpty()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = Icons.Default.Podcasts,
        contentDescription = null,
        modifier = Modifier.size(56.dp),
        tint = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "No podcast digests yet",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Tap \"Catch Me Up\" on the Saved Links tab (or enable Auto-Podcast After Browsing Session in Settings) to generate a multi-host podcast episode across your saved articles.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(20.dp))
      Button(onClick = onGenerateNew) {
        Icon(Icons.Default.Podcasts, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Generate Podcast Episode Now")
      }
    }
  } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(digests, key = { it.id }) { digest ->
        val dateStr = remember(digest.createdAt) {
          DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(digest.createdAt))
        }
        val isPlayingThis =
          playback.isActive && playback.activeDigestId == digest.id && playback.isPlaying
        val hostsLabel = remember(digest.hostNames, digest.language) {
          PodcastCastCatalog.formatHostList(digest.hostNames, digest.language)
        }

        ElevatedCard(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDigest(digest) },
          shape = RoundedCornerShape(16.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = dateStr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                  )
                  if (digest.language == PodcastLanguage.SWEDISH) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                      color = MaterialTheme.colorScheme.tertiaryContainer,
                      shape = CircleShape
                    ) {
                      Text(
                        text = "SV",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                      )
                    }
                  }
                }
                Text(
                  text = buildString {
                    append("🎙️ $hostsLabel • ${digest.sourceWebsites.size} link(s)")
                    when {
                      digest.isGeneratingPodcast -> append(" • Generating...")
                      digest.podcastAudioPath != null -> append(" • Cached Offline")
                    }
                  },
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary
                )
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onPlayOrGeneratePodcast(digest) }) {
                  if (digest.isGeneratingPodcast) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(18.dp),
                      strokeWidth = 2.dp
                    )
                  } else {
                    Icon(
                      imageVector = when {
                        isPlayingThis -> Icons.Default.Pause
                        digest.podcastAudioPath != null -> Icons.Default.PlayArrow
                        else -> Icons.Default.Headphones
                      },
                      contentDescription = "Play or generate podcast",
                      tint = MaterialTheme.colorScheme.primary
                    )
                  }
                }
                IconButton(onClick = { onDeleteDigest(digest.id) }) {
                  Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete digest"
                  )
                }
              }
            }

            if (digest.sourceWebsites.isNotEmpty()) {
              Spacer(modifier = Modifier.height(8.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                digest.sourceWebsites.take(5).forEach { src ->
                  AssistChip(
                    onClick = { onOpenDigest(digest) },
                    leadingIcon = {
                      AsyncImage(
                        model = src.faviconUrl,
                        contentDescription = null,
                        modifier = Modifier
                          .size(14.dp)
                          .clip(RoundedCornerShape(3.dp))
                      )
                    },
                    label = {
                      Text(
                        text = src.domain,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                      )
                    }
                  )
                }
                if (digest.sourceWebsites.size > 5) {
                  AssistChip(
                    onClick = { onOpenDigest(digest) },
                    label = {
                      Text(
                        text = "+${digest.sourceWebsites.size - 5} more",
                        style = MaterialTheme.typography.labelSmall
                      )
                    }
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsTab(
  settings: AppSettings,
  verification: VerificationUiState,
  onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
  onVerifyGeminiTts: () -> Unit,
  onVerifyTextBackend: () -> Unit
) {
  val context = LocalContext.current
  val installedBrowsers = remember {
    ExternalBrowserLauncher.getInstalledExternalBrowsers(context)
  }
  var showDirectGeminiKey by rememberSaveable { mutableStateOf(false) }
  var showOpenAiKey by rememberSaveable { mutableStateOf(false) }
  var showElevenLabsKey by rememberSaveable { mutableStateOf(false) }
  var showAzureKey by rememberSaveable { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Summarization & Session Auto-Podcast Behavior Card
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Summarization & Auto-Podcast Behavior",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
              text = "Auto-summarize text on save",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Automatically generate the Markdown AI text summary right after a link is saved.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = settings.autoSummarizeOnSave,
            onCheckedChange = { enabled ->
              onUpdateSettings { it.copy(autoSummarizeOnSave = enabled) }
            }
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
              text = "Auto-podcast after browsing session",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Waits until you stop saving links (e.g. from Google Feed), groups links saved close in time, silently generates a Catch Me Up podcast, and notifies you with a Play button.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = settings.autoPodcastAfterQuietWindow,
            onCheckedChange = { enabled ->
              onUpdateSettings { it.copy(autoPodcastAfterQuietWindow = enabled) }
            }
          )
        }

        AnimatedVisibility(visible = settings.autoPodcastAfterQuietWindow) {
          Column(modifier = Modifier.padding(top = 12.dp)) {
            var draftQuietMinutes by remember(settings.autoPodcastQuietMinutes) {
              mutableFloatStateOf(settings.autoPodcastQuietMinutes.coerceIn(1, 10).toFloat())
            }
            var draftMinLinks by remember(settings.autoPodcastMinLinks) {
              mutableFloatStateOf(settings.autoPodcastMinLinks.coerceIn(2, 10).toFloat())
            }

            Text(
              text = "Start podcast ${draftQuietMinutes.roundToInt()} min after last saved link",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary
            )
            Slider(
              value = draftQuietMinutes,
              onValueChange = { draftQuietMinutes = it },
              onValueChangeFinished = {
                onUpdateSettings {
                  it.copy(autoPodcastQuietMinutes = draftQuietMinutes.roundToInt().coerceIn(1, 10))
                }
              },
              valueRange = 1f..10f,
              steps = 8
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = "Minimum links in session burst: ${draftMinLinks.roundToInt()} links",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary
            )
            Slider(
              value = draftMinLinks,
              onValueChange = { draftMinLinks = it },
              onValueChangeFinished = {
                onUpdateSettings {
                  it.copy(autoPodcastMinLinks = draftMinLinks.roundToInt().coerceIn(2, 10))
                }
              },
              valueRange = 2f..10f,
              steps = 7
            )
          }
        }
      }
    }

    // Podcast Hosts, Language & Voice Engine Card
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "🎙️ Podcast Hosts, Language & Voices",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Choose podcast language, 2 or 3 hosts, randomized or custom voices, and your TTS engine.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Podcast Language (English / Swedish)
        Text(
          text = "Podcast Episode Language",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          PodcastLanguage.entries.forEach { lang ->
            FilterChip(
              selected = settings.podcastLanguage == lang,
              onClick = { onUpdateSettings { it.copy(podcastLanguage = lang) } },
              label = { Text(lang.displayName) }
            )
          }
        }
        if (settings.podcastLanguage == PodcastLanguage.SWEDISH) {
          Text(
            text = "All saved articles (even English ones) are translated and discussed in natural Swedish.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Number of Hosts (2 or 3)
        Text(
          text = "Number of Podcast Hosts",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          listOf(2 to "2 Hosts (Duo)", 3 to "3 Hosts (Roundtable)").forEach { (count, label) ->
            FilterChip(
              selected = settings.podcastHostCount.coerceIn(2, 3) == count,
              onClick = { onUpdateSettings { it.copy(podcastHostCount = count) } },
              label = { Text(label) }
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Randomize names & voices toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
              text = "Randomize host names & voices each episode",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "Picks contrasting voices and matching ${settings.podcastLanguage.displayName} names automatically for every new episode.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = settings.randomizePodcastHosts,
            onCheckedChange = { enabled ->
              onUpdateSettings { it.copy(randomizePodcastHosts = enabled) }
            }
          )
        }

        AnimatedVisibility(visible = !settings.randomizePodcastHosts) {
          Column(modifier = Modifier.padding(top = 10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedTextField(
                value = settings.hostAName,
                onValueChange = { name -> onUpdateSettings { it.copy(hostAName = name) } },
                label = { Text("Host 1 Name") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = settings.hostBName,
                onValueChange = { name -> onUpdateSettings { it.copy(hostBName = name) } },
                label = { Text("Host 2 Name") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
            }
            if (settings.podcastHostCount >= 3) {
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                value = settings.hostCName,
                onValueChange = { name -> onUpdateSettings { it.copy(hostCName = name) } },
                label = { Text("Host 3 Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        PodcastTtsProvider.entries.forEach { ttsProvider ->
          val isSelected = settings.podcastTtsProvider == ttsProvider
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clickable {
                onUpdateSettings { it.copy(podcastTtsProvider = ttsProvider) }
              },
            colors = CardDefaults.cardColors(
              containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
              } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
              }
            )
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = isSelected,
                onClick = { onUpdateSettings { it.copy(podcastTtsProvider = ttsProvider) } }
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = ttsProvider.displayName,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = ttsProvider.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
              text = "Auto-fallback to On-Device Multi-Voice TTS",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "If a cloud TTS key isn't configured yet or rate-limits, automatically fall back to free offline Android multi-voice synthesis.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = settings.fallbackToOnDeviceTts,
            onCheckedChange = { enabled ->
              onUpdateSettings { it.copy(fallbackToOnDeviceTts = enabled) }
            }
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        val isThreeHosts = settings.podcastHostCount >= 3

        when (settings.podcastTtsProvider) {
          PodcastTtsProvider.GEMINI_MULTISPEAKER -> {
            val resolvedVoiceA = GeminiCatalog.resolveValidVoice(settings.geminiHostAVoice, "Puck")
            val resolvedVoiceB = GeminiCatalog.resolveValidVoice(settings.geminiHostBVoice, "Kore")
            val resolvedVoiceC = GeminiCatalog.resolveValidVoice(settings.geminiHostCVoice, "Aoede")
            val voiceOptions = remember {
              GeminiCatalog.PREBUILT_VOICES.map { it.name to it.displayLabel }
            }
            val ttsModelOptions = remember { GeminiCatalog.TTS_MODELS }

            Text(
              text = "Gemini Multi-Speaker TTS (Free via Google AI Studio)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Uses Google's native multiSpeakerVoiceConfig across all 30 official Gemini studio voices in English and Swedish.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = settings.directGeminiApiKey,
              onValueChange = { value ->
                onUpdateSettings { it.copy(directGeminiApiKey = value) }
              },
              label = { Text("Google AI Studio API Key") },
              placeholder = { Text("AIzaSy...") },
              singleLine = true,
              visualTransformation = if (showDirectGeminiKey) {
                VisualTransformation.None
              } else {
                PasswordVisualTransformation()
              },
              trailingIcon = {
                IconButton(onClick = { showDirectGeminiKey = !showDirectGeminiKey }) {
                  Icon(
                    imageVector = if (showDirectGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle API key visibility"
                  )
                }
              },
              modifier = Modifier.fillMaxWidth()
            )
            if (!settings.randomizePodcastHosts) {
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                SettingsDropdownSelector(
                  label = "${settings.hostAName.ifBlank { "Host 1" }} Voice",
                  selectedValue = resolvedVoiceA,
                  selectedDisplay = GeminiCatalog.PREBUILT_VOICES
                    .firstOrNull { it.name == resolvedVoiceA }?.displayLabel ?: resolvedVoiceA,
                  options = voiceOptions,
                  onSelect = { chosen ->
                    onUpdateSettings { it.copy(geminiHostAVoice = chosen) }
                  },
                  modifier = Modifier.weight(1f)
                )
                SettingsDropdownSelector(
                  label = "${settings.hostBName.ifBlank { "Host 2" }} Voice",
                  selectedValue = resolvedVoiceB,
                  selectedDisplay = GeminiCatalog.PREBUILT_VOICES
                    .firstOrNull { it.name == resolvedVoiceB }?.displayLabel ?: resolvedVoiceB,
                  options = voiceOptions,
                  onSelect = { chosen ->
                    onUpdateSettings { it.copy(geminiHostBVoice = chosen) }
                  },
                  modifier = Modifier.weight(1f)
                )
              }
              if (isThreeHosts) {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsDropdownSelector(
                  label = "${settings.hostCName.ifBlank { "Host 3" }} Voice",
                  selectedValue = resolvedVoiceC,
                  selectedDisplay = GeminiCatalog.PREBUILT_VOICES
                    .firstOrNull { it.name == resolvedVoiceC }?.displayLabel ?: resolvedVoiceC,
                  options = voiceOptions,
                  onSelect = { chosen ->
                    onUpdateSettings { it.copy(geminiHostCVoice = chosen) }
                  },
                  modifier = Modifier.fillMaxWidth()
                )
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
            SettingsDropdownSelector(
              label = "Gemini Multi-Speaker TTS Model",
              selectedValue = settings.geminiTtsModel,
              selectedDisplay = ttsModelOptions
                .firstOrNull { it.first == settings.geminiTtsModel }?.second
                ?: settings.geminiTtsModel,
              options = ttsModelOptions,
              onSelect = { chosenModel ->
                onUpdateSettings { it.copy(geminiTtsModel = chosenModel) }
              },
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              FilledTonalButton(
                enabled = !verification.isVerifyingTts,
                onClick = onVerifyGeminiTts
              ) {
                if (verification.isVerifyingTts) {
                  CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                  Spacer(Modifier.width(8.dp))
                  Text("Testing Multi-Speaker TTS...")
                } else {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("Verify Multi-Speaker TTS")
                }
              }
            }
            verification.ttsMessage?.let { msg ->
              Spacer(modifier = Modifier.height(8.dp))
              VerificationStatusBox(message = msg, isError = verification.ttsIsError)
            }
          }

          PodcastTtsProvider.ANDROID_ON_DEVICE -> {
            AndroidOnDeviceTtsSettingsSection(
              settings = settings,
              onUpdateSettings = onUpdateSettings
            )
          }

          PodcastTtsProvider.AZURE_SPEECH -> {
            Text(
              text = "Microsoft Azure AI Speech (500,000 Free Neural Chars/Month)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Azure Speech F0 tier offers 500,000 free neural characters every month perpetually and supports multi-voice SSML in English and Swedish.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = settings.azureSpeechKey,
              onValueChange = { v -> onUpdateSettings { it.copy(azureSpeechKey = v) } },
              label = { Text("Azure Speech Subscription Key") },
              singleLine = true,
              visualTransformation = if (showAzureKey) {
                VisualTransformation.None
              } else {
                PasswordVisualTransformation()
              },
              trailingIcon = {
                IconButton(onClick = { showAzureKey = !showAzureKey }) {
                  Icon(
                    imageVector = if (showAzureKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle key visibility"
                  )
                }
              },
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = settings.azureSpeechRegion,
              onValueChange = { v -> onUpdateSettings { it.copy(azureSpeechRegion = v) } },
              label = { Text("Azure Region (e.g. eastus, westeurope, swedencentral)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            if (!settings.randomizePodcastHosts) {
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                OutlinedTextField(
                  value = settings.azureHostAVoice,
                  onValueChange = { v -> onUpdateSettings { it.copy(azureHostAVoice = v) } },
                  label = { Text("${settings.hostAName.ifBlank { "Host 1" }} Neural Voice") },
                  singleLine = true,
                  modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                  value = settings.azureHostBVoice,
                  onValueChange = { v -> onUpdateSettings { it.copy(azureHostBVoice = v) } },
                  label = { Text("${settings.hostBName.ifBlank { "Host 2" }} Neural Voice") },
                  singleLine = true,
                  modifier = Modifier.weight(1f)
                )
              }
              if (isThreeHosts) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                  value = settings.azureHostCVoice,
                  onValueChange = { v -> onUpdateSettings { it.copy(azureHostCVoice = v) } },
                  label = { Text("${settings.hostCName.ifBlank { "Host 3" }} Neural Voice") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth()
                )
              }
            }
          }

          PodcastTtsProvider.ELEVENLABS -> {
            Text(
              text = "ElevenLabs Neural TTS",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Create a free account at elevenlabs.io (~10,000 free credits/month). Uses eleven_flash_v2_5 by default for fast multilingual synthesis.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = settings.elevenLabsApiKey,
              onValueChange = { v -> onUpdateSettings { it.copy(elevenLabsApiKey = v) } },
              label = { Text("ElevenLabs API Key (xi-api-key)") },
              singleLine = true,
              visualTransformation = if (showElevenLabsKey) {
                VisualTransformation.None
              } else {
                PasswordVisualTransformation()
              },
              trailingIcon = {
                IconButton(onClick = { showElevenLabsKey = !showElevenLabsKey }) {
                  Icon(
                    imageVector = if (showElevenLabsKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle key visibility"
                  )
                }
              },
              modifier = Modifier.fillMaxWidth()
            )
            if (!settings.randomizePodcastHosts) {
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                OutlinedTextField(
                  value = settings.elevenLabsHostAVoiceId,
                  onValueChange = { v -> onUpdateSettings { it.copy(elevenLabsHostAVoiceId = v) } },
                  label = { Text("${settings.hostAName.ifBlank { "Host 1" }} Voice ID") },
                  singleLine = true,
                  modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                  value = settings.elevenLabsHostBVoiceId,
                  onValueChange = { v -> onUpdateSettings { it.copy(elevenLabsHostBVoiceId = v) } },
                  label = { Text("${settings.hostBName.ifBlank { "Host 2" }} Voice ID") },
                  singleLine = true,
                  modifier = Modifier.weight(1f)
                )
              }
              if (isThreeHosts) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                  value = settings.elevenLabsHostCVoiceId,
                  onValueChange = { v -> onUpdateSettings { it.copy(elevenLabsHostCVoiceId = v) } },
                  label = { Text("${settings.hostCName.ifBlank { "Host 3" }} Voice ID") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth()
                )
              }
            }
          }

          PodcastTtsProvider.OPENAI_COMPATIBLE_TTS -> {
            Text(
              text = "OpenAI / Kokoro / Custom /v1/audio/speech",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Supports OpenAI Audio (gpt-4o-mini-tts / tts-1 with voices like onyx, nova, shimmer) or free self-hosted Kokoro-FastAPI / Groq endpoints.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = settings.openAiTtsBaseUrl,
              onValueChange = { v -> onUpdateSettings { it.copy(openAiTtsBaseUrl = v) } },
              label = { Text("TTS Base URL") },
              placeholder = { Text("https://api.openai.com/v1") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = settings.openAiTtsApiKey,
              onValueChange = { v -> onUpdateSettings { it.copy(openAiTtsApiKey = v) } },
              label = { Text("TTS API Key (leave blank to reuse OpenAI Chat key)") },
              singleLine = true,
              visualTransformation = if (showOpenAiKey) {
                VisualTransformation.None
              } else {
                PasswordVisualTransformation()
              },
              trailingIcon = {
                IconButton(onClick = { showOpenAiKey = !showOpenAiKey }) {
                  Icon(
                    imageVector = if (showOpenAiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle key visibility"
                  )
                }
              },
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = settings.openAiTtsModel,
              onValueChange = { v -> onUpdateSettings { it.copy(openAiTtsModel = v) } },
              label = { Text("TTS Model") },
              placeholder = { Text("gpt-4o-mini-tts / tts-1 / kokoro") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            if (!settings.randomizePodcastHosts) {
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                OutlinedTextField(
                  value = settings.openAiHostAVoice,
                  onValueChange = { v -> onUpdateSettings { it.copy(openAiHostAVoice = v) } },
                  label = { Text("${settings.hostAName.ifBlank { "Host 1" }} Voice") },
                  placeholder = { Text("onyx") },
                  singleLine = true,
                  modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                  value = settings.openAiHostBVoice,
                  onValueChange = { v -> onUpdateSettings { it.copy(openAiHostBVoice = v) } },
                  label = { Text("${settings.hostBName.ifBlank { "Host 2" }} Voice") },
                  placeholder = { Text("nova") },
                  singleLine = true,
                  modifier = Modifier.weight(1f)
                )
              }
              if (isThreeHosts) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                  value = settings.openAiHostCVoice,
                  onValueChange = { v -> onUpdateSettings { it.copy(openAiHostCVoice = v) } },
                  label = { Text("${settings.hostCName.ifBlank { "Host 3" }} Voice") },
                  placeholder = { Text("shimmer") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth()
                )
              }
            }
          }
        }
      }
    }

    // AI Backend Selection Card
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "AI Summarization & Scripting Provider",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Choose which AI backend Summer uses to summarize saved websites and write podcast scripts.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        AiBackend.entries.forEach { backend ->
          val isSelected = settings.selectedBackend == backend
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clickable {
                onUpdateSettings { it.copy(selectedBackend = backend) }
              },
            colors = CardDefaults.cardColors(
              containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
              } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
              }
            )
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = isSelected,
                onClick = { onUpdateSettings { it.copy(selectedBackend = backend) } }
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = backend.displayName,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = backend.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // Provider-specific Configuration Fields
        when (settings.selectedBackend) {
          AiBackend.FIREBASE_AI -> {
            Text(
              text = "Firebase AI Logic Configuration",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Connected to Firebase AI Logic (via app/google-services.json).",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsDropdownSelector(
              label = "Quick Model Preset",
              selectedValue = settings.firebaseModel,
              selectedDisplay = GeminiCatalog.TEXT_MODELS
                .firstOrNull { it.first == settings.firebaseModel }?.second
                ?: settings.firebaseModel,
              options = GeminiCatalog.TEXT_MODELS,
              onSelect = { chosen ->
                onUpdateSettings { it.copy(firebaseModel = chosen) }
              },
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = settings.firebaseModel,
              onValueChange = { value ->
                onUpdateSettings { it.copy(firebaseModel = value) }
              },
              label = { Text("Gemini Model ID (or enter custom)") },
              supportingText = { Text("Recommended: gemini-3.8-flash") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }

          AiBackend.DIRECT_GEMINI -> {
            Text(
              text = "Direct Gemini API (Google AI Studio)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = settings.directGeminiApiKey,
              onValueChange = { value ->
                onUpdateSettings { it.copy(directGeminiApiKey = value) }
              },
              label = { Text("Google AI Studio API Key") },
              placeholder = { Text("AIzaSy...") },
              singleLine = true,
              visualTransformation = if (showDirectGeminiKey) {
                VisualTransformation.None
              } else {
                PasswordVisualTransformation()
              },
              trailingIcon = {
                IconButton(onClick = { showDirectGeminiKey = !showDirectGeminiKey }) {
                  Icon(
                    imageVector = if (showDirectGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle API key visibility"
                  )
                }
              },
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsDropdownSelector(
              label = "Quick Model Preset",
              selectedValue = settings.directGeminiModel,
              selectedDisplay = GeminiCatalog.TEXT_MODELS
                .firstOrNull { it.first == settings.directGeminiModel }?.second
                ?: settings.directGeminiModel,
              options = GeminiCatalog.TEXT_MODELS,
              onSelect = { chosen ->
                onUpdateSettings { it.copy(directGeminiModel = chosen) }
              },
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = settings.directGeminiModel,
              onValueChange = { value ->
                onUpdateSettings { it.copy(directGeminiModel = value) }
              },
              label = { Text("Gemini Model ID (or enter custom)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }

          AiBackend.GEMINI_NANO_ON_DEVICE -> {
            Text(
              text = "On-Device Gemini Nano (Android AICore)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Runs 100% locally on your phone via Google AI Edge AICore SDK. Requires a compatible flagship device (e.g., Pixel 8/9/10 series or Galaxy S24/S25+) with AICore enabled in system settings.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          AiBackend.OPENAI_COMPATIBLE -> {
            Text(
              text = "OpenAI / Anthropic / Custom Endpoint",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = settings.openAiBaseUrl,
              onValueChange = { value ->
                onUpdateSettings { it.copy(openAiBaseUrl = value) }
              },
              label = { Text("Base URL") },
              placeholder = { Text("https://api.openai.com/v1") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = settings.openAiApiKey,
              onValueChange = { value ->
                onUpdateSettings { it.copy(openAiApiKey = value) }
              },
              label = { Text("API Key") },
              placeholder = { Text("sk-...") },
              singleLine = true,
              visualTransformation = if (showOpenAiKey) {
                VisualTransformation.None
              } else {
                PasswordVisualTransformation()
              },
              trailingIcon = {
                IconButton(onClick = { showOpenAiKey = !showOpenAiKey }) {
                  Icon(
                    imageVector = if (showOpenAiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle key visibility"
                  )
                }
              },
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = settings.openAiModel,
              onValueChange = { value ->
                onUpdateSettings { it.copy(openAiModel = value) }
              },
              label = { Text("Model Name") },
              placeholder = { Text("gpt-4o-mini / claude-3-5-haiku-latest") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        FilledTonalButton(
          enabled = !verification.isVerifyingBackend,
          onClick = onVerifyTextBackend
        ) {
          if (verification.isVerifyingBackend) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Testing Model...")
          } else {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Verify Summarization Model")
          }
        }
        verification.backendMessage?.let { msg ->
          Spacer(modifier = Modifier.height(8.dp))
          VerificationStatusBox(message = msg, isError = verification.backendIsError)
        }
      }
    }

    // Google Feed Interception Card
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Only Intercept Google Feed URLs",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "When enabled, Summer only saves links coming from the Google Feed (via search.app/share.google shorteners, Discover tracking params, or Google App referrers). All other links will automatically open in your external browser as if Summer was not installed.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = settings.onlyInterceptGoogleFeed,
            onCheckedChange = { checked ->
              onUpdateSettings { it.copy(onlyInterceptGoogleFeed = checked) }
            }
          )
        }
      }
    }

    // External Browser Selection Card (avoids looping back into Summer)
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "External Browser Launching",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "When opening external articles or passing through non-feed links, Summer targets your installed browser app directly.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Use Custom Tabs overlay",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "When off (default), opens full standalone browser app. When on, opens in an in-app browser tab.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = settings.useCustomTabs,
            onCheckedChange = { checked ->
              onUpdateSettings { it.copy(useCustomTabs = checked) }
            }
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Don't save when opened from notification",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "When enabled, tapping the quick-save notification to open the link in your browser automatically removes it from Summer.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = settings.removeWhenOpenedFromNotification,
            onCheckedChange = { checked ->
              onUpdateSettings { it.copy(removeWhenOpenedFromNotification = checked) }
            }
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Target Browser App",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onUpdateSettings { it.copy(preferredBrowserPackage = "") } }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RadioButton(
            selected = settings.preferredBrowserPackage.isBlank(),
            onClick = { onUpdateSettings { it.copy(preferredBrowserPackage = "") } }
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Auto-detect external browser (Chrome preferred)")
        }

        installedBrowsers.forEach { browser ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                onUpdateSettings { it.copy(preferredBrowserPackage = browser.packageName) }
              }
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = settings.preferredBrowserPackage == browser.packageName,
              onClick = {
                onUpdateSettings { it.copy(preferredBrowserPackage = browser.packageName) }
              }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(browser.label, style = MaterialTheme.typography.bodyMedium)
              Text(
                browser.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AddLinkDialog(
  onDismiss: () -> Unit,
  onAddUrl: (String) -> Unit
) {
  var urlInput by rememberSaveable { mutableStateOf("https://") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Save Website Link") },
    text = {
      Column {
        Text(
          text = "Paste a URL to save and scrape its title, thumbnail, favicon, and content:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = urlInput,
          onValueChange = { urlInput = it },
          label = { Text("Website URL") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (urlInput.isNotBlank()) {
            onAddUrl(urlInput.trim())
          }
        }
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
private fun SettingsDropdownSelector(
  label: String,
  selectedValue: String,
  selectedDisplay: String,
  options: List<Pair<String, String>>,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = modifier) {
    OutlinedCard(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { expanded = true }
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = selectedDisplay,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Icon(
          imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = "Select $label"
        )
      }
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.heightIn(max = 340.dp)
    ) {
      options.forEach { (value, display) ->
        val isSelected = value == selectedValue
        DropdownMenuItem(
          text = {
            Text(
              text = display,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurface
              }
            )
          },
          onClick = {
            onSelect(value)
            expanded = false
          }
        )
      }
    }
  }
}

@Composable
private fun VerificationStatusBox(
  message: String,
  isError: Boolean
) {
  Surface(
    color = if (isError) {
      MaterialTheme.colorScheme.errorContainer
    } else {
      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    },
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodySmall,
      color = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
      } else {
        MaterialTheme.colorScheme.onPrimaryContainer
      },
      modifier = Modifier.padding(12.dp)
    )
  }
}

@Composable
private fun AndroidOnDeviceTtsSettingsSection(
  settings: AppSettings,
  onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit
) {
  val context = LocalContext.current
  val engineOptions = remember {
    listOf("" to "System default TTS engine") +
      OnDeviceTtsCatalog.installedEngines(context).map { it.packageName to "${it.label} (${it.packageName})" }
  }
  var voices by remember { mutableStateOf<List<TtsVoiceOption>>(emptyList()) }
  var voicesLoading by remember { mutableStateOf(true) }

  LaunchedEffect(settings.androidTtsEngine) {
    voicesLoading = true
    voices = OnDeviceTtsCatalog.availableVoices(context, settings.androidTtsEngine)
    voicesLoading = false
  }

  val voiceDropdownOptions = remember(voices) {
    listOf("" to "Auto-select distinct voice") + voices.map { it.name to it.label }
  }

  Text(
    text = "Android On-Device Multi-Voice TTS",
    style = MaterialTheme.typography.titleSmall,
    fontWeight = FontWeight.SemiBold
  )
  Spacer(modifier = Modifier.height(4.dp))
  Text(
    text = "100% free, unlimited, and works offline without any account or API key. Sub-chunks oversized turns on sentence boundaries and stitches all hosts into a single seekable WAV podcast file.",
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant
  )
  Spacer(modifier = Modifier.height(10.dp))

  SettingsDropdownSelector(
    label = "Installed On-Device TTS Engine",
    selectedValue = settings.androidTtsEngine,
    selectedDisplay = engineOptions.firstOrNull { it.first == settings.androidTtsEngine }?.second
      ?: settings.androidTtsEngine.ifBlank { "System default TTS engine" },
    options = engineOptions,
    onSelect = { pkg ->
      onUpdateSettings {
        it.copy(
          androidTtsEngine = pkg,
          androidHostAVoice = "",
          androidHostBVoice = "",
          androidHostCVoice = ""
        )
      }
    },
    modifier = Modifier.fillMaxWidth()
  )

  if (!settings.randomizePodcastHosts) {
    Spacer(modifier = Modifier.height(10.dp))

    if (voicesLoading) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
        Text("Loading installed offline voices...", style = MaterialTheme.typography.bodySmall)
      }
    } else {
      SettingsDropdownSelector(
        label = "${settings.hostAName.ifBlank { "Host 1" }} On-Device Voice",
        selectedValue = settings.androidHostAVoice,
        selectedDisplay = voiceDropdownOptions
          .firstOrNull { it.first == settings.androidHostAVoice }?.second
          ?: settings.androidHostAVoice.ifBlank { "Auto-select distinct voice" },
        options = voiceDropdownOptions,
        onSelect = { chosen ->
          onUpdateSettings { it.copy(androidHostAVoice = chosen) }
        },
        modifier = Modifier.fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(8.dp))
      SettingsDropdownSelector(
        label = "${settings.hostBName.ifBlank { "Host 2" }} On-Device Voice",
        selectedValue = settings.androidHostBVoice,
        selectedDisplay = voiceDropdownOptions
          .firstOrNull { it.first == settings.androidHostBVoice }?.second
          ?: settings.androidHostBVoice.ifBlank { "Auto-select distinct voice" },
        options = voiceDropdownOptions,
        onSelect = { chosen ->
          onUpdateSettings { it.copy(androidHostBVoice = chosen) }
        },
        modifier = Modifier.fillMaxWidth()
      )
      if (settings.podcastHostCount >= 3) {
        Spacer(modifier = Modifier.height(8.dp))
        SettingsDropdownSelector(
          label = "${settings.hostCName.ifBlank { "Host 3" }} On-Device Voice",
          selectedValue = settings.androidHostCVoice,
          selectedDisplay = voiceDropdownOptions
            .firstOrNull { it.first == settings.androidHostCVoice }?.second
            ?: settings.androidHostCVoice.ifBlank { "Auto-select distinct voice" },
          options = voiceDropdownOptions,
          onSelect = { chosen ->
            onUpdateSettings { it.copy(androidHostCVoice = chosen) }
          },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }

  Spacer(modifier = Modifier.height(10.dp))

  var draftRate by remember(settings.androidSpeechRate) {
    mutableFloatStateOf(settings.androidSpeechRate)
  }
  var draftPitch by remember(settings.androidSpeechPitch) {
    mutableFloatStateOf(settings.androidSpeechPitch)
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "Speech Rate: ${"%.2f".format(draftRate)}x",
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Medium
    )
    if (draftRate != 1.0f || draftPitch != 1.0f) {
      TextButton(
        onClick = {
          draftRate = 1.0f
          draftPitch = 1.0f
          onUpdateSettings { it.copy(androidSpeechRate = 1.0f, androidSpeechPitch = 1.0f) }
        }
      ) {
        Text("Reset 1.0x")
      }
    }
  }
  Slider(
    value = draftRate,
    onValueChange = { rate -> draftRate = rate },
    onValueChangeFinished = {
      onUpdateSettings { it.copy(androidSpeechRate = draftRate) }
    },
    valueRange = 0.5f..2.0f,
    steps = 14
  )

  Text(
    text = "Base Pitch: ${"%.2f".format(draftPitch)}x",
    style = MaterialTheme.typography.bodySmall,
    fontWeight = FontWeight.Medium
  )
  Slider(
    value = draftPitch,
    onValueChange = { pitch -> draftPitch = pitch },
    onValueChangeFinished = {
      onUpdateSettings { it.copy(androidSpeechPitch = draftPitch) }
    },
    valueRange = 0.5f..2.0f,
    steps = 14
  )
}
