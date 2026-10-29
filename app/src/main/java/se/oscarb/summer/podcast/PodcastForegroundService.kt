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
import se.oscarb.summer.MainActivity
import java.io.File

/**
 * Foreground service that:
 * 1. Keeps podcast playback alive when the app is backgrounded or the screen is off,
 *    displaying a lock-screen/shade MediaStyle notification with Play/Pause and ±15s controls.
 * 2. Keeps background podcast generation alive with a progress notification, and posts a
 *    "Podcast ready to play!" notification when background generation completes.
 */
class PodcastForegroundService : Service() {

  private var generationWakeLock: PowerManager.WakeLock? = null

  override fun onCreate() {
    super.onCreate()
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
        releaseGenerationWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
      }
      ACTION_PLAY_READY_FILE -> {
        val filePath = intent.getStringExtra(EXTRA_AUDIO_PATH)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Summer Podcast" }
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE).orEmpty()
        val digestId = intent.getStringExtra(EXTRA_DIGEST_ID)
        val websiteId = intent.getStringExtra(EXTRA_WEBSITE_ID)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID_READY)
        if (!filePath.isNullOrBlank()) {
          val file = File(filePath)
          if (file.exists()) {
            controller.playEpisode(
              audioFile = file,
              title = title,
              subtitle = subtitle,
              digestId = digestId,
              websiteId = websiteId
            )
          }
        }
      }
    }

    val state = controller.state.value
    updateGenerationWakeLock(state.isGeneratingInBackground)

    if (state.isActive) {
      val notification = buildPlaybackNotification(state, controller)
      startForegroundCompat(NOTIFICATION_ID_FOREGROUND, notification)
    } else if (state.isGeneratingInBackground) {
      val notification = buildGenerationNotification(state)
      startForegroundCompat(NOTIFICATION_ID_FOREGROUND, notification)
    } else {
      releaseGenerationWakeLock()
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
    }

    return START_NOT_STICKY
  }

  override fun onDestroy() {
    releaseGenerationWakeLock()
    super.onDestroy()
  }

  private fun startForegroundCompat(notificationId: Int, notification: Notification) {
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(
          notificationId,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
      } else {
        startForeground(notificationId, notification)
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
          acquire(10 * 60 * 1000L) // 10-minute safety timeout
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
      .setOngoing(state.isPlaying)
      .setStyle(mediaStyle)
      .addAction(rewindAction)
      .addAction(playPauseAction)
      .addAction(forwardAction)
      .addAction(stopAction)
      .build()
  }

  private fun buildGenerationNotification(state: PodcastPlaybackState): Notification {
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
    val status = state.generationStatusText ?: "Writing 2-host dialogue & synthesizing voices..."

    return Notification.Builder(this, CHANNEL_GENERATION)
      .setSmallIcon(android.R.drawable.ic_btn_speak_now)
      .setContentTitle(title)
      .setContentText(status)
      .setContentIntent(contentPendingIntent)
      .setOngoing(true)
      .setProgress(0, 0, true)
      .setVisibility(Notification.VISIBILITY_PUBLIC)
      .build()
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
    private const val CHANNEL_PLAYBACK = "summer_podcast_playback"
    private const val CHANNEL_GENERATION = "summer_podcast_generation"
    private const val NOTIFICATION_ID_FOREGROUND = 4101
    private const val NOTIFICATION_ID_READY = 4102

    const val ACTION_UPDATE = "se.oscarb.summer.podcast.ACTION_UPDATE"
    const val ACTION_TOGGLE_PLAY = "se.oscarb.summer.podcast.ACTION_TOGGLE_PLAY"
    const val ACTION_REWIND_15 = "se.oscarb.summer.podcast.ACTION_REWIND_15"
    const val ACTION_FORWARD_15 = "se.oscarb.summer.podcast.ACTION_FORWARD_15"
    const val ACTION_STOP = "se.oscarb.summer.podcast.ACTION_STOP"
    const val ACTION_PLAY_READY_FILE = "se.oscarb.summer.podcast.ACTION_PLAY_READY_FILE"

    private const val EXTRA_AUDIO_PATH = "extra_audio_path"
    private const val EXTRA_TITLE = "extra_title"
    private const val EXTRA_SUBTITLE = "extra_subtitle"
    private const val EXTRA_DIGEST_ID = "extra_digest_id"
    private const val EXTRA_WEBSITE_ID = "extra_website_id"

    fun ensureNotificationChannels(context: Context) {
      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val playbackChannel = NotificationChannel(
        CHANNEL_PLAYBACK,
        "Podcast Playback Controls",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Lock-screen and background playback controls for Summer podcasts"
        setShowBadge(false)
      }
      val generationChannel = NotificationChannel(
        CHANNEL_GENERATION,
        "Background Podcast Generation",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Progress and completion notifications when generating podcast episodes in the background"
      }
      nm.createNotificationChannel(playbackChannel)
      nm.createNotificationChannel(generationChannel)
    }

    fun startOrUpdate(context: Context) {
      val appCtx = context.applicationContext
      val state = PodcastPlaybackController.getInstance(appCtx).state.value
      val intent = Intent(appCtx, PodcastForegroundService::class.java).apply {
        action = ACTION_UPDATE
      }
      runCatching {
        if (state.isActive || state.isGeneratingInBackground) {
          appCtx.startForegroundService(intent)
        } else {
          appCtx.startService(intent)
        }
      }
    }

    /**
     * Posts a "Podcast Ready to Play!" notification with a direct "Play Episode" button
     * when a podcast finishes generating in the background without auto-play.
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

      val openAppIntent = Intent(appCtx, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      val contentPendingIntent = PendingIntent.getActivity(
        appCtx,
        300,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val playIntent = Intent(appCtx, PodcastForegroundService::class.java).apply {
        action = ACTION_PLAY_READY_FILE
        putExtra(EXTRA_AUDIO_PATH, audioFile.absolutePath)
        putExtra(EXTRA_TITLE, title)
        putExtra(EXTRA_SUBTITLE, subtitle)
        putExtra(EXTRA_DIGEST_ID, digestId)
        putExtra(EXTRA_WEBSITE_ID, websiteId)
      }
      val playPendingIntent = PendingIntent.getService(
        appCtx,
        301,
        playIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val playAction = Notification.Action.Builder(
        Icon.createWithResource(appCtx, android.R.drawable.ic_media_play),
        "▶ Play Episode",
        playPendingIntent
      ).build()

      val notification = Notification.Builder(appCtx, CHANNEL_GENERATION)
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setContentTitle("☀️ Podcast Ready: $title")
        .setContentText("$subtitle • Tap to listen")
        .setContentIntent(contentPendingIntent)
        .setAutoCancel(true)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .addAction(playAction)
        .build()

      runCatching {
        val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID_READY, notification)
      }
    }
  }
}
