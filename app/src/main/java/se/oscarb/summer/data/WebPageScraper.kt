package se.oscarb.summer.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URI

data class ScrapedPageData(
  val title: String,
  val faviconUrl: String,
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

  suspend fun scrape(url: String): ScrapedPageData = withContext(Dispatchers.IO) {
    val domain = extractDomain(url)
    val fallbackFavicon = defaultFaviconUrl(url)

    try {
      val doc = Jsoup.connect(url)
        .userAgent(USER_AGENT)
        .timeout(12_000)
        .followRedirects(true)
        .ignoreHttpErrors(true)
        .get()

      // 1. Extract Title
      val ogTitle = doc.select("meta[property=og:title]").attr("content").trim()
      val twitterTitle = doc.select("meta[name=twitter:title]").attr("content").trim()
      val docTitle = doc.title().trim()
      val h1Title = doc.selectFirst("h1")?.text()?.trim().orEmpty()

      val title = listOf(ogTitle, docTitle, twitterTitle, h1Title)
        .firstOrNull { it.isNotBlank() }
        ?: domain

      // 2. Extract Favicon
      val iconElement = doc.selectFirst(
        "link[rel~=(?i)^(apple-touch-icon|apple-touch-icon-precomposed|icon|shortcut icon)$]"
      )
      val resolvedIcon = iconElement?.absUrl("href")?.takeIf { it.startsWith("http") }
      val faviconUrl = resolvedIcon ?: fallbackFavicon

      // 3. Extract Description + Clean Article Body
      val description = doc.select("meta[property=og:description], meta[name=description]")
        .attr("content")
        .trim()

      // Remove boilerplate noise before extracting text
      doc.select("script, style, nav, footer, header, aside, noscript, iframe, svg, form").remove()

      val mainContainer = doc.selectFirst("article, main, [role=main], .post-content, .article-body, .entry-content")
        ?: doc.body()

      val paragraphs = mainContainer
        ?.select("h1, h2, h3, p, li, blockquote")
        ?.mapNotNull { el ->
          val text = el.text().trim()
          if (text.length >= 25 || el.tagName().startsWith("h")) text else null
        }
        ?.distinct()
        ?.joinToString("\n\n")
        .orEmpty()

      val bodyText = if (paragraphs.length >= 120) {
        paragraphs
      } else {
        mainContainer?.text()?.trim().orEmpty()
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
        domain = domain,
        readableContent = combinedContent
      )
    } catch (ce: CancellationException) {
      throw ce
    } catch (_: Exception) {
      ScrapedPageData(
        title = domain,
        faviconUrl = fallbackFavicon,
        domain = domain,
        readableContent = ""
      )
    }
  }
}
