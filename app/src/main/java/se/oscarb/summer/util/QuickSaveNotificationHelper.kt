package se.oscarb.summer.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import se.oscarb.summer.MainActivity
import se.oscarb.summer.OpenNotificationLinkActivity
import se.oscarb.summer.R
import se.oscarb.summer.receiver.QuickSaveActionReceiver

/**
 * Helper to display a lightweight, silent, auto-dismissing heads-up notification
 * when a link is saved or bumped from an external app.
 *
 * - Tapping the notification opens the link directly in your browser app.
 * - Swiping away (dismissing) the notification cancels saving and removes the link from Summer.
 * - Action buttons allow opening Summer or kicking off background podcast generation.
 * - Extended 12s auto-dismiss timer ensures it won't disappear prematurely.
 */
object QuickSaveNotificationHelper {

  const val CHANNEL_SAVED_LINK = "summer_saved_link"
  const val NOTIFICATION_ID_SAVED_LINK = 3001
  private const val AUTO_DISMISS_TIMEOUT_MS = 12000L

  private val timeoutScope = CoroutineScope(Dispatchers.Default)
  private var activeTimeoutJob: Job? = null

  @Volatile
  private var channelEnsured = false

  @Volatile
  var activeSaveWebsiteId: String? = null
    private set

  fun ensureChannel(context: Context) {
    if (channelEnsured) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val channel = NotificationChannel(
        CHANNEL_SAVED_LINK,
        "Saved Links & Quick Actions",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Temporary heads-up notifications with quick actions when a link is saved"
        setShowBadge(false)
        setSound(null, null)
        enableVibration(false)
      }
      nm.createNotificationChannel(channel)
      channelEnsured = true
    }
  }

  fun showPostSaveNotification(
    context: Context,
    url: String,
    domain: String,
    websiteId: String,
    wasBumped: Boolean,
    preferredBrowserPackage: String = "",
    useCustomTabs: Boolean = false
  ) {
    activeSaveWebsiteId = websiteId
    ensureChannel(context)
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val title = if (wasBumped) {
      "☀️ Moved to top • $domain"
    } else {
      "☀️ Saved to Summer • $domain"
    }

    // 1. Tapping notification body: Open the link in the browser app (and remove from Summer if enabled)
    val openIntent = Intent(context, OpenNotificationLinkActivity::class.java).apply {
      putExtra(OpenNotificationLinkActivity.EXTRA_URL, url)
      putExtra(OpenNotificationLinkActivity.EXTRA_WEBSITE_ID, websiteId)
    }
    val contentPendingIntent = PendingIntent.getActivity(
      context,
      301,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 2. Swiping away / dismissing the notification: Do NOT save (removes link from Summer)
    val deleteIntent = Intent(context, QuickSaveActionReceiver::class.java).apply {
      action = QuickSaveActionReceiver.ACTION_DISMISS_NOTIFICATION
      putExtra(QuickSaveActionReceiver.EXTRA_WEBSITE_ID, websiteId)
      putExtra(QuickSaveActionReceiver.EXTRA_URL, url)
    }
    val deletePendingIntent = PendingIntent.getBroadcast(
      context,
      303,
      deleteIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 3. Action button: "Open Summer"
    val openSummerIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val openSummerPendingIntent = PendingIntent.getActivity(
      context,
      304,
      openSummerIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val openSummerAction = Notification.Action.Builder(
      Icon.createWithResource(context, R.drawable.ic_launcher_monochrome),
      "Open Summer",
      openSummerPendingIntent
    ).build()

    // 4. Action button: "Podcast"
    val podcastIntent = Intent(context, QuickSaveActionReceiver::class.java).apply {
      action = QuickSaveActionReceiver.ACTION_PODCAST_FROM_NOTIFICATION
    }
    val podcastPendingIntent = PendingIntent.getBroadcast(
      context,
      302,
      podcastIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val podcastAction = Notification.Action.Builder(
      Icon.createWithResource(context, android.R.drawable.ic_btn_speak_now),
      "Podcast",
      podcastPendingIntent
    ).build()

    val builder = Notification.Builder(context, CHANNEL_SAVED_LINK)
      .setSmallIcon(R.drawable.ic_launcher_monochrome)
      .setContentTitle(title)
      .setContentText("Tap to open in browser • Swipe to cancel")
      .setContentIntent(contentPendingIntent)
      .setDeleteIntent(deletePendingIntent)
      .setAutoCancel(true)
      .setTimeoutAfter(AUTO_DISMISS_TIMEOUT_MS)
      .setVisibility(Notification.VISIBILITY_PUBLIC)
      .addAction(openSummerAction)
      .addAction(podcastAction)

    runCatching {
      nm.notify(NOTIFICATION_ID_SAVED_LINK, builder.build())
    }

    // Secondary timer to auto-dismiss after 12s without triggering deleteIntent
    synchronized(this) {
      activeTimeoutJob?.cancel()
      activeTimeoutJob = timeoutScope.launch {
        delay(AUTO_DISMISS_TIMEOUT_MS)
        activeSaveWebsiteId = null
        runCatching { nm.cancel(NOTIFICATION_ID_SAVED_LINK) }
      }
    }
  }

  fun cancelNotification(context: Context) {
    synchronized(this) {
      activeTimeoutJob?.cancel()
      activeTimeoutJob = null
      activeSaveWebsiteId = null
    }
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    runCatching { nm.cancel(NOTIFICATION_ID_SAVED_LINK) }
  }

  fun onUserDismissed(websiteId: String, url: String) {
    synchronized(this) {
      activeTimeoutJob?.cancel()
      activeTimeoutJob = null
      activeSaveWebsiteId = null
    }
  }
}
