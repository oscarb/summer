package se.oscarb.summer.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import se.oscarb.summer.data.GeminiCatalog
import java.io.File
import java.nio.file.Files

class PodcastAudioAndScriptTest {

  @Test
  fun parseTurns_extractsAlternatingSpeakersAndCleansMarkdown() {
    val rawScript = """
      **Alex**: Welcome back to Summer! [laughs] Today we're connecting two big stories [1].
      **Sam**: Right, and the red line between them is how fast on-device AI is moving.
      Alex: Exactly, which brings us to the second article from Android Developers.
    """.trimIndent()

    val turns = PodcastScriptParser.parseTurns(rawScript, hostAName = "Alex", hostBName = "Sam")
    assertEquals(3, turns.size)

    assertEquals("Alex", turns[0].speaker)
    assertFalse(turns[0].isHostB)
    assertEquals("Welcome back to Summer! Today we're connecting two big stories.", turns[0].text)

    assertEquals("Sam", turns[1].speaker)
    assertTrue(turns[1].isHostB)
    assertEquals("Right, and the red line between them is how fast on-device AI is moving.", turns[1].text)

    assertEquals("Alex", turns[2].speaker)
    assertFalse(turns[2].isHostB)
  }

  @Test
  fun formatAsAzureMultiVoiceSsml_escapesXmlAndAssignsVoices() {
    val turns = PodcastScriptParser.parseTurns(
      "Alex: R&D is growing <fast>!\nSam: Let's dive in.",
      hostAName = "Alex",
      hostBName = "Sam"
    )
    val ssml = PodcastScriptParser.formatAsAzureMultiVoiceSsml(
      turns = turns,
      hostAVoice = "en-US-AndrewMultilingualNeural",
      hostBVoice = "en-US-AvaMultilingualNeural"
    )

    assertTrue(ssml.contains("en-US-AndrewMultilingualNeural"))
    assertTrue(ssml.contains("en-US-AvaMultilingualNeural"))
    assertTrue(ssml.contains("R&amp;D is growing &lt;fast&gt;!"))
  }

  @Test
  fun wavAudioUtils_wrapsAndParsesRawPcmCorrectly() {
    val samplePcm = ByteArray(4800) { (it % 120).toByte() } // 0.1s at 24kHz 16-bit mono
    assertFalse(WavAudioUtils.isRiffWave(samplePcm))

    val wavBytes = WavAudioUtils.ensureWavHeader(
      pcmOrWavBytes = samplePcm,
      sampleRate = 24_000,
      channels = 1,
      bitsPerSample = 16
    )
    assertTrue(WavAudioUtils.isRiffWave(wavBytes))
    assertEquals(44 + samplePcm.size, wavBytes.size)

    val parsed = WavAudioUtils.parseWav(wavBytes)
    assertEquals(24_000, parsed.sampleRate)
    assertEquals(1, parsed.channels)
    assertEquals(16, parsed.bitsPerSample)
    assertArrayEquals(samplePcm, parsed.pcmBytes)
  }

  @Test
  fun wavAudioUtils_stitchesMultipleWavFilesWithSilencePause() {
    val tempDir = Files.createTempDirectory("summer_wav_test").toFile()
    try {
      val pcm1 = ByteArray(2400) { 1 }
      val pcm2 = ByteArray(2400) { 2 }
      val file1 = File(tempDir, "turn1.wav").apply {
        writeBytes(WavAudioUtils.wrapPcmInWav(pcm1, sampleRate = 24_000))
      }
      val file2 = File(tempDir, "turn2.wav").apply {
        writeBytes(WavAudioUtils.wrapPcmInWav(pcm2, sampleRate = 24_000))
      }
      val output = File(tempDir, "stitched.wav")

      WavAudioUtils.stitchWavFiles(
        inputWavFiles = listOf(file1, file2),
        outputFile = output,
        pauseMs = 100 // 100ms at 24kHz 16-bit mono = 2400 samples = 4800 bytes
      )

      assertTrue(output.exists())
      val parsed = WavAudioUtils.parseWav(output.readBytes())
      assertEquals(24_000, parsed.sampleRate)
      assertEquals(2400 + 4800 + 2400, parsed.pcmBytes.size)
    } finally {
      tempDir.deleteRecursively()
    }
  }

  @Test
  fun extractSampleRateFromMimeType_parsesL16Rate() {
    assertEquals(
      24_000,
      WavAudioUtils.extractSampleRateFromMimeType("audio/L16;codec=pcm;rate=24000")
    )
    assertEquals(
      16_000,
      WavAudioUtils.extractSampleRateFromMimeType("audio/pcm; rate=16000")
    )
    assertEquals(
      24_000,
      WavAudioUtils.extractSampleRateFromMimeType(null)
    )
  }

  @Test
  fun cleanSpokenText_unwrapsMarkdownLinksAndCodeWithoutReadingRawUrls() {
    val raw = "Check out [the Android blog](https://developer.android.com/blog) using `MediaSession`!"
    val cleaned = PodcastScriptParser.cleanSpokenText(raw)
    assertEquals("Check out the Android blog using MediaSession!", cleaned)
    assertFalse(cleaned.contains("https://"))
  }

