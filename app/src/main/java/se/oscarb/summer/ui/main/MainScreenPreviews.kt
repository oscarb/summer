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

private val samplePlaybackActive = PodcastPlaybackState(
  isActive = true,
  isPlaying = true,
  activeDigestId = "digest-1",
  title = "The On-Device AI & Compose Brief",
  subtitle = "Maya & Leo",
  positionMs = 108000L,
  durationMs = 252000L
)

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
              Image(
                painter = painterResource(id = R.drawable.ic_summer_logo),
                contentDescription = null,
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

