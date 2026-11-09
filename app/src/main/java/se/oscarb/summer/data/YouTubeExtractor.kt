package se.oscarb.summer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URI
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class YouTubeVideoMetadata(
  val videoId: String,
  val title: String,
  val channelTitle: String,
  val thumbnailUrl: String
)

data class YouTubeCaptionSegment(
  val startMs: Long,
  val durationMs: Long,
  val text: String
)

object YouTubeExtractor {

  private const val TAG = "YouTubeExtractor"
  private const val USER_AGENT_DESKTOP =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
  private const val USER_AGENT_ANDROID =
    "com.google.android.youtube/20.10.38 (Linux; U; Android 14) gzip"

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()

  private val json = Json { ignoreUnknownKeys = true }

  private val YOUTUBE_HOSTS = setOf(
    "youtube.com",
    "www.youtube.com",
    "m.youtube.com",
    "music.youtube.com",
    "youtu.be"
  )

  fun isYouTubeUrl(rawUrl: String): Boolean {
    if (rawUrl.isBlank()) return false
    val trimmed = rawUrl.trim()
    val host = runCatching { URI(trimmed).host?.lowercase() }.getOrNull().orEmpty()
    if (host in YOUTUBE_HOSTS || host.endsWith(".youtube.com") || host == "youtu.be") {
      return true
    }
    return trimmed.contains("youtube.com/", ignoreCase = true) || trimmed.contains("youtu.be/", ignoreCase = true)
  }

  fun extractVideoId(rawUrl: String): String? {
    if (rawUrl.isBlank()) return null
    val trimmed = rawUrl.trim()

    // 1. youtu.be/<id>
    val youtuBeMatch = Regex("""^https?://(?:www\.)?youtu\.be/([a-zA-Z0-9_-]{11})""", RegexOption.IGNORE_CASE).find(trimmed)
    if (youtuBeMatch != null) return youtuBeMatch.groupValues[1]

    // 2. /shorts/<id>, /embed/<id>, /live/<id>, /v/<id>
    val pathMatch = Regex("""^https?://(?:[a-zA-Z0-9-]+\.)?youtube\.com/(?:shorts|embed|live|v)/([a-zA-Z0-9_-]{11})""", RegexOption.IGNORE_CASE).find(trimmed)
    if (pathMatch != null) return pathMatch.groupValues[1]

    // 3. /watch?v=<id> or query param v=<id>
    val watchMatch = Regex("""[?&]v=([a-zA-Z0-9_-]{11})""").find(trimmed)
    if (watchMatch != null) return watchMatch.groupValues[1]

    return null
  }

  fun defaultThumbnailUrl(videoId: String): String {
    return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
  }

  fun defaultWatchUrl(videoId: String): String {
    return "https://www.youtube.com/watch?v=$videoId"
  }

  suspend fun fetchMetadata(videoId: String): YouTubeVideoMetadata = withContext(Dispatchers.IO) {
    val fallbackThumbnail = defaultThumbnailUrl(videoId)
    val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"

    try {
      val req = Request.Builder()
        .url(oembedUrl)
        .header("User-Agent", USER_AGENT_DESKTOP)
        .get()
        .build()

      httpClient.newCall(req).execute().use { resp ->
        if (resp.isSuccessful) {
          val bodyStr = resp.body.string()
          val root = json.parseToJsonElement(bodyStr).jsonObject
          val title = root["title"]?.jsonPrimitive?.content?.trim().orEmpty()
          val channel = root["author_name"]?.jsonPrimitive?.content?.trim().orEmpty()
          val thumb = root["thumbnail_url"]?.jsonPrimitive?.content?.trim().orEmpty()

          return@withContext YouTubeVideoMetadata(
            videoId = videoId,
            title = title.ifBlank { "YouTube Video ($videoId)" },
            channelTitle = channel.ifBlank { "YouTube" },
            thumbnailUrl = thumb.ifBlank { fallbackThumbnail }
          )
        }
      }
    } catch (_: Exception) {
      // Ignore network errors and return fallback metadata
    }

    YouTubeVideoMetadata(
      videoId = videoId,
      title = "YouTube Video ($videoId)",
      channelTitle = "YouTube",
      thumbnailUrl = fallbackThumbnail
    )
  }

