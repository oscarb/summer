package se.oscarb.summer.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import se.oscarb.summer.theme.NewsreaderFontFamily
import se.oscarb.summer.theme.ManropeFontFamily
import se.oscarb.summer.theme.SummerCardBackground
import se.oscarb.summer.theme.SummerHeadline
import se.oscarb.summer.theme.SummerOutlineVariant
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.em
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.filled.FormatQuote
import kotlin.math.hypot
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import se.oscarb.summer.R
import se.oscarb.summer.data.AppSettings
import se.oscarb.summer.data.DigestEntry
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Public
import androidx.compose.ui.text.font.FontFamily
import se.oscarb.summer.theme.SummerError
import se.oscarb.summer.theme.SummerSurfaceDim
import se.oscarb.summer.theme.SummerSurfaceContainerLow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kotlin.math.roundToInt
import se.oscarb.summer.data.AiBackend
import se.oscarb.summer.data.GeminiCatalog
import se.oscarb.summer.data.OnDeviceTtsCatalog
import se.oscarb.summer.data.PodcastTtsProvider
import se.oscarb.summer.data.TtsVoiceOption
import se.oscarb.summer.data.LinkFilter
import se.oscarb.summer.data.PodcastCastCatalog
import se.oscarb.summer.data.PodcastLanguage
import se.oscarb.summer.data.PodcastStyle
import se.oscarb.summer.data.SavedWebsite
import se.oscarb.summer.podcast.PodcastPlaybackState
import se.oscarb.summer.theme.SummerBackground
import se.oscarb.summer.theme.SummerBorderSubtle
import se.oscarb.summer.theme.SummerDomain
import se.oscarb.summer.theme.SummerOutline
import se.oscarb.summer.theme.SummerTextPrimary
import se.oscarb.summer.theme.SummerTextSecondary
import se.oscarb.summer.theme.SummerError
import se.oscarb.summer.theme.SummerErrorContainer
import se.oscarb.summer.theme.SummerOnErrorContainer
import se.oscarb.summer.theme.SummerOnPrimaryContainer
import se.oscarb.summer.theme.SummerOnSurface
import se.oscarb.summer.theme.SummerOnSurfaceVariant
import se.oscarb.summer.theme.SummerPrimary
import se.oscarb.summer.theme.SummerPrimaryContainer
import se.oscarb.summer.theme.SummerSecondary
import se.oscarb.summer.theme.SummerSurface
import se.oscarb.summer.theme.SummerSurfaceBright
import se.oscarb.summer.theme.SummerSurfaceContainer
import se.oscarb.summer.theme.SummerSurfaceContainerHigh
import se.oscarb.summer.theme.SummerSurfaceContainerHighest
import se.oscarb.summer.theme.SummerSurfaceContainerLowest
import se.oscarb.summer.ui.components.MarkdownText
import se.oscarb.summer.util.ExternalBrowserLauncher
import se.oscarb.summer.util.ExternalBrowserOption
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

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

enum class FeedMode(val title: String, val icon: ImageVector) {
  ALL("All", Icons.Default.Layers),
  LINKS("Links", Icons.Default.Link),
  EPISODES("Episodes", Icons.Default.Podcasts);

  fun next(): FeedMode = if (this == ALL) LINKS else if (this == LINKS) EPISODES else ALL
}

enum class EpisodeFilter(val label: String) {
  ALL("All Episodes"),
  UNPLAYED("Unplayed"),
  PLAYED("Played"),
  ENGLISH("🇬🇧 English (EN)"),
  SWEDISH("🇸🇪 Svenska (SV)")
}

