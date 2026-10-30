package se.oscarb.summer.util

import se.oscarb.summer.data.DialogueTurn

/**
 * Parses and normalizes conversational podcast scripts across 2 or 3 hosts
 * so they can be synthesized accurately by multi-speaker or turn-by-turn TTS engines.
 */
object PodcastScriptParser {

  const val DEFAULT_MAX_TURN_CHUNK_CHARS = 650

  private val SPEAKER_LINE_REGEX = Regex(
    pattern = """^\s*(?:[-*•]\s*)?(?:\*\*|__)?([\p{L}0-9 _.-]{2,28})(?:\*\*|__)?\s*:\s*(.+)$"""
  )
  private val STAGE_DIRECTION_REGEX = Regex(
    """\[[^\]]*laughs[^\]]*]|\([^)]*laughs[^)]*\)|\[[^\]]*skratt[^\]]*]|\([^)]*skratt[^)]*\)|\[[^\]]*sighs[^\]]*]|\([^)]*sighs[^)]*\)|\[[^\]]*music[^\]]*]|\([^)]*music[^)]*\)|\[[^\]]*musik[^\]]*]|\([^)]*musik[^)]*\)|\[[^\]]*pause[^\]]*]""",
    RegexOption.IGNORE_CASE
  )
  private val HORIZONTAL_RULE_REGEX = Regex("""^\s*([-*_])\1{2,}\s*$""")
  private val MARKDOWN_IMAGE_REGEX = Regex("""!\[([^\]]*)]\([^)]*\)""")
  private val MARKDOWN_LINK_REGEX = Regex("""\[([^\]]+)]\([^)]*\)""")
  private val INLINE_CODE_REGEX = Regex("""`([^`]*)`""")
  private val CITATION_BRACKET_REGEX = Regex("""\[\d+]""")
  private val RESIDUAL_BRACKET_NOTE_REGEX = Regex("""\[[^\]]*]""")
  private val MARKDOWN_DECORATION_REGEX = Regex("""(\*\*|__|`)""")
  private val ITALIC_STAR_REGEX = Regex("""(?<!\*)\*(?!\s)([^*]+?)(?<!\s)\*(?!\*)""")
  private val ITALIC_UNDERSCORE_REGEX = Regex("""(?<!_)_(?!\s)([^_]+?)(?<!\s)_(?!_)""")

  private val SECOND_HOST_FALLBACK_REGEX = Regex(
    pattern = """^(?:co-?host|(?:host|speaker|person|voice|värd|talare)\s*[-#]?\s*(?:2|b|two|två))$|\bco-?host\b""",
    option = RegexOption.IGNORE_CASE
  )
  private val THIRD_HOST_FALLBACK_REGEX = Regex(
    pattern = """^(?:(?:host|speaker|person|voice|värd|talare)\s*[-#]?\s*(?:3|c|three|tre)|guest|gäst)$""",
    option = RegexOption.IGNORE_CASE
  )

  fun parseTurns(
    rawScript: String,
    hostAName: String = "Alex",
    hostBName: String = "Sam"
  ): List<DialogueTurn> {
    return parseTurns(
      rawScript = rawScript,
      hostNames = listOf(
        hostAName.trim().ifBlank { "Alex" },
        hostBName.trim().ifBlank { "Sam" }
      )
    )
  }

