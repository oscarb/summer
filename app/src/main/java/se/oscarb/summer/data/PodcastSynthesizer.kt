package se.oscarb.summer.data

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device text-to-speech wrapper used to turn a podcast script into listenable audio.
 *
 * Speech is cached as per-chunk audio files in the app's private storage so an episode can be
 * replayed without regenerating it. If the platform engine cannot synthesize to a file, callers
 * can fall back to live [speakChunk] playback.
 */
class PodcastSynthesizer(private val appContext: Context) {

  private var tts: TextToSpeech? = null
  private var initialized = false

  private val utteranceListener = object : UtteranceProgressListener() {
    override fun onStart(utteranceId: String?) {
      pending[utteranceId]?.onStart?.invoke()
    }

    override fun onDone(utteranceId: String?) {
      pending.remove(utteranceId)?.onDone?.invoke()
    }

    @Deprecated("Deprecated in Java")
    override fun onError(utteranceId: String?) {
      pending.remove(utteranceId)?.onError?.invoke("Text-to-speech failed (code unknown).")
    }

    override fun onError(utteranceId: String?, errorCode: Int) {
      pending.remove(utteranceId)?.onError?.invoke("Text-to-speech failed (code $errorCode).")
    }

    override fun onStop(utteranceId: String?, interrupted: Boolean) {
      pending.remove(utteranceId)?.onError?.invoke("Text-to-speech playback was interrupted.")
    }
  }

  private class PendingUtterance(
    val onStart: (() -> Unit)? = null,
    val onDone: () -> Unit,
    val onError: (String) -> Unit
  )

  private val pending = ConcurrentHashMap<String, PendingUtterance>()

  /**
   * Initializes the underlying engine if needed. Safe to call repeatedly.
   * Throws if the engine cannot be initialized or has no usable language data.
   */
  suspend fun ensureReady(settings: AppSettings) = withContext(Dispatchers.Main) {
    if (initialized) return@withContext
    // A previous attempt may have left a half-initialized engine behind; retry from scratch.
    if (tts != null) release()

    val enginePackage = settings.podcastTtsEngine.trim().ifBlank { null }
    val deferred = CompletableDeferred<Int>()

    val engine = TextToSpeech(appContext, { status -> deferred.complete(status) }, enginePackage)
    engine.setOnUtteranceProgressListener(utteranceListener)
    tts = engine

    // The init callback is posted to the main looper, so the suspension lets it run.
    val status = withTimeoutOrNull(15_000) { deferred.await() }

    if (status == null) {
      release()
      throw IllegalStateException("Text-to-speech engine timed out while starting up.")
    }

    if (status != TextToSpeech.SUCCESS) {
      release()
      throw IllegalStateException(
        "Text-to-speech engine could not be initialized. Install or enable a system speech engine."
      )
    }

    val languageResult = engine.setLanguage(Locale.getDefault())
    if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
      release()
      throw IllegalStateException(
        "Text-to-speech has no voice data for ${Locale.getDefault().displayLanguage}. " +
          "Install the language in your device's text-to-speech settings."
      )
    }

