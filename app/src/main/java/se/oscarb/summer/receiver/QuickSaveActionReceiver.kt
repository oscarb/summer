package se.oscarb.summer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import se.oscarb.summer.data.SummerRepository
import se.oscarb.summer.util.QuickSaveNotificationHelper

/**
 * BroadcastReceiver triggered for actions or user dismissal on the quick-save notification.
 */
class QuickSaveActionReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent?) {
    when (intent?.action) {
      ACTION_PODCAST_FROM_NOTIFICATION -> {
        QuickSaveNotificationHelper.cancelNotification(context)
        Toast.makeText(context, "🎙️ Generating Summer podcast...", Toast.LENGTH_SHORT).show()
        SummerRepository.getInstance(context.applicationContext).triggerCatchMeUpPodcastFromQuickAction()
      }
      ACTION_DISMISS_NOTIFICATION -> {
        val websiteId = intent.getStringExtra(EXTRA_WEBSITE_ID).orEmpty()
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        QuickSaveNotificationHelper.onUserDismissed(websiteId, url)

        val repo = SummerRepository.getInstance(context.applicationContext)
        if (websiteId.isNotBlank()) {
          repo.deleteWebsite(websiteId)
        } else if (url.isNotBlank()) {
          repo.deleteWebsiteByUrl(url)
        }
        Toast.makeText(context, "Summer: Removed from list", Toast.LENGTH_SHORT).show()
      }
    }
  }

  companion object {
    const val ACTION_PODCAST_FROM_NOTIFICATION = "se.oscarb.summer.action.PODCAST_FROM_NOTIFICATION"
    const val ACTION_DISMISS_NOTIFICATION = "se.oscarb.summer.action.DISMISS_NOTIFICATION"
    const val EXTRA_WEBSITE_ID = "extra_website_id"
    const val EXTRA_URL = "extra_url"
  }
}
