package se.oscarb.summer.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import se.oscarb.summer.util.PodcastScriptParser
import se.oscarb.summer.util.WavAudioUtils
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PodcastAudioResult(
  val audioFile: File,
  val providerUsed: String,
  val turnsCount: Int
)

/**
 * Multi-backend Text-to-Speech synthesis engine for generating 2-host podcast episodes
 * and natural spoken article summaries.
 *
 * Supports:
 * 1. Google Gemini Native Multi-Speaker TTS (`gemini-2.5-flash-preview-tts` via Google AI Studio free tier)
 * 2. Android On-Device System Multi-Voice TTS (100% free & offline via `TextToSpeech.synthesizeToFile`)
 * 3. Microsoft Azure AI Speech (500,000 neural chars/month perpetual free tier via multi-voice SSML)
 * 4. ElevenLabs Neural TTS (`pcm_24000` turn-by-turn stitching)
 * 5. OpenAI / Kokoro / Custom `/v1/audio/speech` endpoint
 */
class PodcastTtsService(private val appContext: Context) {

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(45, TimeUnit.SECONDS)
    .readTimeout(180, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

  private val json = Json { ignoreUnknownKeys = true }

  suspend fun synthesizePodcastScript(
    rawScript: String,
    outputWavFile: File,
    settings: AppSettings,
    onProgress: (String) -> Unit = {}
  ): Result<PodcastAudioResult> = withContext(Dispatchers.IO) {
    val hostA = settings.hostAName.trim().ifBlank { "Alex" }
    val hostB = settings.hostBName.trim().ifBlank { "Sam" }
    val turns = PodcastScriptParser.parseTurns(rawScript, hostA, hostB)

    if (turns.isEmpty()) {
      return@withContext Result.failure(IllegalArgumentException("Podcast script is empty."))
    }

    outputWavFile.parentFile?.mkdirs()

    try {
      val result = when (settings.podcastTtsProvider) {
        PodcastTtsProvider.GEMINI_MULTISPEAKER -> {
          onProgress("Synthesizing 2-host audio with Gemini Multi-Speaker TTS...")
          synthesizeWithGeminiMultiSpeaker(turns, outputWavFile, settings, onProgress)
        }
        PodcastTtsProvider.ANDROID_ON_DEVICE -> {
          onProgress("Synthesizing 2-host audio on-device...")
          synthesizeWithAndroidOnDeviceTts(turns, outputWavFile, settings, onProgress)
        }
        PodcastTtsProvider.AZURE_SPEECH -> {
          onProgress("Synthesizing 2-host SSML audio via Azure AI Speech...")
          synthesizeWithAzureSpeech(turns, outputWavFile, settings)
        }
        PodcastTtsProvider.ELEVENLABS -> {
          onProgress("Synthesizing 2-host voices via ElevenLabs...")
          synthesizeWithElevenLabs(turns, outputWavFile, settings, onProgress)
        }
        PodcastTtsProvider.OPENAI_COMPATIBLE_TTS -> {
          onProgress("Synthesizing 2-host audio via OpenAI / Custom TTS...")
          synthesizeWithOpenAiCompatibleTts(turns, outputWavFile, settings, onProgress)
        }
      }
      Result.success(result)
    } catch (ce: CancellationException) {
      throw ce
    } catch (primaryError: Exception) {
      if (settings.fallbackToOnDeviceTts && settings.podcastTtsProvider != PodcastTtsProvider.ANDROID_ON_DEVICE) {
        try {
          onProgress("Cloud TTS unavailable (${primaryError.message?.take(60)}); falling back to On-Device Multi-Voice TTS...")
          val fallbackResult = synthesizeWithAndroidOnDeviceTts(turns, outputWavFile, settings, onProgress)
          Result.success(
            fallbackResult.copy(
              providerUsed = "${PodcastTtsProvider.ANDROID_ON_DEVICE.displayName} (Auto-Fallback)"
            )
          )
        } catch (ce: CancellationException) {
          throw ce
        } catch (fallbackError: Exception) {
          Result.failure(primaryError)
        }
      } else {
        Result.failure(primaryError)
      }
    }
  }

  /**
   * Verifies that the configured Google AI Studio key and Gemini TTS model support
   * native 2-speaker `multiSpeakerVoiceConfig` audio generation.
   */
  suspend fun verifyGeminiTtsAccess(settings: AppSettings): Result<String> = withContext(Dispatchers.IO) {
    try {
      val apiKey = settings.directGeminiApiKey.trim()
      if (apiKey.isBlank()) {
        return@withContext Result.failure(
          IllegalStateException("Enter your Google AI Studio API key first.")
        )
      }

      val model = settings.geminiTtsModel.trim().ifBlank { "gemini-3.8-flash-tts" }
      val hostA = settings.hostAName.trim().ifBlank { "Alex" }
      val hostB = settings.hostBName.trim().ifBlank { "Sam" }
      val voiceA = GeminiCatalog.resolveValidVoice(settings.geminiHostAVoice, "Puck")
      val voiceB = GeminiCatalog.resolveValidVoice(settings.geminiHostBVoice, "Kore")

      // 1. Query /v1beta/models to list TTS models enabled on this key
      val availableTtsModels = runCatching {
        val listReq = Request.Builder()
          .url("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
          .get()
          .build()
        httpClient.newCall(listReq).execute().use { resp ->
          if (!resp.isSuccessful) emptyList()
          else {
            val root = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
            root["models"]?.jsonArray.orEmpty()
              .mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content?.removePrefix("models/") }
              .filter { it.contains("tts", ignoreCase = true) }
          }
        }
      }.getOrDefault(emptyList())

      // 2. Run a tiny live 2-speaker multiSpeakerVoiceConfig test call
      val testTurns = listOf(
        DialogueTurn(speaker = hostA, isHostB = false, text = "Hey $hostB!"),
        DialogueTurn(speaker = hostB, isHostB = true, text = "Ready to go!")
      )
      val (pcmBytes, sampleRate) = requestGeminiMultiSpeakerBatch(
        batchTurns = testTurns,
        model = model,
        apiKey = apiKey,
        hostA = hostA,
        hostB = hostB,
        voiceA = voiceA,
        voiceB = voiceB
      )

      val modelsNote = if (availableTtsModels.isNotEmpty()) {
        "\nAvailable TTS models on your key: ${availableTtsModels.joinToString(", ")}"
      } else {
        ""
      }

      Result.success(
        "Verified $model! Generated native 2-speaker audio (${pcmBytes.size / 1024} KB at ${sampleRate} Hz) using voices $voiceA & $voiceB.$modelsNote"
      )
    } catch (ce: CancellationException) {
      throw ce
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun synthesizeWithGeminiMultiSpeaker(
    turns: List<DialogueTurn>,
    outputWavFile: File,
    settings: AppSettings,
    onProgress: (String) -> Unit
  ): PodcastAudioResult {
    val apiKey = settings.directGeminiApiKey.trim()
    if (apiKey.isBlank()) {
      throw IllegalStateException(
        "Google AI Studio API key is required for Gemini Multi-Speaker TTS. Add your free key in Settings."
      )
    }

    val model = settings.geminiTtsModel.trim().ifBlank { "gemini-3.8-flash-tts" }
    val hostA = settings.hostAName.trim().ifBlank { "Alex" }
    val hostB = settings.hostBName.trim().ifBlank { "Sam" }
    val voiceA = GeminiCatalog.resolveValidVoice(settings.geminiHostAVoice, "Puck")
    val voiceB = GeminiCatalog.resolveValidVoice(settings.geminiHostBVoice, "Kore")

    // Gemini multiSpeakerVoiceConfig synthesizes both hosts together natively in one pass.
    // Its dialogue prompt payload limit is ~4,000 bytes, so normal articles & digests run in
    // 1 single native pass, while extra-long digests batch turns into <= 3,400-byte multi-speaker scenes.
    val batches = groupTurnsIntoMultiSpeakerBatches(turns, hostA, hostB, maxUtf8Bytes = 3_400)

    if (batches.size == 1) {
      val (rawBytes, sampleRate) = requestGeminiMultiSpeakerBatch(
        batchTurns = batches[0],
        model = model,
        apiKey = apiKey,
        hostA = hostA,
        hostB = hostB,
        voiceA = voiceA,
        voiceB = voiceB
      )
      val wavBytes = WavAudioUtils.ensureWavHeader(
        pcmOrWavBytes = rawBytes,
        sampleRate = sampleRate,
        channels = 1,
        bitsPerSample = 16
      )
      outputWavFile.writeBytes(wavBytes)
    } else {
      val pcmChunks = mutableListOf<ByteArray>()
      var detectedSampleRate = 24_000
      batches.forEachIndexed { batchIndex, batchTurns ->
        onProgress("Gemini Multi-Speaker TTS ($model): Synthesizing dialogue scene ${batchIndex + 1}/${batches.size}...")
        val (rawBytes, sampleRate) = requestGeminiMultiSpeakerBatch(
          batchTurns = batchTurns,
          model = model,
          apiKey = apiKey,
          hostA = hostA,
          hostB = hostB,
          voiceA = voiceA,
          voiceB = voiceB
        )
        detectedSampleRate = sampleRate
        val parsedPcm = if (WavAudioUtils.isRiffWave(rawBytes)) {
          WavAudioUtils.parseWav(rawBytes).pcmBytes
        } else {
          rawBytes
        }
        if (parsedPcm.isNotEmpty()) {
          pcmChunks.add(parsedPcm)
        }
      }
      WavAudioUtils.stitchPcmChunksToWavFile(
        pcmChunks = pcmChunks,
        outputFile = outputWavFile,
        sampleRate = detectedSampleRate,
        channels = 1,
        bitsPerSample = 16,
        pauseMs = 240
      )
    }

    return PodcastAudioResult(
      audioFile = outputWavFile,
      providerUsed = "$model ($voiceA & $voiceB)",
      turnsCount = turns.size
    )
  }

  private fun groupTurnsIntoMultiSpeakerBatches(
    turns: List<DialogueTurn>,
    hostA: String,
    hostB: String,
    maxUtf8Bytes: Int
  ): List<List<DialogueTurn>> {
    if (turns.isEmpty()) return emptyList()
    val batches = mutableListOf<List<DialogueTurn>>()
    var currentBatch = mutableListOf<DialogueTurn>()
    var currentBytes = 0

    for (turn in turns) {
      val speaker = if (turn.isHostB) hostB else hostA
      val lineBytes = "$speaker: ${turn.text}\n".toByteArray(Charsets.UTF_8).size
      if (currentBatch.isNotEmpty() && currentBytes + lineBytes > maxUtf8Bytes) {
        batches.add(currentBatch)
        currentBatch = mutableListOf()
        currentBytes = 0
      }
      currentBatch.add(turn)
      currentBytes += lineBytes
    }
    if (currentBatch.isNotEmpty()) {
      batches.add(currentBatch)
    }
    return batches
  }

  private fun requestGeminiMultiSpeakerBatch(
    batchTurns: List<DialogueTurn>,
    model: String,
    apiKey: String,
    hostA: String,
    hostB: String,
    voiceA: String,
    voiceB: String
  ): Pair<ByteArray, Int> {
    val defaultUseSpeechMetadata =
      !model.startsWith("gemini-2.", ignoreCase = true) &&
        !model.startsWith("gemini-3.1", ignoreCase = true)

    return try {
      executeGeminiMultiSpeakerHttpCall(
        batchTurns = batchTurns,
        model = model,
        apiKey = apiKey,
        hostA = hostA,
        hostB = hostB,
        voiceA = voiceA,
        voiceB = voiceB,
        usePerPartSpeechMetadata = defaultUseSpeechMetadata
      )
    } catch (e: IllegalStateException) {
      val msg = e.message.orEmpty()
      val needsSpeechMetadata = !defaultUseSpeechMetadata && msg.contains("speech_metadata", ignoreCase = true)
      val forbidsSpeechMetadata = defaultUseSpeechMetadata && msg.contains("Speech metadata is not supported", ignoreCase = true)
      if (needsSpeechMetadata || forbidsSpeechMetadata) {
        executeGeminiMultiSpeakerHttpCall(
          batchTurns = batchTurns,
          model = model,
          apiKey = apiKey,
          hostA = hostA,
          hostB = hostB,
          voiceA = voiceA,
          voiceB = voiceB,
          usePerPartSpeechMetadata = !defaultUseSpeechMetadata
        )
      } else {
        throw e
      }
    }
  }

  private fun executeGeminiMultiSpeakerHttpCall(
    batchTurns: List<DialogueTurn>,
    model: String,
    apiKey: String,
    hostA: String,
    hostB: String,
    voiceA: String,
    voiceB: String,
    usePerPartSpeechMetadata: Boolean
  ): Pair<ByteArray, Int> {
    val endpoint =
      "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

    val requestPayload = buildJsonObject {
      put("contents", buildJsonArray {
        add(buildJsonObject {
          put("parts", buildJsonArray {
            if (usePerPartSpeechMetadata) {
              // Gemini 3.8+ TTS multi-speaker format: one part per dialogue turn with speech_metadata.speaker
              batchTurns.forEach { turn ->
                val speakerName = if (turn.isHostB) hostB else hostA
                add(buildJsonObject {
                  put("text", JsonPrimitive(turn.text))
                  put("speech_metadata", buildJsonObject {
                    put("speaker", JsonPrimitive(speakerName))
                  })
                })
              }
            } else {
              // Gemini 2.5 / 3.1 TTS multi-speaker format: single formatted dialogue block
              val formattedDialogue =
                PodcastScriptParser.formatForGeminiMultiSpeaker(batchTurns, hostA, hostB)
              val ttsPrompt = """
                Read the following podcast conversation between $hostA and $hostB naturally, with warm, engaging podcast energy and smooth conversational transitions:
                
                $formattedDialogue
              """.trimIndent()
              add(buildJsonObject {
                put("text", JsonPrimitive(ttsPrompt))
              })
            }
          })
        })
      })
      put("generationConfig", buildJsonObject {
        put("responseModalities", buildJsonArray {
          add(JsonPrimitive("AUDIO"))
        })
        put("speechConfig", buildJsonObject {
          put("multiSpeakerVoiceConfig", buildJsonObject {
            put("speakerVoiceConfigs", buildJsonArray {
              add(buildJsonObject {
                put("speaker", JsonPrimitive(hostA))
                put("voiceConfig", buildJsonObject {
                  put("prebuiltVoiceConfig", buildJsonObject {
                    put("voiceName", JsonPrimitive(voiceA))
                  })
                })
              })
              add(buildJsonObject {
                put("speaker", JsonPrimitive(hostB))
                put("voiceConfig", buildJsonObject {
                  put("prebuiltVoiceConfig", buildJsonObject {
                    put("voiceName", JsonPrimitive(voiceB))
                  })
                })
              })
            })
          })
        })
      })
    }.toString()

    val request = Request.Builder()
      .url(endpoint)
      .post(requestPayload.toRequestBody("application/json; charset=utf-8".toMediaType()))
      .build()

    httpClient.newCall(request).execute().use { response ->
      val bodyStr = response.body?.string().orEmpty()
      if (!response.isSuccessful) {
        throw IllegalStateException("Gemini TTS ($model) HTTP ${response.code}: ${bodyStr.take(280)}")
      }

      val root = json.parseToJsonElement(bodyStr).jsonObject
      val candidates = root["candidates"]?.jsonArray
      val firstCandidate = candidates?.firstOrNull()?.jsonObject
      val parts = firstCandidate?.get("content")?.jsonObject?.get("parts")?.jsonArray

      val inlineData = parts
        ?.mapNotNull { it.jsonObject["inlineData"]?.jsonObject }
        ?.firstOrNull()
        ?: throw IllegalStateException("Gemini TTS response did not contain audio inlineData.")

      val base64Audio = inlineData["data"]?.jsonPrimitive?.content
        ?: throw IllegalStateException("Gemini TTS inlineData.data was empty.")
      val mimeType = inlineData["mimeType"]?.jsonPrimitive?.content

      val rawBytes = Base64.decode(base64Audio, Base64.DEFAULT)
      val sampleRate = if (WavAudioUtils.isRiffWave(rawBytes)) {
        WavAudioUtils.parseWav(rawBytes).sampleRate
      } else {
        WavAudioUtils.extractSampleRateFromMimeType(mimeType, defaultRate = 24_000)
      }
      return rawBytes to sampleRate
    }
  }

  private fun synthesizeWithAzureSpeech(
    turns: List<DialogueTurn>,
    outputWavFile: File,
    settings: AppSettings
  ): PodcastAudioResult {
    val apiKey = settings.azureSpeechKey.trim()
    val region = settings.azureSpeechRegion.trim().ifBlank { "eastus" }
    if (apiKey.isBlank()) {
      throw IllegalStateException("Azure Speech key is missing. Add your free Azure Speech key in Settings.")
    }

    val voiceA = settings.azureHostAVoice.trim().ifBlank { "en-US-AndrewMultilingualNeural" }
    val voiceB = settings.azureHostBVoice.trim().ifBlank { "en-US-AvaMultilingualNeural" }
    val ssml = PodcastScriptParser.formatAsAzureMultiVoiceSsml(turns, voiceA, voiceB)

    val endpoint = "https://$region.tts.speech.microsoft.com/cognitiveservices/v1"
    val request = Request.Builder()
      .url(endpoint)
      .post(ssml.toRequestBody("application/ssml+xml; charset=utf-8".toMediaType()))
      .header("Ocp-Apim-Subscription-Key", apiKey)
      .header("X-Microsoft-OutputFormat", "riff-24khz-16bit-mono-pcm")
      .header("User-Agent", "SummerPodcastAndroid")
      .build()

    httpClient.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        val errText = response.body?.string().orEmpty()
        throw IllegalStateException("Azure Speech HTTP ${response.code}: ${errText.take(250)}")
      }
      val bytes = response.body?.bytes()
        ?: throw IllegalStateException("Empty audio response from Azure Speech.")
      val wavBytes = WavAudioUtils.ensureWavHeader(bytes, sampleRate = 24_000)
      outputWavFile.writeBytes(wavBytes)
      return PodcastAudioResult(
        audioFile = outputWavFile,
        providerUsed = "Azure Neural TTS ($voiceA & $voiceB)",
        turnsCount = turns.size
      )
    }
  }

  private fun synthesizeWithElevenLabs(
    turns: List<DialogueTurn>,
    outputWavFile: File,
    settings: AppSettings,
    onProgress: (String) -> Unit
  ): PodcastAudioResult {
    val apiKey = settings.elevenLabsApiKey.trim()
    if (apiKey.isBlank()) {
      throw IllegalStateException("ElevenLabs API key is missing. Add it in Settings.")
    }

    val modelId = settings.elevenLabsModel.trim().ifBlank { "eleven_flash_v2_5" }
    val voiceA = settings.elevenLabsHostAVoiceId.trim().ifBlank { "pNInz6obpgDQGcFmaJgB" }
    val voiceB = settings.elevenLabsHostBVoiceId.trim().ifBlank { "21m00Tcm4TlvDq8ikWAM" }

    val pcmChunks = mutableListOf<ByteArray>()
    turns.forEachIndexed { index, turn ->
      onProgress("ElevenLabs: Synthesizing turn ${index + 1}/${turns.size} (${turn.speaker})...")
      val voiceId = if (turn.isHostB) voiceB else voiceA
      val endpoint = "https://api.elevenlabs.io/v1/text-to-speech/$voiceId?output_format=pcm_24000"
      val payload = buildJsonObject {
        put("text", JsonPrimitive(turn.text))
        put("model_id", JsonPrimitive(modelId))
      }.toString()

      val request = Request.Builder()
        .url(endpoint)
        .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
        .header("xi-api-key", apiKey)
        .header("Content-Type", "application/json")
        .build()

      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          val errText = response.body?.string().orEmpty()
          throw IllegalStateException("ElevenLabs HTTP ${response.code}: ${errText.take(250)}")
        }
        val pcm = response.body?.bytes() ?: ByteArray(0)
        if (pcm.isNotEmpty()) {
          pcmChunks.add(pcm)
        }
      }
    }

