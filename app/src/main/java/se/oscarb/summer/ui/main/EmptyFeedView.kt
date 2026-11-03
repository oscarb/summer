package se.oscarb.summer.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.oscarb.summer.R
import se.oscarb.summer.theme.SummerBackground
import se.oscarb.summer.theme.SummerOnSurface
import se.oscarb.summer.theme.SummerOnSurfaceVariant
import se.oscarb.summer.theme.SummerTheme

@Composable
internal fun EmptyFeedView(
  feedMode: FeedMode,
  onAddLinkClick: () -> Unit,
  onGeneratePodcast: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Image(
      painter = painterResource(R.drawable.ic_summer_logo),
      contentDescription = null,
      modifier = Modifier.size(68.dp)
    )
    Spacer(modifier = Modifier.height(18.dp))
    Text(
      text = if (feedMode == FeedMode.ALL) {
        "Your feed is clear"
      } else if (feedMode == FeedMode.LINKS) {
        "No saved links yet"
      } else {
        "No episodes created yet"
      },
      style = MaterialTheme.typography.headlineSmall,
      color = SummerOnSurface
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = if (feedMode == FeedMode.ALL) {
        "Share articles to Summer from your browser or tap Add URL to curate your personal reading list."
      } else if (feedMode == FeedMode.LINKS) {
        "Save links from other apps or tap + to start saving content for your daily summaries."
      } else {
        "Save some interesting articles and tap Catch Me Up to voice your first personalized podcast episode."
      },
      style = MaterialTheme.typography.bodyMedium,
      color = SummerOnSurfaceVariant,
      textAlign = TextAlign.Center
    )
  }
}

@Preview(name = "Empty Feed View - Clear", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun EmptyFeedViewClearPreview() {
  SummerTheme {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = SummerBackground
    ) {
      EmptyFeedView(
        feedMode = FeedMode.ALL,
        onAddLinkClick = {},
        onGeneratePodcast = {}
      )
    }
  }
}

@Preview(name = "Empty Feed View - Links", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun EmptyFeedViewLinksPreview() {
  SummerTheme {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = SummerBackground
    ) {
      EmptyFeedView(
        feedMode = FeedMode.LINKS,
        onAddLinkClick = {},
        onGeneratePodcast = {}
      )
    }
  }
}

@Preview(name = "Empty Feed View - Episodes", showBackground = true, backgroundColor = 0xFF131316)
@Composable
fun EmptyFeedViewEpisodesPreview() {
  SummerTheme {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = SummerBackground
    ) {
      EmptyFeedView(
        feedMode = FeedMode.EPISODES,
        onAddLinkClick = {},
        onGeneratePodcast = {}
      )
    }
  }
}