private data class FeedDateGroup(
  val date: LocalDate,
  val label: String,
  val episodes: List<DigestEntry>,
  val websites: List<SavedWebsite>
)

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

  var feedMode by rememberSaveable { mutableStateOf(FeedMode.ALL) }
  var activeEpisodeFilter by rememberSaveable { mutableStateOf(EpisodeFilter.ALL) }
  var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
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

  // Full-screen Settings subpage when opened
  if (isSettingsOpen) {
    BackHandler(enabled = true) { isSettingsOpen = false }
    Scaffold(
      modifier = modifier.fillMaxSize(),
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        TopAppBar(
          navigationIcon = {
            IconButton(onClick = { isSettingsOpen = false }) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = SummerOnSurfaceVariant
              )
            }
          },
          title = {
            Text(
              text = "Settings",
              style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = NewsreaderFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
              ),
              color = SummerOnSurface
            )
          },
          actions = {
            Box(
              modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(SummerSurfaceContainerHigh)
                .border(1.dp, SummerBorderSubtle, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_summer_sunrise),
                contentDescription = "Summer",
                tint = SummerPrimaryContainer,
                modifier = Modifier.size(18.dp)
              )
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SummerSurface
          )
        )
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = innerPadding.calculateTopPadding())
      ) {
        SettingsTab(
          settings = state.settings,
          verification = state.verification,
          bottomPadding = innerPadding.calculateBottomPadding() + 24.dp,
          onUpdateSettings = viewModel::updateSettings,
          onVerifyGeminiTts = viewModel::verifyGeminiTts,
          onVerifyTextBackend = viewModel::verifyTextBackend
        )

        if (innerPadding.calculateBottomPadding() > 0.dp) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(innerPadding.calculateBottomPadding())
              .align(Alignment.BottomCenter)
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    SummerBackground.copy(alpha = 0.50f),
                    SummerBackground.copy(alpha = 0.85f)
                  )
                )
              )
          )
        }
      }
    }
    return
  }

  // Main Feed Screen with Stitch Editorial Architecture
  Scaffold(
    modifier = modifier.fillMaxSize(),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable { feedMode = feedMode.next() }
              .padding(horizontal = 4.dp, vertical = 4.dp)
          ) {
            if (feedMode == FeedMode.ALL) {
              Image(
                painter = painterResource(id = R.drawable.ic_summer_logo),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
              )
            } else {
              Icon(
                imageVector = feedMode.icon,
                contentDescription = null,
                tint = SummerPrimaryContainer,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = feedMode.title,
              style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = NewsreaderFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 26.sp
              ),
              color = SummerOnSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Default.ExpandMore,
              contentDescription = "Switch view mode",
              tint = MaterialTheme.colorScheme.outline,
              modifier = Modifier.size(22.dp)
            )
          }
        },
        actions = {
          IconButton(onClick = { showAddLinkDialog = true }) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Add URL manually",
              tint = SummerOnSurfaceVariant
            )
          }
          IconButton(onClick = { isSettingsOpen = true }) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Settings",
              tint = SummerOnSurfaceVariant
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = SummerSurface
        )
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = innerPadding.calculateTopPadding())
    ) {
      val today = remember { LocalDate.now() }
      val yesterday = remember { today.minusDays(1) }

      val filteredDigests = remember(state.digests, activeEpisodeFilter) {
        if (activeEpisodeFilter == EpisodeFilter.ALL) {
          state.digests
        } else if (activeEpisodeFilter == EpisodeFilter.UNPLAYED) {
          state.digests.filter { !it.isPlayed }
        } else if (activeEpisodeFilter == EpisodeFilter.PLAYED) {
          state.digests.filter { it.isPlayed }
        } else if (activeEpisodeFilter == EpisodeFilter.ENGLISH) {
          state.digests.filter { it.language == PodcastLanguage.ENGLISH }
        } else {
          state.digests.filter { it.language == PodcastLanguage.SWEDISH }
        }
      }

      val dateGroups = remember(feedMode, state.filteredWebsites, filteredDigests) {
        val dateToEpisodes = mutableMapOf<LocalDate, MutableList<DigestEntry>>()
        val dateToWebsites = mutableMapOf<LocalDate, MutableList<SavedWebsite>>()

        if (feedMode == FeedMode.ALL || feedMode == FeedMode.EPISODES) {
          for (digest in filteredDigests) {
            val d = Instant.ofEpochMilli(digest.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            dateToEpisodes.getOrPut(d) { mutableListOf() }.add(digest)
          }
        }

        if (feedMode == FeedMode.ALL || feedMode == FeedMode.LINKS) {
          for (site in state.filteredWebsites) {
            val d = Instant.ofEpochMilli(site.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            dateToWebsites.getOrPut(d) { mutableListOf() }.add(site)
          }
        }

        val allDates = (dateToEpisodes.keys + dateToWebsites.keys).sortedDescending()

        allDates.map { date ->
          val label = when (date) {
            today -> "TODAY"
            yesterday -> "YESTERDAY"
            else -> date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())).uppercase()
          }
          FeedDateGroup(
            date = date,
            label = label,
            episodes = dateToEpisodes[date]?.sortedByDescending { it.createdAt } ?: emptyList(),
            websites = dateToWebsites[date]?.sortedByDescending { it.createdAt } ?: emptyList()
          )
        }
      }

      Column(modifier = Modifier.fillMaxSize()) {
        // Contextual Filter Chips bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (feedMode == FeedMode.ALL || feedMode == FeedMode.LINKS) {
            LinkFilter.entries.forEach { filter ->
              val selected = state.activeFilter == filter
              FilterChip(
                selected = selected,
                onClick = { viewModel.setFilter(filter) },
                label = {
                  Text(
                    text = if (filter == LinkFilter.UNREAD) {
                      "Unread (${state.unreadCount})"
                    } else if (filter == LinkFilter.ALL) {
                      "All Links"
                    } else {
                      "Archived"
                    },
                    style = MaterialTheme.typography.labelMedium
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SummerPrimaryContainer.copy(alpha = 0.2f),
                  selectedLabelColor = SummerPrimaryContainer,
                  containerColor = SummerSurfaceContainerHigh.copy(alpha = 0.6f),
                  labelColor = SummerOnSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selected,
                  borderColor = SummerBorderSubtle,
                  selectedBorderColor = SummerPrimaryContainer.copy(alpha = 0.5f)
                ),
                shape = CircleShape
              )
            }
          } else {
            EpisodeFilter.entries.forEach { filter ->
              val selected = activeEpisodeFilter == filter
              FilterChip(
                selected = selected,
                onClick = { activeEpisodeFilter = filter },
                label = {
                  Text(
                    text = filter.label,
                    style = MaterialTheme.typography.labelMedium
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SummerPrimaryContainer.copy(alpha = 0.2f),
                  selectedLabelColor = SummerPrimaryContainer,
                  containerColor = SummerSurfaceContainerHigh.copy(alpha = 0.6f),
                  labelColor = SummerOnSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selected,
                  borderColor = SummerBorderSubtle,
                  selectedBorderColor = SummerPrimaryContainer.copy(alpha = 0.5f)
                ),
                shape = CircleShape
              )
            }
          }
        }

        // Transient In-App Post-Save Banner (if recently saved)
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

        // Feed List / Empty State
        if (state.isInitialLoad) {
          // Show nothing while loading (avoids the empty view flash)
        } else if (dateGroups.isEmpty()) {
          EmptyFeedView(
            feedMode = feedMode,
            onAddLinkClick = { showAddLinkDialog = true },
            onGeneratePodcast = {
              ensureNotificationPermissionForPodcast()
              viewModel.triggerCatchMeUpDigest()
            }
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
              start = 16.dp,
              end = 16.dp,
              top = 6.dp,
              bottom = innerPadding.calculateBottomPadding() + 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            dateGroups.forEach { group ->
              item(key = "header_${group.date}") {
                val totalCount = group.episodes.size + group.websites.size
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = group.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = SummerOnSurfaceVariant,
                    letterSpacing = 1.sp
                  )
                  Text(
                    text = "$totalCount ${if (totalCount == 1) "ITEM" else "ITEMS"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                  )
                }
              }

              // Episodes first within date group
              items(group.episodes, key = { "episode_${it.id}" }) { episode ->
                DigestEpisodeCard(
                  digest = episode,
                  playback = state.podcastPlayback,
                  onOpenDigest = { viewModel.openSavedDigest(episode) },
                  onPlayOrGenerate = {
                    ensureNotificationPermissionForPodcast()
                    viewModel.playOrGenerateDigestPodcast(episode, forceRegenerate = false)
                  },
                  onDelete = { viewModel.deleteDigest(episode.id) }
                )
              }

              // Websites next within date group
              items(group.websites, key = { "link_${it.id}" }) { website ->
                var isDismissed by remember(website.id) { mutableStateOf(false) }

                AnimatedVisibility(
                  visible = !isDismissed,
                  enter = fadeIn() + expandVertically(),
                  exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top
                  ) + fadeOut(animationSpec = tween(150)),
                  modifier = Modifier.animateItem()
                ) {
                  val dismissState = rememberSwipeToDismissBoxState(
                    positionalThreshold = { distance -> distance * 0.55f },
                    confirmValueChange = { value ->
                      if (value == SwipeToDismissBoxValue.StartToEnd) {
                        isDismissed = true
                        viewModel.toggleArchive(website)
                        true
                      } else if (value == SwipeToDismissBoxValue.EndToStart) {
                        isDismissed = true
                        viewModel.deleteWebsite(website.id)
                        true
                      } else {
                        false
                      }
                    }
                  )

                  SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                      val direction = dismissState.dismissDirection
                      val bgColor = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                        SummerPrimaryContainer.copy(alpha = 0.35f)
                      } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                        SummerErrorContainer.copy(alpha = 0.55f)
                      } else {
                        Color.Transparent
                      }
                      Box(
                        modifier = Modifier
                          .fillMaxSize()
                          .clip(RoundedCornerShape(16.dp))
                          .background(bgColor)
                          .padding(horizontal = 20.dp),
                        contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                          Alignment.CenterStart
                        } else {
                          Alignment.CenterEnd
                        }
                      ) {
                        if (direction == SwipeToDismissBoxValue.StartToEnd) {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                              imageVector = if (website.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                              contentDescription = null,
                              tint = SummerPrimaryContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                              text = if (website.isArchived) "Unarchive" else "Archive",
                              style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = ManropeFontFamily,
                                fontWeight = FontWeight.Bold
                              ),
                              color = SummerPrimaryContainer
                            )
                          }
                        } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                              text = "Delete",
                              style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = ManropeFontFamily,
                                fontWeight = FontWeight.Bold
                              ),
                              color = SummerError
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                              imageVector = Icons.Default.DeleteOutline,
                              contentDescription = null,
                              tint = SummerError
                            )
                          }
                        }
                      }
                    },
                    content = {
                      WebsiteItemCard(
                        website = website,
                        isExpanded = website.id in state.expandedWebsiteIds,
                        onCardClick = { viewModel.onWebsiteCardClicked(website) },
                        onSummarizeClick = { viewModel.resummarizeWebsite(website.id) },
                        onToggleArchive = { viewModel.toggleArchive(website) },
                        onDelete = { viewModel.deleteWebsite(website.id) },
                        onOpenExternal = {
                          ExternalBrowserLauncher.openInExternalBrowser(
                            context = context,
                            url = website.url,
                            preferredBrowserPackage = state.settings.preferredBrowserPackage,
                            useCustomTabs = state.settings.useCustomTabs
                          )
                        }
                      )
                    }
                  )
                }
              }
            }
          }
        }
      }

      // Floating Interactive Bottom Dock / Mini-Player
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(
            bottom = innerPadding.calculateBottomPadding() + 16.dp,
            start = 16.dp,
            end = 16.dp
          ),
        contentAlignment = Alignment.BottomCenter
      ) {
        SummerFloatingDock(
          unreadCount = state.unreadCount,
          isGeneratingDigest = state.isGeneratingDigest,
          playback = state.podcastPlayback,
          onCatchMeUp = {
            ensureNotificationPermissionForPodcast()
            viewModel.triggerCatchMeUpDigest()
          },
          onTogglePlayPause = viewModel::togglePodcastPlayPause,
          onClosePlayer = viewModel::stopPodcastPlayback,
          onOpenPlayerDetails = viewModel::openActivePodcastDetails
        )
      }

      // Half-transparent scrim over navigation bar for 3-button / gesture navigation
      if (innerPadding.calculateBottomPadding() > 0.dp) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(innerPadding.calculateBottomPadding())
            .align(Alignment.BottomCenter)
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  SummerBackground.copy(alpha = 0.50f),
                  SummerBackground.copy(alpha = 0.85f)
                )
              )
            )
        )
      }
    }
  }

  // Catch Me Up Podcast Digest Modal Bottom Sheet
  state.activeDigestSheet?.let { activeDigest ->
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = viewModel::dismissDigestSheet,
      sheetState = sheetState,
      containerColor = SummerBackground
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
internal fun CatchMeUpDigestSheetContent(  digest: DigestEntry,
  settings: AppSettings,
  playback: PodcastPlaybackState,
  onPlayOrGeneratePodcast: (Boolean) -> Unit,
  onOpenSourceUrl: (String) -> Unit,
  onMarkAllArchived: () -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
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
      IconButton(onClick = onDismiss) {
        Icon(
          imageVector = Icons.Default.KeyboardArrowDown,
          contentDescription = "Dismiss",
          tint = SummerOnSurfaceVariant,
          modifier = Modifier.size(28.dp)
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = digest.episodeBadgeText,
          style = MaterialTheme.typography.labelSmall,
          color = SummerPrimaryContainer,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "•",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = dateStr,
          style = MaterialTheme.typography.labelSmall,
          color = SummerOnSurfaceVariant
        )
      }

      IconButton(
        onClick = {
          val shareText = buildString {
            appendLine("☀️ Summer Podcast Digest — $dateStr")
            appendLine("Episode ${digest.episodeNumber}: ${digest.displayTitle}")
            appendLine("Hosted by $hostsLabel (${if (digest.language == PodcastLanguage.SWEDISH) "Swedish" else "English"})")
            appendLine()
            appendLine("Sources covered (${digest.sourceWebsites.size}):")
            digest.sourceWebsites.forEachIndexed { i, s ->
              appendLine("${i + 1}. ${s.title} (${s.url})")
            }
            if (!digest.podcastScript.isNullOrBlank()) {
              appendLine()
              appendLine("Transcript preview:")
              appendLine(digest.podcastScript.take(300) + "...")
            }
          }

          val audioFile = digest.podcastAudioPath?.let { java.io.File(it) }
          if (audioFile != null && audioFile.exists()) {
            try {
              val contentUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                audioFile
              )
              val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, digest.displayTitle)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
              }
              context.startActivity(Intent.createChooser(sendIntent, "Share Podcast Audio File"))
              return@IconButton
            } catch (e: Exception) {
              android.util.Log.e("MainScreen", "Failed to share podcast audio file via FileProvider", e)
            }
          }

          // Fallback or script sharing as a Markdown file
          try {
            val mdFile = java.io.File(context.cacheDir, "summer-digest-ep${digest.episodeNumber}.md").apply {
              writeText(shareText)
            }
            val contentUri = androidx.core.content.FileProvider.getUriForFile(
              context,
              "${context.packageName}.fileprovider",
              mdFile
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/markdown"
              putExtra(Intent.EXTRA_STREAM, contentUri)
              putExtra(Intent.EXTRA_SUBJECT, digest.displayTitle)
              putExtra(Intent.EXTRA_TEXT, shareText)
              addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Podcast Digest File"))
          } catch (e: Exception) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Podcast Episode"))
          }
        }
      ) {
        Icon(
          imageVector = Icons.Default.Share,
          contentDescription = "Share",
          tint = SummerOnSurfaceVariant,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = if (digest.sourceWebsites.isNotEmpty()) {
        digest.sourceWebsites.first().title.ifBlank { "Daily AI Podcast Briefing" }
      } else "Daily AI Podcast Briefing",
      style = MaterialTheme.typography.headlineLarge,
      color = SummerOnSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = if (digest.language == PodcastLanguage.SWEDISH) "🇸🇪 Svenska" else "🇬🇧 English",
        style = MaterialTheme.typography.labelMedium,
        color = SummerOnSurfaceVariant
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(text = "•", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "🎙️ $hostsLabel",
        style = MaterialTheme.typography.labelMedium,
        color = SummerOnSurfaceVariant
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(text = "•", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "📰 ${digest.sourceWebsites.size} sources",
        style = MaterialTheme.typography.labelMedium,
        color = SummerOnSurfaceVariant
      )
    }

    Spacer(modifier = Modifier.height(18.dp))

    Surface(
      color = SummerSurfaceContainer,
      shape = RoundedCornerShape(18.dp),
      border = BorderStroke(1.dp, SummerBorderSubtle),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        when {
          digest.isGeneratingPodcast -> {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = SummerPrimaryContainer,
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = digest.podcastStatusText ?: "Scripting & synthesizing podcast...",
                  style = MaterialTheme.typography.bodySmall,
                  color = SummerOnSurface,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "Generating audio dialogue in the background...",
                  style = MaterialTheme.typography.labelSmall,
                  color = SummerOnSurfaceVariant
                )
              }
            }
          }
          digest.podcastError != null -> {
            Text(
              text = digest.podcastError,
              style = MaterialTheme.typography.bodySmall,
              color = SummerError
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = { onPlayOrGeneratePodcast(true) },
              colors = ButtonDefaults.buttonColors(
                containerColor = SummerPrimaryContainer,
                contentColor = SummerOnPrimaryContainer
              )
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Retry Podcast")
            }
          }
          digest.podcastAudioPath != null -> {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
              modifier = Modifier.fillMaxWidth()
            ) {
              IconButton(
                onClick = { /* Rewind 15s */ },
                modifier = Modifier.size(48.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.FastRewind,
                  contentDescription = "Rewind",
                  tint = SummerOnSurface,
                  modifier = Modifier.size(26.dp)
                )
              }

              Spacer(modifier = Modifier.width(20.dp))

              IconButton(
                onClick = { onPlayOrGeneratePodcast(false) },
                modifier = Modifier
                  .size(64.dp)
                  .clip(CircleShape)
                  .background(SummerPrimaryContainer)
              ) {
                Icon(
                  imageVector = if (isThisDigestPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                  contentDescription = if (isThisDigestPlaying) "Pause" else "Play",
                  tint = SummerOnPrimaryContainer,
                  modifier = Modifier.size(36.dp)
                )
              }

              Spacer(modifier = Modifier.width(20.dp))

              IconButton(
                onClick = { /* Forward 30s */ },
                modifier = Modifier.size(48.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.FastForward,
                  contentDescription = "Fast Forward",
                  tint = SummerOnSurface,
                  modifier = Modifier.size(26.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val posText = if (isThisDigestPlaying) formatDurationMs(playback.positionMs) else "00:00"
            val durText = if (isThisDigestPlaying && playback.durationMs > 0) formatDurationMs(playback.durationMs) else "--:--"
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(text = posText, style = MaterialTheme.typography.labelSmall, color = SummerPrimaryContainer)
              Text(text = durText, style = MaterialTheme.typography.labelSmall, color = SummerOnSurfaceVariant)
            }
          }
          else -> {
            Button(
              onClick = { onPlayOrGeneratePodcast(false) },
              colors = ButtonDefaults.buttonColors(
                containerColor = SummerPrimaryContainer,
                contentColor = SummerOnPrimaryContainer
              ),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text("Generate & Play Podcast Audio")
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = "SOURCES COVERED IN THIS EPISODE (${digest.sourceWebsites.size})",
      style = MaterialTheme.typography.labelSmall,
      color = SummerOnSurfaceVariant,
      letterSpacing = 1.sp
    )
    Spacer(modifier = Modifier.height(8.dp))

    Column(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      digest.sourceWebsites.forEachIndexed { index, source ->
        Surface(
          color = SummerSurfaceContainer,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, SummerBorderSubtle),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenSourceUrl(source.url) }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            val isInspection = LocalInspectionMode.current
            val faviconPlaceholder = if (isInspection) {
              painterResource(R.drawable.ic_summer_logo)
            } else {
              null
            }
            AsyncImage(
              model = source.faviconUrl,
              contentDescription = null,
              placeholder = faviconPlaceholder,
              error = faviconPlaceholder,
              modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = source.title.ifBlank { source.domain },
                style = MaterialTheme.typography.bodyMedium,
                color = SummerOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "[${index + 1}] ${source.domain}",
                style = MaterialTheme.typography.labelSmall,
                color = SummerOnSurfaceVariant
              )
            }
            Icon(
              imageVector = Icons.AutoMirrored.Filled.OpenInNew,
              contentDescription = "Open source link",
              modifier = Modifier.size(16.dp),
              tint = SummerOnSurfaceVariant
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(22.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      OutlinedButton(
        onClick = onDismiss,
        border = BorderStroke(1.dp, SummerBorderSubtle),
        modifier = Modifier.weight(1f)
      ) {
        Text("Close", color = SummerOnSurface)
      }

      Button(
        onClick = onMarkAllArchived,
        colors = ButtonDefaults.buttonColors(
          containerColor = SummerPrimaryContainer,
          contentColor = SummerOnPrimaryContainer
        ),
        modifier = Modifier.weight(1.4f)
      ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Archive Sources")
      }
    }

    if (digest.podcastAudioPath != null && !digest.isGeneratingPodcast) {
      Spacer(modifier = Modifier.height(8.dp))
      TextButton(
        onClick = { onPlayOrGeneratePodcast(true) },
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = SummerPrimaryContainer)
        Spacer(Modifier.width(6.dp))
        Text("Regenerate Audio", color = SummerPrimaryContainer)
      }
    }

    Spacer(modifier = Modifier.height(28.dp))
  }
}

@Composable
internal fun SettingsTab(
  settings: AppSettings,
  verification: VerificationUiState,
  bottomPadding: androidx.compose.ui.unit.Dp = 16.dp,
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
      .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = bottomPadding),
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    // Summarization & Session Auto-Podcast Behavior Card
    SettingsSectionCard(
      title = "Summarization & Auto-Podcast",
      subtitle = "Automate reading digests and background episode creation",
      icon = Icons.Default.AutoAwesome
    ) {
      SettingsSwitchRow(
        title = "Auto-summarize text on save",
        subtitle = "Automatically generates Markdown AI summary after saving a link",
        checked = settings.autoSummarizeOnSave,
        onCheckedChange = { enabled ->
          onUpdateSettings { it.copy(autoSummarizeOnSave = enabled) }
        }
      )

      HorizontalDivider(
        color = SummerBorderSubtle.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 12.dp)
      )

      SettingsSwitchRow(
        title = "Auto-podcast after browsing session",
        subtitle = "Silently groups session links, generates a Catch Me Up episode, and alerts you with Play",
        checked = settings.autoPodcastAfterQuietWindow,
        onCheckedChange = { enabled ->
          onUpdateSettings { it.copy(autoPodcastAfterQuietWindow = enabled) }
        }
      )

      AnimatedVisibility(visible = settings.autoPodcastAfterQuietWindow) {
        Surface(
          color = SummerSurfaceDim,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, SummerBorderSubtle),
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            // Session Quiet Window Slider
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Start podcast after session quiet-window",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = ManropeFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                  ),
                  color = SummerOnSurface,
                  modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "${settings.autoPodcastQuietMinutes} min after last saved link",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = ManropeFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End
                  ),
                  color = SummerPrimaryContainer
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Slider(
                value = settings.autoPodcastQuietMinutes.toFloat(),
                onValueChange = { onUpdateSettings { s -> s.copy(autoPodcastQuietMinutes = it.roundToInt()) } },
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(
                  thumbColor = SummerPrimaryContainer,
                  activeTrackColor = SummerPrimaryContainer,
                  inactiveTrackColor = SummerSurfaceBright
                )
              )
            }

            // Minimum Session Links Slider
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Minimum session burst threshold",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = ManropeFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                  ),
                  color = SummerOnSurface,
                  modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "${settings.autoPodcastMinLinks} links",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = ManropeFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End
                  ),
                  color = SummerPrimaryContainer
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Slider(
                value = settings.autoPodcastMinLinks.toFloat(),
                onValueChange = { onUpdateSettings { s -> s.copy(autoPodcastMinLinks = it.roundToInt()) } },
                valueRange = 1f..8f,
                steps = 6,
                colors = SliderDefaults.colors(
                  thumbColor = SummerPrimaryContainer,
                  activeTrackColor = SummerPrimaryContainer,
                  inactiveTrackColor = SummerSurfaceBright
                )
              )
            }
          }
        }
      }
    }

    // -------------------------------------------------------------
    // Section 2: Podcast Hosts, Language & Voices
    // -------------------------------------------------------------
    SettingsSectionCard(
      title = "Podcast Hosts, Language & Voices",
      subtitle = "Acoustic profile and multi-voice generation orchestrator",
      icon = Icons.Default.GraphicEq
    ) {
      // Format & Style
      Text(
        text = "PODCAST FORMAT & STYLE",
        style = MaterialTheme.typography.labelSmall.copy(
          fontFamily = ManropeFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          letterSpacing = 0.06.sp
        ),
        color = SummerOnSurfaceVariant
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        PodcastStyle.entries.forEach { style ->
          val isSelected = settings.podcastStyle == style
          FilterChip(
            selected = isSelected,
            onClick = { onUpdateSettings { it.copy(podcastStyle = style) } },
            label = {
              Text(
                text = if (style == PodcastStyle.MKBHD_WAVEFORM) "🎙️ Waveform (MKBHD)" else "☀️ Standard Briefing",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontFamily = ManropeFontFamily,
                  fontSize = 13.sp,
                  fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SummerPrimaryContainer.copy(alpha = 0.2f),
              selectedLabelColor = SummerPrimaryContainer,
              containerColor = SummerSurfaceContainerHigh.copy(alpha = 0.6f),
              labelColor = SummerOnSurfaceVariant
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = isSelected,
              borderColor = SummerBorderSubtle,
              selectedBorderColor = SummerPrimaryContainer.copy(alpha = 0.5f)
            ),
            shape = CircleShape
          )
        }
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = settings.podcastStyle.subtitle,
        style = MaterialTheme.typography.bodySmall.copy(
          fontFamily = ManropeFontFamily,
          fontSize = 12.sp
        ),
        color = SummerPrimaryContainer
      )

      HorizontalDivider(
        color = SummerBorderSubtle.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 14.dp)
      )

      // Episode Language
      Text(
        text = "EPISODE LANGUAGE",
        style = MaterialTheme.typography.labelSmall.copy(
          fontFamily = ManropeFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          letterSpacing = 0.06.sp
        ),
        color = SummerOnSurfaceVariant
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        listOf(PodcastLanguage.ENGLISH to "• English (EN)", PodcastLanguage.SWEDISH to "Svenska (SV)").forEach { (lang, label) ->
          val isSelected = settings.podcastLanguage == lang
          FilterChip(
            selected = isSelected,
            onClick = { onUpdateSettings { it.copy(podcastLanguage = lang) } },
            label = {
              Text(
                text = if (isSelected) label else if (lang == PodcastLanguage.ENGLISH) "English (EN)" else "Svenska (SV)",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontFamily = ManropeFontFamily,
                  fontSize = 13.sp,
                  fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SummerPrimaryContainer.copy(alpha = 0.2f),
              selectedLabelColor = SummerPrimaryContainer,
              containerColor = SummerSurfaceContainerHigh.copy(alpha = 0.6f),
              labelColor = SummerOnSurfaceVariant
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = isSelected,
              borderColor = SummerBorderSubtle,
              selectedBorderColor = SummerPrimaryContainer.copy(alpha = 0.5f)
            ),
            shape = CircleShape
          )
        }
      }
      if (settings.podcastLanguage == PodcastLanguage.SWEDISH) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "All saved articles (even English ones) are translated and discussed in natural Swedish.",
          style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = ManropeFontFamily,
            fontSize = 12.sp
          ),
          color = SummerPrimaryContainer
        )
      }

      HorizontalDivider(
        color = SummerBorderSubtle.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 14.dp)
      )

      // Host Cast
      Text(
        text = "HOST CAST",
        style = MaterialTheme.typography.labelSmall.copy(
          fontFamily = ManropeFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          letterSpacing = 0.06.sp
        ),
        color = SummerOnSurfaceVariant
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        listOf(2 to "2 Hosts (Duo)", 3 to "3 Hosts (Roundtable)").forEach { (count, label) ->
          val isSelected = settings.podcastHostCount.coerceIn(2, 3) == count
          FilterChip(
            selected = isSelected,
            onClick = { onUpdateSettings { it.copy(podcastHostCount = count) } },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = if (isSelected) SummerPrimaryContainer else SummerOnSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
            },
            label = {
              Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                  fontFamily = ManropeFontFamily,
                  fontSize = 13.sp,
                  fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SummerPrimaryContainer.copy(alpha = 0.2f),
              selectedLabelColor = SummerPrimaryContainer,
              containerColor = SummerSurfaceContainerHigh.copy(alpha = 0.6f),
              labelColor = SummerOnSurfaceVariant
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = isSelected,
              borderColor = SummerBorderSubtle,
              selectedBorderColor = SummerPrimaryContainer.copy(alpha = 0.5f)
            ),
            shape = CircleShape
          )
        }
      }

      HorizontalDivider(
        color = SummerBorderSubtle.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 14.dp)
      )

      // Randomize Host names & voices
      SettingsSwitchRow(
        title = "Randomize host names & voices each episode",
        subtitle = "Picks contrasting voices and matching names automatically",
        checked = settings.randomizePodcastHosts,
        onCheckedChange = { enabled ->
          onUpdateSettings { it.copy(randomizePodcastHosts = enabled) }
        }
      )

      AnimatedVisibility(visible = !settings.randomizePodcastHosts) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedTextField(
              value = settings.hostAName,
              onValueChange = { name -> onUpdateSettings { it.copy(hostAName = name) } },
              label = { Text("Host 1 Name", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
              textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SummerPrimaryContainer,
                unfocusedBorderColor = SummerBorderSubtle,
                focusedLabelColor = SummerPrimaryContainer,
                unfocusedLabelColor = SummerTextSecondary,
                cursorColor = SummerPrimaryContainer
              ),
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = settings.hostBName,
              onValueChange = { name -> onUpdateSettings { it.copy(hostBName = name) } },
              label = { Text("Host 2 Name", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
              textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SummerPrimaryContainer,
                unfocusedBorderColor = SummerBorderSubtle,
                focusedLabelColor = SummerPrimaryContainer,
                unfocusedLabelColor = SummerTextSecondary,
                cursorColor = SummerPrimaryContainer
              ),
              modifier = Modifier.weight(1f)
            )
          }
          if (settings.podcastHostCount >= 3) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = settings.hostCName,
              onValueChange = { name -> onUpdateSettings { it.copy(hostCName = name) } },
              label = { Text("Host 3 Name", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
              textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SummerPrimaryContainer,
                unfocusedBorderColor = SummerBorderSubtle,
                focusedLabelColor = SummerPrimaryContainer,
                unfocusedLabelColor = SummerTextSecondary,
                cursorColor = SummerPrimaryContainer
              ),
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }

      HorizontalDivider(
        color = SummerBorderSubtle.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 14.dp)
      )

      // TTS Providers List
      Text(
        text = "TTS PROVIDER",
        style = MaterialTheme.typography.labelSmall.copy(
          fontFamily = ManropeFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          letterSpacing = 0.06.sp
        ),
        color = SummerOnSurfaceVariant
      )
      Spacer(modifier = Modifier.height(10.dp))

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsRadioCard(
          selected = settings.podcastTtsProvider == PodcastTtsProvider.GEMINI_MULTISPEAKER,
          title = "Gemini Multi-Speaker TTS",
          subtitle = "Native multi-host dialogue via Google AI Studio",
          badgeText = "FREE TIER",
          codeTag = "gemini-3.8-flash-tts",
          onClick = { onUpdateSettings { it.copy(podcastTtsProvider = PodcastTtsProvider.GEMINI_MULTISPEAKER) } }
        )
        SettingsRadioCard(
          selected = settings.podcastTtsProvider == PodcastTtsProvider.ANDROID_ON_DEVICE,
          title = "Android On-Device Multi-Voice TTS",
          subtitle = "100% free & offline system voices",
          onClick = { onUpdateSettings { it.copy(podcastTtsProvider = PodcastTtsProvider.ANDROID_ON_DEVICE) } }
        )
        SettingsRadioCard(
          selected = settings.podcastTtsProvider == PodcastTtsProvider.AZURE_SPEECH,
          title = "Microsoft Azure AI Speech",
          subtitle = "500k free neural chars/month",
          onClick = { onUpdateSettings { it.copy(podcastTtsProvider = PodcastTtsProvider.AZURE_SPEECH) } }
        )
        SettingsRadioCard(
          selected = settings.podcastTtsProvider == PodcastTtsProvider.ELEVENLABS,
          title = "ElevenLabs Neural TTS",
          subtitle = "Ultra-realistic voices",
          onClick = { onUpdateSettings { it.copy(podcastTtsProvider = PodcastTtsProvider.ELEVENLABS) } }
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Fallback Switch
      SettingsSwitchRow(
        title = "Auto-fallback to On-Device Multi-Voice TTS",
        subtitle = "If a cloud TTS key isn't configured yet or rate-limits, automatically fall back to free offline Android multi-voice synthesis.",
        checked = settings.fallbackToOnDeviceTts,
        onCheckedChange = { enabled ->
          onUpdateSettings { it.copy(fallbackToOnDeviceTts = enabled) }
        }
      )

      HorizontalDivider(
        color = SummerBorderSubtle,
        modifier = Modifier.padding(vertical = 12.dp)
      )

      val isThreeHosts = settings.podcastHostCount >= 3

      // Active TTS Provider Configuration Details
      if (settings.podcastTtsProvider == PodcastTtsProvider.GEMINI_MULTISPEAKER) {
          val resolvedVoiceA = GeminiCatalog.resolveValidVoice(settings.geminiHostAVoice, "Puck")
          val resolvedVoiceB = GeminiCatalog.resolveValidVoice(settings.geminiHostBVoice, "Kore")
          val resolvedVoiceC = GeminiCatalog.resolveValidVoice(settings.geminiHostCVoice, "Aoede")
          val voiceOptions = remember {
            GeminiCatalog.PREBUILT_VOICES.map { it.name to it.displayLabel }
          }
          val ttsModelOptions = remember { GeminiCatalog.TTS_MODELS }

          Text(
            text = "Gemini Multi-Speaker TTS (Free via Google AI Studio)",
            style = MaterialTheme.typography.bodyLarge.copy(
              fontFamily = ManropeFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp
            ),
            color = SummerOnSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Uses Google's native multiSpeakerVoiceConfig across all 30 official Gemini studio voices in English and Swedish.",
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = ManropeFontFamily,
              fontSize = 12.sp,
              lineHeight = 17.sp
            ),
            color = SummerOnSurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = settings.directGeminiApiKey,
            onValueChange = { value ->
              onUpdateSettings { it.copy(directGeminiApiKey = value) }
            },
            label = { Text("Google AI Studio API Key", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
            placeholder = { Text("AIzaSy...", fontFamily = ManropeFontFamily, fontSize = 13.sp) },
            textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SummerPrimaryContainer,
              unfocusedBorderColor = SummerBorderSubtle,
              focusedLabelColor = SummerPrimaryContainer,
              unfocusedLabelColor = SummerTextSecondary,
              cursorColor = SummerPrimaryContainer
            ),
            visualTransformation = if (showDirectGeminiKey) {
              VisualTransformation.None
            } else {
              PasswordVisualTransformation()
            },
            trailingIcon = {
              IconButton(onClick = { showDirectGeminiKey = !showDirectGeminiKey }) {
                Icon(
                  imageVector = if (showDirectGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = "Toggle API key visibility",
                  tint = SummerTextSecondary
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
          Spacer(modifier = Modifier.height(14.dp))
          Button(
            enabled = !verification.isVerifyingTts,
            onClick = onVerifyGeminiTts,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = SummerPrimaryContainer,
              contentColor = SummerOnPrimaryContainer
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp)
          ) {
            if (verification.isVerifyingTts) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SummerOnPrimaryContainer)
              Spacer(Modifier.width(8.dp))
              Text(
                "Testing Multi-Speaker TTS...",
                color = SummerOnPrimaryContainer,
                fontFamily = ManropeFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
              )
            } else {
              Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = SummerOnPrimaryContainer
              )
              Spacer(Modifier.width(8.dp))
              Text(
                "Verify Multi-Speaker TTS",
                color = SummerOnPrimaryContainer,
                fontFamily = ManropeFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
          verification.ttsMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            VerificationStatusBox(message = msg, isError = verification.ttsIsError)
          }
        } else if (settings.podcastTtsProvider == PodcastTtsProvider.ANDROID_ON_DEVICE) {
          AndroidOnDeviceTtsSettingsSection(
            settings = settings,
            onUpdateSettings = onUpdateSettings
          )
        } else if (settings.podcastTtsProvider == PodcastTtsProvider.AZURE_SPEECH) {
          Text(
            text = "Microsoft Azure AI Speech (500,000 Free Neural Chars/Month)",
            style = MaterialTheme.typography.bodyLarge.copy(
              fontFamily = ManropeFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp
            ),
            color = SummerOnSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Azure Speech F0 tier offers 500,000 free neural characters every month perpetually and supports multi-voice SSML in English and Swedish.",
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = ManropeFontFamily,
              fontSize = 12.sp,
              lineHeight = 17.sp
            ),
            color = SummerOnSurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = settings.azureSpeechKey,
            onValueChange = { v -> onUpdateSettings { it.copy(azureSpeechKey = v) } },
            label = { Text("Azure Speech Subscription Key", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
            textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SummerPrimaryContainer,
              unfocusedBorderColor = SummerBorderSubtle,
              focusedLabelColor = SummerPrimaryContainer,
              unfocusedLabelColor = SummerTextSecondary,
              cursorColor = SummerPrimaryContainer
            ),
            visualTransformation = if (showAzureKey) {
              VisualTransformation.None
            } else {
              PasswordVisualTransformation()
            },
            trailingIcon = {
              IconButton(onClick = { showAzureKey = !showAzureKey }) {
                Icon(
                  imageVector = if (showAzureKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = "Toggle key visibility",
                  tint = SummerTextSecondary
                )
              }
            },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = settings.azureSpeechRegion,
            onValueChange = { v -> onUpdateSettings { it.copy(azureSpeechRegion = v) } },
            label = { Text("Azure Region (e.g. eastus, westeurope, swedencentral)", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
            textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SummerPrimaryContainer,
              unfocusedBorderColor = SummerBorderSubtle,
              focusedLabelColor = SummerPrimaryContainer,
              unfocusedLabelColor = SummerTextSecondary,
              cursorColor = SummerPrimaryContainer
            ),
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
                label = { Text("${settings.hostAName.ifBlank { "Host 1" }} Neural Voice", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
                textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = SummerPrimaryContainer,
                  unfocusedBorderColor = SummerBorderSubtle,
                  focusedLabelColor = SummerPrimaryContainer,
                  unfocusedLabelColor = SummerTextSecondary,
                  cursorColor = SummerPrimaryContainer
                ),
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = settings.azureHostBVoice,
                onValueChange = { v -> onUpdateSettings { it.copy(azureHostBVoice = v) } },
                label = { Text("${settings.hostBName.ifBlank { "Host 2" }} Neural Voice", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
                textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = SummerPrimaryContainer,
                  unfocusedBorderColor = SummerBorderSubtle,
                  focusedLabelColor = SummerPrimaryContainer,
                  unfocusedLabelColor = SummerTextSecondary,
                  cursorColor = SummerPrimaryContainer
                ),
                modifier = Modifier.weight(1f)
              )
            }
            if (isThreeHosts) {
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                value = settings.azureHostCVoice,
                onValueChange = { v -> onUpdateSettings { it.copy(azureHostCVoice = v) } },
                label = { Text("${settings.hostCName.ifBlank { "Host 3" }} Neural Voice", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
                textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = SummerPrimaryContainer,
                  unfocusedBorderColor = SummerBorderSubtle,
                  focusedLabelColor = SummerPrimaryContainer,
                  unfocusedLabelColor = SummerTextSecondary,
                  cursorColor = SummerPrimaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        } else if (settings.podcastTtsProvider == PodcastTtsProvider.ELEVENLABS) {
          Text(
            text = "ElevenLabs Neural TTS",
            style = MaterialTheme.typography.bodyLarge.copy(
              fontFamily = ManropeFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp
            ),
            color = SummerOnSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Create a free account at elevenlabs.io (~10,000 free credits/month). Uses eleven_flash_v2_5 by default for fast multilingual synthesis.",
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = ManropeFontFamily,
              fontSize = 12.sp,
              lineHeight = 17.sp
            ),
            color = SummerOnSurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = settings.elevenLabsApiKey,
            onValueChange = { v -> onUpdateSettings { it.copy(elevenLabsApiKey = v) } },
            label = { Text("ElevenLabs API Key (xi-api-key)", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
            textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SummerPrimaryContainer,
              unfocusedBorderColor = SummerBorderSubtle,
              focusedLabelColor = SummerPrimaryContainer,
              unfocusedLabelColor = SummerTextSecondary,
              cursorColor = SummerPrimaryContainer
            ),
            visualTransformation = if (showElevenLabsKey) {
              VisualTransformation.None
            } else {
              PasswordVisualTransformation()
            },
            trailingIcon = {
              IconButton(onClick = { showElevenLabsKey = !showElevenLabsKey }) {
                Icon(
                  imageVector = if (showElevenLabsKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = "Toggle key visibility",
                  tint = SummerTextSecondary
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
                label = { Text("${settings.hostAName.ifBlank { "Host 1" }} Voice ID", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
                textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = SummerPrimaryContainer,
                  unfocusedBorderColor = SummerBorderSubtle,
                  focusedLabelColor = SummerPrimaryContainer,
                  unfocusedLabelColor = SummerTextSecondary,
                  cursorColor = SummerPrimaryContainer
                ),
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = settings.elevenLabsHostBVoiceId,
                onValueChange = { v -> onUpdateSettings { it.copy(elevenLabsHostBVoiceId = v) } },
                label = { Text("${settings.hostBName.ifBlank { "Host 2" }} Voice ID", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
                textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = SummerPrimaryContainer,
                  unfocusedBorderColor = SummerBorderSubtle,
                  focusedLabelColor = SummerPrimaryContainer,
                  unfocusedLabelColor = SummerTextSecondary,
                  cursorColor = SummerPrimaryContainer
                ),
                modifier = Modifier.weight(1f)
              )
            }
            if (isThreeHosts) {
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                value = settings.elevenLabsHostCVoiceId,
                onValueChange = { v -> onUpdateSettings { it.copy(elevenLabsHostCVoiceId = v) } },
                label = { Text("${settings.hostCName.ifBlank { "Host 3" }} Voice ID", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
                textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = SummerPrimaryContainer,
                  unfocusedBorderColor = SummerBorderSubtle,
                  focusedLabelColor = SummerPrimaryContainer,
                  unfocusedLabelColor = SummerTextSecondary,
                  cursorColor = SummerPrimaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        } else if (settings.podcastTtsProvider == PodcastTtsProvider.OPENAI_COMPATIBLE_TTS) {
          Text(
            text = "OpenAI / Kokoro / Custom /v1/audio/speech",
            style = MaterialTheme.typography.bodyLarge.copy(
              fontFamily = ManropeFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp
            ),
            color = SummerOnSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Supports OpenAI Audio (gpt-4o-mini-tts / tts-1 with voices like onyx, nova, shimmer) or free self-hosted Kokoro-FastAPI / Groq endpoints.",
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = ManropeFontFamily,
              fontSize = 12.sp,
              lineHeight = 17.sp
            ),
            color = SummerOnSurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = settings.openAiTtsBaseUrl,
            onValueChange = { v -> onUpdateSettings { it.copy(openAiTtsBaseUrl = v) } },
            label = { Text("TTS Base URL", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
            placeholder = { Text("https://api.openai.com/v1", fontFamily = ManropeFontFamily, fontSize = 13.sp) },
            textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SummerPrimaryContainer,
              unfocusedBorderColor = SummerBorderSubtle,
              focusedLabelColor = SummerPrimaryContainer,
              unfocusedLabelColor = SummerTextSecondary,
              cursorColor = SummerPrimaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = settings.openAiTtsApiKey,
            onValueChange = { v -> onUpdateSettings { it.copy(openAiTtsApiKey = v) } },
            label = { Text("TTS API Key (leave blank to reuse OpenAI Chat key)", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
            textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SummerPrimaryContainer,
              unfocusedBorderColor = SummerBorderSubtle,
              focusedLabelColor = SummerPrimaryContainer,
              unfocusedLabelColor = SummerTextSecondary,
              cursorColor = SummerPrimaryContainer
            ),
            visualTransformation = if (showOpenAiKey) {
              VisualTransformation.None
            } else {
              PasswordVisualTransformation()
            },
            trailingIcon = {
              IconButton(onClick = { showOpenAiKey = !showOpenAiKey }) {
                Icon(
                  imageVector = if (showOpenAiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = "Toggle key visibility",
                  tint = SummerTextSecondary
                )
              }
            },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = settings.openAiTtsModel,
            onValueChange = { v -> onUpdateSettings { it.copy(openAiTtsModel = v) } },
            label = { Text("TTS Model", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
            placeholder = { Text("gpt-4o-mini-tts / tts-1 / kokoro", fontFamily = ManropeFontFamily, fontSize = 13.sp) },
            textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SummerPrimaryContainer,
              unfocusedBorderColor = SummerBorderSubtle,
              focusedLabelColor = SummerPrimaryContainer,
              unfocusedLabelColor = SummerTextSecondary,
              cursorColor = SummerPrimaryContainer
            ),
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
                label = { Text("${settings.hostAName.ifBlank { "Host 1" }} Voice", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
                placeholder = { Text("onyx", fontFamily = ManropeFontFamily, fontSize = 13.sp) },
                textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = SummerPrimaryContainer,
                  unfocusedBorderColor = SummerBorderSubtle,
                  focusedLabelColor = SummerPrimaryContainer,
                  unfocusedLabelColor = SummerTextSecondary,
                  cursorColor = SummerPrimaryContainer
                ),
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = settings.openAiHostBVoice,
                onValueChange = { v -> onUpdateSettings { it.copy(openAiHostBVoice = v) } },
                label = { Text("${settings.hostBName.ifBlank { "Host 2" }} Voice", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
                placeholder = { Text("nova", fontFamily = ManropeFontFamily, fontSize = 13.sp) },
                textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = SummerPrimaryContainer,
                  unfocusedBorderColor = SummerBorderSubtle,
                  focusedLabelColor = SummerPrimaryContainer,
                  unfocusedLabelColor = SummerTextSecondary,
                  cursorColor = SummerPrimaryContainer
                ),
                modifier = Modifier.weight(1f)
              )
            }
            if (isThreeHosts) {
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                value = settings.openAiHostCVoice,
                onValueChange = { v -> onUpdateSettings { it.copy(openAiHostCVoice = v) } },
                label = { Text("${settings.hostCName.ifBlank { "Host 3" }} Voice", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
                placeholder = { Text("shimmer", fontFamily = ManropeFontFamily, fontSize = 13.sp) },
                textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = SummerPrimaryContainer,
                  unfocusedBorderColor = SummerBorderSubtle,
                  focusedLabelColor = SummerPrimaryContainer,
                  unfocusedLabelColor = SummerTextSecondary,
                  cursorColor = SummerPrimaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        }
    }

    // -------------------------------------------------------------
    // Section 3: AI Intelligence Backend
    // -------------------------------------------------------------
    SettingsSectionCard(
      title = "AI Intelligence Backend",
      subtitle = "Model used to parse HTML and author multi-speaker dialogue",
      icon = Icons.Default.Psychology
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsRadioCard(
          selected = settings.selectedBackend == AiBackend.FIREBASE_AI,
          title = "Firebase AI Logic",
          subtitle = "Connected via app project",
          badgeText = "gemini-3.8-flash",
          onClick = { onUpdateSettings { it.copy(selectedBackend = AiBackend.FIREBASE_AI) } }
        )
        SettingsRadioCard(
          selected = settings.selectedBackend == AiBackend.DIRECT_GEMINI,
          title = "Direct Gemini API",
          subtitle = "Custom key with full rate limits",
          badgeText = "BYOK",
          onClick = { onUpdateSettings { it.copy(selectedBackend = AiBackend.DIRECT_GEMINI) } }
        )
        SettingsRadioCard(
          selected = settings.selectedBackend == AiBackend.GEMINI_NANO_ON_DEVICE,
          title = "On-Device Gemini Nano",
          subtitle = "Offline private processing",
          badgeText = "AICore NPU",
          onClick = { onUpdateSettings { it.copy(selectedBackend = AiBackend.GEMINI_NANO_ON_DEVICE) } }
        )
        SettingsRadioCard(
          selected = settings.selectedBackend == AiBackend.OPENAI_COMPATIBLE,
          title = "OpenAI / Anthropic Compatible",
          subtitle = "Custom endpoints for Claude 3.5 & GPT-4o",
          onClick = { onUpdateSettings { it.copy(selectedBackend = AiBackend.OPENAI_COMPATIBLE) } }
        )
      }

      HorizontalDivider(
        color = SummerBorderSubtle.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 14.dp)
      )

      // Provider-specific Configuration Fields
      if (settings.selectedBackend == AiBackend.FIREBASE_AI) {
          Text(
            text = "Firebase AI Logic Configuration",
            style = MaterialTheme.typography.bodyLarge.copy(
              fontFamily = ManropeFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp
            ),
            color = SummerOnSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Connected to Firebase AI Logic (via app/google-services.json).",
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = ManropeFontFamily,
              fontSize = 12.sp
            ),
            color = SummerPrimaryContainer
          )
          Spacer(modifier = Modifier.height(10.dp))
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
            label = { Text("Gemini Model ID (or enter custom)", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
            supportingText = { Text("Recommended: gemini-3.8-flash", fontFamily = ManropeFontFamily, fontSize = 11.sp) },
            textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SummerPrimaryContainer,
              unfocusedBorderColor = SummerBorderSubtle,
              focusedLabelColor = SummerPrimaryContainer,
              unfocusedLabelColor = SummerTextSecondary,
              cursorColor = SummerPrimaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
          )
        } else if (settings.selectedBackend == AiBackend.DIRECT_GEMINI) {
          Text(
            text = "Direct Gemini API (Google AI Studio)",
            style = MaterialTheme.typography.bodyLarge.copy(
              fontFamily = ManropeFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp
            ),
            color = SummerOnSurface
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = settings.directGeminiApiKey,
            onValueChange = { value ->
              onUpdateSettings { it.copy(directGeminiApiKey = value) }
            },
            label = { Text("Google AI Studio API Key", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
            placeholder = { Text("AIzaSy...", fontFamily = ManropeFontFamily, fontSize = 13.sp) },
            textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SummerPrimaryContainer,
              unfocusedBorderColor = SummerBorderSubtle,
              focusedLabelColor = SummerPrimaryContainer,
              unfocusedLabelColor = SummerTextSecondary,
              cursorColor = SummerPrimaryContainer
            ),
            visualTransformation = if (showDirectGeminiKey) {
              VisualTransformation.None
            } else {
              PasswordVisualTransformation()
            },
            trailingIcon = {
              IconButton(onClick = { showDirectGeminiKey = !showDirectGeminiKey }) {
                Icon(
                  imageVector = if (showDirectGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = "Toggle API key visibility",
                  tint = SummerTextSecondary
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
            label = { Text("Gemini Model ID (or enter custom)", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
            textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SummerPrimaryContainer,
              unfocusedBorderColor = SummerBorderSubtle,
              focusedLabelColor = SummerPrimaryContainer,
              unfocusedLabelColor = SummerTextSecondary,
              cursorColor = SummerPrimaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
          )
        } else if (settings.selectedBackend == AiBackend.GEMINI_NANO_ON_DEVICE) {
          Text(
            text = "On-Device Gemini Nano (Android AICore)",
            style = MaterialTheme.typography.bodyLarge.copy(
              fontFamily = ManropeFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp
            ),
            color = SummerOnSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Runs 100% locally on your phone via Google AI Edge AICore SDK. Requires a compatible flagship device (e.g., Pixel 8/9/10 series or Galaxy S24/S25+) with AICore enabled in system settings.",
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = ManropeFontFamily,
              fontSize = 12.sp,
              lineHeight = 17.sp
            ),
            color = SummerOnSurfaceVariant
          )
        } else if (settings.selectedBackend == AiBackend.OPENAI_COMPATIBLE) {
          Text(
            text = "OpenAI / Anthropic / Custom Endpoint",
            style = MaterialTheme.typography.bodyLarge.copy(
              fontFamily = ManropeFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp
            ),
            color = SummerOnSurface
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = settings.openAiBaseUrl,
            onValueChange = { value ->
              onUpdateSettings { it.copy(openAiBaseUrl = value) }
            },
            label = { Text("Base URL", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
            placeholder = { Text("https://api.openai.com/v1", fontFamily = ManropeFontFamily, fontSize = 13.sp) },
            textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SummerPrimaryContainer,
              unfocusedBorderColor = SummerBorderSubtle,
              focusedLabelColor = SummerPrimaryContainer,
              unfocusedLabelColor = SummerTextSecondary,
              cursorColor = SummerPrimaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = settings.openAiApiKey,
            onValueChange = { value ->
              onUpdateSettings { it.copy(openAiApiKey = value) }
            },
            label = { Text("API Key", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
            placeholder = { Text("sk-...", fontFamily = ManropeFontFamily, fontSize = 13.sp) },
            textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SummerPrimaryContainer,
              unfocusedBorderColor = SummerBorderSubtle,
              focusedLabelColor = SummerPrimaryContainer,
              unfocusedLabelColor = SummerTextSecondary,
              cursorColor = SummerPrimaryContainer
            ),
            visualTransformation = if (showOpenAiKey) {
              VisualTransformation.None
            } else {
              PasswordVisualTransformation()
            },
            trailingIcon = {
              IconButton(onClick = { showOpenAiKey = !showOpenAiKey }) {
                Icon(
                  imageVector = if (showOpenAiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = "Toggle key visibility",
                  tint = SummerTextSecondary
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
            label = { Text("Model Name", fontFamily = ManropeFontFamily, fontSize = 12.sp) },
            placeholder = { Text("gpt-4o-mini / claude-3-5-haiku-latest", fontFamily = ManropeFontFamily, fontSize = 13.sp) },
            textStyle = TextStyle(fontFamily = ManropeFontFamily, fontSize = 13.sp, color = SummerOnSurface),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SummerPrimaryContainer,
              unfocusedBorderColor = SummerBorderSubtle,
              focusedLabelColor = SummerPrimaryContainer,
              unfocusedLabelColor = SummerTextSecondary,
              cursorColor = SummerPrimaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }

      Spacer(modifier = Modifier.height(14.dp))
      Button(
        enabled = !verification.isVerifyingBackend,
        onClick = onVerifyTextBackend,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = SummerPrimaryContainer,
          contentColor = SummerOnPrimaryContainer
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(44.dp)
      ) {
        if (verification.isVerifyingBackend) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SummerOnPrimaryContainer)
          Spacer(Modifier.width(8.dp))
          Text(
            "Testing Model...",
            color = SummerOnPrimaryContainer,
            fontFamily = ManropeFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
        } else {
          Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = SummerOnPrimaryContainer
          )
          Spacer(Modifier.width(8.dp))
          Text(
            "Verify Summarization Model",
            color = SummerOnPrimaryContainer,
            fontFamily = ManropeFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
      verification.backendMessage?.let { msg ->
        Spacer(modifier = Modifier.height(8.dp))
        VerificationStatusBox(message = msg, isError = verification.backendIsError)
      }
    }

    // -------------------------------------------------------------
    // Section 4: Google Feed & Browser Capture
    // -------------------------------------------------------------
    SettingsSectionCard(
      title = "Google Feed & Browser Capture",
      subtitle = "Manage link interception and external reading tabs",
      icon = Icons.Default.Public
    ) {
      SettingsSwitchRow(
        title = "Only Intercept Google Feed URLs",
        subtitle = "Ignores routine searches, intercepts long-form articles",
        checked = settings.onlyInterceptGoogleFeed,
        onCheckedChange = { checked ->
          onUpdateSettings { it.copy(onlyInterceptGoogleFeed = checked) }
        }
      )

      HorizontalDivider(
        color = SummerBorderSubtle.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 12.dp)
      )

      SettingsSwitchRow(
        title = "Use Custom Tabs overlay",
        subtitle = "When disabled, links open directly in your chosen browser",
        checked = settings.useCustomTabs,
        onCheckedChange = { checked ->
          onUpdateSettings { it.copy(useCustomTabs = checked) }
        }
      )

      HorizontalDivider(
        color = SummerBorderSubtle.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 12.dp)
      )

      SettingsSwitchRow(
        title = "Don't save when opened from notification",
        subtitle = "Skips automatic queueing when tapping preview cards",
        checked = settings.removeWhenOpenedFromNotification,
        onCheckedChange = { checked ->
          onUpdateSettings { it.copy(removeWhenOpenedFromNotification = checked) }
        }
      )

      HorizontalDivider(
        color = SummerBorderSubtle.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 14.dp)
      )

      Text(
        text = "TARGET EXTERNAL BROWSER",
        style = MaterialTheme.typography.labelSmall.copy(
          fontFamily = ManropeFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          letterSpacing = 0.06.sp
        ),
        color = SummerOnSurfaceVariant
      )
      Spacer(modifier = Modifier.height(10.dp))

      val allBrowserOptions = remember(installedBrowsers) {
        listOf(ExternalBrowserOption(packageName = "", label = "System Default")) + installedBrowsers
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        allBrowserOptions.chunked(2).forEach { rowOptions ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            rowOptions.forEach { option ->
              val isSelected = settings.preferredBrowserPackage == option.packageName
              Surface(
                onClick = { onUpdateSettings { it.copy(preferredBrowserPackage = option.packageName) } },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) SummerPrimaryContainer.copy(alpha = 0.12f) else SummerSurfaceDim,
                border = BorderStroke(
                  width = if (isSelected) 1.5.dp else 1.dp,
                  color = if (isSelected) SummerPrimaryContainer else SummerBorderSubtle
                ),
                modifier = Modifier
                  .weight(1f)
                  .height(46.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Icon(
                    imageVector = when {
                      option.packageName.isEmpty() -> Icons.Outlined.OpenInBrowser
                      option.label.contains("Chrome", ignoreCase = true) -> Icons.Outlined.Public
                      option.label.contains("Firefox", ignoreCase = true) -> Icons.Outlined.LocalFireDepartment
                      option.label.contains("Brave", ignoreCase = true) -> Icons.Outlined.Shield
                      else -> Icons.Outlined.Language
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) SummerPrimary else SummerOnSurfaceVariant
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontFamily = ManropeFontFamily,
                      fontSize = 13.sp,
                      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = if (isSelected) SummerPrimary else SummerOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
            if (rowOptions.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }

    // -------------------------------------------------------------
    // Footer: Version & Engine Status
    // -------------------------------------------------------------
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 16.dp, bottom = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Summer v2.4.1 (Build 412) · Engine: gemini-3.8-flash-tts",
        style = MaterialTheme.typography.labelSmall.copy(
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          letterSpacing = 0.6.sp
        ),
        color = SummerTextSecondary.copy(alpha = 0.6f)
      )
    }
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
    style = MaterialTheme.typography.bodyLarge.copy(
      fontFamily = ManropeFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.sp
    ),
    color = SummerOnSurface
  )
  Spacer(modifier = Modifier.height(4.dp))
  Text(
    text = "100% free, unlimited, and works offline without any account or API key. Sub-chunks oversized turns on sentence boundaries and stitches all hosts into a single seekable WAV podcast file.",
    style = MaterialTheme.typography.bodySmall.copy(
      fontFamily = ManropeFontFamily,
      fontSize = 12.sp,
      lineHeight = 17.sp
    ),
    color = SummerOnSurfaceVariant
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
      style = MaterialTheme.typography.bodySmall.copy(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Medium
      ),
      color = SummerOnSurface
    )
    if (draftRate != 1.0f || draftPitch != 1.0f) {
      TextButton(
        onClick = {
          draftRate = 1.0f
          draftPitch = 1.0f
          onUpdateSettings { it.copy(androidSpeechRate = 1.0f, androidSpeechPitch = 1.0f) }
        }
      ) {
        Text("Reset 1.0x", fontFamily = ManropeFontFamily, fontSize = 12.sp, color = SummerPrimary)
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
    steps = 14,
    colors = SliderDefaults.colors(
      thumbColor = SummerPrimaryContainer,
      activeTrackColor = SummerPrimaryContainer,
      inactiveTrackColor = SummerSurfaceContainerHigh
    )
  )

  Text(
    text = "Base Pitch: ${"%.2f".format(draftPitch)}x",
    style = MaterialTheme.typography.bodySmall.copy(
      fontFamily = ManropeFontFamily,
      fontWeight = FontWeight.Medium
    ),
    color = SummerOnSurface
  )
  Slider(
    value = draftPitch,
    onValueChange = { pitch -> draftPitch = pitch },
    onValueChangeFinished = {
      onUpdateSettings { it.copy(androidSpeechPitch = draftPitch) }
    },
    valueRange = 0.5f..2.0f,
    steps = 14,
    colors = SliderDefaults.colors(
      thumbColor = SummerPrimaryContainer,
      activeTrackColor = SummerPrimaryContainer,
      inactiveTrackColor = SummerSurfaceContainerHigh
    )
  )
}

internal fun stripTlDr(text: String): String {
  return text
    .replaceFirst(Regex("""^\s*(\*{1,2})?\s*TL;?DR:?\s*(\*{1,2})?:?\s*""", RegexOption.IGNORE_CASE), "")
    .trimStart()
}

