package se.oscarb.summer.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.oscarb.summer.theme.SummerBackground
import se.oscarb.summer.theme.SummerOnSurface
import se.oscarb.summer.theme.SummerOnSurfaceVariant
import se.oscarb.summer.theme.SummerPrimaryContainer
import se.oscarb.summer.theme.SummerSurfaceContainerHigh
import se.oscarb.summer.theme.SummerTheme

@Composable
internal fun InAppPostSaveActionBar(
  event: PostSaveBannerEvent,
  onOpenLink: () -> Unit,
  onGeneratePodcast: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = SummerSurfaceContainerHigh,
    contentColor = SummerOnSurface,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, SummerPrimaryContainer.copy(alpha = 0.4f)),
    shadowElevation = 8.dp,
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = if (event.wasBumped) {
          "Moved ${event.domain} to top"
        } else {
          "Saved ${event.domain}"
        },
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )
      TextButton(onClick = onOpenLink) {
        Text(
          text = "Open",
          color = SummerPrimaryContainer,
          style = MaterialTheme.typography.labelMedium
        )
      }
      TextButton(onClick = onGeneratePodcast) {
        Text(
          text = "Podcast",
          color = SummerPrimaryContainer,
          style = MaterialTheme.typography.labelMedium
        )
      }
      IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Dismiss",
          tint = SummerOnSurfaceVariant,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}

@Preview(name = "Post Save Action Bar - Saved", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun InAppPostSaveActionBarSavedPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      InAppPostSaveActionBar(
        event = PostSaveBannerEvent(
          url = "https://example.com/article",
          domain = "example.com",
          wasBumped = false
        ),
        onOpenLink = {},
        onGeneratePodcast = {},
        onDismiss = {}
      )
    }
  }
}

@Preview(name = "Post Save Action Bar - Bumped", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun InAppPostSaveActionBarBumpedPreview() {
  SummerTheme {
    Surface(color = SummerBackground, modifier = Modifier.padding(16.dp)) {
      InAppPostSaveActionBar(
        event = PostSaveBannerEvent(
          url = "https://example.com/article",
          domain = "example.com",
          wasBumped = true
        ),
        onOpenLink = {},
        onGeneratePodcast = {},
        onDismiss = {}
      )
    }
  }
}
