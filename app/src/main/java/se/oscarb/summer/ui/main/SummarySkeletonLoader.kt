package se.oscarb.summer.ui.main

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.oscarb.summer.theme.SummerBackground
import se.oscarb.summer.theme.SummerOnSurfaceVariant
import se.oscarb.summer.theme.SummerPrimaryContainer
import se.oscarb.summer.theme.SummerSecondary
import se.oscarb.summer.theme.SummerTheme

@Composable
internal fun SummarySkeletonLoader() {
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
  val barColor = SummerOnSurfaceVariant.copy(alpha = pulseAlpha * 0.4f)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = Icons.Default.Refresh,
        contentDescription = null,
        modifier = Modifier.size(14.dp),
        tint = SummerSecondary
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Generating AI digest...",
        style = MaterialTheme.typography.labelSmall,
        color = SummerSecondary,
        fontWeight = FontWeight.Medium
      )
    }

    Spacer(modifier = Modifier.height(2.dp))

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

    listOf(0.91f, 0.84f, 0.68f).forEach { fraction ->
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(SummerPrimaryContainer.copy(alpha = pulseAlpha * 0.7f))
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

@Preview(name = "Summary Skeleton Loader", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun SummarySkeletonLoaderPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      SummarySkeletonLoader()
    }
  }
}