  @Test
  fun chunkTurnText_splitsOversizedTurnsOnSentenceBoundariesWithoutLosingWords() {
    val shortTurn = "A concise podcast turn that fits comfortably."
    assertEquals(listOf(shortTurn), PodcastScriptParser.chunkTurnText(shortTurn, maxChars = 100))

    val sentence = "This is a complete spoken sentence about today's news. "
    val longTurn = sentence.repeat(6).trim()
    val chunks = PodcastScriptParser.chunkTurnText(longTurn, maxChars = 120)
    assertTrue(chunks.size > 1)
    chunks.forEach { chunk ->
      assertTrue("Chunk exceeded maxChars: ${chunk.length}", chunk.length <= 120)
      assertTrue("Chunk should end cleanly on sentence boundary", chunk.endsWith("."))
    }

    val unbrokenWords = (1..150).joinToString(" ") { "word$it" }
    val wordChunks = PodcastScriptParser.chunkTurnText(unbrokenWords, maxChars = 90)
    wordChunks.forEach { assertTrue(it.length <= 90) }
    assertEquals(150, wordChunks.sumOf { it.split(" ").size })
  }

  @Test
  fun geminiCatalog_containsAll30OfficialVoicesAndNormalizesInvalidNames() {
    assertEquals(30, GeminiCatalog.PREBUILT_VOICES.size)
    assertEquals("Zephyr", GeminiCatalog.resolveValidVoice("zephyr", "Puck"))
    assertEquals("Kore", GeminiCatalog.resolveValidVoice("Kore", "Puck"))
    // Speaker names like "Joe" or "Jane" are not valid prebuiltVoiceConfig IDs and should normalize
    assertEquals("Puck", GeminiCatalog.resolveValidVoice("Joe", "Puck"))
    assertEquals("Kore", GeminiCatalog.resolveValidVoice("Jane", "Kore"))
  }

  @Test
  fun parseTurns_distinguishesSubstringHostNamesAndAvoidsTrailingBFalsePositive() {
    // Host A ("Alex") contains Host B ("Al") as a prefix — Alex turns must NOT be misclassified as Host B
    val substringScript = """
      Alex: Welcome to the show!
      Al: Glad to be here, Alex.
      Host Alex: Let's jump into story one.
    """.trimIndent()
    val turns = PodcastScriptParser.parseTurns(substringScript, hostAName = "Alex", hostBName = "Al")
    assertEquals(3, turns.size)
    assertFalse(turns[0].isHostB)
    assertEquals("Alex", turns[0].speaker)
    assertTrue(turns[1].isHostB)
    assertEquals("Al", turns[1].speaker)
    assertFalse(turns[2].isHostB)
    assertEquals("Alex", turns[2].speaker)

    // Host A ("Bob") ends with 'b' — must NOT trigger "Host B" fallback
    val bobScript = """
      Bob: First story comes from KotlinConf.
      Speaker 2: And it's a huge update!
    """.trimIndent()
    val bobTurns = PodcastScriptParser.parseTurns(bobScript, hostAName = "Bob", hostBName = "Sam")
    assertEquals(2, bobTurns.size)
    assertFalse(bobTurns[0].isHostB)
    assertEquals("Bob", bobTurns[0].speaker)
    assertTrue(bobTurns[1].isHostB)
    assertEquals("Sam", bobTurns[1].speaker)
  }

  @Test
  fun wavAudioUtils_normalizesMixedSampleRatesAndStereoWhenStitching() {
    val tempDir = Files.createTempDirectory("summer_wav_resample_test").toFile()
    try {
      // Turn 1: 0.1s at 16,000 Hz mono (1,600 samples = 3,200 bytes)
      val pcm16kMono = ByteArray(3_200) { 10 }
      val file1 = File(tempDir, "turn_16k_mono.wav").apply {
        writeBytes(WavAudioUtils.wrapPcmInWav(pcm16kMono, sampleRate = 16_000, channels = 1, bitsPerSample = 16))
      }

      // Turn 2: 0.1s at 24,000 Hz stereo 2ch (2,400 frames * 4 bytes = 9,600 bytes)
      val pcm24kStereo = ByteArray(9_600) { 20 }
      val file2 = File(tempDir, "turn_24k_stereo.wav").apply {
        writeBytes(WavAudioUtils.wrapPcmInWav(pcm24kStereo, sampleRate = 24_000, channels = 2, bitsPerSample = 16))
      }

      val output = File(tempDir, "normalized_stitched.wav")
      WavAudioUtils.stitchWavFiles(
        inputWavFiles = listOf(file1, file2),
        outputFile = output,
        pauseMs = 100 // 0.1s at 24kHz mono = 2,400 samples = 4,800 bytes
      )

      val parsed = WavAudioUtils.parseWav(output.readBytes())
      assertEquals(24_000, parsed.sampleRate)
      assertEquals(1, parsed.channels)
      assertEquals(16, parsed.bitsPerSample)
      // 0.1s (4,800B) + 0.1s silence (4,800B) + 0.1s downmixed mono (4,800B) = 14,400 bytes
      assertEquals(14_400, parsed.pcmBytes.size)
    } finally {
      tempDir.deleteRecursively()
    }
  }

  @Test
  fun writeBytesAtomically_overwritesTargetCleanlyWithoutLeavingTempFiles() {
    val tempDir = Files.createTempDirectory("summer_atomic_write_test").toFile()
    try {
      val target = File(tempDir, "episode.wav")
      WavAudioUtils.writeBytesAtomically(target, byteArrayOf(1, 2, 3))
      assertArrayEquals(byteArrayOf(1, 2, 3), target.readBytes())

      WavAudioUtils.writeBytesAtomically(target, byteArrayOf(4, 5, 6, 7))
      assertArrayEquals(byteArrayOf(4, 5, 6, 7), target.readBytes())

      // Only the final file should remain in tempDir (no leftover .tmp files)
      assertEquals(listOf("episode.wav"), tempDir.listFiles()?.map { it.name })
    } finally {
      tempDir.deleteRecursively()
    }
  }
}
