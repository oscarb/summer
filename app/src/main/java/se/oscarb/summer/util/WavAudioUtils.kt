package se.oscarb.summer.util

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * Utilities for wrapping raw Linear PCM (L16) audio into standard RIFF WAVE (.wav) files
 * and stitching multiple speaker turn WAV/PCM segments together with natural pauses.
 */
object WavAudioUtils {

  data class WavFormatData(
    val sampleRate: Int,
    val channels: Int,
    val bitsPerSample: Int,
    val pcmBytes: ByteArray
  )

  /**
   * Returns true if [bytes] already begins with a RIFF....WAVE header.
   */
  fun isRiffWave(bytes: ByteArray): Boolean {
    if (bytes.size < 12) return false
    return bytes[0] == 'R'.code.toByte() &&
      bytes[1] == 'I'.code.toByte() &&
      bytes[2] == 'F'.code.toByte() &&
      bytes[3] == 'F'.code.toByte() &&
      bytes[8] == 'W'.code.toByte() &&
      bytes[9] == 'A'.code.toByte() &&
      bytes[10] == 'V'.code.toByte() &&
      bytes[11] == 'E'.code.toByte()
  }

  /**
   * Prepends a 44-byte RIFF WAVE header to raw 16-bit signed little-endian PCM bytes.
   * If [pcmOrWavBytes] already has a RIFF WAVE header, returns it unchanged.
   */
  fun ensureWavHeader(
    pcmOrWavBytes: ByteArray,
    sampleRate: Int = 24_000,
    channels: Int = 1,
    bitsPerSample: Int = 16
  ): ByteArray {
    if (isRiffWave(pcmOrWavBytes)) return pcmOrWavBytes
    return wrapPcmInWav(
      pcmBytes = pcmOrWavBytes,
      sampleRate = sampleRate,
      channels = channels,
      bitsPerSample = bitsPerSample
    )
  }

  /**
   * Wraps raw PCM bytes in a 44-byte RIFF WAVE header.
   */
  fun wrapPcmInWav(
    pcmBytes: ByteArray,
    sampleRate: Int = 24_000,
    channels: Int = 1,
    bitsPerSample: Int = 16
  ): ByteArray {
    val byteRate = sampleRate * channels * (bitsPerSample / 8)
    val blockAlign = (channels * (bitsPerSample / 8)).toShort()
    val dataSize = pcmBytes.size
    val totalFileSizeMinus8 = 36 + dataSize

    val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
    // RIFF header
    buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
    buffer.putInt(totalFileSizeMinus8)
    buffer.put("WAVE".toByteArray(Charsets.US_ASCII))

    // fmt subchunk
    buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
    buffer.putInt(16) // Subchunk1Size for PCM
    buffer.putShort(1) // AudioFormat 1 = Linear PCM
    buffer.putShort(channels.toShort())
    buffer.putInt(sampleRate)
    buffer.putInt(byteRate)
    buffer.putShort(blockAlign)
    buffer.putShort(bitsPerSample.toShort())

    // data subchunk
    buffer.put("data".toByteArray(Charsets.US_ASCII))
    buffer.putInt(dataSize)
    buffer.put(pcmBytes)

    return buffer.array()
  }

  /**
   * Parses a RIFF WAVE byte array to extract its sample rate, channel count, bits per sample,
   * and raw PCM payload (properly skipping any non-data subchunks like LIST or JUNK).
   */
  fun parseWav(wavBytes: ByteArray): WavFormatData {
    if (!isRiffWave(wavBytes) || wavBytes.size < 44) {
      return WavFormatData(
        sampleRate = 24_000,
        channels = 1,
        bitsPerSample = 16,
        pcmBytes = wavBytes
      )
    }

    val buffer = ByteBuffer.wrap(wavBytes).order(ByteOrder.LITTLE_ENDIAN)
    var offset = 12
    var sampleRate = 24_000
    var channels = 1
    var bitsPerSample = 16
    var pcmData: ByteArray? = null

    while (offset + 8 <= wavBytes.size) {
      val chunkId = String(wavBytes, offset, 4, Charsets.US_ASCII)
      val chunkSize = buffer.getInt(offset + 4).coerceAtLeast(0)
      val chunkDataStart = offset + 8

      if (chunkId == "fmt " && chunkDataStart + 16 <= wavBytes.size) {
        channels = buffer.getShort(chunkDataStart + 2).toInt().coerceAtLeast(1)
        sampleRate = buffer.getInt(chunkDataStart + 4).coerceAtLeast(8_000)
        bitsPerSample = buffer.getShort(chunkDataStart + 14).toInt().coerceAtLeast(8)
      } else if (chunkId == "data") {
        val safeEnd = (chunkDataStart + chunkSize).coerceAtMost(wavBytes.size)
        if (safeEnd > chunkDataStart) {
          pcmData = wavBytes.copyOfRange(chunkDataStart, safeEnd)
        }
        break
      }

      // Word-aligned advance
      val paddedChunkSize = if (chunkSize % 2 == 1) chunkSize + 1 else chunkSize
      offset = chunkDataStart + paddedChunkSize
    }

    val extractedPcm = pcmData ?: wavBytes.copyOfRange(44, wavBytes.size)
    return WavFormatData(
      sampleRate = sampleRate,
      channels = channels,
      bitsPerSample = bitsPerSample,
      pcmBytes = extractedPcm
    )
  }

