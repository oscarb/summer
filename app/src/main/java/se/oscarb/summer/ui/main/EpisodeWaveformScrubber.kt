package se.oscarb.summer.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.oscarb.summer.theme.SummerBackground
import se.oscarb.summer.theme.SummerBorderSubtle
import se.oscarb.summer.theme.SummerOutline
import se.oscarb.summer.theme.SummerPrimary
import se.oscarb.summer.theme.SummerPrimaryContainer
import se.oscarb.summer.theme.SummerSurfaceContainerHigh
import se.oscarb.summer.theme.SummerTheme

@Composable
internal fun EpisodeWaveformScrubber(
  positionMs: Long,
  durationMs: Long,
  isPlaying: Boolean,
  onPlayOrGenerate: () -> Unit,
  modifier: Modifier = Modifier
) {
  val barHeights = remember {
    listOf(
      12f, 20f, 16f, 24f, 12f, 26f, 20f, 16f, 24f, 20f,
      26f, 12f, 22f, 18f, 14f, 26f, 12f, 20f, 8f, 22f,
      16f, 20f, 12f, 24f, 8f, 18f, 14f, 22f
    )
  }
  val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
  val activeIndex = (fraction * barHeights.size).toInt().coerceIn(0, barHeights.lastIndex)
  val trackShape = RoundedCornerShape(8.dp)

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier
          .weight(1f)
          .height(32.dp)
          .clip(trackShape)
          .background(SummerSurfaceContainerHigh)
          .border(1.dp, SummerBorderSubtle.copy(alpha = 0.4f), trackShape)
          .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        barHeights.forEachIndexed { index, heightDp ->
          val isCurrent = index == activeIndex
          Box(
            modifier = Modifier
              .width(if (isCurrent) 4.dp else 3.dp)
              .height(if (isCurrent) 24.dp else heightDp.dp)
              .clip(CircleShape)
              .background(
                when {
                  isCurrent -> SummerPrimary
                  index < activeIndex -> SummerPrimaryContainer
                  else -> WaveformIdleBarColor
                }
              )
          )
        }
      }

      IconButton(
        onClick = onPlayOrGenerate,
        modifier = Modifier
          .size(44.dp)
          .shadow(elevation = 10.dp, shape = CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
          .clip(CircleShape)
          .background(SummerPrimaryContainer)
      ) {
        Icon(
          imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = if (isPlaying) "Pause podcast" else "Play podcast",
          tint = SummerBackground,
          modifier = Modifier.size(22.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(end = 56.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = formatDurationMs(positionMs),
        style = CardMetaStyle.copy(fontWeight = FontWeight.SemiBold),
        color = SummerPrimary
      )
      Text(
        text = if (durationMs > 0) formatDurationMs(durationMs) else "--:--",
        style = CardMetaStyle,
        color = SummerOutline
      )
    }
  }
}

@Preview(name = "Waveform Scrubber - Playing", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun EpisodeWaveformScrubberPlayingPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      EpisodeWaveformScrubber(
        positionMs = 45000L,
        durationMs = 180000L,
        isPlaying = true,
        onPlayOrGenerate = {}
      )
    }
  }
}

@Preview(name = "Waveform Scrubber - Paused", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun EpisodeWaveformScrubberPausedPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      EpisodeWaveformScrubber(
        positionMs = 0L,
        durationMs = 180000L,
        isPlaying = false,
        onPlayOrGenerate = {}
      )
    }
  }
}
