package se.oscarb.summer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI

data class ScrapedPageData(
  val title: String,
  val faviconUrl: String,
  val thumbnailUrl: String = "",
  val domain: String,
  val readableContent: String
)

object WebPageScraper {
  private const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

  fun extractDomain(url: String): String {
    return try {
      val host = URI(url).host ?: url
      host.removePrefix("www.")
    } catch (_: Exception) {
      url.removePrefix("https://").removePrefix("http://").substringBefore("/")
    }
  }

  fun defaultFaviconUrl(url: String): String {
    val domain = extractDomain(url)
    return "https://www.google.com/s2/favicons?domain=$domain&sz=128"
  }

  internal fun extractThumbnailUrl(doc: Document): String {
    val metaSelectors = listOf(
      "meta[property=og:image]" to "content",
      "meta[property=og:image:secure_url]" to "content",
      "meta[property=og:image:url]" to "content",
      "meta[name=og:image]" to "content",
      "meta[name=twitter:image]" to "content",
      "meta[name=twitter:image:src]" to "content",
      "meta[property=twitter:image]" to "content",
      "link[rel=image_src]" to "href"
    )

    for ((selector, attrName) in metaSelectors) {
      val el = doc.selectFirst(selector) ?: continue
      val resolved = el.absUrl(attrName).trim().ifBlank {
        resolveAgainstBase(doc.baseUri(), el.attr(attrName).trim())
      }
      if (resolved.startsWith("http://", ignoreCase = true) ||
        resolved.startsWith("https://", ignoreCase = true)
      ) {
        return resolved
      }
    }

    // Fallback: first substantive article/main image
    val candidateImages = doc.select("article img, main img, [role=main] img")
    for (img in candidateImages) {
      val src = img.absUrl("src").trim().ifBlank {
        img.absUrl("data-src").trim()
      }
      val lower = src.lowercase()
      val isLikelyDecorative = lower.endsWith(".svg") ||
        lower.contains("1x1") ||
        lower.contains("pixel") ||
        lower.contains("spacer") ||
        lower.contains("avatar") ||
        lower.contains("favicon") ||
        lower.contains("/logo")
      val widthAttr = img.attr("width").toIntOrNull()
      val heightAttr = img.attr("height").toIntOrNull()
      val tooSmall = (widthAttr != null && widthAttr < 80) || (heightAttr != null && heightAttr < 80)

      if ((src.startsWith("http://", ignoreCase = true) || src.startsWith("https://", ignoreCase = true)) &&
        !isLikelyDecorative &&
        !tooSmall
      ) {
        return src
      }
    }

    return ""
  }

  private fun resolveAgainstBase(baseUri: String, rawRef: String): String {
    if (rawRef.isBlank()) return ""
    if (rawRef.startsWith("http://", ignoreCase = true) || rawRef.startsWith("https://", ignoreCase = true)) {
      return rawRef
    }
    if (rawRef.startsWith("//")) {
      return "https:$rawRef"
    }
    return try {
      if (baseUri.isNotBlank()) URI(baseUri).resolve(rawRef).toString() else ""
    } catch (_: Exception) {
      ""
    }
  }

  suspend fun scrape(url: String): ScrapedPageData = withContext(Dispatchers.IO) {
    val domain = extractDomain(url)
    val fallbackFavicon = defaultFaviconUrl(url)

    try {
      val doc = Jsoup.connect(url)
        .userAgent(USER_AGENT)
        .timeout(12_000)
        .maxBodySize(2 * 1024 * 1024)
        .followRedirects(true)
        .ignoreHttpErrors(true)
        .get()

      val finalDomain = extractDomain(doc.location().ifBlank { url }).ifBlank { domain }

      // 1. Extract Title
      val ogTitle = doc.select("meta[property=og:title]").attr("content").trim()
      val twitterTitle = doc.select("meta[name=twitter:title]").attr("content").trim()
      val docTitle = doc.title().trim()
      val h1Title = doc.selectFirst("h1")?.text()?.trim().orEmpty()

      val title = listOf(ogTitle, docTitle, twitterTitle, h1Title)
        .firstOrNull { it.isNotBlank() }
        ?: finalDomain

      // 2. Extract Favicon (prefer compact standard icon or Google S2 for crisp small inline display)
      val iconElement = doc.selectFirst(
        "link[rel~=(?i)^(icon|shortcut icon|apple-touch-icon|apple-touch-icon-precomposed)$]"
      )
      val resolvedIcon = iconElement?.absUrl("href")?.takeIf { it.startsWith("http") }
      val faviconUrl = resolvedIcon ?: defaultFaviconUrl(doc.location().ifBlank { url })

      // 3. Extract Preview Thumbnail (og:image / twitter:image / lead article image)
      val thumbnailUrl = extractThumbnailUrl(doc)

      // 4. Extract Description + Clean Article Body
      val description = doc.select("meta[property=og:description], meta[name=description]")
        .attr("content")
        .trim()

      // Remove boilerplate noise before extracting text
      doc.select("script, style, nav, footer, header, aside, noscript, iframe, svg, form").remove()

      val mainContainer = doc.selectFirst("article, main, [role=main], .post-content, .article-body, .entry-content")
        ?: doc.body()

      val paragraphs = mainContainer
        .select("h1, h2, h3, p, li, blockquote")
        .mapNotNull { el ->
          val text = el.text().trim()
          if (text.length >= 25 || el.tagName().startsWith("h")) text else null
        }
        .distinct()
        .joinToString("\n\n")

      val bodyText = if (paragraphs.length >= 120) {
        paragraphs
      } else {
        mainContainer.text().trim()
      }

      val combinedContent = buildString {
        if (description.isNotBlank()) {
          appendLine("Meta Description: $description")
          appendLine()
        }
        append(bodyText.take(14_000))
      }.trim()

      ScrapedPageData(
        title = title,
        faviconUrl = faviconUrl,
        thumbnailUrl = thumbnailUrl,
        domain = finalDomain,
        readableContent = combinedContent
      )
    } catch (_: Exception) {
      ScrapedPageData(
        title = domain,
        faviconUrl = fallbackFavicon,
        thumbnailUrl = "",
        domain = domain,
        readableContent = ""
      )
    }
  }
}
