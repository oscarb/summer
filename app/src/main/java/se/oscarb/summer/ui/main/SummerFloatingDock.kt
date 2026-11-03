package se.oscarb.summer.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import se.oscarb.summer.podcast.PodcastPlaybackState
import se.oscarb.summer.theme.SummerErrorContainer
import se.oscarb.summer.theme.SummerOnErrorContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SummerFloatingDock(
  unreadCount: Int,
  isGeneratingDigest: Boolean,
  playback: PodcastPlaybackState,
  onCatchMeUp: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onClosePlayer: () -> Unit,
  onOpenPlayerDetails: () -> Unit
) {
  AnimatedVisibility(
    visible = playback.isActive || isGeneratingDigest || playback.isGeneratingInBackground || unreadCount > 0,
    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
  ) {
    when {
      playback.isActive -> {
        val dismissState = rememberSwipeToDismissBoxState(
          confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
              onClosePlayer()
              true
            } else false
          }
        )
        SwipeToDismissBox(
          state = dismissState,
          backgroundContent = {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SummerErrorContainer.copy(alpha = 0.5f))
                .padding(horizontal = 20.dp),
              contentAlignment = Alignment.CenterEnd
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = SummerOnErrorContainer
              )
            }
          },
          content = {
            FloatingMiniPlayerCard(
              playback = playback,
              onTogglePlayPause = onTogglePlayPause,
              onClose = onClosePlayer,
              onOpenDetails = onOpenPlayerDetails
            )
          }
        )
      }
      isGeneratingDigest || playback.isGeneratingInBackground -> {
        FloatingGeneratingCard(
          title = playback.generationTitle ?: "GENERATING EPISODE",
          statusText = playback.generationStatusText ?: "Writing podcast script..."
        )
      }
      unreadCount > 0 -> {
        FloatingCatchMeUpPill(
          unreadCount = unreadCount,
          onClick = onCatchMeUp
        )
      }
    }
  }
}
