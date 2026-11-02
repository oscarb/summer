package se.oscarb.summer.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.oscarb.summer.R
import se.oscarb.summer.theme.ManropeFontFamily
import se.oscarb.summer.theme.NewsreaderFontFamily
import se.oscarb.summer.theme.SummerBorderSubtle
import se.oscarb.summer.theme.SummerOnSurface
import se.oscarb.summer.theme.SummerOnSurfaceVariant
import se.oscarb.summer.theme.SummerPrimary
import se.oscarb.summer.theme.SummerPrimaryContainer
import se.oscarb.summer.theme.SummerSurface
import se.oscarb.summer.theme.SummerSurfaceContainerHigh
import se.oscarb.summer.theme.SummerSurfaceDim
import se.oscarb.summer.theme.SummerTextSecondary
import se.oscarb.summer.data.AppSettings
import se.oscarb.summer.data.DigestEntry
import se.oscarb.summer.data.DigestSourceWebsite
import se.oscarb.summer.data.PodcastLanguage
import se.oscarb.summer.data.SavedWebsite
import se.oscarb.summer.podcast.PodcastPlaybackState
import se.oscarb.summer.theme.SummerBackground
import se.oscarb.summer.theme.SummerTheme
import se.oscarb.summer.util.ExternalBrowserOption

private val sampleWebsite1 = SavedWebsite(
  id = "web-1",
  url = "https://simonwillison.net/2026/local-llm-reasoning",
  domain = "simonwillison.net",
  title = "Catching Edge Cases with Local LLM Reasoning",
  faviconUrl = "",
  thumbnailUrl = "",
  extractedContent = "Exploring how local models like Gemini Nano reason about edge cases in web extraction.",
  summary = "Combines private on-device Gemini Nano with cloud models for instant offline processing and rich digests.\n\n• **AICore NPU**: Zero latency variance and 100% offline privacy on Tensor G4.\n• **Cloud Synthesis**: Effortlessly handles 30,000+ token context windows across English and Swedish.",
  summaryProvider = "Gemini 3.8 Flash",
  createdAt = System.currentTimeMillis() - 3600000L,
  updatedAt = System.currentTimeMillis() - 3600000L,
  isRead = false,
  isArchived = false
)

private val sampleWebsite2 = SavedWebsite(
  id = "web-2",
  url = "https://developer.android.com/ai/hybrid-architecture",
  domain = "developer.android.com",
  title = "Hybrid On-Device and Cloud AI Architecture on Modern Android",
  faviconUrl = "",
  thumbnailUrl = "",
  extractedContent = "Android's approach to partitioning intelligence between local NPUs and cloud models.",
  summary = null,
  createdAt = System.currentTimeMillis() - 7200000L,
  updatedAt = System.currentTimeMillis() - 7200000L,
  isRead = false,
  isArchived = false
)

private val sampleWebsiteWithThumbnail = SavedWebsite(
  id = "web-thumb",
  url = "https://developer.android.com/ai/hybrid-architecture",
  domain = "developer.android.com",
  title = "Hybrid On-Device and Cloud AI Architecture on Modern Android",
  faviconUrl = "",
  thumbnailUrl = "https://developer.android.com/images/android-logo.png",
  extractedContent = "Android's approach to partitioning intelligence between local NPUs and cloud models.",
  summary = "Combines private on-device Gemini Nano with cloud models for instant offline processing and rich digests.\n\n• **AICore NPU**: Zero latency variance and 100% offline privacy on Tensor G4.\n• **Cloud Synthesis**: Effortlessly handles 30,000+ token context windows across English and Swedish.",
  summaryProvider = "Gemini 3.8 Flash",
  createdAt = System.currentTimeMillis() - 3600000L,
  updatedAt = System.currentTimeMillis() - 3600000L,
  isRead = false,
  isArchived = false
)

