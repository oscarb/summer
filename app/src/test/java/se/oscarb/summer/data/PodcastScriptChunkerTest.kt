package se.oscarb.summer.data

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class PodcastScriptChunkerTest {

  @Test
  fun toSpeakableText_stripsMarkdownFormatting() {
    val markdown = """
      ## Intro

      Welcome to **Summer**! Here is today's update.

      - First point about [the article](https://example.com)
      - Second point with `inline code`

      ---
    """.trimIndent()

    val speakable = PodcastScriptChunker.toSpeakableText(markdown)

    assertTrue("Heading marker should be removed", !speakable.contains("#"))
    assertTrue("Bold markers should be removed", !speakable.contains("**"))
    assertTrue("Link syntax should be unwrapped", speakable.contains("the article"))
    assertTrue("Link URL should be removed", !speakable.contains("https://example.com"))
    assertTrue("List markers should be removed", !speakable.contains("- First"))
    assertTrue("Horizontal rule should be removed", !speakable.contains("---"))
    assertTrue(speakable.contains("Welcome to Summer"))
  }

  @Test
  fun chunk_returnsEmptyForBlankInput() {
    assertEquals(emptyList<String>(), PodcastScriptChunker.chunk("   \n  "))
  }

  @Test
  fun chunk_respectsMaximumSize() {
    val text = (1..200).joinToString(" ") { "word$it" }

    val chunks = PodcastScriptChunker.chunk(text, maxChars = 100)

    assertTrue("Expected more than one chunk", chunks.size > 1)
    chunks.forEach { chunk ->
      assertTrue("Chunk exceeded limit: ${chunk.length}", chunk.length <= 100)
    }
    assertEquals("Chunks should not lose words", 200, chunks.sumOf { it.split(" ").size })
  }

  @Test
  fun chunk_keepsShortTextInSingleChunk() {
    val text = "A short podcast intro that fits comfortably."
    assertEquals(listOf(text), PodcastScriptChunker.chunk(text, maxChars = 100))
  }

  @Test
  fun chunk_splitsOnSentenceBoundariesWhenPossible() {
    val sentence = "This is a complete sentence about the news. "
    val text = sentence.repeat(6).trim()

    val chunks = PodcastScriptChunker.chunk(text, maxChars = 80)

    assertTrue(chunks.size > 1)
    chunks.forEach { chunk ->
      assertTrue("Chunk exceeded limit: ${chunk.length}", chunk.length <= 80)
      assertTrue("Chunks should read as whole sentences", chunk.trim().endsWith("."))
    }
  }
}
