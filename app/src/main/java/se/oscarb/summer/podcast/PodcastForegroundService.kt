package se.oscarb.summer.podcast

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import se.oscarb.summer.MainActivity
import se.oscarb.summer.data.SummerRepository
import java.io.File

/**
 * Foreground service that:
 * 1. Keeps podcast playback alive when the app is backgrounded or the screen is off,
 *    displaying a lock-screen/shade MediaStyle notification with Play/Pause and ±15s controls.
 * 2. Keeps background podcast generation & browsing-session quiet-window countdown alive with a
 *    100% SILENT status-bar notification.
 * 3. Posts a DEFAULT-importance "Podcast Ready!" notification when generation finishes, where
 *    both tapping the notification (opens Summer & plays) and tapping "▶ Play Episode" (starts
 *    playback immediately in background) play the episode right away.
 */
class PodcastForegroundService : Service() {

  private var generationWakeLock: PowerManager.WakeLock? = null
  private var isInForeground: Boolean = false

  override fun onCreate() {
    super.onCreate()
    isServiceRunning = true
    ensureNotificationChannels(this)
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val controller = PodcastPlaybackController.getInstance(applicationContext)

    when (intent?.action) {
      ACTION_TOGGLE_PLAY -> controller.togglePlayPause()
      ACTION_REWIND_15 -> controller.skipBy(-15_000L)
      ACTION_FORWARD_15 -> controller.skipBy(15_000L)
      ACTION_STOP -> {
        controller.stop()
        if (!controller.state.value.isGeneratingInBackground && !controller.state.value.isQuietWindowWaiting) {
          shutdownForegroundService()
          return START_NOT_STICKY
        }
      }
      ACTION_TRIGGER_QUIET_NOW -> {
        SummerRepository.getInstance(applicationContext).triggerQuietWindowPodcastNow()
      }
      ACTION_CANCEL_QUIET_WINDOW -> {
        SummerRepository.getInstance(applicationContext).cancelQuietWindowCountdown()
      }
      ACTION_PLAY_READY_FILE -> {
        val filePath = intent.getStringExtra(EXTRA_AUDIO_PATH)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Summer Podcast" }
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE).orEmpty()
        val digestId = intent.getStringExtra(EXTRA_DIGEST_ID)
        val websiteId = intent.getStringExtra(EXTRA_WEBSITE_ID)
        val readyNotifId = intent.getIntExtra(EXTRA_READY_NOTIFICATION_ID, NOTIFICATION_ID_READY_BASE)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(readyNotifId)
        if (!filePath.isNullOrBlank()) {
          val file = File(filePath)
          if (file.exists()) {
            controller.playEpisode(
              audioFile = file,
              title = title,
              subtitle = subtitle,
              digestId = digestId,
              websiteId = websiteId,
              forceReload = true
            )
          }
        }
      }
    }

    val state = controller.state.value
    updateGenerationWakeLock(state.isGeneratingInBackground)

    when {
      state.isActive -> {
        val notification = buildPlaybackNotification(state, controller)
        startForegroundCompat(NOTIFICATION_ID_FOREGROUND, notification)
      }
      state.isGeneratingInBackground -> {
        val notification = buildSilentGenerationNotification(state)
        startForegroundCompat(NOTIFICATION_ID_FOREGROUND, notification)
      }
      state.isQuietWindowWaiting -> {
        val notification = buildSilentQuietWindowNotification(state)
        startForegroundCompat(NOTIFICATION_ID_FOREGROUND, notification)
      }
      else -> {
        shutdownForegroundService()
      }
    }