private val sampleDigestAudioReady = DigestEntry(
  id = "digest-1",
  createdAt = System.currentTimeMillis(),
  episodeNumber = 3,
  title = "The On-Device AI & Compose Performance Brief",
  summaryMarkdown = "Today's summary of Android performance and on-device machine learning architectures.",
  providerName = "Gemini 3.8 Flash",
  sourceWebsites = listOf(
    DigestSourceWebsite(
      id = "s-1",
      title = "The On-Device AI & Compose Performance Brief",
      url = "https://developer.android.com/compose",
      domain = "developer.android.com",
      faviconUrl = ""
    ),
    DigestSourceWebsite(
      id = "s-2",
      title = "Local LLM Reasoning",
      url = "https://simonwillison.net",
      domain = "simonwillison.net",
      faviconUrl = ""
    )
  ),
  hostNames = listOf("Maya", "Leo"),
  language = PodcastLanguage.ENGLISH,
  podcastScript = "Maya: Welcome back to Summer! Today Leo and I are breaking down Compose skipped recompositions...",
  podcastAudioPath = "/data/user/0/se.oscarb.summer/files/podcasts/digest-1.wav"
)

private val sampleDigestScriptReady = DigestEntry(
  id = "digest-2",
  createdAt = System.currentTimeMillis() - 86400000L,
  episodeNumber = 4,
  title = "Native Multi-Speaker Audio Synthesis vs Neural TTS",
  summaryMarkdown = "Yesterday's deep dive on neural vocoding and real-time TTS audio generation.",
  providerName = "Gemini 3.8 Flash",
  sourceWebsites = listOf(
    DigestSourceWebsite(
      id = "s-3",
      title = "Native Multi-Speaker Audio Synthesis vs Neural TTS",
      url = "https://research.google",
      domain = "research.google",
      faviconUrl = ""
    )
  ),
  hostNames = listOf("Astrid", "Erik"),
  language = PodcastLanguage.SWEDISH,
  podcastScript = "Astrid: Hej och välkomna till Summer! Idag pratar vi om syntetiskt tal och realtidskodning...",
  podcastAudioPath = null
)

private val samplePlaybackActive = PodcastPlaybackState(
  isActive = true,
  isPlaying = true,
  activeDigestId = "digest-1",
  title = "The On-Device AI & Compose Brief",
  subtitle = "Maya & Leo",
  positionMs = 108000L,
  durationMs = 252000L
)

@Preview(name = "Website Card - Collapsed", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun WebsiteCardCollapsedPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      WebsiteItemCard(
        website = sampleWebsite1,
        isExpanded = false,
        onCardClick = {},
        onSummarizeClick = {},
        onToggleArchive = {},
        onDelete = {},
        onOpenExternal = {}
      )
    }
  }
}

@Preview(name = "Website Card - Expanded", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun WebsiteCardExpandedPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      WebsiteItemCard(
        website = sampleWebsite1,
        isExpanded = true,
        onCardClick = {},
        onSummarizeClick = {},
        onToggleArchive = {},
        onDelete = {},
        onOpenExternal = {}
      )
    }
  }
}

@Preview(name = "Website Card - Collapsed with Thumbnail", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun WebsiteCardCollapsedWithThumbnailPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      WebsiteItemCard(
        website = sampleWebsiteWithThumbnail,
        isExpanded = false,
        onCardClick = {},
        onSummarizeClick = {},
        onToggleArchive = {},
        onDelete = {},
        onOpenExternal = {}
      )
    }
  }
}

@Preview(name = "Website Card - Expanded with Thumbnail", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun WebsiteCardExpandedWithThumbnailPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      WebsiteItemCard(
        website = sampleWebsiteWithThumbnail,
        isExpanded = true,
        onCardClick = {},
        onSummarizeClick = {},
        onToggleArchive = {},
        onDelete = {},
        onOpenExternal = {}
      )
    }
  }
}

