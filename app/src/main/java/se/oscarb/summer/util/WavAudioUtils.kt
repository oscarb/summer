package se.oscarb.summer.util

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
    outputFile.parentFile?.mkdirs()
    outputFile.writeBytes(wavBytes)
  }

  /**
   * Stitches multiple WAV files (such as per-turn outputs from Android TextToSpeech.synthesizeToFile
   * or cloud TTS endpoints) into a single playable WAV file at [outputFile].
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
    val primary = parsedSegments.first()

    stitchPcmChunksToWavFile(
      pcmChunks = parsedSegments.map { it.pcmBytes },
      outputFile = outputFile,
      sampleRate = primary.sampleRate,
      channels = primary.channels,
      bitsPerSample = primary.bitsPerSample,
      pauseMs = pauseMs
    )
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
