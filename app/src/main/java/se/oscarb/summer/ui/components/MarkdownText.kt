package se.oscarb.summer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

sealed interface MarkdownBlock {
  data class Heading(val level: Int, val text: String) : MarkdownBlock
  data class Paragraph(val text: String) : MarkdownBlock
  data class BulletList(val items: List<String>) : MarkdownBlock
  data class NumberedList(val items: List<Pair<String, String>>) : MarkdownBlock
  data class BlockQuote(val text: String) : MarkdownBlock
  data class CodeFence(val code: String) : MarkdownBlock
  data object Divider : MarkdownBlock
}

object MarkdownParser {
  private val HEADING_REGEX = Regex("""^(#{1,6})\s+(.+)$""")
  private val BULLET_REGEX = Regex("""^\s*[-*•]\s+(.+)$""")
  private val NUMBERED_REGEX = Regex("""^\s*(\d+[.)])\s+(.+)$""")
  private val DIVIDER_REGEX = Regex("""^\s*([-*_])\1{2,}\s*$""")
  private val INLINE_TOKEN_REGEX = Regex(
    """(`[^`]+`)|(\*\*[^*]+\*\*)|(__[^_]+__)|(~~[^~]+~~)|(\[([^\]]+)]\((https?://[^)\s]+)\))|((?<!\*)\*(?!\s)([^*]+?)(?<!\s)\*(?!\*))|((?<!_)_(?!\s)([^_]+?)(?<!\s)_(?!_))"""
  )

  fun parseBlocks(markdown: String): List<MarkdownBlock> {
    if (markdown.isBlank()) return emptyList()
    val lines = markdown.replace("\r\n", "\n").lines()
    val blocks = mutableListOf<MarkdownBlock>()

    val paragraphBuf = mutableListOf<String>()
    val bulletBuf = mutableListOf<String>()
    val numberedBuf = mutableListOf<Pair<String, String>>()
    val quoteBuf = mutableListOf<String>()
    val codeBuf = mutableListOf<String>()
    var inCodeFence = false

    fun flushAllTextBuffers() {
      if (paragraphBuf.isNotEmpty()) {
        blocks.add(MarkdownBlock.Paragraph(paragraphBuf.joinToString(" ").trim()))
        paragraphBuf.clear()
      }
      if (bulletBuf.isNotEmpty()) {
        blocks.add(MarkdownBlock.BulletList(bulletBuf.toList()))
        bulletBuf.clear()
      }
      if (numberedBuf.isNotEmpty()) {
        blocks.add(MarkdownBlock.NumberedList(numberedBuf.toList()))
        numberedBuf.clear()
      }
      if (quoteBuf.isNotEmpty()) {
        blocks.add(MarkdownBlock.BlockQuote(quoteBuf.joinToString("\n").trim()))
        quoteBuf.clear()
      }
    }

    for (rawLine in lines) {
      val trimmed = rawLine.trim()

      if (trimmed.startsWith("```")) {
        if (inCodeFence) {
          blocks.add(MarkdownBlock.CodeFence(codeBuf.joinToString("\n").trimEnd()))
          codeBuf.clear()
          inCodeFence = false
        } else {
          flushAllTextBuffers()
          inCodeFence = true
        }
        continue
      }

      if (inCodeFence) {
        codeBuf.add(rawLine)
        continue
      }

      if (trimmed.isEmpty()) {
        flushAllTextBuffers()
        continue
      }

      if (DIVIDER_REGEX.matches(trimmed)) {
        flushAllTextBuffers()
        blocks.add(MarkdownBlock.Divider)
        continue
      }

      val headingMatch = HEADING_REGEX.matchEntire(trimmed)
      if (headingMatch != null) {
        flushAllTextBuffers()
        val level = headingMatch.groupValues[1].length
        val headingText = headingMatch.groupValues[2].trim()
        blocks.add(MarkdownBlock.Heading(level = level, text = headingText))
        continue
      }

      if (trimmed.startsWith(">")) {
        if (paragraphBuf.isNotEmpty() || bulletBuf.isNotEmpty() || numberedBuf.isNotEmpty()) {
          flushAllTextBuffers()
        }
        quoteBuf.add(trimmed.removePrefix(">").trim())
        continue
      }

      val bulletMatch = BULLET_REGEX.matchEntire(rawLine)
      if (bulletMatch != null) {
        if (paragraphBuf.isNotEmpty() || numberedBuf.isNotEmpty() || quoteBuf.isNotEmpty()) {
          flushAllTextBuffers()
        }
        bulletBuf.add(bulletMatch.groupValues[1].trim())
        continue
      }

      val numberedMatch = NUMBERED_REGEX.matchEntire(rawLine)
      if (numberedMatch != null) {
        if (paragraphBuf.isNotEmpty() || bulletBuf.isNotEmpty() || quoteBuf.isNotEmpty()) {
          flushAllTextBuffers()
        }
        numberedBuf.add(numberedMatch.groupValues[1] to numberedMatch.groupValues[2].trim())
        continue
      }

      // Continuation of an active bullet or numbered item if indented, otherwise paragraph
      if (bulletBuf.isNotEmpty() && rawLine.startsWith("  ")) {
        val last = bulletBuf.removeAt(bulletBuf.lastIndex)
        bulletBuf.add("$last $trimmed")
      } else if (numberedBuf.isNotEmpty() && rawLine.startsWith("  ")) {
        val (num, last) = numberedBuf.removeAt(numberedBuf.lastIndex)
        numberedBuf.add(num to "$last $trimmed")
      } else {
        if (bulletBuf.isNotEmpty() || numberedBuf.isNotEmpty() || quoteBuf.isNotEmpty()) {
          flushAllTextBuffers()
        }
        paragraphBuf.add(trimmed)
      }
    }

    if (inCodeFence && codeBuf.isNotEmpty()) {
      blocks.add(MarkdownBlock.CodeFence(codeBuf.joinToString("\n").trimEnd()))
    }
    flushAllTextBuffers()
    return blocks
  }

