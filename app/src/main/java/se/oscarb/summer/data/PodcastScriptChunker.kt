package se.oscarb.summer.data

/**
 * Turns a Markdown podcast script into clean, speakable text and splits it into
 * chunks small enough for on-device text-to-speech engines (which often cut off
 * after a few hundred characters).
 *
 * This is intentionally free of Android dependencies so it can be unit tested.
 */
object PodcastScriptChunker {

  private const val DEFAULT_MAX_CHUNK_CHARS = 650

  private val HORIZONTAL_RULE = Regex("^\\s*([-*_])\\1{2,}\\s*$")
  private val HEADING = Regex("^\\s{0,3}#{1,6}\\s*")
  private val LIST_MARKER = Regex("^\\s*(?:[-*+]|\\d+[.)])\\s+")
  private val INLINE_CODE = Regex("`([^`]*)`")
  private val IMAGE = Regex("!\\[([^\\]]*)\\]\\([^)]*\\)")
  private val LINK = Regex("\\[([^\\]]+)\\]\\([^)]*\\)")
  private val BOLD = Regex("(\\*\\*|__)(.*?)\\1")
  private val ITALIC_STAR = Regex("(?<!\\*)\\*(?!\\s)([^*]+?)(?<!\\s)\\*(?!\\*)")
  private val ITALIC_UNDERSCORE = Regex("(?<!_)_(?!\\s)([^_]+?)(?<!\\s)_(?!_)")

  /**
   * Strips Markdown syntax so the text can be read aloud naturally, and drops
   * structural lines (headings, horizontal rules) that would otherwise be spoken.
   */
  fun toSpeakableText(markdown: String): String {
    if (markdown.isBlank()) return ""

    return markdown
      .replace("\r\n", "\n")
      .replace("\r", "\n")
      .lineSequence()
      .map { line ->
        val trimmed = line.trim()
        when {
          trimmed.isEmpty() -> ""
          HORIZONTAL_RULE.matches(trimmed) -> ""
          else -> {
            var text = HEADING.replace(trimmed, "")
            text = LIST_MARKER.replace(text, "")
            text = IMAGE.replace(text, "$1")
            text = LINK.replace(text, "$1")
            text = INLINE_CODE.replace(text, "$1")
            text = BOLD.replace(text, "$2")
            text = ITALIC_STAR.replace(text, "$1")
            text = ITALIC_UNDERSCORE.replace(text, "$1")
            text.trim()
          }
        }
      }
      .joinToString("\n")
      .replace(Regex("\n{3,}"), "\n\n")
      .trim()
  }

  /**
   * Splits [text] into chunks of at most [maxChars] characters, preferring to
   * break on paragraph, sentence, then word boundaries so each chunk stays
   * pronounceable on its own.
   */
  fun chunk(text: String, maxChars: Int = DEFAULT_MAX_CHUNK_CHARS): List<String> {
    if (text.isBlank()) return emptyList()
    val limit = if (maxChars > 0) maxChars else DEFAULT_MAX_CHUNK_CHARS

    val chunks = mutableListOf<String>()
    val current = StringBuilder()

    fun flush() {
      val piece = current.toString().trim()
      if (piece.isNotEmpty()) chunks.add(piece)
      current.setLength(0)
    }

    // First split on paragraph breaks; further split any paragraph that is too long.
    val paragraphs = text.split(Regex("\\n\\s*\\n"))
    for (paragraph in paragraphs) {
      val normalized = paragraph.replace(Regex("\\s+"), " ").trim()
      if (normalized.isEmpty()) continue

      val candidate = if (current.isEmpty()) normalized else "${current} $normalized"
      if (candidate.length <= limit) {
        current.setLength(0)
        current.append(candidate)
      } else {
        flush()
        if (normalized.length <= limit) {
          current.append(normalized)
        } else {
          splitOversized(normalized, limit).forEach { chunks.add(it) }
        }
      }
    }
    flush()

    return chunks
  }

  private fun splitOversized(text: String, limit: Int): List<String> {
    val result = mutableListOf<String>()
    val sentenceSplit = Regex("(?<=[.!?])\\s+")
    val sentences = text.split(sentenceSplit)

    val buffer = StringBuilder()
    fun flushBuffer() {
      val piece = buffer.toString().trim()
      if (piece.isNotEmpty()) result.add(piece)
      buffer.setLength(0)
    }

    for (sentence in sentences) {
      val s = sentence.trim()
      if (s.isEmpty()) continue

      val candidate = if (buffer.isEmpty()) s else "${buffer} $s"
      when {
        candidate.length <= limit -> {
          buffer.setLength(0)
          buffer.append(candidate)
        }
        s.length <= limit -> {
          flushBuffer()
          buffer.append(s)
        }
        else -> {
          flushBuffer()
          result.addAll(splitByWords(s, limit))
        }
      }
    }
    flushBuffer()
    return result
  }

  private fun splitByWords(text: String, limit: Int): List<String> {
    val result = mutableListOf<String>()
    val buffer = StringBuilder()
    for (word in text.split(Regex("\\s+"))) {
      if (word.isEmpty()) continue
      val candidate = if (buffer.isEmpty()) word else "${buffer} $word"
      if (candidate.length <= limit) {
        buffer.setLength(0)
        buffer.append(candidate)
      } else {
        if (buffer.isNotEmpty()) result.add(buffer.toString())
        buffer.setLength(0)
        buffer.append(word)
      }
    }
    if (buffer.isNotEmpty()) result.add(buffer.toString())
    return result
  }
}