    WavAudioUtils.stitchPcmChunksToWavFile(
      pcmChunks = pcmChunks,
      outputFile = outputWavFile,
      sampleRate = 24_000,
      channels = 1,
      bitsPerSample = 16,
      pauseMs = 260
    )

    return PodcastAudioResult(
      audioFile = outputWavFile,
      providerUsed = "ElevenLabs Neural TTS",
      turnsCount = turns.size
    )
  }

  private fun synthesizeWithOpenAiCompatibleTts(
    turns: List<DialogueTurn>,
    outputWavFile: File,
    settings: AppSettings,
    onProgress: (String) -> Unit
  ): PodcastAudioResult {
    val baseUrl = settings.openAiTtsBaseUrl.trim().trimEnd('/').ifBlank {
      settings.openAiBaseUrl.trim().trimEnd('/')
    }
    val apiKey = settings.openAiTtsApiKey.trim().ifBlank { settings.openAiApiKey.trim() }
    val isLocalOrCustom = !baseUrl.contains("api.openai.com")
    if (apiKey.isBlank() && !isLocalOrCustom) {
      throw IllegalStateException("OpenAI API key is missing. Add it in Settings.")
    }

    val model = settings.openAiTtsModel.trim().ifBlank { "gpt-4o-mini-tts" }
    val voiceA = settings.openAiHostAVoice.trim().ifBlank { "onyx" }
    val voiceB = settings.openAiHostBVoice.trim().ifBlank { "nova" }
    val endpoint = if (baseUrl.endsWith("/audio/speech")) baseUrl else "$baseUrl/audio/speech"

    val tempDir = File(appContext.cacheDir, "tts_openai_${UUID.randomUUID()}")
    tempDir.mkdirs()
    val turnFiles = mutableListOf<File>()

    try {
      turns.forEachIndexed { index, turn ->
        onProgress("Synthesizing turn ${index + 1}/${turns.size} (${turn.speaker})...")
        val voice = if (turn.isHostB) voiceB else voiceA
        val payload = buildJsonObject {
          put("model", JsonPrimitive(model))
          put("input", JsonPrimitive(turn.text))
          put("voice", JsonPrimitive(voice))
          put("response_format", JsonPrimitive("wav"))
        }.toString()

        val reqBuilder = Request.Builder()
          .url(endpoint)
          .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
          .header("Content-Type", "application/json")
        if (apiKey.isNotBlank()) {
          reqBuilder.header("Authorization", "Bearer $apiKey")
        }

        httpClient.newCall(reqBuilder.build()).execute().use { response ->
          if (!response.isSuccessful) {
            val err = response.body?.string().orEmpty()
            throw IllegalStateException("TTS HTTP ${response.code}: ${err.take(250)}")
          }
          val bytes = response.body?.bytes() ?: ByteArray(0)
          if (bytes.isNotEmpty()) {
            val wavFile = File(tempDir, "turn_$index.wav")
            wavFile.writeBytes(WavAudioUtils.ensureWavHeader(bytes, sampleRate = 24_000))
            turnFiles.add(wavFile)
          }
        }
      }

      WavAudioUtils.stitchWavFiles(
        inputWavFiles = turnFiles,
        outputFile = outputWavFile,
        pauseMs = 260
      )

      return PodcastAudioResult(
        audioFile = outputWavFile,
        providerUsed = "OpenAI / Custom TTS ($voiceA & $voiceB)",
        turnsCount = turns.size
      )
    } finally {
      tempDir.deleteRecursively()
    }
  }

  private suspend fun synthesizeWithAndroidOnDeviceTts(
    turns: List<DialogueTurn>,
    outputWavFile: File,
    settings: AppSettings,
    onProgress: (String) -> Unit
  ): PodcastAudioResult {
    val tempDir = File(appContext.cacheDir, "tts_android_${UUID.randomUUID()}")
    tempDir.mkdirs()

    var ttsInstance: TextToSpeech? = null
    try {
      val preferredEngine = settings.androidTtsEngine.trim().ifBlank { null }
      val tts = try {
        initAndroidTts(appContext, preferredEngine)
      } catch (ce: CancellationException) {
        throw ce
      } catch (engineErr: Exception) {
        if (preferredEngine != null) {
          initAndroidTts(appContext, null)
        } else {
          throw engineErr
        }
      }
      ttsInstance = tts

      val (voiceA, voiceB) = resolveTwoHostVoices(
        tts = tts,
        preferredVoiceAName = settings.androidHostAVoice.trim(),
        preferredVoiceBName = settings.androidHostBVoice.trim()
      )
      val baseRate = settings.androidSpeechRate.coerceIn(0.5f, 2.0f)
      val basePitch = settings.androidSpeechPitch.coerceIn(0.5f, 2.0f)
      val hasDistinctVoices = voiceA != null && voiceB != null && voiceA.name != voiceB.name

      val turnFiles = mutableListOf<File>()

      turns.forEachIndexed { index, turn ->
        onProgress("On-device TTS: Synthesizing turn ${index + 1}/${turns.size} (${turn.speaker})...")
        if (turn.isHostB) {
          if (voiceB != null) tts.voice = voiceB
          val pitchOffset = if (hasDistinctVoices) 1.01f else 1.10f
          tts.setPitch((basePitch * pitchOffset).coerceIn(0.5f, 2.0f))
          tts.setSpeechRate((baseRate * 1.01f).coerceIn(0.5f, 2.0f))
        } else {
          if (voiceA != null) tts.voice = voiceA
          val pitchOffset = if (hasDistinctVoices) 0.99f else 0.92f
          tts.setPitch((basePitch * pitchOffset).coerceIn(0.5f, 2.0f))
          tts.setSpeechRate(baseRate)
        }

        // Sub-chunk oversized turns (~650 chars max on sentence boundaries) so strict
        // OEM TextToSpeech engines never truncate long dialogue paragraphs.
        val subChunks = PodcastScriptParser.chunkTurnText(
          turn.text,
          maxChars = PodcastScriptParser.DEFAULT_MAX_TURN_CHUNK_CHARS
        )
        if (subChunks.isEmpty()) return@forEachIndexed

        val turnFile = File(tempDir, "turn_$index.wav")
        if (subChunks.size == 1) {
          synthesizeSingleUtteranceToFile(tts, subChunks[0], turnFile)
        } else {
          val subFiles = mutableListOf<File>()
          subChunks.forEachIndexed { subIdx, chunkText ->
            val subFile = File(tempDir, "turn_${index}_part_$subIdx.wav")
            synthesizeSingleUtteranceToFile(tts, chunkText, subFile)
            if (subFile.exists() && subFile.length() > 44) {
              subFiles.add(subFile)
            }
          }
          if (subFiles.isNotEmpty()) {
            WavAudioUtils.stitchWavFiles(
              inputWavFiles = subFiles,
              outputFile = turnFile,
              pauseMs = 90
            )
          }
        }

        if (turnFile.exists() && turnFile.length() > 44) {
          turnFiles.add(turnFile)
        }
      }

      if (turnFiles.isEmpty()) {
        throw IllegalStateException("On-device TextToSpeech produced no audio output.")
      }

      WavAudioUtils.stitchWavFiles(
        inputWavFiles = turnFiles,
        outputFile = outputWavFile,
        pauseMs = 250
      )

      return PodcastAudioResult(
        audioFile = outputWavFile,
        providerUsed = PodcastTtsProvider.ANDROID_ON_DEVICE.displayName,
        turnsCount = turns.size
      )
    } finally {
      runCatching { ttsInstance?.stop() }
      runCatching { ttsInstance?.shutdown() }
      tempDir.deleteRecursively()
    }
  }

  private suspend fun initAndroidTts(
    context: Context,
    enginePackage: String?
  ): TextToSpeech = withContext(Dispatchers.Main) {
    val deferred = CompletableDeferred<Int>()
    val created = TextToSpeech(
      context.applicationContext,
      { status -> deferred.complete(status) },
      enginePackage
    )
    val status = try {
      withTimeoutOrNull(15_000L) { deferred.await() }
    } catch (ce: CancellationException) {
      runCatching { created.shutdown() }
      throw ce
    }

    if (status == null) {
      runCatching { created.shutdown() }
      throw IllegalStateException("Android TextToSpeech engine timed out while initializing.")
    }
    if (status != TextToSpeech.SUCCESS) {
      runCatching { created.shutdown() }
      throw IllegalStateException("Android TextToSpeech engine failed to initialize (status=$status).")
    }

    val langRes = created.setLanguage(Locale.US)
    if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
      created.setLanguage(Locale.getDefault())
    }
    created
  }

  private fun resolveTwoHostVoices(
    tts: TextToSpeech,
    preferredVoiceAName: String,
    preferredVoiceBName: String
  ): Pair<Voice?, Voice?> {
    return runCatching {
      val allVoices = tts.voices.orEmpty()
      val explicitA = preferredVoiceAName.takeIf { it.isNotBlank() }?.let { target ->
        allVoices.firstOrNull { it.name == target }
      }
      val explicitB = preferredVoiceBName.takeIf { it.isNotBlank() }?.let { target ->
        allVoices.firstOrNull { it.name == target }
      }

      if (explicitA != null && explicitB != null) {
        return@runCatching Pair(explicitA, explicitB)
      }

      val available = allVoices.filter { voice ->
        voice.locale.language.equals("en", ignoreCase = true) &&
          !voice.isNetworkConnectionRequired
      }.ifEmpty {
        allVoices.filter {
          it.locale.language.equals("en", ignoreCase = true) ||
            it.locale.language.equals(Locale.getDefault().language, ignoreCase = true)
        }
      }.sortedByDescending { it.quality }

      val resolvedA = explicitA ?: available.firstOrNull() ?: tts.defaultVoice
      val resolvedB = explicitB
        ?: available.firstOrNull { it.name != resolvedA?.name }
        ?: resolvedA

      Pair(resolvedA, resolvedB)
    }.getOrElse { Pair(null, null) }
  }

  private suspend fun synthesizeSingleUtteranceToFile(
    tts: TextToSpeech,
    text: String,
    destFile: File
  ) = suspendCancellableCoroutine<Unit> { cont ->
    val utteranceId = "utt_${UUID.randomUUID()}"
    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
      override fun onStart(id: String?) = Unit

      override fun onDone(id: String?) {
        if (id == utteranceId && cont.isActive) {
          cont.resume(Unit)
        }
      }

      @Deprecated("Deprecated in Java")
      override fun onError(id: String?) {
        if (id == utteranceId && cont.isActive) {
          cont.resumeWithException(IllegalStateException("Android TTS failed on utterance $id"))
        }
      }

      override fun onError(id: String?, errorCode: Int) {
        if (id == utteranceId && cont.isActive) {
          cont.resumeWithException(
            IllegalStateException("Android TTS failed on utterance $id (errorCode=$errorCode)")
          )
        }
      }

      override fun onStop(id: String?, interrupted: Boolean) {
        if (id == utteranceId && cont.isActive) {
          cont.resumeWithException(
            IllegalStateException("Android TTS synthesis was interrupted on utterance $id")
          )
        }
      }
    })

    val params = Bundle()
    val res = tts.synthesizeToFile(text, params, destFile, utteranceId)
    if (res != TextToSpeech.SUCCESS && cont.isActive) {
      cont.resumeWithException(IllegalStateException("TextToSpeech.synthesizeToFile returned error $res"))
    }
  }
}