  fun parseInlineAnnotatedString(
    text: String,
    codeBackground: Color = Color.Unspecified,
    linkColor: Color = Color.Unspecified
  ): AnnotatedString {
    return buildAnnotatedString {
      var cursor = 0
      for (match in INLINE_TOKEN_REGEX.findAll(text)) {
        if (match.range.first > cursor) {
          append(text.substring(cursor, match.range.first))
        }
        val token = match.value
        when {
          token.startsWith("`") && token.endsWith("`") && token.length >= 2 -> {
            val inner = token.substring(1, token.length - 1)
            val start = length
            append(inner)
            addStyle(
              SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = codeBackground
              ),
              start,
              length
            )
          }
          (token.startsWith("**") && token.endsWith("**") && token.length >= 4) ||
            (token.startsWith("__") && token.endsWith("__") && token.length >= 4) -> {
            val inner = token.substring(2, token.length - 2)
            val start = length
            append(parseInlineAnnotatedString(inner, codeBackground, linkColor))
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
          }
          token.startsWith("~~") && token.endsWith("~~") && token.length >= 4 -> {
            val inner = token.substring(2, token.length - 2)
            val start = length
            append(parseInlineAnnotatedString(inner, codeBackground, linkColor))
            addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, length)
          }
          match.groups[5] != null -> {
            val label = match.groupValues[6]
            val url = match.groupValues[7]
            val start = length
            append(label)
            addLink(
              url = LinkAnnotation.Url(
                url = url,
                styles = TextLinkStyles(
                  style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                  )
                )
              ),
              start = start,
              end = length
            )
          }
          (token.startsWith("*") && token.endsWith("*") && token.length >= 3) ||
            (token.startsWith("_") && token.endsWith("_") && token.length >= 3) -> {
            val inner = token.substring(1, token.length - 1)
            val start = length
            append(parseInlineAnnotatedString(inner, codeBackground, linkColor))
            addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
          }
          else -> append(token)
        }
        cursor = match.range.last + 1
      }
      if (cursor < text.length) {
        append(text.substring(cursor))
      }
    }
  }
}

@Composable
fun MarkdownText(
  markdown: String,
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.bodyMedium,
  color: Color = MaterialTheme.colorScheme.onSurface
) {
  val blocks = remember(markdown) { MarkdownParser.parseBlocks(markdown) }
  val codeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
  val linkColor = MaterialTheme.colorScheme.primary

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    blocks.forEach { block ->
      when (block) {
        is MarkdownBlock.Heading -> {
          val headingStyle = when (block.level) {
            1 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            2 -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            else -> style.copy(fontWeight = FontWeight.Bold)
          }
          val annotated = remember(block.text, codeBg, linkColor) {
            MarkdownParser.parseInlineAnnotatedString(block.text, codeBg, linkColor)
          }
          Text(
            text = annotated,
            style = headingStyle,
            color = color
          )
        }

        is MarkdownBlock.Paragraph -> {
          val annotated = remember(block.text, codeBg, linkColor) {
            MarkdownParser.parseInlineAnnotatedString(block.text, codeBg, linkColor)
          }
          Text(
            text = annotated,
            style = style,
            color = color
          )
        }

        is MarkdownBlock.BulletList -> {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            block.items.forEach { itemText ->
              val annotated = remember(itemText, codeBg, linkColor) {
                MarkdownParser.parseInlineAnnotatedString(itemText, codeBg, linkColor)
              }
              Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                  text = "•",
                  style = style.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                  text = annotated,
                  style = style,
                  color = color,
                  modifier = Modifier.weight(1f)
                )
              }
            }
          }
        }

        is MarkdownBlock.NumberedList -> {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            block.items.forEach { (prefix, itemText) ->
              val annotated = remember(itemText, codeBg, linkColor) {
                MarkdownParser.parseInlineAnnotatedString(itemText, codeBg, linkColor)
              }
              Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                  text = prefix,
                  style = style.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                  text = annotated,
                  style = style,
                  color = color,
                  modifier = Modifier.weight(1f)
                )
              }
            }
          }
        }

        is MarkdownBlock.BlockQuote -> {
          val annotated = remember(block.text, codeBg, linkColor) {
            MarkdownParser.parseInlineAnnotatedString(block.text, codeBg, linkColor)
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(IntrinsicSize.Min)
          ) {
            Box(
              modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                  shape = RoundedCornerShape(2.dp)
                )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = annotated,
              style = style.copy(fontStyle = FontStyle.Italic),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f)
            )
          }
        }

        is MarkdownBlock.CodeFence -> {
          Surface(
            color = codeBg,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = block.code,
              style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
              color = color,
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        MarkdownBlock.Divider -> {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
      }
    }
  }
}