    applyVoice(engine, settings)
    initialized = true
  }

  private fun applyVoice(engine: TextToSpeech, settings: AppSettings) {
    val requested = settings.podcastVoiceName.trim()
    if (requested.isNotEmpty()) {
      engine.voices?.firstOrNull { it.name == requested }?.let { engine.voice = it }
    }
    engine.setSpeechRate(settings.podcastSpeechRate.coerceIn(0.5f, 2.0f))
    engine.setPitch(settings.podcastSpeechPitch.coerceIn(0.5f, 2.0f))
  }

  /** Re-applies rate/pitch/voice settings, e.g. after the user changes them in Settings. */
  suspend fun applySettings(settings: AppSettings) = withContext(Dispatchers.Main) {
    val engine = tts ?: return@withContext
    if (!initialized) return@withContext
    applyVoice(engine, settings)
  }

  /**
   * Renders [text] to a list of cached audio files, one per speakable chunk, inside [outputDir].
   * Any files left over from a previous run for the same [baseName] are removed first.
   */
  suspend fun synthesizeToFiles(
    text: String,
    settings: AppSettings,
    outputDir: File,
    baseName: String,
    onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
  ): List<File> = withContext(Dispatchers.Main) {
    ensureReady(settings)

    val chunks = PodcastScriptChunker.chunk(PodcastScriptChunker.toSpeakableText(text))
    if (chunks.isEmpty()) throw IllegalStateException("The podcast script was empty.")

    if (!outputDir.exists()) outputDir.mkdirs()
    clearParts(outputDir, baseName)

    val files = mutableListOf<File>()
    chunks.forEachIndexed { index, chunk ->
      val file = File(outputDir, "${baseName}_${index}.wav")
      speakChunkToFile(chunk, file)
      files.add(file)
      onProgress(index + 1, chunks.size)
    }
    files
  }

  private suspend fun speakChunkToFile(chunk: String, file: File) {
    val engine = tts ?: throw IllegalStateException("Text-to-speech engine is not available.")
    val utteranceId = UUID.randomUUID().toString()

    suspendCancellableCoroutine<Unit> { cont ->
      pending[utteranceId] = PendingUtterance(
        onDone = { if (cont.isActive) cont.resume(Unit) },
        onError = { message -> if (cont.isActive) cont.resumeWithException(IllegalStateException(message)) }
      )
      cont.invokeOnCancellation { pending.remove(utteranceId) }

      val result = engine.synthesizeToFile(chunk, null, file, utteranceId)
      if (result != TextToSpeech.SUCCESS) {
        pending.remove(utteranceId)
        if (cont.isActive) {
          cont.resumeWithException(
            IllegalStateException("This text-to-speech engine cannot save audio to a file.")
          )
        }
      }
    }
  }

  /** Speaks a single chunk out loud, suspending until it finishes. Used for live fallback playback. */
  suspend fun speakChunk(chunk: String, settings: AppSettings) = withContext(Dispatchers.Main) {
    ensureReady(settings)
    val engine = tts ?: throw IllegalStateException("Text-to-speech engine is not available.")
    val utteranceId = UUID.randomUUID().toString()

    suspendCancellableCoroutine<Unit> { cont ->
      pending[utteranceId] = PendingUtterance(
        onDone = { if (cont.isActive) cont.resume(Unit) },
        onError = { message -> if (cont.isActive) cont.resumeWithException(IllegalStateException(message)) }
      )
      cont.invokeOnCancellation { pending.remove(utteranceId) }
      engine.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }
  }

  fun stop() {
    runCatching { tts?.stop() }
  }

  fun release() {
    runCatching { tts?.stop() }
    runCatching { tts?.shutdown() }
    tts = null
    initialized = false
    pending.clear()
  }

  /** Voices offered by the currently configured engine for the device's default language. */
  suspend fun availableVoices(settings: AppSettings): List<TtsVoiceOption> = withContext(Dispatchers.Main) {
    runCatching {
      ensureReady(settings)
      val engine = tts ?: return@withContext emptyList()
      val language = Locale.getDefault().language
      engine.voices.orEmpty()
        .filter { it.locale.language == language && !it.isNetworkConnectionRequired }
        .map { voice ->
          val quality = when (voice.quality) {
            android.speech.tts.Voice.QUALITY_VERY_HIGH -> "Very high"
            android.speech.tts.Voice.QUALITY_HIGH -> "High"
            android.speech.tts.Voice.QUALITY_NORMAL -> "Normal"
            android.speech.tts.Voice.QUALITY_VERY_LOW -> "Very low"
            else -> "Low"
          }
          TtsVoiceOption(name = voice.name, label = "${voice.locale.displayName} • $quality")
        }
        .distinctBy { it.name }
        .sortedBy { it.label }
    }.getOrDefault(emptyList())
  }

  private fun clearParts(outputDir: File, baseName: String) {
    runCatching {
      outputDir.listFiles { f -> f.name.startsWith("${baseName}_") }?.forEach { it.delete() }
    }
  }
}

/** A text-to-speech engine installed on the device. */
data class TtsEngineOption(val packageName: String, val label: String)

/** A single voice offered by a text-to-speech engine. */
data class TtsVoiceOption(val name: String, val label: String)

/** Lightweight lookups for the podcast voice settings, without spinning up an engine. */
object PodcastVoiceCatalog {
  fun engines(context: Context): List<TtsEngineOption> = runCatching {
    val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
    context.packageManager.queryIntentServices(intent, 0)
      .mapNotNull { resolved ->
        val service = resolved.serviceInfo ?: return@mapNotNull null
        val label = service.loadLabel(context.packageManager).toString()
          .ifBlank { service.packageName }
        TtsEngineOption(packageName = service.packageName, label = label)
      }
      .distinctBy { it.packageName }
      .sortedBy { it.label.lowercase() }
  }.getOrDefault(emptyList())
}