@Preview(name = "Episode Card - Audio Ready", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun EpisodeCardAudioReadyPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      DigestEpisodeCard(
        digest = sampleDigestAudioReady,
        playback = samplePlaybackActive,
        onOpenDigest = {},
        onPlayOrGenerate = {},
        onDelete = {}
      )
    }
  }
}

@Preview(name = "Episode Card - Script Ready", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun EpisodeCardScriptReadyPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      DigestEpisodeCard(
        digest = sampleDigestScriptReady,
        playback = PodcastPlaybackState(),
        onOpenDigest = {},
        onPlayOrGenerate = {},
        onDelete = {}
      )
    }
  }
}

@Preview(name = "Floating Mini Player", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun FloatingMiniPlayerPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      FloatingMiniPlayerCard(
        playback = samplePlaybackActive,
        onTogglePlayPause = {},
        onClose = {},
        onOpenDetails = {}
      )
    }
  }
}

@Preview(name = "Catch Me Up Pill", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun CatchMeUpPillPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      FloatingCatchMeUpPill(
        unreadCount = 3,
        onClick = {}
      )
    }
  }
}

@Preview(name = "Feed Showcase Combined", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun FeedShowcasePreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        DigestEpisodeCard(
          digest = sampleDigestAudioReady,
          playback = samplePlaybackActive,
          onOpenDigest = {},
          onPlayOrGenerate = {},
          onDelete = {}
        )
        WebsiteItemCard(
          website = sampleWebsite1,
          isExpanded = false,
          onCardClick = {},
          onSummarizeClick = {},
          onToggleArchive = {},
          onDelete = {},
          onOpenExternal = {}
        )
        WebsiteItemCard(
          website = sampleWebsite2,
          isExpanded = false,
          onCardClick = {},
          onSummarizeClick = {},
          onToggleArchive = {},
          onDelete = {},
          onOpenExternal = {}
        )
        FloatingMiniPlayerCard(
          playback = samplePlaybackActive,
          onTogglePlayPause = {},
          onClose = {},
          onOpenDetails = {}
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Main Screen - Full Feed View", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun MainScreenFeedFullPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Mock Top App Bar matching Stitch
        TopAppBar(
          title = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = null,
                tint = SummerPrimaryContainer,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "All",
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
            IconButton(onClick = {}) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = SummerOnSurfaceVariant
              )
            }
            IconButton(onClick = {}) {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = SummerOnSurfaceVariant
              )
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SummerSurface
          )
        )

        // Filter chips row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = true,
            onClick = {},
            label = {
              Text(
                "Unread (3)",
                style = MaterialTheme.typography.labelMedium
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SummerPrimaryContainer.copy(alpha = 0.2f),
              selectedLabelColor = SummerPrimaryContainer
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = true,
              borderColor = SummerBorderSubtle,
              selectedBorderColor = SummerPrimaryContainer.copy(alpha = 0.5f)
            ),
            shape = CircleShape
          )
          FilterChip(
            selected = false,
            onClick = {},
            label = {
              Text(
                "All Links",
                style = MaterialTheme.typography.labelMedium
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              containerColor = SummerSurfaceContainerHigh.copy(alpha = 0.6f),
              labelColor = SummerOnSurfaceVariant
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = false,
              borderColor = SummerBorderSubtle,
              selectedBorderColor = SummerPrimaryContainer.copy(alpha = 0.5f)
            ),
            shape = CircleShape
          )
          FilterChip(
            selected = false,
            onClick = {},
            label = {
              Text(
                "Archived",
                style = MaterialTheme.typography.labelMedium
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              containerColor = SummerSurfaceContainerHigh.copy(alpha = 0.6f),
              labelColor = SummerOnSurfaceVariant
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = false,
              borderColor = SummerBorderSubtle,
              selectedBorderColor = SummerPrimaryContainer.copy(alpha = 0.5f)
            ),
            shape = CircleShape
          )
        }

        // Section header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "TODAY",
            style = MaterialTheme.typography.labelSmall,
            color = SummerOnSurfaceVariant,
            letterSpacing = 1.sp
          )
          Text(
            text = "3 ITEMS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
          )
        }

        // Feed items
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          DigestEpisodeCard(
            digest = sampleDigestAudioReady,
            playback = samplePlaybackActive,
            onOpenDigest = {},
            onPlayOrGenerate = {},
            onDelete = {}
          )
          WebsiteItemCard(
            website = sampleWebsite1,
            isExpanded = false,
            onCardClick = {},
            onSummarizeClick = {},
            onToggleArchive = {},
            onDelete = {},
            onOpenExternal = {}
          )
          WebsiteItemCard(
            website = sampleWebsite2,
            isExpanded = false,
            onCardClick = {},
            onSummarizeClick = {},
            onToggleArchive = {},
            onDelete = {},
            onOpenExternal = {}
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Floating dock at bottom
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          FloatingCatchMeUpPill(
            unreadCount = 3,
            onClick = {}
          )
        }
      }
    }
  }
}