  /**
   * Parses a raw LLM-generated script into structured [DialogueTurn] items across 2 or 3 hosts.
   */
  fun parseTurns(
    rawScript: String,
    hostNames: List<String>
  ): List<DialogueTurn> {
    if (rawScript.isBlank()) return emptyList()

    val cleanHosts = hostNames
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .ifEmpty { listOf("Alex", "Sam") }

    val turns = mutableListOf<DialogueTurn>()
    var currentHostIndex = 0
    var currentSpeaker = cleanHosts[0]
    val currentBuffer = StringBuilder()

    fun flushBuffer() {
      val text = cleanSpokenText(currentBuffer.toString())
      if (text.isNotBlank()) {
        turns.add(
          DialogueTurn(
            speaker = currentSpeaker,
            hostIndex = currentHostIndex,
            text = text
          )
        )
      }
      currentBuffer.setLength(0)
    }

    for (rawLine in rawScript.lines()) {
      val trimmed = rawLine.trim()
      if (trimmed.isEmpty()) continue
      if (trimmed.startsWith("#") || HORIZONTAL_RULE_REGEX.matches(trimmed)) continue

      val match = SPEAKER_LINE_REGEX.matchEntire(trimmed)
      if (match != null) {
        val rawSpeakerLabel = match.groupValues[1].trim()
        val spokenPart = match.groupValues[2].trim()

        val lowerLabel = rawSpeakerLabel.lowercase()
        val isMetadataPrefix = lowerLabel == "tl;dr" ||
          lowerLabel == "summary" ||
          lowerLabel == "sammanfattning" ||
          lowerLabel == "title" ||
          lowerLabel == "titel" ||
          lowerLabel == "url" ||
          lowerLabel.startsWith("article") ||
          lowerLabel.startsWith("artikel") ||
          lowerLabel.startsWith("http")

        if (!isMetadataPrefix) {
          flushBuffer()
          val idx = resolveHostIndex(rawSpeakerLabel, cleanHosts)
          currentHostIndex = idx
          currentSpeaker = cleanHosts.getOrElse(idx) { cleanHosts[0] }
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

    if (turns.isEmpty()) {
      val fallback = cleanSpokenText(rawScript)
      if (fallback.isNotBlank()) {
        return listOf(DialogueTurn(speaker = cleanHosts[0], hostIndex = 0, text = fallback))
      }
    }

    return turns
  }

  private fun resolveHostIndex(
    rawSpeakerLabel: String,
    cleanHosts: List<String>
  ): Int {
    val normalized = rawSpeakerLabel.trim().lowercase()
    val lowers = cleanHosts.map { it.lowercase() }

    // 1. Exact case-insensitive match first
    val exactIdx = lowers.indexOfFirst { it.isNotEmpty() && it == normalized }
    if (exactIdx >= 0) return exactIdx

    // 2. Whole-word match (preferring longer host name if multiple match)
    val wholeMatches = lowers.mapIndexedNotNull { index, name ->
      if (containsWholeName(normalized, name)) index to name.length else null
    }
    if (wholeMatches.isNotEmpty()) {
      return wholeMatches.maxBy { it.second }.first
    }

    // 3. Substring match (preferring longer host name if multiple match)
    val subMatches = lowers.mapIndexedNotNull { index, name ->
      if (name.isNotEmpty() && normalized.contains(name)) index to name.length else null
    }
    if (subMatches.isNotEmpty()) {
      return subMatches.maxBy { it.second }.first
    }

    // 4. Generic role fallback ("Host 3" / "Host C", "Host 2" / "Co-host", else Host 0)
    if (cleanHosts.size >= 3 && THIRD_HOST_FALLBACK_REGEX.containsMatchIn(normalized)) {
      return 2
    }
    if (cleanHosts.size >= 2 && SECOND_HOST_FALLBACK_REGEX.containsMatchIn(normalized)) {
      return 1
    }
    return 0
  }

  private fun containsWholeName(haystack: String, needle: String): Boolean {
    if (needle.isBlank()) return false
    val pattern = Regex("""(?<!\p{L})${Regex.escape(needle)}(?!\p{L})""", RegexOption.IGNORE_CASE)
    return pattern.containsMatchIn(haystack)
  }

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
      .replace(Regex("""\s+([.,!?;:])"""), "$1")
      .trim()
  }

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

  fun formatForGeminiMultiSpeaker(
    turns: List<DialogueTurn>,
    hostAName: String = "Alex",
    hostBName: String = "Sam"
  ): String {
    return formatForGeminiMultiSpeaker(turns, listOf(hostAName, hostBName))
  }

  fun formatForGeminiMultiSpeaker(
    turns: List<DialogueTurn>,
    hostNames: List<String>
  ): String {
    val cleanHosts = hostNames.map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Alex", "Sam") }
    return turns.joinToString("\n") { turn ->
      val speakerName = cleanHosts.getOrElse(turn.hostIndex) { cleanHosts[0] }
      "$speakerName: ${turn.text}"
    }
  }

  fun formatAsAzureMultiVoiceSsml(
    turns: List<DialogueTurn>,
    hostAVoice: String,
    hostBVoice: String
  ): String {
    return formatAsAzureMultiVoiceSsml(
      turns = turns,
      hostVoices = listOf(hostAVoice, hostBVoice),
      xmlLang = "en-US"
    )
  }

  fun formatAsAzureMultiVoiceSsml(
    turns: List<DialogueTurn>,
    hostVoices: List<String>,
    xmlLang: String = "en-US"
  ): String {
    val cleanVoices = hostVoices.ifEmpty { listOf("en-US-AndrewMultilingualNeural", "en-US-AvaMultilingualNeural") }
    val body = turns.joinToString("\n") { turn ->
      val voice = cleanVoices.getOrElse(turn.hostIndex) { cleanVoices[0] }
      val escaped = escapeXml(turn.text)
      """  <voice name="${escapeXml(voice)}">$escaped <break time="240ms"/></voice>"""
    }
    return """
      <speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis" xml:lang="${escapeXml(xmlLang)}">
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
