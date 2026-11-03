package se.oscarb.summer.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.oscarb.summer.podcast.PodcastPlaybackState
import se.oscarb.summer.theme.ManropeFontFamily
import se.oscarb.summer.theme.NewsreaderFontFamily
import se.oscarb.summer.theme.SummerBackground
import se.oscarb.summer.theme.SummerOnSurface
import se.oscarb.summer.theme.SummerOnSurfaceVariant
import se.oscarb.summer.theme.SummerPrimaryContainer
import se.oscarb.summer.theme.SummerSurfaceContainerHigh
import se.oscarb.summer.theme.SummerSurfaceContainerHighest
import se.oscarb.summer.theme.SummerSurfaceContainerLowest
import se.oscarb.summer.theme.SummerTheme

@Composable
internal fun FloatingMiniPlayerCard(
  playback: PodcastPlaybackState,
  onTogglePlayPause: () -> Unit,
  onClose: () -> Unit,
  onOpenDetails: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = SummerSurfaceContainerHigh,
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, SummerPrimaryContainer.copy(alpha = 0.35f)),
    shadowElevation = 12.dp,
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .clickable(onClick = onOpenDetails)
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Artwork Box with animated EQ bars and EP badge
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SummerSurfaceContainerHighest)
            .border(1.dp, SummerPrimaryContainer.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          AnimatedEqBars(isPlaying = playback.isPlaying)
          Text(
            text = "EP",
            style = MaterialTheme.typography.labelSmall.copy(
              fontFamily = ManropeFontFamily,
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            ),
            color = SummerPrimaryContainer.copy(alpha = 0.8f),
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(bottom = 2.dp)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = playback.title.ifBlank { "Catch Me Up Podcast" },
            style = MaterialTheme.typography.titleSmall.copy(
              fontFamily = NewsreaderFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 14.sp
            ),
            color = SummerOnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = playback.subtitle.ifBlank { "Maya & Leo" },
              style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
              ),
              color = SummerPrimaryContainer
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "•",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(6.dp))
            val timerText = "${formatDurationMs(playback.positionMs)} / ${formatDurationMs(playback.durationMs)}"
            Text(
              text = timerText,
              style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = ManropeFontFamily,
                fontSize = 11.sp
              ),
              color = SummerOnSurfaceVariant
            )
          }
        }

        IconButton(
          onClick = onTogglePlayPause,
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(SummerPrimaryContainer)
        ) {
          Icon(
            imageVector = if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (playback.isPlaying) "Pause" else "Play",
            tint = SummerBackground,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(2.dp))

        IconButton(
          onClick = onClose,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Dismiss",
            tint = SummerOnSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      val progress = if (playback.durationMs > 0) {
        (playback.positionMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f)
      } else 0f
      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(2.5.dp),
        color = SummerPrimaryContainer,
        trackColor = SummerSurfaceContainerLowest
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
        playback = PodcastPlaybackState(
          isActive = true,
          isPlaying = true,
          activeDigestId = "digest-1",
          title = "The On-Device AI & Compose Brief",
          subtitle = "Maya & Leo",
          positionMs = 108000L,
          durationMs = 252000L
        ),
        onTogglePlayPause = {},
        onClose = {},
        onOpenDetails = {}
      )
    }
  }
}