@Preview(name = "Add Link Dialog", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun AddLinkDialogPreview() {
  SummerTheme {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      AddLinkDialogContent(
        onDismiss = {},
        onAddUrl = {}
      )
    }
  }
}

@Preview(name = "Settings Screen", showBackground = true, backgroundColor = 0xFF131316, heightDp = 3000)
@Composable
fun SettingsScreenPreview() {
  SummerTheme {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = SummerBackground
    ) {
      SettingsTab(
        settings = AppSettings(
          autoSummarizeOnSave = true,
          autoPodcastAfterQuietWindow = true,
          autoPodcastQuietMinutes = 2,
          autoPodcastMinLinks = 3,
          directGeminiApiKey = "AIzaSyDummyKey1234567890",
          selectedBackend = se.oscarb.summer.data.AiBackend.FIREBASE_AI,
          podcastTtsProvider = se.oscarb.summer.data.PodcastTtsProvider.GEMINI_MULTISPEAKER
        ),
        verification = VerificationUiState(),
        bottomPadding = 24.dp,
        onUpdateSettings = {},
        onVerifyGeminiTts = {},
        onVerifyTextBackend = {}
      )
    }
  }
}

@Preview(name = "Settings Browser Capture Section", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun SettingsBrowserCapturePreview() {
  SummerTheme {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      color = SummerBackground
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsSectionCard(
          title = "Google Feed & Browser Capture",
          subtitle = "Manage link interception and external reading tabs",
          icon = Icons.Default.Public
        ) {
          SettingsSwitchRow(
            title = "Only Intercept Google Feed URLs",
            subtitle = "Ignores routine searches, intercepts long-form articles",
            checked = true,
            onCheckedChange = {}
          )
          HorizontalDivider(
            color = SummerBorderSubtle.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 12.dp)
          )
          SettingsSwitchRow(
            title = "Use Custom Tabs overlay",
            subtitle = "When disabled, links open directly in your chosen browser",
            checked = false,
            onCheckedChange = {}
          )
          HorizontalDivider(
            color = SummerBorderSubtle.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 12.dp)
          )
          SettingsSwitchRow(
            title = "Don't save when opened from notification",
            subtitle = "Skips automatic queueing when tapping preview cards",
            checked = true,
            onCheckedChange = {}
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
          val mockOptions = listOf(
            ExternalBrowserOption(packageName = "", label = "System Default"),
            ExternalBrowserOption(packageName = "com.android.chrome", label = "Chrome"),
            ExternalBrowserOption(packageName = "org.mozilla.firefox", label = "Firefox"),
            ExternalBrowserOption(packageName = "com.brave.browser", label = "Brave")
          )
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            mockOptions.chunked(2).forEach { rowOptions ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                rowOptions.forEach { option ->
                  val isSelected = option.packageName.isEmpty()
                  Surface(
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
                        color = if (isSelected) SummerPrimary else SummerOnSurface
                      )
                    }
                  }
                }
              }
            }
          }
        }
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
  }
}