  /**
   * Stitches multiple raw PCM chunks into a single RIFF WAVE file with [pauseMs] of silence
   * inserted between turns.
   */
  fun stitchPcmChunksToWavFile(
    pcmChunks: List<ByteArray>,
    outputFile: File,
    sampleRate: Int = 24_000,
    channels: Int = 1,
    bitsPerSample: Int = 16,
    pauseMs: Int = 260
  ) {
    val nonEmpty = pcmChunks.filter { it.isNotEmpty() }
    val frameSize = (channels * (bitsPerSample / 8)).coerceAtLeast(2)
    val rawSilenceBytes = ((sampleRate.toLong() * pauseMs) / 1000L).toInt() * frameSize
    val silenceBytes = ByteArray((rawSilenceBytes / frameSize) * frameSize)

    val combinedPcm = ByteArrayOutputStream()
    nonEmpty.forEachIndexed { index, chunk ->
      // Ensure chunk length is frame-aligned
      val alignedLen = (chunk.size / frameSize) * frameSize
      if (alignedLen > 0) {
        combinedPcm.write(chunk, 0, alignedLen)
        if (index < nonEmpty.lastIndex && silenceBytes.isNotEmpty()) {
          combinedPcm.write(silenceBytes)
        }
      }
    }

    val wavBytes = wrapPcmInWav(
      pcmBytes = combinedPcm.toByteArray(),
      sampleRate = sampleRate,
      channels = channels,
      bitsPerSample = bitsPerSample
    )
    writeBytesAtomically(outputFile, wavBytes)
  }

  /**
   * Normalizes and stitches multiple [WavFormatData] segments (handling mixed sample rates or
   * stereo/mono channel counts across distinct voices) into a single 16-bit mono RIFF WAVE file.
   */
  fun stitchFormatSegmentsToWavFile(
    segments: List<WavFormatData>,
    outputFile: File,
    pauseMs: Int = 260
  ) {
    val nonEmpty = segments.filter { it.pcmBytes.isNotEmpty() }
    if (nonEmpty.isEmpty()) {
      throw IllegalStateException("No synthesized audio segments were generated.")
    }

    val targetSampleRate = nonEmpty.maxOf { it.sampleRate }.coerceIn(8_000, 48_000)
    val normalizedChunks = nonEmpty.map { normalizeToMono16Pcm(it, targetSampleRate) }

    stitchPcmChunksToWavFile(
      pcmChunks = normalizedChunks,
      outputFile = outputFile,
      sampleRate = targetSampleRate,
      channels = 1,
      bitsPerSample = 16,
      pauseMs = pauseMs
    )
  }

  /**
   * Stitches multiple WAV files (such as per-turn outputs from Android TextToSpeech.synthesizeToFile
   * or cloud TTS endpoints) into a single playable WAV file at [outputFile], automatically
   * normalizing any mixed sample rates or channel counts.
   */
  fun stitchWavFiles(
    inputWavFiles: List<File>,
    outputFile: File,
    pauseMs: Int = 260
  ) {
    val validFiles = inputWavFiles.filter { it.exists() && it.length() > 44 }
    if (validFiles.isEmpty()) {
      throw IllegalStateException("No synthesized audio segments were generated.")
    }

    val parsedSegments = validFiles.map { parseWav(it.readBytes()) }
    stitchFormatSegmentsToWavFile(
      segments = parsedSegments,
      outputFile = outputFile,
      pauseMs = pauseMs
    )
  }