data class TtsEngineOption(
  val packageName: String,
  val label: String
)

data class TtsVoiceOption(
  val name: String,
  val label: String
)

object OnDeviceTtsCatalog {
  fun installedEngines(context: Context): List<TtsEngineOption> = runCatching {
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

  suspend fun availableVoices(
    context: Context,
    enginePackage: String
  ): List<TtsVoiceOption> = withContext(Dispatchers.Main) {
    var tts: TextToSpeech? = null
    try {
      val deferred = CompletableDeferred<Int>()
      val enginePkg = enginePackage.trim().ifBlank { null }
      val created = TextToSpeech(
        context.applicationContext,
        { status -> deferred.complete(status) },
        enginePkg
      )
      tts = created

      val status = withTimeoutOrNull(10_000L) { deferred.await() }
      if (status != TextToSpeech.SUCCESS) return@withContext emptyList()

      val defaultLang = Locale.getDefault().language
      val matching = created.voices.orEmpty().filter { voice ->
        !voice.isNetworkConnectionRequired &&
          (voice.locale.language.equals("en", ignoreCase = true) ||
            voice.locale.language.equals(defaultLang, ignoreCase = true))
      }.ifEmpty {
        created.voices.orEmpty().filter { !it.isNetworkConnectionRequired }
      }

      matching
        .sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.locale.displayName }.thenBy { it.name })
        .map { voice ->
          val qualityLabel = when (voice.quality) {
            Voice.QUALITY_VERY_HIGH -> "Very high"
            Voice.QUALITY_HIGH -> "High"
            Voice.QUALITY_NORMAL -> "Normal"
            Voice.QUALITY_LOW -> "Low"
            else -> "Standard"
          }
          TtsVoiceOption(
            name = voice.name,
            label = "${voice.locale.displayName} • $qualityLabel (${voice.name})"
          )
        }
        .distinctBy { it.name }
    } catch (ce: CancellationException) {
      throw ce
    } catch (_: Exception) {
      emptyList()
    } finally {
      runCatching { tts?.shutdown() }
    }
  }
}