    return START_NOT_STICKY
  }

  override fun onDestroy() {
    isServiceRunning = false
    isInForeground = false
    releaseGenerationWakeLock()
    super.onDestroy()
  }

  private fun shutdownForegroundService() {
    releaseGenerationWakeLock()
    if (isInForeground) {
      runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
      isInForeground = false
    }
    stopSelf()
  }

  private fun startForegroundCompat(notificationId: Int, notification: Notification) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(
          notificationId,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
      } else {
        startForeground(notificationId, notification)
      }
      isInForeground = true
    } catch (e: Exception) {
      Log.w(TAG, "Unable to promote PodcastForegroundService to foreground: ${e.message}")
      releaseGenerationWakeLock()
      if (!isInForeground) {
        stopSelf()
      }
    }
  }

  private fun updateGenerationWakeLock(isGenerating: Boolean) {
    if (isGenerating) {
      if (generationWakeLock?.isHeld != true) {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        generationWakeLock = pm.newWakeLock(
          PowerManager.PARTIAL_WAKE_LOCK,
          "Summer:PodcastGenerationWakeLock"
        ).apply {
          setReferenceCounted(false)
          acquire(10 * 60 * 1000L)
        }
      }
    } else {
      releaseGenerationWakeLock()
    }
  }

  private fun releaseGenerationWakeLock() {
    runCatching {
      if (generationWakeLock?.isHeld == true) {
        generationWakeLock?.release()
      }
    }
    generationWakeLock = null
  }

  private fun buildPlaybackNotification(
    state: PodcastPlaybackState,
    controller: PodcastPlaybackController
  ): Notification {
    val openAppIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val contentPendingIntent = PendingIntent.getActivity(
      this,
      100,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val rewindAction = Notification.Action.Builder(
      Icon.createWithResource(this, android.R.drawable.ic_media_rew),
      "-15s",
      serviceActionPendingIntent(ACTION_REWIND_15, 101)
    ).build()

    val playPauseAction = Notification.Action.Builder(
      Icon.createWithResource(
        this,
        if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
      ),
      if (state.isPlaying) "Pause" else "Play",
      serviceActionPendingIntent(ACTION_TOGGLE_PLAY, 102)
    ).build()

    val forwardAction = Notification.Action.Builder(
      Icon.createWithResource(this, android.R.drawable.ic_media_ff),
      "+15s",
      serviceActionPendingIntent(ACTION_FORWARD_15, 103)
    ).build()

    val stopAction = Notification.Action.Builder(
      Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
      "Close",
      serviceActionPendingIntent(ACTION_STOP, 104)
    ).build()

    val mediaStyle = Notification.MediaStyle()
      .setMediaSession(controller.sessionToken)
      .setShowActionsInCompactView(0, 1, 2)

    val subText = if (state.isGeneratingInBackground) {
      "Also generating: ${state.generationStatusText.orEmpty()}"
    } else {
      state.subtitle.ifBlank { "☀️ Summer Podcast" }
    }

    return Notification.Builder(this, CHANNEL_PLAYBACK)
      .setSmallIcon(android.R.drawable.ic_btn_speak_now)
      .setContentTitle(state.title.ifBlank { "☀️ Summer Podcast" })
      .setContentText(subText)
      .setContentIntent(contentPendingIntent)
      .setDeleteIntent(serviceActionPendingIntent(ACTION_STOP, 105))
      .setVisibility(Notification.VISIBILITY_PUBLIC)
      .setOnlyAlertOnce(true)
      .setOngoing(state.isPlaying)
      .setStyle(mediaStyle)
      .addAction(rewindAction)
      .addAction(playPauseAction)
      .addAction(forwardAction)
      .addAction(stopAction)
      .build()
  }

  private fun buildSilentGenerationNotification(state: PodcastPlaybackState): Notification {
    val openAppIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val contentPendingIntent = PendingIntent.getActivity(
      this,
      200,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val title = state.generationTitle?.let { "🎙️ Generating: $it" }
      ?: "🎙️ Generating Summer Podcast..."
    val status = state.generationStatusText ?: "Writing dialogue & synthesizing voices..."

    return Notification.Builder(this, CHANNEL_GENERATION_SILENT)
      .setSmallIcon(android.R.drawable.ic_btn_speak_now)
      .setContentTitle(title)
      .setContentText(status)
      .setContentIntent(contentPendingIntent)
      .setOnlyAlertOnce(true)
      .setOngoing(true)
      .setProgress(0, 0, true)
      .setVisibility(Notification.VISIBILITY_PUBLIC)
      .build()
  }

  private fun buildSilentQuietWindowNotification(state: PodcastPlaybackState): Notification {
    val openAppIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val contentPendingIntent = PendingIntent.getActivity(
      this,
      210,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val generateNowAction = Notification.Action.Builder(
      Icon.createWithResource(this, android.R.drawable.ic_media_play),
      "Generate Now",
      serviceActionPendingIntent(ACTION_TRIGGER_QUIET_NOW, 211)
    ).build()

    val cancelAction = Notification.Action.Builder(
      Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
      "Cancel",
      serviceActionPendingIntent(ACTION_CANCEL_QUIET_WINDOW, 212)
    ).build()

    val builder = Notification.Builder(this, CHANNEL_GENERATION_SILENT)
      .setSmallIcon(android.R.drawable.ic_btn_speak_now)
      .setContentTitle("☀️ ${state.quietWindowLinkCount} links queued for podcast")
      .setContentText("Waiting for you to finish saving links...")
      .setContentIntent(contentPendingIntent)
      .setOnlyAlertOnce(true)
      .setOngoing(true)
      .setVisibility(Notification.VISIBILITY_PUBLIC)
      .addAction(generateNowAction)
      .addAction(cancelAction)

    if (state.quietWindowDeadlineMs > System.currentTimeMillis()) {
      builder
        .setWhen(state.quietWindowDeadlineMs)
        .setShowWhen(true)
        .setUsesChronometer(true)
        .setChronometerCountDown(true)
    }

    return builder.build()
  }

  private fun serviceActionPendingIntent(action: String, requestCode: Int): PendingIntent {
    val intent = Intent(this, PodcastForegroundService::class.java).apply {
      this.action = action
    }
    return PendingIntent.getService(
      this,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  companion object {
    private const val TAG = "PodcastFgService"
    private const val CHANNEL_PLAYBACK = "summer_podcast_playback"
    private const val LEGACY_CHANNEL_GENERATION = "summer_podcast_generation"
    private const val CHANNEL_GENERATION_SILENT = "summer_podcast_generation_silent"
    private const val CHANNEL_PODCAST_READY = "summer_podcast_ready"
    private const val NOTIFICATION_ID_FOREGROUND = 4101
    private const val NOTIFICATION_ID_READY_BASE = 4200

    @Volatile
    private var isServiceRunning: Boolean = false

    const val ACTION_UPDATE = "se.oscarb.summer.podcast.ACTION_UPDATE"
    const val ACTION_TOGGLE_PLAY = "se.oscarb.summer.podcast.ACTION_TOGGLE_PLAY"
    const val ACTION_REWIND_15 = "se.oscarb.summer.podcast.ACTION_REWIND_15"
    const val ACTION_FORWARD_15 = "se.oscarb.summer.podcast.ACTION_FORWARD_15"
    const val ACTION_STOP = "se.oscarb.summer.podcast.ACTION_STOP"
    const val ACTION_PLAY_READY_FILE = "se.oscarb.summer.podcast.ACTION_PLAY_READY_FILE"
    const val ACTION_TRIGGER_QUIET_NOW = "se.oscarb.summer.podcast.ACTION_TRIGGER_QUIET_NOW"
    const val ACTION_CANCEL_QUIET_WINDOW = "se.oscarb.summer.podcast.ACTION_CANCEL_QUIET_WINDOW"

    const val EXTRA_AUTO_PLAY_DIGEST_ID = "extra_auto_play_digest_id"
    const val EXTRA_READY_NOTIFICATION_ID = "extra_ready_notification_id"

    private const val EXTRA_AUDIO_PATH = "extra_audio_path"
    private const val EXTRA_TITLE = "extra_title"
    private const val EXTRA_SUBTITLE = "extra_subtitle"
    private const val EXTRA_DIGEST_ID = "extra_digest_id"
    private const val EXTRA_WEBSITE_ID = "extra_website_id"

    fun ensureNotificationChannels(context: Context) {
      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      // Remove legacy combined channel so stale noisy importance is replaced cleanly
      runCatching { nm.deleteNotificationChannel(LEGACY_CHANNEL_GENERATION) }

      val playbackChannel = NotificationChannel(
        CHANNEL_PLAYBACK,
        "Podcast Playback Controls",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Lock-screen and background playback controls for Summer podcasts"
        setShowBadge(false)
        setSound(null, null)
        enableVibration(false)
      }

      val silentGenerationChannel = NotificationChannel(
        CHANNEL_GENERATION_SILENT,
        "Background Podcast Progress (Silent)",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Silent progress indicator while generating podcast episodes in the background"
        setShowBadge(false)
        setSound(null, null)
        enableVibration(false)
      }

      val podcastReadyChannel = NotificationChannel(
        CHANNEL_PODCAST_READY,
        "Podcast Ready Alerts",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Alerts you when a Summer podcast episode finishes generating and is ready to play"
      }

      nm.createNotificationChannel(playbackChannel)
      nm.createNotificationChannel(silentGenerationChannel)
      nm.createNotificationChannel(podcastReadyChannel)
    }

    fun startOrUpdate(context: Context) {
      val appCtx = context.applicationContext
      val state = PodcastPlaybackController.getInstance(appCtx).state.value
      val intent = Intent(appCtx, PodcastForegroundService::class.java).apply {
        action = ACTION_UPDATE
      }
      runCatching {
        if (state.isActive || state.isGeneratingInBackground || state.isQuietWindowWaiting) {
          appCtx.startForegroundService(intent)
        } else if (isServiceRunning) {
          appCtx.stopService(Intent(appCtx, PodcastForegroundService::class.java))
        }
      }
    }

    /**
     * Posts a DEFAULT-importance "Podcast Ready!" notification:
     * - Tapping the notification opens Summer AND immediately plays the podcast.
     * - Tapping the "▶ Play Episode" button starts audio playback immediately in the background
     *   without leaving the user's current app or lock screen.
     */
    fun notifyPodcastReady(
      context: Context,
      title: String,
      subtitle: String,
      audioFile: File,
      digestId: String? = null,
      websiteId: String? = null
    ) {
      val appCtx = context.applicationContext
      ensureNotificationChannels(appCtx)

      val key = digestId ?: websiteId ?: audioFile.absolutePath
      val notificationId = NOTIFICATION_ID_READY_BASE + (key.hashCode() and 0x7FFF)

      val openAndPlayIntent = Intent(appCtx, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (digestId != null) {
          putExtra(EXTRA_AUTO_PLAY_DIGEST_ID, digestId)
        }
        putExtra(EXTRA_READY_NOTIFICATION_ID, notificationId)
      }
      val contentPendingIntent = PendingIntent.getActivity(
        appCtx,
        notificationId + 1,
        openAndPlayIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val playIntent = Intent(appCtx, PodcastForegroundService::class.java).apply {
        action = ACTION_PLAY_READY_FILE
        putExtra(EXTRA_AUDIO_PATH, audioFile.absolutePath)
        putExtra(EXTRA_TITLE, title)
        putExtra(EXTRA_SUBTITLE, subtitle)
        putExtra(EXTRA_DIGEST_ID, digestId)
        putExtra(EXTRA_WEBSITE_ID, websiteId)
        putExtra(EXTRA_READY_NOTIFICATION_ID, notificationId)
      }
      val playPendingIntent = PendingIntent.getForegroundService(
        appCtx,
        notificationId,
        playIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val playAction = Notification.Action.Builder(
        Icon.createWithResource(appCtx, android.R.drawable.ic_media_play),
        "▶ Play Episode",
        playPendingIntent
      ).build()

      val notification = Notification.Builder(appCtx, CHANNEL_PODCAST_READY)
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setContentTitle("☀️ Podcast Ready: $title")
        .setContentText("$subtitle • Tap to play")
        .setContentIntent(contentPendingIntent)
        .setAutoCancel(true)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .addAction(playAction)
        .build()

      runCatching {
        val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, notification)
      }
    }
  }
}