  /**
   * Converts a [WavFormatData] segment to 16-bit signed little-endian mono PCM at [targetSampleRate].
   * Fast-paths with zero copying when the segment already matches.
   */
  fun normalizeToMono16Pcm(segment: WavFormatData, targetSampleRate: Int): ByteArray {
    if (segment.channels == 1 && segment.bitsPerSample == 16 && segment.sampleRate == targetSampleRate) {
      return segment.pcmBytes
    }
    if (segment.pcmBytes.isEmpty()) return ByteArray(0)

    val ch = segment.channels.coerceAtLeast(1)
    val monoSamples: ShortArray = when (segment.bitsPerSample) {
      8 -> {
        val frameCount = segment.pcmBytes.size / ch
        ShortArray(frameCount) { frameIdx ->
          var sum = 0
          val base = frameIdx * ch
          for (c in 0 until ch) {
            val unsigned = segment.pcmBytes[base + c].toInt() and 0xFF
            sum += (unsigned - 128) shl 8
          }
          (sum / ch).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
      }
      else -> {
        // Default 16-bit signed little-endian
        val frameSize = ch * 2
        val frameCount = segment.pcmBytes.size / frameSize
        val srcBuf = ByteBuffer.wrap(segment.pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        ShortArray(frameCount) { frameIdx ->
          var sum = 0
          val base = frameIdx * frameSize
          for (c in 0 until ch) {
            sum += srcBuf.getShort(base + c * 2).toInt()
          }
          (sum / ch).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
      }
    }

    if (monoSamples.isEmpty()) return ByteArray(0)

    val srcRate = segment.sampleRate.coerceAtLeast(1)
    val dstRate = targetSampleRate.coerceAtLeast(1)
    val finalSamples = if (srcRate == dstRate) {
      monoSamples
    } else {
      val dstLength = ((monoSamples.size.toLong() * dstRate) / srcRate).toInt().coerceAtLeast(1)
      val resampled = ShortArray(dstLength)
      val ratio = srcRate.toDouble() / dstRate.toDouble()
      val lastIdx = monoSamples.lastIndex
      for (i in 0 until dstLength) {
        val srcPos = i * ratio
        val idx0 = srcPos.toInt().coerceIn(0, lastIdx)
        val idx1 = (idx0 + 1).coerceAtMost(lastIdx)
        val frac = srcPos - idx0
        val s0 = monoSamples[idx0].toInt()
        val s1 = monoSamples[idx1].toInt()
        val interp = (s0 + (s1 - s0) * frac).toInt()
          .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        resampled[i] = interp.toShort()
      }
      resampled
    }

    val outBuf = ByteBuffer.allocate(finalSamples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
    for (sample in finalSamples) {
      outBuf.putShort(sample)
    }
    return outBuf.array()
  }

  /**
   * Writes [bytes] to a sibling temporary file first and atomically moves it into place at
   * [targetFile] so concurrent readers (such as `MediaPlayer` or JSON loaders) never see a
   * partially-written or truncated file.
   */
  fun writeBytesAtomically(targetFile: File, bytes: ByteArray) {
    val parent = targetFile.parentFile
    parent?.mkdirs()
    val tempFile = File(parent, ".${targetFile.name}.${UUID.randomUUID()}.tmp")
    try {
      tempFile.writeBytes(bytes)
      try {
        Files.move(
          tempFile.toPath(),
          targetFile.toPath(),
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE
        )
      } catch (_: Exception) {
        Files.move(
          tempFile.toPath(),
          targetFile.toPath(),
          StandardCopyOption.REPLACE_EXISTING
        )
      }
    } finally {
      if (tempFile.exists()) {
        runCatching { tempFile.delete() }
      }
    }
  }

  /**
   * Parses a MIME type like `audio/L16;codec=pcm;rate=24000` to extract the sample rate,
   * defaulting to [defaultRate] if not specified.
   */
  fun extractSampleRateFromMimeType(mimeType: String?, defaultRate: Int = 24_000): Int {
    if (mimeType.isNullOrBlank()) return defaultRate
    val match = Regex("""rate\s*=\s*(\d+)""", RegexOption.IGNORE_CASE).find(mimeType)
    return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: defaultRate
  }
}
