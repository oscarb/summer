package se.oscarb.summer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeExtractorTest {

  @Test
  fun isYouTubeUrl_recognizesAllSupportedFormats() {
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://youtube.com/watch?v=dQw4w9WgXcQ"))
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://m.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://youtu.be/dQw4w9WgXcQ"))
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://youtu.be/dQw4w9WgXcQ?si=abc123xyz"))
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://youtube.com/shorts/dQw4w9WgXcQ?feature=share"))
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://www.youtube.com/live/dQw4w9WgXcQ"))
    assertTrue(YouTubeExtractor.isYouTubeUrl("https://www.youtube.com/embed/dQw4w9WgXcQ"))

    assertFalse(YouTubeExtractor.isYouTubeUrl("https://example.com/watch?v=dQw4w9WgXcQ"))
    assertFalse(YouTubeExtractor.isYouTubeUrl("https://theverge.com/article"))
    assertFalse(YouTubeExtractor.isYouTubeUrl(""))
  }

  @Test
  fun extractVideoId_extracts11CharacterIdCorrectly() {
    assertEquals("dQw4w9WgXcQ", YouTubeExtractor.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertEquals("dQw4w9WgXcQ", YouTubeExtractor.extractVideoId("https://www.youtube.com/watch?feature=share&v=dQw4w9WgXcQ&t=42s"))
    assertEquals("dQw4w9WgXcQ", YouTubeExtractor.extractVideoId("https://m.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertEquals("dQw4w9WgXcQ", YouTubeExtractor.extractVideoId("https://youtu.be/dQw4w9WgXcQ"))
    assertEquals("dQw4w9WgXcQ", YouTubeExtractor.extractVideoId("https://youtu.be/dQw4w9WgXcQ?si=test12345"))
    assertEquals("dQw4w9WgXcQ", YouTubeExtractor.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
    assertEquals("dQw4w9WgXcQ", YouTubeExtractor.extractVideoId("https://www.youtube.com/live/dQw4w9WgXcQ?feature=share"))
    assertEquals("dQw4w9WgXcQ", YouTubeExtractor.extractVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ"))

    assertNull(YouTubeExtractor.extractVideoId("https://www.youtube.com/"))
    assertNull(YouTubeExtractor.extractVideoId("https://example.com/watch?v=invalid"))
    assertNull(YouTubeExtractor.extractVideoId(""))
  }

  @Test
  fun formatTimestamp_formatsSecondsMinutesAndHours() {
    assertEquals("00:00", YouTubeExtractor.formatTimestamp(0L))
    assertEquals("00:05", YouTubeExtractor.formatTimestamp(5_000L))
    assertEquals("01:15", YouTubeExtractor.formatTimestamp(75_000L))
    assertEquals("10:30", YouTubeExtractor.formatTimestamp(630_000L))
    assertEquals("1:01:05", YouTubeExtractor.formatTimestamp(3_665_000L))
    assertEquals("2:15:42", YouTubeExtractor.formatTimestamp(8_142_000L))
  }

  @Test
  fun cleanSubtitleText_unescapesHtmlEntitiesAndStripsTags() {
    val raw = "<font color=\"#ffffff\">We&#39;re &quot;ready&quot; &amp; &lt;steady&gt; &nbsp;</font>"
    val cleaned = YouTubeExtractor.cleanSubtitleText(raw)
    assertEquals("We're \"ready\" & <steady>", cleaned)
  }

  @Test
  fun parseCaptionXml_handlesTimedTextFormat3() {
    val xml = """
      <?xml version="1.0" encoding="utf-8" ?>
      <timedtext format="3">
        <body>
          <p t="1360" d="1680">[Music]</p>
          <p t="18640" d="3240">We&#39;re no strangers to love</p>
          <p t="22640" d="4320">You know the rules &amp; so do I</p>
        </body>
      </timedtext>
    """.trimIndent()

    val segments = YouTubeExtractor.parseCaptionXml(xml)
    assertEquals(3, segments.size)
    assertEquals(1360L, segments[0].startMs)
    assertEquals(1680L, segments[0].durationMs)
    assertEquals("[Music]", segments[0].text)

    assertEquals(18640L, segments[1].startMs)
    assertEquals("We're no strangers to love", segments[1].text)

    assertEquals(22640L, segments[2].startMs)
    assertEquals("You know the rules & so do I", segments[2].text)
  }

  @Test
  fun parseCaptionXml_handlesStandardTimedTextFormat() {
    val xml = """
      <transcript>
        <text start="1.5" dur="3.0">Hello &lt;world&gt; of Kotlin</text>
        <text start="5.25" dur="2.5">Testing subtitles parser</text>
      </transcript>
    """.trimIndent()

    val segments = YouTubeExtractor.parseCaptionXml(xml)
    assertEquals(2, segments.size)
    assertEquals(1500L, segments[0].startMs)
    assertEquals(3000L, segments[0].durationMs)
    assertEquals("Hello <world> of Kotlin", segments[0].text)

    assertEquals(5250L, segments[1].startMs)
    assertEquals(2500L, segments[1].durationMs)
    assertEquals("Testing subtitles parser", segments[1].text)
  }

  @Test
  fun formatTranscriptForReading_groupsSegmentsIntoTimestampedParagraphs() {
    val segments = listOf(
      YouTubeCaptionSegment(0L, 2000L, "Welcome to the video."),
      YouTubeCaptionSegment(2500L, 3000L, "Today we will explore quantum computing."),
      YouTubeCaptionSegment(50000L, 4000L, "First let us define qubits."),
      YouTubeCaptionSegment(55000L, 4000L, "Qubits are quantum bits.")
    )

    val formatted = YouTubeExtractor.formatTranscriptForReading(segments, targetParagraphDurationMs = 30_000L)
    assertNotNull(formatted)
    assertTrue(formatted.contains("[00:00] Welcome to the video. Today we will explore quantum computing."))
    assertTrue(formatted.contains("[00:50] First let us define qubits. Qubits are quantum bits."))
  }

  @Test
  fun extractCaptionBaseUrl_parsesJsonPlayerResponseCorrectly() {
    val playerJson = """
      {
        "captions": {
          "playerCaptionsTracklistRenderer": {
            "captionTracks": [
              {
                "baseUrl": "https://www.youtube.com/api/timedtext?v=123&lang=sv",
                "languageCode": "sv",
                "name": { "simpleText": "Swedish" }
              },
              {
                "baseUrl": "https://www.youtube.com/api/timedtext?v=123&lang=en",
                "languageCode": "en",
                "name": { "simpleText": "English" }
              }
            ]
          }
        }
      }
    """.trimIndent()

    val baseUrl = YouTubeExtractor.extractCaptionBaseUrl(playerJson)
    assertTrue(baseUrl.startsWith("https://www.youtube.com/api/timedtext"))
  }
}
