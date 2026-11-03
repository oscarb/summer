package se.oscarb.summer.ui.main

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.oscarb.summer.theme.SummerBackground
import se.oscarb.summer.theme.SummerPrimaryContainer
import se.oscarb.summer.theme.SummerTheme

@Composable
internal fun AnimatedEqBars(isPlaying: Boolean, modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "eq_bars")
  val bar1 by transition.animateFloat(
    initialValue = 4f,
    targetValue = 16f,
    animationSpec = infiniteRepeatable(
      animation = tween(400),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bar1"
  )
  val bar2 by transition.animateFloat(
    initialValue = 6f,
    targetValue = 20f,
    animationSpec = infiniteRepeatable(
      animation = tween(300),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bar2"
  )
  val bar3 by transition.animateFloat(
    initialValue = 3f,
    targetValue = 14f,
    animationSpec = infiniteRepeatable(
      animation = tween(500),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bar3"
  )

  Row(
    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
    verticalAlignment = Alignment.Bottom,
    modifier = modifier.height(20.dp)
  ) {
    val h1 = if (isPlaying) bar1.dp else 4.dp
    val h2 = if (isPlaying) bar2.dp else 8.dp
    val h3 = if (isPlaying) bar3.dp else 5.dp

    Box(
      modifier = Modifier
        .width(3.dp)
        .height(h1)
        .clip(RoundedCornerShape(1.5.dp))
        .background(SummerPrimaryContainer)
    )
    Box(
      modifier = Modifier
        .width(3.dp)
        .height(h2)
        .clip(RoundedCornerShape(1.5.dp))
        .background(SummerPrimaryContainer)
    )
    Box(
      modifier = Modifier
        .width(3.dp)
        .height(h3)
        .clip(RoundedCornerShape(1.5.dp))
        .background(SummerPrimaryContainer)
    )
  }
}

@Preview(name = "Animated EQ Bars - Playing", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun AnimatedEqBarsPlayingPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      AnimatedEqBars(isPlaying = true)
    }
  }
}

@Preview(name = "Animated EQ Bars - Paused", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun AnimatedEqBarsPausedPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      AnimatedEqBars(isPlaying = false)
    }
  }
}
