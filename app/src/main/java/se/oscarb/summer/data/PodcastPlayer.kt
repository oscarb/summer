package se.oscarb.summer.data

import android.media.MediaPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * Plays back a synthesized [PodcastEpisode], moving through its cached audio parts in order and
 * reporting progress through [onProgress].
 *
 * When the episode has no cached audio (synthesis failed or is still running), playback falls back
 * to live text-to-speech via [PodcastSynthesizer].
 */
class PodcastPlayer(
  private val synthesizer: PodcastSynthesizer,
  private val onProgress: (episodeId: String, partIndex: Int, totalParts: Int) -> Unit,
  private val onFinished: (episodeId: String) -> Unit,
  private val onError: (episodeId: String, message: String) -> Unit
) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private var playbackJob: Job? = null
  private var mediaPlayer: MediaPlayer? = null

  private var activeEpisodeId: String? = null
  private var activeEpisode: PodcastEpisode? = null
  private var activeSettings: AppSettings = AppSettings()
  private var pausedPartIndex = 0

  val currentEpisodeId: String? get() = activeEpisodeId

  fun play(episode: PodcastEpisode, settings: AppSettings, startAtPart: Int = 0) {
    stopInternal(notify = false)
    activeEpisodeId = episode.id
    activeEpisode = episode
    activeSettings = settings
    pausedPartIndex = startAtPart.coerceAtLeast(0)

    playbackJob = scope.launch {
      try {
        val parts = episode.audioFilePaths.map { File(it) }.filter { it.exists() }
        if (parts.isEmpty()) {
          playWithLiveTts(episode, settings, pausedPartIndex)
        } else {
          playParts(episode, parts, pausedPartIndex)
        }
        onFinished(episode.id)
      } catch (e: CancellationException) {
        // Pausing/stopping cancels this job; that is not a playback failure.
        throw e
      } catch (e: Exception) {
        onError(episode.id, e.message ?: "Podcast playback failed.")
      }
    }
  }

  private suspend fun playParts(episode: PodcastEpisode, parts: List<File>, startAtPart: Int) {
    for (index in startAtPart until parts.size) {
      pausedPartIndex = index
      onProgress(episode.id, index, parts.size)
      playFile(parts[index])
    }
    pausedPartIndex = 0
  }

  private suspend fun playFile(file: File) = suspendCancellableCoroutine<Unit> { cont ->
    val player = MediaPlayer()
    mediaPlayer = player
    try {
      player.setDataSource(file.absolutePath)
      player.setOnPreparedListener { prepared ->
        if (cont.isActive) {
          prepared.start()
        } else {
          releasePlayer()
        }
      }
      player.setOnCompletionListener {
        releasePlayer()
        if (cont.isActive) cont.resume(Unit)
      }
      player.setOnErrorListener { _, _, _ ->
        releasePlayer()
        if (cont.isActive) cont.resume(Unit)
        true
      }
      player.prepareAsync()
    } catch (e: Exception) {
      releasePlayer()
      if (cont.isActive) cont.resumeWith(Result.failure(e))
    }
    cont.invokeOnCancellation {
      runCatching { player.stop() }
      releasePlayer()
    }
  }

  private suspend fun playWithLiveTts(episode: PodcastEpisode, settings: AppSettings, startAtPart: Int) {
    val chunks = PodcastScriptChunker.chunk(PodcastScriptChunker.toSpeakableText(episode.scriptMarkdown))
    if (chunks.isEmpty()) throw IllegalStateException("Nothing to read aloud for this episode.")
    for (index in startAtPart until chunks.size) {
      pausedPartIndex = index
      onProgress(episode.id, index, chunks.size)
      withContext(Dispatchers.Main) { synthesizer.speakChunk(chunks[index], settings) }
    }
    pausedPartIndex = 0
  }

  /** Pauses playback, keeping track of where to resume from. */
  fun pause() {
    val job = playbackJob
    playbackJob = null
    // Release synchronously so a stale completion listener cannot fire into the next playback.
    releasePlayer()
    synthesizer.stop()
    job?.cancel()
  }

  fun resume() {
    val episode = activeEpisode ?: return
    if (playbackJob?.isActive == true) return
    play(episode, activeSettings, startAtPart = pausedPartIndex)
  }

  fun stop() = stopInternal(notify = true)

  private fun stopInternal(notify: Boolean) {
    playbackJob?.cancel()
    playbackJob = null
    releasePlayer()
    synthesizer.stop()
    if (notify) {
      activeEpisodeId = null
      activeEpisode = null
      pausedPartIndex = 0
    }
  }

  private fun releasePlayer() {
    mediaPlayer?.let { player ->
      runCatching { player.reset() }
      runCatching { player.release() }
    }
    mediaPlayer = null
  }

  fun release() {
    stopInternal(notify = true)
    scope.cancel()
  }
}
