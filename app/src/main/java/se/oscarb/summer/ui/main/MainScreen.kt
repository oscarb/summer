package se.oscarb.summer.ui.main

import android.content.Intent
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import coil.compose.AsyncImage
import se.oscarb.summer.data.AiBackend
import se.oscarb.summer.data.AppSettings
import se.oscarb.summer.data.DigestEntry
import se.oscarb.summer.data.LinkFilter
import se.oscarb.summer.data.SavedWebsite
import se.oscarb.summer.util.ExternalBrowserLauncher
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
                1 -> "${state.digests.size} saved AI briefing(s)"
                else -> "AI Providers & Browser Setup"
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
          onClick = { viewModel.triggerCatchMeUpDigest() },
          icon = {
            if (state.isGeneratingDigest) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
              )
            } else {
              Icon(Icons.Default.AutoAwesome, contentDescription = null)
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
              preferredBrowserPackage = state.settings.preferredBrowserPackage
            )
          },
          onAddSampleOrUrl = { showAddLinkDialog = true }
        )
        1 -> DigestsHistoryTab(
          digests = state.digests,
          onOpenDigest = viewModel::openSavedDigest,
          onDeleteDigest = viewModel::deleteDigest,
          onGenerateNew = {
            selectedTab = 0
            viewModel.triggerCatchMeUpDigest()
          }
        )
        2 -> SettingsTab(
          settings = state.settings,
          onUpdateSettings = viewModel::updateSettings
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
        onOpenSourceUrl = { url ->
          ExternalBrowserLauncher.openInExternalBrowser(
            context = context,
            url = url,
            preferredBrowserPackage = state.settings.preferredBrowserPackage
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

@OptIn(ExperimentalMaterial3Api::class)
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
  val dateStr = remember(website.updatedAt) {
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
      .format(Date(website.updatedAt))
  }

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

      // Inline Expanded Section: AI Summary + Action Buttons
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
  onOpenSourceUrl: (String) -> Unit,
  onMarkAllArchived: () -> Unit,
  onDismiss: () -> Unit
) {
  val dateStr = remember(digest.createdAt) {
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
      .format(Date(digest.createdAt))
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

    Spacer(modifier = Modifier.height(16.dp))

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
  onOpenDigest: (DigestEntry) -> Unit,
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
        text = "Tap the \"Catch Me Up\" button on the Saved Links tab to generate a combined AI briefing across all your unread websites.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(20.dp))
      Button(onClick = onGenerateNew) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Generate Digest Now")
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
                  text = "${digest.sourceWebsites.size} website(s) • ${digest.providerName}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary
                )
              }
              IconButton(onClick = { onDeleteDigest(digest.id) }) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = "Delete digest"
                )
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
  onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit
) {
  val context = LocalContext.current
  val installedBrowsers = remember {
    ExternalBrowserLauncher.getInstalledExternalBrowsers(context)
  }
  var showDirectGeminiKey by rememberSaveable { mutableStateOf(false) }
  var showOpenAiKey by rememberSaveable { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Summarization Behavior Card
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Summarization Behavior",
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
              text = "Automatically generate the AI summary right after a link is saved from another app.",
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
            OutlinedTextField(
              value = settings.firebaseModel,
              onValueChange = { value ->
                onUpdateSettings { it.copy(firebaseModel = value) }
              },
              label = { Text("Gemini Model Name") },
              SupportingText = { Text("Default: gemini-2.5-flash or gemini-3-flash-preview") },
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
            OutlinedTextField(
              value = settings.directGeminiModel,
              onValueChange = { value ->
                onUpdateSettings { it.copy(directGeminiModel = value) }
              },
              label = { Text("Gemini Model") },
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
      }
    }

    // External Browser Selection Card (avoids looping back into Summer)
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "External Browser for \"Open in Browser\"",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Since Summer registers as a browser itself, opening an article from inside Summer routes directly to an external browser (via Chrome Custom Tabs) so it never loops back.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

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
private fun OutlinedTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: @Composable () -> Unit,
  SupportingText: @Composable () -> Unit,
  singleLine: Boolean,
  modifier: Modifier = Modifier
) {
  androidx.compose.material3.OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = label,
    supportingText = SupportingText,
    singleLine = singleLine,
    modifier = modifier
  )
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
