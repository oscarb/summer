package se.oscarb.summer.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import se.oscarb.summer.data.AiBackend
import se.oscarb.summer.data.AppSettings
import se.oscarb.summer.data.DigestEntry
import se.oscarb.summer.data.GeminiCatalog
import se.oscarb.summer.data.LinkFilter
import se.oscarb.summer.data.OnDeviceTtsCatalog
import se.oscarb.summer.data.PodcastTtsProvider
import se.oscarb.summer.data.SavedWebsite
import se.oscarb.summer.data.TtsEngineOption
import se.oscarb.summer.data.TtsVoiceOption
import se.oscarb.summer.podcast.PodcastPlaybackState
import se.oscarb.summer.util.ExternalBrowserLauncher
import se.oscarb.summer.util.PodcastScriptParser
import java.text.DateFormat
import java.util.Date

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
                1 -> "${state.digests.size} saved AI briefing(s) & podcast(s)"
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
                "Summarizing ${state.unreadCount}..."
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
        if (state.podcastPlayback.isActive || state.podcastPlayback.isGeneratingInBackground) {
          PodcastMiniPlayerBar(
            playback = state.podcastPlayback,
            onTogglePlayPause = viewModel::togglePodcastPlayPause,
            onSkipBack15 = { viewModel.skipPodcastBy(-15_000L) },
            onSkipForward15 = { viewModel.skipPodcastBy(15_000L) },
            onSeekTo = viewModel::seekPodcastTo,
            onClose = viewModel::stopPodcastPlayback,
            onOpenDetails = viewModel::openActivePodcastDetails
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
          onListenArticleClick = { website, forceRegen ->
            ensureNotificationPermissionForPodcast()
            viewModel.playOrGenerateWebsitePodcast(website, forceRegenerate = forceRegen)
          },
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

  // Catch Me Up Digest Modal Bottom Sheet
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
        onGenerateInBackground = {
          ensureNotificationPermissionForPodcast()
          viewModel.generateDigestPodcastInBackground(activeDigest)
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
private fun PodcastMiniPlayerBar(
  playback: PodcastPlaybackState,
  onTogglePlayPause: () -> Unit,
  onSkipBack15: () -> Unit,
  onSkipForward15: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onClose: () -> Unit,
  onOpenDetails: () -> Unit
) {
  Surface(
    tonalElevation = 6.dp,
    shadowElevation = 6.dp,
    color = MaterialTheme.colorScheme.secondaryContainer,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      if (playback.isGeneratingInBackground && !playback.isActive) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
                ?: "Safe to background the app or turn off screen",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      } else if (playback.isActive) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedLinksTab(
  state: SummerUiState,
  onFilterSelect: (LinkFilter) -> Unit,
  onCardClick: (SavedWebsite) -> Unit,
  onSummarizeClick: (String) -> Unit,
  onListenArticleClick: (SavedWebsite, Boolean) -> Unit,
  onToggleArchive: (SavedWebsite) -> Unit,
  onDelete: (String) -> Unit,
  onOpenExternal: (String) -> Unit,
  onAddSampleOrUrl: () -> Unit
) {
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
          top = 4.dp,
          bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(
          items = state.filteredWebsites,
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
            val isThisArticlePlaying =
              state.podcastPlayback.isActive &&
                state.podcastPlayback.activeWebsiteId == website.id &&
                state.podcastPlayback.isPlaying
            WebsiteItemCard(
              website = website,
              isExpanded = website.id in state.expandedWebsiteIds,
              isPlayingAudio = isThisArticlePlaying,
              onCardClick = { onCardClick(website) },
              onSummarizeClick = { onSummarizeClick(website.id) },
              onListenClick = { forceRegen -> onListenArticleClick(website, forceRegen) },
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

@Composable
private fun WebsiteItemCard(
  website: SavedWebsite,
  isExpanded: Boolean,
  isPlayingAudio: Boolean,
  onCardClick: () -> Unit,
  onSummarizeClick: () -> Unit,
  onListenClick: (Boolean) -> Unit,
  onToggleArchive: () -> Unit,
  onDelete: () -> Unit,
  onOpenExternal: () -> Unit
) {
  val context = LocalContext.current
  val dateStr = remember(website.updatedAt) {
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
      .format(Date(website.updatedAt))
  }
  var showSpokenTranscript by rememberSaveable(website.id) { mutableStateOf(false) }

  ElevatedCard(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onCardClick),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Top Row: Favicon + Title + Expand Icon
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Surface(
          modifier = Modifier.size(40.dp),
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Language,
              contentDescription = null,
              modifier = Modifier.size(22.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AsyncImage(
              model = website.faviconUrl,
              contentDescription = "${website.domain} favicon",
              modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
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
          Spacer(modifier = Modifier.height(2.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "${website.domain} • $dateStr",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (website.summary != null) {
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
              ) {
                Text(
                  text = "AI Ready",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            if (website.podcastAudioPath != null) {
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = CircleShape
              ) {
                Text(
                  text = "🎧 Audio",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onTertiaryContainer,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }
        }

        IconButton(onClick = onCardClick) {
          Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand"
          )
        }
      }

      if (website.isLoadingMetadata) {
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }

      // Inline Expanded Section: AI Summary + Conversational Audio + Action Buttons
      AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
          HorizontalDivider()
          Spacer(modifier = Modifier.height(12.dp))

          when {
            website.isSummarizing -> {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 12.dp)
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = "Generating AI summary...",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.primary
                )
              }
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
              Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = website.summaryProvider ?: "AI Summary",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                      )
                    }
                    IconButton(
                      onClick = onSummarizeClick,
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate summary",
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = website.summary,
                    style = MaterialTheme.typography.bodyMedium
                  )
                }
              }
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

          Spacer(modifier = Modifier.height(10.dp))

          // Conversational Spoken Podcast Audio for this Article
          when {
            website.isGeneratingPodcast -> {
              Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(12.dp)
                ) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = website.podcastStatusText ?: "Generating conversational audio...",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold
                    )
                    Text(
                      text = "Continues in background if you lock screen or leave app",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
            website.podcastError != null -> {
              Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                  Text(
                    text = website.podcastError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                  )
                  TextButton(onClick = { onListenClick(true) }) {
                    Text("Retry Audio")
                  }
                }
              }
            }
            else -> {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                FilledTonalButton(
                  onClick = { onListenClick(false) },
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(
                    imageVector = when {
                      isPlayingAudio -> Icons.Default.Pause
                      website.podcastAudioPath != null -> Icons.Default.PlayArrow
                      else -> Icons.Default.Headphones
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = when {
                      isPlayingAudio -> "Pause Conversational Audio"
                      website.podcastAudioPath != null -> "Play Conversational Audio"
                      else -> "Listen (Natural 2-Host Audio)"
                    }
                  )
                }

                if (website.podcastAudioPath != null) {
                  IconButton(onClick = { onListenClick(true) }) {
                    Icon(
                      imageVector = Icons.Default.Refresh,
                      contentDescription = "Regenerate spoken audio"
                    )
                  }
                }
              }

              if (!website.podcastScript.isNullOrBlank()) {
                TextButton(
                  onClick = { showSpokenTranscript = !showSpokenTranscript },
                  contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                  Text(
                    text = if (showSpokenTranscript) "Hide Spoken Dialogue Script" else "Show Spoken Dialogue Script",
                    style = MaterialTheme.typography.labelSmall
                  )
                }
                if (showSpokenTranscript) {
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = website.podcastScript,
                      style = MaterialTheme.typography.bodySmall,
                      modifier = Modifier.padding(10.dp)
                    )
                  }
                }
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
      text = "When browsing Google Feed or any app, tap a link and choose \"Summer\" (or use Share → Summer). The link will be saved here with a quick toast while you stay in your app.",
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
  onGenerateInBackground: () -> Unit,
  onOpenSourceUrl: (String) -> Unit,
  onMarkAllArchived: () -> Unit,
  onDismiss: () -> Unit
) {
  val dateStr = remember(digest.createdAt) {
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
      .format(Date(digest.createdAt))
  }
  var selectedSheetTab by rememberSaveable(digest.id) { mutableIntStateOf(0) }
  val isThisDigestPlaying =
    playback.isActive && playback.activeDigestId == digest.id && playback.isPlaying
  val turns = remember(digest.podcastScript, settings.hostAName, settings.hostBName) {
    PodcastScriptParser.parseTurns(
      rawScript = digest.podcastScript.orEmpty(),
      hostAName = settings.hostAName,
      hostBName = settings.hostBName
    )
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
        Text(
          text = "☀️ Catch Me Up Digest",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "${digest.sourceWebsites.size} website(s) • ${digest.providerName} • $dateStr",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Two-Host Podcast Episode Card
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
                text = "Two-Host Podcast (${settings.hostAName} & ${settings.hostBName})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = digest.podcastVoiceProvider
                  ?: "Weaves a narrative red line across all ${digest.sourceWebsites.size} articles",
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

        Spacer(modifier = Modifier.height(10.dp))

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
                  text = digest.podcastStatusText ?: "Generating 2-host podcast...",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "Runs in background — feel free to lock your screen or switch apps",
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(onClick = { onPlayOrGeneratePodcast(true) }) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry Podcast")
              }
            }
          }
          digest.podcastAudioPath != null -> {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Button(
                onClick = { onPlayOrGeneratePodcast(false) },
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = if (isThisDigestPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                  contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isThisDigestPlaying) "Pause Podcast Episode" else "Play Podcast Episode")
              }
            }
          }
          else -> {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Button(
                onClick = { onPlayOrGeneratePodcast(false) },
                modifier = Modifier.weight(1.2f)
              ) {
                Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Generate & Play")
              }
              OutlinedButton(
                onClick = onGenerateInBackground,
                modifier = Modifier.weight(1f)
              ) {
                Text("In Background")
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Toggle between Written Briefing and 2-Host Podcast Transcript
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      FilterChip(
        selected = selectedSheetTab == 0,
        onClick = { selectedSheetTab = 0 },
        label = { Text("📋 Written Briefing") }
      )
      FilterChip(
        selected = selectedSheetTab == 1,
        onClick = { selectedSheetTab = 1 },
        label = {
          Text(
            if (turns.isNotEmpty()) {
              "🎙️ 2-Host Transcript (${turns.size} turns)"
            } else {
              "🎙️ 2-Host Transcript"
            }
          )
        }
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (selectedSheetTab == 0) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = digest.summaryMarkdown,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(16.dp)
        )
      }
    } else {
      if (turns.isEmpty()) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = if (digest.isGeneratingPodcast) {
                "Writing the 2-host dialogue script with a narrative red line across your saved links..."
              } else {
                "Tap \"Generate & Play\" above to create the 2-host conversation transcript and audio episode."
              },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          turns.forEach { turn ->
            Surface(
              color = if (turn.isHostB) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
              } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
              },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = turn.speaker,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (turn.isHostB) {
                    MaterialTheme.colorScheme.secondary
                  } else {
                    MaterialTheme.colorScheme.primary
                  }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = turn.text,
                  style = MaterialTheme.typography.bodyMedium
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "Sources in this Digest (tap to open in browser)",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      digest.sourceWebsites.forEachIndexed { index, source ->
        AssistChip(
          onClick = { onOpenSourceUrl(source.url) },
          leadingIcon = {
            AsyncImage(
              model = source.faviconUrl,
              contentDescription = null,
              modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
            )
          },
          label = {
            Text(
              text = "[${index + 1}] ${source.domain}",
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        )
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
        Text("Mark All as Read")
      }
    }

    Spacer(modifier = Modifier.height(28.dp))
  }
}

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
        imageVector = Icons.Default.History,
        contentDescription = null,
        modifier = Modifier.size(56.dp),
        tint = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "No Catch Me Up digests yet",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Tap the \"Catch Me Up\" button on the Saved Links tab to generate a combined AI briefing and a 2-host podcast across all your unread websites.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(20.dp))
      Button(onClick = onGenerateNew) {
        Icon(Icons.Default.Podcasts, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Generate Digest & Podcast Now")
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
                Text(
                  text = dateStr,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = buildString {
                    append("${digest.sourceWebsites.size} website(s) • ${digest.providerName}")
                    if (digest.podcastAudioPath != null) {
                      append(" • 🎧 Cached Offline")
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = digest.summaryMarkdown,
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 4,
              overflow = TextOverflow.Ellipsis,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    // Summarization & Background Podcast Behavior Card
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Summarization & Podcast Behavior",
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
              text = "Auto-summarize in background on save",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Automatically generate the AI text summary right after a link is saved from another app.",
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
              text = "Auto-generate 2-host podcast on Catch Me Up",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Automatically script and synthesize the 2-host podcast episode in the background whenever you generate a Catch Me Up digest.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = settings.autoGeneratePodcastOnDigest,
            onCheckedChange = { enabled ->
              onUpdateSettings { it.copy(autoGeneratePodcastOnDigest = enabled) }
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
              text = "Auto-generate article audio on save",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Automatically generate a conversational spoken summary in the background whenever a new link is saved.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = settings.autoGenerateArticlePodcastOnSave,
            onCheckedChange = { enabled ->
              onUpdateSettings { it.copy(autoGenerateArticlePodcastOnSave = enabled) }
            }
          )
        }
      }
    }

    // Podcast Voices & TTS Engine Card
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "🎙️ Podcast Hosts & Voice Engine",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Configure the two podcast hosts and choose which TTS voice engine generates your episodes.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
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

        when (settings.podcastTtsProvider) {
          PodcastTtsProvider.GEMINI_MULTISPEAKER -> {
            val resolvedVoiceA = GeminiCatalog.resolveValidVoice(settings.geminiHostAVoice, "Puck")
            val resolvedVoiceB = GeminiCatalog.resolveValidVoice(settings.geminiHostBVoice, "Kore")
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
              text = "Uses Google's native multiSpeakerVoiceConfig to generate both hosts conversing together in a single dialogue pass (no per-turn stitching). Choose from all 30 official Gemini studio voices below.",
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
                  Text("Testing 2-Speaker TTS...")
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
              text = "Azure Speech F0 tier offers 500,000 free neural characters every month perpetually and supports multi-voice SSML in a single request.",
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
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              OutlinedTextField(
                value = settings.azureHostAVoice,
                onValueChange = { v -> onUpdateSettings { it.copy(azureHostAVoice = v) } },
                label = { Text("${settings.hostAName} Neural Voice") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = settings.azureHostBVoice,
                onValueChange = { v -> onUpdateSettings { it.copy(azureHostBVoice = v) } },
                label = { Text("${settings.hostBName} Neural Voice") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
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
              text = "Create a free account at elevenlabs.io (~10,000 free credits/month). Uses eleven_flash_v2_5 by default for fast, low-cost multi-voice synthesis.",
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
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              OutlinedTextField(
                value = settings.elevenLabsHostAVoiceId,
                onValueChange = { v -> onUpdateSettings { it.copy(elevenLabsHostAVoiceId = v) } },
                label = { Text("${settings.hostAName} Voice ID") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = settings.elevenLabsHostBVoiceId,
                onValueChange = { v -> onUpdateSettings { it.copy(elevenLabsHostBVoiceId = v) } },
                label = { Text("${settings.hostBName} Voice ID") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
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
              text = "Supports OpenAI Audio (gpt-4o-mini-tts / tts-1 with voices like onyx, nova, alloy, echo) or free self-hosted Kokoro-FastAPI / Groq endpoints.",
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
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              OutlinedTextField(
                value = settings.openAiHostAVoice,
                onValueChange = { v -> onUpdateSettings { it.copy(openAiHostAVoice = v) } },
                label = { Text("${settings.hostAName} Voice") },
                placeholder = { Text("onyx") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = settings.openAiHostBVoice,
                onValueChange = { v -> onUpdateSettings { it.copy(openAiHostBVoice = v) } },
                label = { Text("${settings.hostBName} Voice") },
                placeholder = { Text("nova") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }
    }

    // AI Backend Selection Card
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "AI Summarization Provider",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Choose which AI backend Summer uses to summarize saved websites.",
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
          text = "Paste a URL to save and scrape its title, favicon, and content:",
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
    text = "100% free, unlimited, and works offline without any account or API key. Sub-chunks oversized turns on sentence boundaries and stitches both hosts into a single seekable WAV podcast file.",
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
          androidHostBVoice = ""
        )
      }
    },
    modifier = Modifier.fillMaxWidth()
  )

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
