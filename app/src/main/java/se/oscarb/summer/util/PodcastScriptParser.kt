package se.oscarb.summer.util

import se.oscarb.summer.data.DialogueTurn

/**
 * Parses and normalizes conversational podcast scripts between two hosts (e.g., Alex and Sam)
 * so they can be rendered as an interactive transcript in the UI and synthesized by
 * multi-speaker or turn-by-turn TTS engines.
 */
object PodcastScriptParser {

  const val DEFAULT_MAX_TURN_CHUNK_CHARS = 650

  private val SPEAKER_LINE_REGEX = Regex(
    pattern = """^\s*(?:[-*•]\s*)?(?:\*\*|__)?([A-Za-z0-9 _.-]{2,24})(?:\*\*|__)?\s*:\s*(.+)$"""
  )
  private val STAGE_DIRECTION_REGEX = Regex("""\[[^\]]*laughs[^\]]*]|\([^)]*laughs[^)]*\)|\[[^\]]*sighs[^\]]*]|\([^)]*sighs[^)]*\)|\[[^\]]*music[^\]]*]|\([^)]*music[^)]*\)|\[[^\]]*pause[^\]]*]""", RegexOption.IGNORE_CASE)
  private val HORIZONTAL_RULE_REGEX = Regex("""^\s*([-*_])\1{2,}\s*$""")
  private val MARKDOWN_IMAGE_REGEX = Regex("""!\[([^\]]*)]\([^)]*\)""")
  private val MARKDOWN_LINK_REGEX = Regex("""\[([^\]]+)]\([^)]*\)""")
  private val INLINE_CODE_REGEX = Regex("""`([^`]*)`""")
  private val CITATION_BRACKET_REGEX = Regex("""\[\d+]""")
  private val RESIDUAL_BRACKET_NOTE_REGEX = Regex("""\[[^\]]*]""")
  private val MARKDOWN_DECORATION_REGEX = Regex("""(\*\*|__|`)""")
  private val ITALIC_STAR_REGEX = Regex("""(?<!\*)\*(?!\s)([^*]+?)(?<!\s)\*(?!\*)""")
  private val ITALIC_UNDERSCORE_REGEX = Regex("""(?<!_)_(?!\s)([^_]+?)(?<!\s)_(?!_)""")

  /**
   * Parses a raw LLM-generated script into structured [DialogueTurn] items.
   * If the script has no explicit speaker prefixes (e.g. a single-narrator paragraph summary),
   * it splits paragraphs naturally or assigns them to [hostAName].
   */
  fun parseTurns(
    rawScript: String,
    hostAName: String = "Alex",
    hostBName: String = "Sam"
  ): List<DialogueTurn> {
    if (rawScript.isBlank()) return emptyList()

    val cleanHostA = hostAName.trim().ifBlank { "Alex" }
    val cleanHostB = hostBName.trim().ifBlank { "Sam" }

    val turns = mutableListOf<DialogueTurn>()
    var currentSpeaker = cleanHostA
    var currentIsHostB = false
    val currentBuffer = StringBuilder()

    fun flushBuffer() {
      val text = cleanSpokenText(currentBuffer.toString())
      if (text.isNotBlank()) {
        turns.add(
          DialogueTurn(
            speaker = currentSpeaker,
            isHostB = currentIsHostB,
            text = text
          )
        )
      }
      currentBuffer.setLength(0)
    }

    for (rawLine in rawScript.lines()) {
      val trimmed = rawLine.trim()
      if (trimmed.isEmpty()) continue
      // Skip markdown section headers or horizontal rules if any leaked in
      if (trimmed.startsWith("#") || HORIZONTAL_RULE_REGEX.matches(trimmed)) continue

      val match = SPEAKER_LINE_REGEX.matchEntire(trimmed)
      if (match != null) {
        val rawSpeakerLabel = match.groupValues[1].trim()
        val spokenPart = match.groupValues[2].trim()

        // Ignore false positives like "TL;DR:" or "Note:" or "Article 1:"
        val lowerLabel = rawSpeakerLabel.lowercase()
        val isMetadataPrefix = lowerLabel == "tl;dr" ||
          lowerLabel == "summary" ||
          lowerLabel == "title" ||
          lowerLabel == "url" ||
          lowerLabel.startsWith("article") ||
          lowerLabel.startsWith("http")

        if (!isMetadataPrefix) {
          flushBuffer()
          val isB = isSecondHost(rawSpeakerLabel, cleanHostA, cleanHostB)
          currentIsHostB = isB
          currentSpeaker = if (isB) cleanHostB else cleanHostA
          currentBuffer.append(spokenPart)
          continue
        }
      }

      if (currentBuffer.isNotEmpty()) {
        currentBuffer.append(" ")
      }
      currentBuffer.append(trimmed)
    }

    flushBuffer()

    // If no turns were extracted, fallback to a single turn with cleaned text
    if (turns.isEmpty()) {
      val fallback = cleanSpokenText(rawScript)
      if (fallback.isNotBlank()) {
        return listOf(DialogueTurn(speaker = cleanHostA, isHostB = false, text = fallback))
      }
    }

    return turns
  }

