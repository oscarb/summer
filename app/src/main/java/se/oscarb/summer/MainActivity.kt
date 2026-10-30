package se.oscarb.summer

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import se.oscarb.summer.data.SummerRepository
import se.oscarb.summer.podcast.PodcastForegroundService
import se.oscarb.summer.theme.SummerTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    handleAutoPlayIntent(intent)

    enableEdgeToEdge()
    setContent {
      SummerTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainNavigation()
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleAutoPlayIntent(intent)
  }

  private fun handleAutoPlayIntent(incoming: Intent?) {
    if (incoming == null) return
    val readyNotifId = incoming.getIntExtra(PodcastForegroundService.EXTRA_READY_NOTIFICATION_ID, -1)
    if (readyNotifId > 0) {
      runCatching {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(readyNotifId)
      }
    }
    val digestId = incoming.getStringExtra(PodcastForegroundService.EXTRA_AUTO_PLAY_DIGEST_ID)
    if (!digestId.isNullOrBlank()) {
      incoming.removeExtra(PodcastForegroundService.EXTRA_AUTO_PLAY_DIGEST_ID)
      SummerRepository.getInstance(applicationContext).requestOpenAndPlayDigest(digestId)
    }
  }
}
