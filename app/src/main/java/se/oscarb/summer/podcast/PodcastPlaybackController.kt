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

/**
 * Manages podcast audio playback, MediaSession integration (lock screen, notification shade,
 * and Bluetooth headphone controls), audio focus, and background generation status.
 */
class PodcastPlaybackController private constructor(private val appContext: Context) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

  private val _state = MutableStateFlow(PodcastPlaybackState())
  val state: StateFlow<PodcastPlaybackState> = _state.asStateFlow()

  private var mediaPlayer: MediaPlayer? = null
  private var progressJob: Job? = null
  private var audioFocusRequest: AudioFocusRequest? = null

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
    websiteId: String? = null
  ) {
    if (!audioFile.exists()) return

    scope.launch {
      // If the exact same file is already loaded, just resume playback
      val current = _state.value
      if (current.isActive && current.audioFilePath == audioFile.absolutePath && mediaPlayer != null) {
        resume()
        return@launch
      }

      releasePlayerOnly()
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
          setOnCompletionListener {
            onPlaybackCompleted()
          }
          prepare()
          start()
        }

        mediaPlayer = player
        val duration = player.duration.toLong().coerceAtLeast(0L)

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
            durationMs = duration
          )
        }

        mediaSession.isActive = true
        updateMediaSessionMetadata(title, subtitle, duration)
        updateMediaSessionPlaybackState(isPlaying = true, positionMs = 0L)
        startProgressTicker()
        PodcastForegroundService.startOrUpdate(appContext)
      } catch (ce: CancellationException) {
        throw ce
      } catch (e: Exception) {
        releasePlayerOnly()
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
          websiteId = _state.value.activeWebsiteId
        )
      }
      return
    }
    requestAudioFocus()
    runCatching {
      if (!player.isPlaying) {
        player.start()
      }
    }
    val pos = runCatching { player.currentPosition.toLong() }.getOrDefault(_state.value.positionMs)
    _state.update { it.copy(isPlaying = true, positionMs = pos) }
    mediaSession.isActive = true
    updateMediaSessionPlaybackState(isPlaying = true, positionMs = pos)
    startProgressTicker()
    PodcastForegroundService.startOrUpdate(appContext)
  }

  fun seekTo(positionMs: Long) {
    val player = mediaPlayer ?: return
    val duration = _state.value.durationMs.coerceAtLeast(1L)
    val clamped = positionMs.coerceIn(0L, duration)
    runCatching {
      player.seekTo(clamped.toInt())
    }
    _state.update { it.copy(positionMs = clamped) }
    updateMediaSessionPlaybackState(isPlaying = _state.value.isPlaying, positionMs = clamped)
  }

  fun skipBy(deltaMs: Long) {
    val player = mediaPlayer ?: return
    val currentPos = runCatching { player.currentPosition.toLong() }.getOrDefault(_state.value.positionMs)
    seekTo(currentPos + deltaMs)
  }

  fun stop() {
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

  fun updateBackgroundGenerationStatus(
    isGenerating: Boolean,
    title: String? = null,
    statusText: String? = null
  ) {
    _state.update {
      it.copy(
        isGeneratingInBackground = isGenerating,
        generationTitle = if (isGenerating) title else null,
        generationStatusText = if (isGenerating) statusText else null
      )
    }
    PodcastForegroundService.startOrUpdate(appContext)
  }

  private fun onPlaybackCompleted() {
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
        if (player != null && runCatching { player.isPlaying }.getOrDefault(false)) {
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
    runCatching {
      mediaPlayer?.stop()
    }
    runCatching {
      mediaPlayer?.release()
    }
    mediaPlayer = null
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