  private fun isSecondHost(
    rawSpeakerLabel: String,
    hostAName: String,
    hostBName: String
  ): Boolean {
    val normalized = rawSpeakerLabel.trim().lowercase()
    val aLower = hostAName.trim().lowercase()
    val bLower = hostBName.trim().lowercase()

    return when {
      normalized == bLower || normalized.contains(bLower) -> true
      normalized == aLower || normalized.contains(aLower) -> false
      normalized.endsWith("2") || normalized.endsWith("b") || normalized.contains("co-host") -> true
      else -> false
    }
  }

  /**
   * Strips markdown symbols, unwraps links/inline code, and removes stage directions
   * so TTS engines read natural speech without spelling out raw URLs.
   */
  fun cleanSpokenText(raw: String): String {
    var cleaned = raw
    cleaned = MARKDOWN_IMAGE_REGEX.replace(cleaned, "$1")
    cleaned = MARKDOWN_LINK_REGEX.replace(cleaned, "$1")
    cleaned = CITATION_BRACKET_REGEX.replace(cleaned, "")
    cleaned = STAGE_DIRECTION_REGEX.replace(cleaned, " ")
    cleaned = RESIDUAL_BRACKET_NOTE_REGEX.replace(cleaned, " ")
    cleaned = INLINE_CODE_REGEX.replace(cleaned, "$1")
    cleaned = MARKDOWN_DECORATION_REGEX.replace(cleaned, "")
    cleaned = ITALIC_STAR_REGEX.replace(cleaned, "$1")
    cleaned = ITALIC_UNDERSCORE_REGEX.replace(cleaned, "$1")
    return cleaned
      .replace(Regex("""\s+"""), " ")
      .trim()
  }

  /**
   * Splits a spoken turn into chunks of at most [maxChars] characters, breaking on
   * paragraph, sentence, then word boundaries so strict on-device Android TTS engines
   * never truncate an unusually long dialogue turn.
   */
  fun chunkTurnText(
    text: String,
    maxChars: Int = DEFAULT_MAX_TURN_CHUNK_CHARS
  ): List<String> {
    if (text.isBlank()) return emptyList()
    val limit = if (maxChars > 0) maxChars else DEFAULT_MAX_TURN_CHUNK_CHARS

    val chunks = mutableListOf<String>()
    val current = StringBuilder()

    fun flush() {
      val piece = current.toString().trim()
      if (piece.isNotEmpty()) chunks.add(piece)
      current.setLength(0)
    }

    val paragraphs = text.split(Regex("""\n\s*\n"""))
    for (paragraph in paragraphs) {
      val normalized = paragraph.replace(Regex("""\s+"""), " ").trim()
      if (normalized.isEmpty()) continue

      val candidate = if (current.isEmpty()) normalized else "$current $normalized"
      if (candidate.length <= limit) {
        current.setLength(0)
        current.append(candidate)
      } else {
        flush()
        if (normalized.length <= limit) {
          current.append(normalized)
        } else {
          splitOversizedBySentence(normalized, limit).forEach { chunks.add(it) }
        }
      }
    }
    flush()
    return chunks
  }

  private fun splitOversizedBySentence(text: String, limit: Int): List<String> {
    val result = mutableListOf<String>()
    val sentences = text.split(Regex("""(?<=[.!?])\s+"""))
    val buffer = StringBuilder()

    fun flushBuffer() {
      val piece = buffer.toString().trim()
      if (piece.isNotEmpty()) result.add(piece)
      buffer.setLength(0)
    }

    for (sentence in sentences) {
      val s = sentence.trim()
      if (s.isEmpty()) continue

      val candidate = if (buffer.isEmpty()) s else "$buffer $s"
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
    for (word in text.split(Regex("""\s+"""))) {
      if (word.isEmpty()) continue
      val candidate = if (buffer.isEmpty()) word else "$buffer $word"
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

  /**
   * Formats turns into the exact `Speaker: text` format expected by Gemini's
   * `multiSpeakerVoiceConfig` API.
   */
  fun formatForGeminiMultiSpeaker(
    turns: List<DialogueTurn>,
    hostAName: String = "Alex",
    hostBName: String = "Sam"
  ): String {
    val cleanA = hostAName.trim().ifBlank { "Alex" }
    val cleanB = hostBName.trim().ifBlank { "Sam" }
    return turns.joinToString("\n") { turn ->
      val speakerName = if (turn.isHostB) cleanB else cleanA
      "$speakerName: ${turn.text}"
    }
  }

  /**
   * Formats turns into multi-voice SSML for Microsoft Azure AI Speech.
   */
  fun formatAsAzureMultiVoiceSsml(
    turns: List<DialogueTurn>,
    hostAVoice: String,
    hostBVoice: String
  ): String {
    val body = turns.joinToString("\n") { turn ->
      val voice = if (turn.isHostB) hostBVoice else hostAVoice
      val escaped = escapeXml(turn.text)
      """  <voice name="${escapeXml(voice)}">$escaped <break time="250ms"/></voice>"""
    }
    return """
      <speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis" xml:lang="en-US">
      $body
      </speak>
    """.trimIndent()
  }

  private fun escapeXml(input: String): String {
    return input
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")
  }
}
