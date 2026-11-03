package se.oscarb.summer.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.oscarb.summer.data.DigestEntry
import se.oscarb.summer.data.DigestSourceWebsite
import se.oscarb.summer.data.PodcastCastCatalog
import se.oscarb.summer.data.PodcastLanguage
import se.oscarb.summer.podcast.PodcastPlaybackState
import se.oscarb.summer.theme.ManropeFontFamily
import se.oscarb.summer.theme.SummerBackground
import se.oscarb.summer.theme.SummerBorderSubtle
import se.oscarb.summer.theme.SummerCardBackground
import se.oscarb.summer.theme.SummerHeadline
import se.oscarb.summer.theme.SummerOnSurfaceVariant
import se.oscarb.summer.theme.SummerPrimaryContainer
import se.oscarb.summer.theme.SummerSecondary
import se.oscarb.summer.theme.SummerSurfaceContainerHigh
import se.oscarb.summer.theme.SummerTheme

@Composable
internal fun DigestEpisodeCard(
  digest: DigestEntry,
  playback: PodcastPlaybackState,
  onOpenDigest: () -> Unit,
  onPlayOrGenerate: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isPlayingThis =
    playback.isActive && playback.activeDigestId == digest.id && playback.isPlaying
  val hostsLabel = remember(digest.hostNames, digest.language) {
    PodcastCastCatalog.formatHostList(digest.hostNames, digest.language)
  }

  Surface(
    color = SummerCardBackground,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(
      1.dp,
      if (isPlayingThis) SummerPrimaryContainer.copy(alpha = 0.65f) else SummerPrimaryContainer.copy(alpha = 0.35f)
    ),
    shadowElevation = if (isPlayingThis) 8.dp else 3.dp,
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onOpenDigest)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = digest.episodeBadgeText,
            style = MaterialTheme.typography.labelSmall.copy(
              fontFamily = ManropeFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              letterSpacing = 1.sp
            ),
            color = SummerPrimaryContainer
          )
          if (digest.isPlayed) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              shape = CircleShape,
              color = SummerPrimaryContainer.copy(alpha = 0.2f)
            ) {
              Text(
                text = "✓ PLAYED",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontFamily = ManropeFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 9.sp,
                  letterSpacing = 0.5.sp
                ),
                color = SummerPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Delete Episode",
            tint = SummerOnSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = digest.displayTitle,
        style = MaterialTheme.typography.headlineSmall,
        color = SummerHeadline,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Surface(
          color = SummerSurfaceContainerHigh,
          shape = CircleShape
        ) {
          Text(
            text = if (digest.language == PodcastLanguage.SWEDISH) "🇸🇪" else "🇬🇧",
            style = MaterialTheme.typography.labelSmall.copy(
              fontFamily = ManropeFontFamily,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            ),
            color = SummerOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = SummerOnSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(13.dp)
          )
          Text(
            text = hostsLabel,
            style = MaterialTheme.typography.labelSmall.copy(
              fontFamily = ManropeFontFamily,
              fontSize = 11.sp
            ),
            color = SummerOnSurfaceVariant
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.AutoStories,
            contentDescription = null,
            tint = SummerOnSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(13.dp)
          )
          Text(
            text = "${digest.sourceWebsites.size} ${if (digest.sourceWebsites.size == 1) "article" else "articles"}",
            style = MaterialTheme.typography.labelSmall.copy(
              fontFamily = ManropeFontFamily,
              fontSize = 11.sp
            ),
            color = SummerOnSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (digest.podcastAudioPath != null) {
        EpisodeWaveformScrubber(
          positionMs = if (isPlayingThis) playback.positionMs else 0L,
          durationMs = if (isPlayingThis && playback.durationMs > 0) playback.durationMs else 180000L,
          isPlaying = isPlayingThis,
          onPlayOrGenerate = onPlayOrGenerate
        )
      } else {
        Surface(
          color = SummerSurfaceContainerHigh,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, SummerBorderSubtle),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = "SCRIPT READY",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontFamily = ManropeFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  letterSpacing = 0.5.sp
                ),
                color = SummerSecondary
              )
              Text(
                text = digest.podcastScript?.take(70)?.let { "\"$it...\"" } ?: "Dialogue script ready to voice",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontFamily = ManropeFontFamily,
                  fontSize = 12.sp
                ),
                color = SummerOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            Button(
              onClick = onPlayOrGenerate,
              colors = ButtonDefaults.buttonColors(
                containerColor = SummerPrimaryContainer,
                contentColor = SummerBackground
              ),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              if (digest.isGeneratingPodcast) {
                CircularProgressIndicator(
                  modifier = Modifier.size(14.dp),
                  color = SummerBackground,
                  strokeWidth = 2.dp
                )
              } else {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = SummerBackground,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  "Generate Audio",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.Bold
                  ),
                  color = SummerBackground
                )
              }
            }
          }
        }
      }
    }
  }
}

@Preview(name = "Episode Card - Audio Ready", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun EpisodeCardAudioReadyPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      DigestEpisodeCard(
        digest = DigestEntry(
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
        ),
        playback = PodcastPlaybackState(
          isActive = true,
          isPlaying = true,
          activeDigestId = "digest-1",
          title = "The On-Device AI & Compose Brief",
          subtitle = "Maya & Leo",
          positionMs = 108000L,
          durationMs = 252000L
        ),
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
        digest = DigestEntry(
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
        ),
        playback = PodcastPlaybackState(),
        onOpenDigest = {},
        onPlayOrGenerate = {},
        onDelete = {}
      )
    }
  }
}
