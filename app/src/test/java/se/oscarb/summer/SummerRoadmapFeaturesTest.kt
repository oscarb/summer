package se.oscarb.summer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.oscarb.summer.data.AppSettings
import se.oscarb.summer.data.AssignedHostVoice
import se.oscarb.summer.data.DialogueTurn
import se.oscarb.summer.data.PodcastCastCatalog
import se.oscarb.summer.data.PodcastLanguage
import se.oscarb.summer.data.PodcastTtsProvider
import se.oscarb.summer.data.PodcastTtsService
import se.oscarb.summer.data.SavedWebsite
import se.oscarb.summer.data.SummerRepository
import se.oscarb.summer.data.VoicePersona
import se.oscarb.summer.data.WebPageScraper
import se.oscarb.summer.ui.components.MarkdownBlock
import se.oscarb.summer.ui.components.MarkdownParser
import se.oscarb.summer.ui.main.formatDateHeaderLabel
import se.oscarb.summer.ui.main.groupWebsitesByCreatedDate
import se.oscarb.summer.util.PodcastScriptParser
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.random.Random

class SummerRoadmapFeaturesTest {

  @Test
  fun markdownParser_parsesTldrParagraphHeadingsBulletsAndInlineStyles() {
    val md = """
      **TL;DR:** Android 17 brings faster *on-device* inference with `AICore`.

      ## Key Takeaways
      - **Zero latency** startup on flagship devices
      - Read more on [Android Developers](https://developer.android.com/ai)

      1. First step
      2. Second step
    """.trimIndent()

    val blocks = MarkdownParser.parseBlocks(md)
    assertEquals(4, blocks.size)
    assertTrue(blocks[0] is MarkdownBlock.Paragraph)
    assertTrue(blocks[1] is MarkdownBlock.Heading)
    assertTrue(blocks[2] is MarkdownBlock.BulletList)
    assertTrue(blocks[3] is MarkdownBlock.NumberedList)

    val heading = blocks[1] as MarkdownBlock.Heading
    assertEquals(2, heading.level)
    assertEquals("Key Takeaways", heading.text)

    val bullets = (blocks[2] as MarkdownBlock.BulletList).items
    assertEquals(2, bullets.size)

    val inline = MarkdownParser.parseInlineAnnotatedString(
      text = "**TL;DR:** Fast *local* `AI`",
      codeBackground = Color.LightGray,
      linkColor = Color.Blue
    )
    assertEquals("TL;DR: Fast local AI", inline.text)
    assertTrue(inline.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
    assertTrue(inline.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
  }

  @Test
  fun podcastScriptParser_parsesThreeSwedishHostsAndStripsSwedishStageDirections() {
    val swedishScript = """
      **Astrid**: Välkomna tillbaka till Summer! Idag är det fredag den 30 oktober. Jag heter Astrid, och med mig har jag Erik och Freja.
      **Erik**: Hej allihopa! [skrattar] Vilken spännande samling artiklar vi har idag [1].
      **Freja**: Verkligen, den röda tråden genom alla fem länkarna är hur snabbt AI på mobilen utvecklas.
      Talare 3: Och det märks extra tydligt i den sista artikeln.
    """.trimIndent()

    val turns = PodcastScriptParser.parseTurns(
      rawScript = swedishScript,
      hostNames = listOf("Astrid", "Erik", "Freja")
    )
    assertEquals(4, turns.size)

    assertEquals(0, turns[0].hostIndex)
    assertEquals("Astrid", turns[0].speaker)
    assertTrue(turns[0].text.startsWith("Välkomna tillbaka till Summer!"))

    assertEquals(1, turns[1].hostIndex)
    assertEquals("Erik", turns[1].speaker)
    assertEquals("Hej allihopa! Vilken spännande samling artiklar vi har idag.", turns[1].text)

    assertEquals(2, turns[2].hostIndex)
    assertEquals("Freja", turns[2].speaker)

    assertEquals(2, turns[3].hostIndex)
    assertEquals("Freja", turns[3].speaker)
  }

  @Test
  fun groupTurnsIntoGeminiBatches_neverExceedsTwoDistinctSpeakersPerBatchForThreeHostPodcast() {
    val cast = listOf(
      AssignedHostVoice(0, "Astrid", "Kore", VoicePersona.FEMININE),
      AssignedHostVoice(1, "Erik", "Puck", VoicePersona.MASCULINE),
      AssignedHostVoice(2, "Freja", "Aoede", VoicePersona.FEMININE)
    )
    val turns = listOf(
      DialogueTurn("Astrid", 0, "Welcome everyone!"),
      DialogueTurn("Erik", 1, "Great to be here, Astrid."),
      DialogueTurn("Astrid", 0, "Freja, what caught your eye first?"),
      DialogueTurn("Freja", 2, "Definitely the first story about Jetpack Compose."),
      DialogueTurn("Erik", 1, "I noticed that too!"),
      DialogueTurn("Freja", 2, "And it connects directly to story two."),
      DialogueTurn("Astrid", 0, "That wraps up today's episode!")
    )

    val batches = PodcastTtsService.groupTurnsIntoGeminiBatches(turns, cast, maxUtf8Bytes = 3_400)
    // Batch 1: hosts {0, 1} (turns 0, 1, 2)
    // Batch 2: hosts {2, 1} (turns 3, 4, 5)
    // Batch 3: host {0} (turn 6)
    assertEquals(3, batches.size)
    batches.forEach { batch ->
      val distinctHosts = batch.map { it.hostIndex }.toSet()
      assertTrue(
        "Gemini batch had ${distinctHosts.size} speakers ($distinctHosts), expected <= 2",
        distinctHosts.size <= 2
      )
    }
    assertEquals(turns.map { it.text }, batches.flatten().map { it.text })
  }

  @Test
  fun podcastCastCatalog_randomizesDistinctNamesAndVoicesInEnglishAndSwedishForTwoAndThreeHosts() {
    val svThreeSettings = AppSettings(
      podcastLanguage = PodcastLanguage.SWEDISH,
      podcastHostCount = 3,
      randomizePodcastHosts = true,
      podcastTtsProvider = PodcastTtsProvider.GEMINI_MULTISPEAKER
    )
    val castSv = PodcastCastCatalog.selectEpisodeCast(svThreeSettings, Random(42))
    assertEquals(3, castSv.size)
    assertEquals(3, castSv.map { it.name }.toSet().size)
    assertEquals(3, castSv.map { it.voiceId }.toSet().size)
    // Contrasting adjacent personas (F/M/F or M/F/M)
    assertTrue(castSv[0].persona != castSv[1].persona)
    assertTrue(castSv[1].persona != castSv[2].persona)

    val enTwoSettings = AppSettings(
      podcastLanguage = PodcastLanguage.ENGLISH,
      podcastHostCount = 2,
      randomizePodcastHosts = true,
      podcastTtsProvider = PodcastTtsProvider.GEMINI_MULTISPEAKER
    )
    val castEn = PodcastCastCatalog.selectEpisodeCast(enTwoSettings, Random(99))
    assertEquals(2, castEn.size)
    assertEquals(2, castEn.map { it.name }.toSet().size)
    assertEquals(2, castEn.map { it.voiceId }.toSet().size)
    assertEquals("${castEn[0].name} & ${castEn[1].name}", PodcastCastCatalog.formatHostList(castEn.map { it.name }))
  }

  @Test
  fun selectRecentSessionCluster_groupsRecentGoogleFeedBurstAndSkipsOlderSession() {
    val base = 1_760_000_000_000L
    val min = 60_000L

    fun site(id: String, offsetMinutes: Long) = SavedWebsite(
      id = id,
      url = "https://example.com/$id",
      domain = "example.com",
      title = "Article $id",
      faviconUrl = "",
      createdAt = base + offsetMinutes * min,
      updatedAt = base + offsetMinutes * min
    )

    // Older morning session (0 min, 3 min) + 3-hour gap + recent afternoon burst (180, 182, 185 min)
    val unread = listOf(
      site("old1", 0),
      site("old2", 3),
      site("burst1", 180),
      site("burst2", 182),
      site("burst3", 185)
    )

    val cluster = SummerRepository.selectRecentSessionCluster(unread, maxGapMinutes = 30)
    assertEquals(listOf("burst1", "burst2", "burst3"), cluster.map { it.id })
  }

  @Test
  fun extractThumbnailUrl_prefersOgImageThenTwitterImageThenArticleImg() {
    val ogHtml = """
      <html><head>
        <meta property="og:image" content="https://cdn.example.com/hero.jpg" />
        <meta name="twitter:image" content="https://cdn.example.com/twitter.jpg" />
      </head><body></body></html>
    """.trimIndent()
    val ogDoc = Jsoup.parse(ogHtml, "https://example.com/story")
    assertEquals("https://cdn.example.com/hero.jpg", WebPageScraper.extractThumbnailUrl(ogDoc))

    val fallbackHtml = """
      <html><head></head><body>
        <article>
          <img src="/assets/logo.svg" width="32" height="32" />
          <img src="/images/feature-photo.webp" width="800" height="450" />
        </article>
      </body></html>
    """.trimIndent()
    val fallbackDoc = Jsoup.parse(fallbackHtml, "https://example.com/post")
    assertEquals("https://example.com/images/feature-photo.webp", WebPageScraper.extractThumbnailUrl(fallbackDoc))
  }

  @Test
  fun groupWebsitesByCreatedDate_groupsDescendingWithTodayYesterdayAndFormattedDates() {
    val zone = ZoneId.of("Europe/Stockholm")
    val today = LocalDate.of(2026, 10, 30)

    fun epochAt(date: LocalDate, hour: Int): Long =
      date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    fun site(id: String, createdAt: Long) = SavedWebsite(
      id = id,
      url = "https://example.com/$id",
      domain = "example.com",
      title = id,
      faviconUrl = "",
      createdAt = createdAt,
      updatedAt = createdAt
    )

    val sites = listOf(
      site("oct20", epochAt(LocalDate.of(2026, 10, 20), 10)),
      site("today1", epochAt(today, 9)),
      site("yesterday1", epochAt(today.minusDays(1), 18)),
      site("today2", epochAt(today, 14)),
      site("lastYear", epochAt(LocalDate.of(2025, 12, 15), 12))
    )

    val grouped = groupWebsitesByCreatedDate(
      websites = sites,
      zoneId = zone,
      today = today,
      locale = Locale.US
    )
    assertEquals(listOf("Today", "Yesterday", "Oct 20", "Dec 15, 2025"), grouped.map { it.first })
    assertEquals(listOf("today2", "today1"), grouped[0].second.map { it.id })
    assertEquals("Today", formatDateHeaderLabel(today, today, Locale.US))
  }
}
