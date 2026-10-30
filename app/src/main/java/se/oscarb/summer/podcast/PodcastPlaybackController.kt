package se.oscarb.summer.podcast

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.PowerManager
import android.view.KeyEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PodcastPlaybackState(
  val isActive: Boolean = false,
  val isPlaying: Boolean = false,
  val activeDigestId: String? = null,
  val activeWebsiteId: String? = null,
  val title: String = "",
  val subtitle: String = "",
  val audioFilePath: String? = null,
  val positionMs: Long = 0L,
  val durationMs: Long = 0L,
  val isGeneratingInBackground: Boolean = false,
  val generationTitle: String? = null,
  val generationStatusText: String? = null
)

private data class ActiveGenerationInfo(
  val title: String,
  val statusText: String
)

/**
 * Manages podcast audio playback, MediaSession integration (lock screen, notification shade,
 * and Bluetooth headphone controls), audio focus, and keyed background generation status.
 */
class PodcastPlaybackController private constructor(private val appContext: Context) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

  private val _state = MutableStateFlow(PodcastPlaybackState())
  val state: StateFlow<PodcastPlaybackState> = _state.asStateFlow()

  private var mediaPlayer: MediaPlayer? = null
  private var isPrepared: Boolean = false
  private var playbackCompleted: Boolean = false
  private var loadedFileLastModified: Long = -1L
  private var activePlayToken: Long = 0L
  private var progressJob: Job? = null
  private var audioFocusRequest: AudioFocusRequest? = null

  private val generationsLock = Any()
  private val activeGenerations = LinkedHashMap<String, ActiveGenerationInfo>()

  private val mediaSession: MediaSession = MediaSession(appContext, "SummerPodcastSession").apply {
    setCallback(object : MediaSession.Callback() {
      override fun onPlay() {
        resume()
      }

      override fun onPause() {
        pause()
      }

      override fun onStop() {
        stop()
      }

      override fun onSeekTo(pos: Long) {
        seekTo(pos)
      }

      override fun onRewind() {
        skipBy(-15_000L)
      }

      override fun onFastForward() {
        skipBy(15_000L)
      }

      override fun onSkipToPrevious() {
        skipBy(-15_000L)
      }

      override fun onSkipToNext() {
        skipBy(15_000L)
      }

      override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
        @Suppress("DEPRECATION")
        val keyEvent = mediaButtonIntent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
        if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
          when (keyEvent.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
              resume()
              return true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
              pause()
              return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
              togglePlayPause()
              return true
            }
            KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
              skipBy(15_000L)
              return true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_REWIND -> {
              skipBy(-15_000L)
              return true
            }
          }
        }
        return super.onMediaButtonEvent(mediaButtonIntent)
      }
    })
  }

  val sessionToken: MediaSession.Token
    get() = mediaSession.sessionToken

  fun playEpisode(
    audioFile: File,
    title: String,
    subtitle: String,
    digestId: String? = null,
    websiteId: String? = null,
    forceReload: Boolean = false
  ) {
    if (!audioFile.exists()) return
    val fileMtime = audioFile.lastModified()

    // If the exact same unmodified file is already prepared, just resume (with EOF rewind support)
    val current = _state.value
    if (
      !forceReload &&
      current.isActive &&
      current.audioFilePath == audioFile.absolutePath &&
      mediaPlayer != null &&
      isPrepared &&
      fileMtime == loadedFileLastModified
    ) {
      resume()
      return
    }

    // Update state synchronously first so foreground-service promotion in onStartCommand sees
    // isActive == true immediately while MediaPlayer prepares asynchronously off the UI thread.
    _state.update {
      it.copy(
        isActive = true,
        isPlaying = true,
        activeDigestId = digestId,
        activeWebsiteId = websiteId,
        title = title,
        subtitle = subtitle,
        audioFilePath = audioFile.absolutePath,
        positionMs = 0L,
        durationMs = 0L
      )
    }

    scope.launch {
      val token = ++activePlayToken
      releasePlayerOnly()
      loadedFileLastModified = fileMtime
      requestAudioFocus()

      try {
        val player = MediaPlayer().apply {
          setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
          setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build()
          )
          setDataSource(audioFile.absolutePath)
          setOnPreparedListener { mp ->
            if (token != activePlayToken) {
              runCatching { mp.release() }
              return@setOnPreparedListener
            }
            isPrepared = true
            playbackCompleted = false
            val duration = runCatching { mp.duration.toLong() }.getOrDefault(0L).coerceAtLeast(0L)
            val shouldStart = _state.value.isPlaying
            if (shouldStart) {
              runCatching { mp.start() }
            }
            _state.update { it.copy(durationMs = duration, positionMs = 0L) }
            mediaSession.isActive = true
            updateMediaSessionMetadata(title, subtitle, duration)
            updateMediaSessionPlaybackState(isPlaying = shouldStart, positionMs = 0L)
            if (shouldStart) {
              startProgressTicker()
            }
            PodcastForegroundService.startOrUpdate(appContext)
          }
          setOnCompletionListener {
            if (token == activePlayToken) {
              onPlaybackCompleted()
            }
          }
          setOnErrorListener { _, _, _ ->
            if (token == activePlayToken) {
              stop()
            }
            true
          }
        }

        mediaPlayer = player
        mediaSession.isActive = true
        updateMediaSessionMetadata(title, subtitle, 0L)
        updateMediaSessionPlaybackState(isPlaying = true, positionMs = 0L)
        PodcastForegroundService.startOrUpdate(appContext)
        player.prepareAsync()
      } catch (ce: CancellationException) {
        throw ce
      } catch (_: Exception) {
        releasePlayerOnly()
        _state.update { it.copy(isActive = false, isPlaying = false) }
        PodcastForegroundService.startOrUpdate(appContext)
      }
    }
  }

  fun togglePlayPause() {
    if (_state.value.isPlaying) {
      pause()
    } else {
      resume()
    }
  }

  fun pause() {
    val player = mediaPlayer ?: return
    if (!isPrepared) {
      _state.update { it.copy(isPlaying = false) }
      updateMediaSessionPlaybackState(isPlaying = false, positionMs = 0L)
      PodcastForegroundService.startOrUpdate(appContext)
      return
    }
    runCatching {
      if (player.isPlaying) {
        player.pause()
      }
    }
    val pos = runCatching { player.currentPosition.toLong() }.getOrDefault(_state.value.positionMs)
    _state.update { it.copy(isPlaying = false, positionMs = pos) }
    updateMediaSessionPlaybackState(isPlaying = false, positionMs = pos)
    stopProgressTicker()
    PodcastForegroundService.startOrUpdate(appContext)
  }

  fun resume() {
    val player = mediaPlayer
    if (player == null) {
      val path = _state.value.audioFilePath ?: return
      val file = File(path)
      if (file.exists()) {
        playEpisode(
          audioFile = file,
          title = _state.value.title,
          subtitle = _state.value.subtitle,
          digestId = _state.value.activeDigestId,
          websiteId = _state.value.activeWebsiteId,
          forceReload = true
        )
      }
      return
    }
    if (!isPrepared) {
      _state.update { it.copy(isPlaying = true) }
      updateMediaSessionPlaybackState(isPlaying = true, positionMs = 0L)
      PodcastForegroundService.startOrUpdate(appContext)
      return
    }

    val duration = runCatching { player.duration.toLong() }
      .getOrDefault(_state.value.durationMs)
      .coerceAtLeast(_state.value.durationMs)
    val currentPos = runCatching { player.currentPosition.toLong() }
      .getOrDefault(_state.value.positionMs)
    val shouldRewind = playbackCompleted || (duration > 0L && currentPos >= duration - 250L)

    requestAudioFocus()
    if (shouldRewind) {
      runCatching { player.seekTo(0) }
      playbackCompleted = false
    }
    runCatching {
      if (!player.isPlaying) {
        player.start()
      }
    }
    val pos = if (shouldRewind) {
      0L
    } else {
      runCatching { player.currentPosition.toLong() }.getOrDefault(currentPos)
    }
    _state.update { it.copy(isPlaying = true, positionMs = pos) }
    mediaSession.isActive = true
    updateMediaSessionPlaybackState(isPlaying = true, positionMs = pos)
    startProgressTicker()
    PodcastForegroundService.startOrUpdate(appContext)
  }

  fun seekTo(positionMs: Long) {
    val player = mediaPlayer ?: return
    if (!isPrepared) return
    val duration = _state.value.durationMs.coerceAtLeast(1L)
    val clamped = positionMs.coerceIn(0L, duration)
    if (clamped < duration - 250L) {
      playbackCompleted = false
    }
    runCatching {
      player.seekTo(clamped.toInt())
    }
    _state.update { it.copy(positionMs = clamped) }
    updateMediaSessionPlaybackState(isPlaying = _state.value.isPlaying, positionMs = clamped)
  }

  fun skipBy(deltaMs: Long) {
    val player = mediaPlayer ?: return
    if (!isPrepared) return
    val currentPos = runCatching { player.currentPosition.toLong() }.getOrDefault(_state.value.positionMs)
    seekTo(currentPos + deltaMs)
  }

  fun stop() {
    activePlayToken++
    releasePlayerOnly()
    abandonAudioFocus()
    mediaSession.isActive = false
    _state.update {
      it.copy(
        isActive = false,
        isPlaying = false,
        activeDigestId = null,
        activeWebsiteId = null,
        positionMs = 0L,
        durationMs = 0L
      )
    }
    updateMediaSessionPlaybackState(isPlaying = false, positionMs = 0L, stopped = true)
    PodcastForegroundService.startOrUpdate(appContext)
  }

  fun setGenerationProgress(
    jobKey: String,
    title: String,
    statusText: String
  ) {
    val (displayTitle, displayStatus) = synchronized(generationsLock) {
      activeGenerations[jobKey] = ActiveGenerationInfo(title = title, statusText = statusText)
      val count = activeGenerations.size
      val formattedTitle = if (count > 1) "$title (+${count - 1} more)" else title
      formattedTitle to statusText
    }
    _state.update {
      it.copy(
        isGeneratingInBackground = true,
        generationTitle = displayTitle,
        generationStatusText = displayStatus
      )
    }
    PodcastForegroundService.startOrUpdate(appContext)
  }

  fun clearGenerationProgress(jobKey: String) {
    val remainingSnapshot: Triple<Boolean, String?, String?> = synchronized(generationsLock) {
      activeGenerations.remove(jobKey)
      val latest = activeGenerations.values.lastOrNull()
      val count = activeGenerations.size
      if (latest != null) {
        val formattedTitle = if (count > 1) "${latest.title} (+${count - 1} more)" else latest.title
        Triple(true, formattedTitle, latest.statusText)
      } else {
        Triple(false, null, null)
      }
    }
    _state.update {
      it.copy(
        isGeneratingInBackground = remainingSnapshot.first,
        generationTitle = remainingSnapshot.second,
        generationStatusText = remainingSnapshot.third
      )
    }
    PodcastForegroundService.startOrUpdate(appContext)
  }

  private fun onPlaybackCompleted() {
    playbackCompleted = true
    val duration = _state.value.durationMs
    _state.update { it.copy(isPlaying = false, positionMs = duration) }
    updateMediaSessionPlaybackState(isPlaying = false, positionMs = duration)
    stopProgressTicker()
    PodcastForegroundService.startOrUpdate(appContext)
  }

  private fun startProgressTicker() {
    progressJob?.cancel()
    progressJob = scope.launch {
      while (isActive && _state.value.isPlaying) {
        val player = mediaPlayer
        if (player != null && isPrepared && runCatching { player.isPlaying }.getOrDefault(false)) {
          val pos = runCatching { player.currentPosition.toLong() }.getOrDefault(0L)
          val dur = runCatching { player.duration.toLong() }.getOrDefault(_state.value.durationMs)
          _state.update { it.copy(positionMs = pos, durationMs = dur.coerceAtLeast(it.durationMs)) }
          updateMediaSessionPlaybackState(isPlaying = true, positionMs = pos)
        }
        delay(400L)
      }
    }
  }

  private fun stopProgressTicker() {
    progressJob?.cancel()
    progressJob = null
  }

  private fun releasePlayerOnly() {
    stopProgressTicker()
    if (isPrepared) {
      runCatching { mediaPlayer?.stop() }
    }
    runCatching { mediaPlayer?.release() }
    mediaPlayer = null
    isPrepared = false
    playbackCompleted = false
  }

  private fun requestAudioFocus() {
    val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
          .build()
      )
      .setOnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
          AudioManager.AUDIOFOCUS_LOSS,
          AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
            pause()
          }
        }
      }
      .build()
    audioFocusRequest = focusReq
    runCatching {
      audioManager.requestAudioFocus(focusReq)
    }
  }

  private fun abandonAudioFocus() {
    audioFocusRequest?.let { req ->
      runCatching { audioManager.abandonAudioFocusRequest(req) }
    }
    audioFocusRequest = null
  }

  private fun updateMediaSessionMetadata(title: String, subtitle: String, durationMs: Long) {
    val metadata = MediaMetadata.Builder()
      .putString(MediaMetadata.METADATA_KEY_TITLE, title)
      .putString(MediaMetadata.METADATA_KEY_ARTIST, subtitle.ifBlank { "☀️ Summer Podcast" })
      .putString(MediaMetadata.METADATA_KEY_ALBUM, "Summer Catch Me Up")
      .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs)
      .build()
    mediaSession.setMetadata(metadata)
  }

  private fun updateMediaSessionPlaybackState(
    isPlaying: Boolean,
    positionMs: Long,
    stopped: Boolean = false
  ) {
    val stateCode = when {
      stopped -> PlaybackState.STATE_STOPPED
      isPlaying -> PlaybackState.STATE_PLAYING
      else -> PlaybackState.STATE_PAUSED
    }
    val pbState = PlaybackState.Builder()
      .setActions(
        PlaybackState.ACTION_PLAY or
          PlaybackState.ACTION_PAUSE or
          PlaybackState.ACTION_PLAY_PAUSE or
          PlaybackState.ACTION_REWIND or
          PlaybackState.ACTION_FAST_FORWARD or
          PlaybackState.ACTION_SKIP_TO_PREVIOUS or
          PlaybackState.ACTION_SKIP_TO_NEXT or
          PlaybackState.ACTION_SEEK_TO or
          PlaybackState.ACTION_STOP
      )
      .setState(stateCode, positionMs, if (isPlaying) 1.0f else 0f)
      .build()
    mediaSession.setPlaybackState(pbState)
  }

  companion object {
    @Volatile
    private var instance: PodcastPlaybackController? = null

    fun getInstance(context: Context): PodcastPlaybackController {
      return instance ?: synchronized(this) {
        instance ?: PodcastPlaybackController(context.applicationContext).also { instance = it }
      }
    }
  }
}
