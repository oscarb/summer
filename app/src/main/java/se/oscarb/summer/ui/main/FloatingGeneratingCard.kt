package se.oscarb.summer.ui.main

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.CircularProgressIndicator
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
import se.oscarb.summer.theme.ManropeFontFamily
import se.oscarb.summer.theme.SummerBackground
import se.oscarb.summer.theme.SummerOnSurface
import se.oscarb.summer.theme.SummerPrimaryContainer
import se.oscarb.summer.theme.SummerSurfaceContainerHigh
import se.oscarb.summer.theme.SummerSurfaceContainerLowest
import se.oscarb.summer.theme.SummerTheme

@Composable
internal fun FloatingGeneratingCard(
  title: String,
  statusText: String,
  modifier: Modifier = Modifier
) {
  Surface(
    color = SummerSurfaceContainerHigh,
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, SummerPrimaryContainer.copy(alpha = 0.35f)),
    shadowElevation = 12.dp,
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(SummerPrimaryContainer)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
              fontFamily = ManropeFontFamily,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            ),
            color = SummerPrimaryContainer
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
          modifier = Modifier.size(18.dp),
          color = SummerPrimaryContainer,
          strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = statusText,
          style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = ManropeFontFamily
          ),
          color = SummerOnSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      LinearProgressIndicator(
        modifier = Modifier
          .fillMaxWidth()
          .height(3.dp)
          .clip(RoundedCornerShape(1.5.dp)),
        color = SummerPrimaryContainer,
        trackColor = SummerSurfaceContainerLowest
      )
    }
  }
}

@Preview(name = "Floating Generating Card", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun FloatingGeneratingCardPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      FloatingGeneratingCard(
        title = "GENERATING EPISODE",
        statusText = "Writing podcast script..."
      )
    }
  }
}