  suspend fun fetchTranscript(videoId: String): List<YouTubeCaptionSegment> = withContext(Dispatchers.IO) {
    try {
      // Step 1: Fetch watch page to extract INNERTUBE_API_KEY and check for inline caption tracks
      val watchUrl = defaultWatchUrl(videoId)
      val watchReq = Request.Builder()
        .url(watchUrl)
        .header("User-Agent", USER_AGENT_DESKTOP)
        .header("Accept-Language", "en-US,en;q=0.9")
        .get()
        .build()

      val watchHtml = runCatching {
        httpClient.newCall(watchReq).execute().use { it.body.string() }
      }.getOrDefault("")

      val keyPattern = Pattern.compile(""""INNERTUBE_API_KEY":\s*"([a-zA-Z0-9_-]+)"""")
      val keyMatcher = keyPattern.matcher(watchHtml)
      val apiKey = if (keyMatcher.find()) keyMatcher.group(1).orEmpty() else ""

      // Step 2: Query InnerTube player endpoint as Android client
      val playerEndpoint = if (apiKey.isNotBlank()) {
        "https://www.youtube.com/youtubei/v1/player?key=$apiKey"
      } else {
        "https://www.youtube.com/youtubei/v1/player"
      }
      val playerPayload = buildJsonObject {
        put("context", buildJsonObject {
          put("client", buildJsonObject {
            put("clientName", kotlinx.serialization.json.JsonPrimitive("ANDROID"))
            put("clientVersion", kotlinx.serialization.json.JsonPrimitive("20.10.38"))
            put("androidSdkVersion", kotlinx.serialization.json.JsonPrimitive(34))
          })
        })
        put("videoId", kotlinx.serialization.json.JsonPrimitive(videoId))
      }.toString()

      val playerReq = Request.Builder()
        .url(playerEndpoint)
        .header("Content-Type", "application/json; charset=utf-8")
        .header("User-Agent", USER_AGENT_ANDROID)
        .post(playerPayload.toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()

      val playerJsonStr = runCatching {
        httpClient.newCall(playerReq).execute().use { it.body.string() }
      }.getOrDefault("")

      // Step 3: Find caption tracks (first from player response, then fallback to watch HTML)
      val captionBaseUrl = extractCaptionBaseUrl(playerJsonStr).ifBlank {
        extractCaptionBaseUrlFromHtml(watchHtml)
      }

      if (captionBaseUrl.isNotBlank()) {
        val transcriptReq = Request.Builder()
          .url(captionBaseUrl)
          .header("User-Agent", USER_AGENT_DESKTOP)
          .get()
          .build()

        val xmlData = runCatching {
          httpClient.newCall(transcriptReq).execute().use { it.body.string() }
        }.getOrDefault("")

        if (xmlData.isNotBlank()) {
          val segments = parseCaptionXml(xmlData)
          if (segments.isNotEmpty()) {
            return@withContext segments
          }
        }
      }
    } catch (_: Exception) {
      // Transcript unavailable
    }

    emptyList()
  }

  internal fun extractCaptionBaseUrl(playerResponseJson: String): String {
    if (playerResponseJson.isBlank()) return ""
    return runCatching {
      val root = json.parseToJsonElement(playerResponseJson).jsonObject
      val captionsObj = root["captions"]?.jsonObject
      val tracklist = captionsObj?.get("playerCaptionsTracklistRenderer")?.jsonObject
      val tracks = tracklist?.get("captionTracks")?.jsonArray.orEmpty()
      if (tracks.isEmpty()) return@runCatching ""

      // Prefer device locale or English, otherwise first available
      val deviceLang = Locale.getDefault().language.lowercase()
      val selectedTrack = tracks.firstOrNull {
        val code = it.jsonObject["languageCode"]?.jsonPrimitive?.content?.lowercase().orEmpty()
        code == deviceLang || code.startsWith(deviceLang)
      } ?: tracks.firstOrNull {
        val code = it.jsonObject["languageCode"]?.jsonPrimitive?.content?.lowercase().orEmpty()
        code == "en" || code.startsWith("en")
      } ?: tracks.first()

      selectedTrack.jsonObject["baseUrl"]?.jsonPrimitive?.content.orEmpty()
    }.getOrDefault("")
  }

  internal fun extractCaptionBaseUrlFromHtml(html: String): String {
    if (html.isBlank()) return ""
    return runCatching {
      val match = Regex(""""captionTracks":\s*(\[.*?\])""").find(html) ?: return@runCatching ""
      val tracksArray = json.parseToJsonElement(match.groupValues[1]).jsonArray
      if (tracksArray.isEmpty()) return@runCatching ""

      val deviceLang = Locale.getDefault().language.lowercase()
      val selectedTrack = tracksArray.firstOrNull {
        val code = it.jsonObject["languageCode"]?.jsonPrimitive?.content?.lowercase().orEmpty()
        code == deviceLang || code.startsWith(deviceLang)
      } ?: tracksArray.firstOrNull {
        val code = it.jsonObject["languageCode"]?.jsonPrimitive?.content?.lowercase().orEmpty()
        code == "en" || code.startsWith("en")
      } ?: tracksArray.first()

      selectedTrack.jsonObject["baseUrl"]?.jsonPrimitive?.content.orEmpty()
    }.getOrDefault("")
  }

  internal fun parseCaptionXml(xml: String): List<YouTubeCaptionSegment> {
    if (xml.isBlank()) return emptyList()
    val segments = mutableListOf<YouTubeCaptionSegment>()

    // Match either <p ...>content</p> or <text ...>content</text>
    val tagPattern = Pattern.compile("""<(p|text)\s+([^>]+)>(.*?)</\1>""", Pattern.DOTALL)
    val matcher = tagPattern.matcher(xml)

    val startPattern = Pattern.compile("""\b(?:t|start)="([^"]+)"""")
    val durPattern = Pattern.compile("""\b(?:d|dur)="([^"]+)"""")

    while (matcher.find()) {
      val tag = matcher.group(1).orEmpty()
      val attrs = matcher.group(2).orEmpty()
      val rawContent = matcher.group(3).orEmpty()

      val startMatcher = startPattern.matcher(attrs)
      val durMatcher = durPattern.matcher(attrs)

      val rawStart = if (startMatcher.find()) startMatcher.group(1) else null
      val rawDur = if (durMatcher.find()) durMatcher.group(1) else null

      val startMs = if (tag == "p") {
        rawStart?.toLongOrNull() ?: 0L
      } else {
        ((rawStart?.toDoubleOrNull() ?: 0.0) * 1000).toLong()
      }

      val durMs = if (tag == "p") {
        rawDur?.toLongOrNull() ?: 0L
      } else {
        ((rawDur?.toDoubleOrNull() ?: 0.0) * 1000).toLong()
      }

      val cleanText = cleanSubtitleText(rawContent)
      if (cleanText.isNotBlank()) {
        segments.add(YouTubeCaptionSegment(startMs, durMs, cleanText))
      }
    }

    return segments
  }

  internal fun cleanSubtitleText(raw: String): String {
    // 1. Strip inner xml/html tags (e.g. <s>, </s>, <font ...>)
    var text = raw.replace(Regex("""<[^>]+>"""), "")

    // 2. Unescape common HTML entities
    text = text
      .replace("&#39;", "'")
      .replace("&quot;", "\"")
      .replace("&amp;", "&")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&nbsp;", " ")
      .replace("&#34;", "\"")
      .replace("&#38;", "&")
      .replace("&#60;", "<")
      .replace("&#62;", ">")

    // Handle any remaining &#NNN; entities
    text = Regex("""&#(\d+);""").replace(text) { match ->
      val code = match.groupValues[1].toIntOrNull()
      if (code != null) code.toChar().toString() else match.value
    }

    // 3. Normalize whitespace
    return text.replace(Regex("""\s+"""), " ").trim()
  }

  fun formatTimestamp(ms: Long): String {
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    val hours = minutes / 60
    return if (hours > 0) {
      val remMinutes = minutes % 60
      "%d:%02d:%02d".format(hours, remMinutes, seconds)
    } else {
      "%02d:%02d".format(minutes, seconds)
    }
  }

  fun formatTranscriptForReading(
    segments: List<YouTubeCaptionSegment>,
    targetParagraphDurationMs: Long = 40_000L
  ): String {
    if (segments.isEmpty()) return ""

    val paragraphs = mutableListOf<String>()
    var currentGroup = mutableListOf<String>()
    var groupStartMs = segments.first().startMs

    for (segment in segments) {
      val timeSinceGroupStart = segment.startMs - groupStartMs
      if (currentGroup.isNotEmpty() && timeSinceGroupStart >= targetParagraphDurationMs) {
        val timeLabel = formatTimestamp(groupStartMs)
        val body = currentGroup.joinToString(" ")
        paragraphs.add("[$timeLabel] $body")
        currentGroup = mutableListOf()
        groupStartMs = segment.startMs
      }
      currentGroup.add(segment.text)
    }

    if (currentGroup.isNotEmpty()) {
      val timeLabel = formatTimestamp(groupStartMs)
      val body = currentGroup.joinToString(" ")
      paragraphs.add("[$timeLabel] $body")
    }

    return paragraphs.joinToString("\n\n")
  }

  suspend fun scrapeYouTube(url: String): ScrapedPageData? = withContext(Dispatchers.IO) {
    val videoId = extractVideoId(url) ?: return@withContext null
    val metadata = fetchMetadata(videoId)
    val segments = fetchTranscript(videoId)

    val readableText = if (segments.isNotEmpty()) {
      val formattedTranscript = formatTranscriptForReading(segments)
      buildString {
        appendLine("Video Title: ${metadata.title}")
        appendLine("Channel: ${metadata.channelTitle}")
        appendLine("URL: ${defaultWatchUrl(videoId)}")
        appendLine()
        appendLine("Full Transcript:")
        append(formattedTranscript.take(24_000))
      }.trim()
    } else {
      buildString {
        appendLine("Video Title: ${metadata.title}")
        appendLine("Channel: ${metadata.channelTitle}")
        appendLine("URL: ${defaultWatchUrl(videoId)}")
        appendLine()
        appendLine("(Closed captions or spoken transcript are not available for this video.)")
      }.trim()
    }

    val displayDomain = if (metadata.channelTitle.isNotBlank() && metadata.channelTitle != "YouTube") {
      "youtube.com • ${metadata.channelTitle}"
    } else {
      "youtube.com"
    }

    ScrapedPageData(
      title = metadata.title,
      faviconUrl = "https://www.google.com/s2/favicons?domain=youtube.com&sz=128",
      thumbnailUrl = metadata.thumbnailUrl,
      domain = displayDomain,
      readableContent = readableText
    )
  }
}
