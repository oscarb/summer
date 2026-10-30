package se.oscarb.summer.data

import android.content.Context
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class AiSummaryService(private val appContext: Context) {

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

  private val json = Json { ignoreUnknownKeys = true }

  suspend fun summarizeSingleWebsite(
    website: SavedWebsite,
    settings: AppSettings
  ): Result<String> = withContext(Dispatchers.IO) {
    val contentSnippet = website.extractedContent.ifBlank {
      "(Article body could not be fully extracted; summarize based on the title and URL context.)"
    }.take(12_000)

    val prompt = """
      You are a concise, insightful reading assistant inside the "Summer" Android app.
      Summarize the following saved web page so the user can quickly catch up on what it's about and its key takeaways.
      
      Format your response in clean Markdown:
      - Start with `**TL;DR:**` followed by a crisp 1-2 sentence overview.
      - Follow with 3-5 Markdown bullet points (`- **Lead takeaway**: ...`) covering the most important facts, arguments, or insights.
      - Keep it scannable, accurate, and free of filler phrases.
      
      Title: ${website.title}
      URL: ${website.url}
      Domain: ${website.domain}
      
      Extracted Page Content:
      $contentSnippet
    """.trimIndent()

    generateWithSelectedBackend(prompt, settings)
  }

  /**
   * Generates a cohesive 2-host or 3-host "Catch Me Up" podcast episode script that:
   * 1. Opens with a warm welcome, today's date, and host name introductions.
   * 2. Weaves a narrative "red line" (connecting thread) across all saved articles.
   * 3. Translates and discusses everything in idiomatic Swedish when [language] is [PodcastLanguage.SWEDISH].
   */
  suspend fun generateCatchMeUpPodcastScript(
    websites: List<SavedWebsite>,
    settings: AppSettings,
    hostNames: List<String> = listOf(settings.hostAName, settings.hostBName),
    language: PodcastLanguage = settings.podcastLanguage,
    nowMillis: Long = System.currentTimeMillis()
  ): Result<String> = withContext(Dispatchers.IO) {
    if (websites.isEmpty()) {
      return@withContext Result.failure(IllegalArgumentException("No articles available for podcast script."))
    }

    val cleanHosts = hostNames.map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty {
      if (language == PodcastLanguage.SWEDISH) listOf("Astrid", "Erik") else listOf("Alex", "Sam")
    }
    val hostA = cleanHosts[0]
    val hostB = cleanHosts.getOrElse(1) { if (language == PodcastLanguage.SWEDISH) "Erik" else "Sam" }
    val hostC = cleanHosts.getOrNull(2)

    val localDate = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val formattedDate = when (language) {
      PodcastLanguage.SWEDISH -> DateTimeFormatter.ofPattern("EEEE 'den' d MMMM yyyy", Locale.forLanguageTag("sv-SE"))
        .format(localDate)
      PodcastLanguage.ENGLISH -> DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US)
        .format(localDate)
    }

    val itemsBlock = websites.mapIndexed { index, site ->
      val body = site.summary?.let { "Key Summary:\n$it" }
        ?: site.extractedContent.take(3_200).ifBlank { "(Title/URL only)" }
      """
      ARTICLE ${index + 1}: "${site.title}" (from ${site.domain})
      $body
      """.trimIndent()
    }.joinToString("\n\n")

    val hostRolesBlock = if (hostC != null) {
      """
      HOSTS (3-HOST PANEL):
      - $hostA: The warm, curious lead host who opens the show, drives the narrative arc, and connects the stories.
      - $hostB: The sharp analyst who unpacks the deepest facts, technical/cultural details, and core takeaways.
      - $hostC: The lively, pragmatic third co-host who adds fresh perspectives, witty reactions, and real-world implications.
      - Keep all three hosts actively involved across the episode with natural back-and-forth dynamics.
      """.trimIndent()
    } else {
      """
      HOSTS (2-HOST CONVERSATION):
      - $hostA: The warm, curious lead host who opens the show, drives the narrative arc, and connects the dots.
      - $hostB: The sharp, insightful co-host who brings out the most interesting facts, nuances, and "why it matters" takeaways.
      """.trimIndent()
    }

    val introRequirement = when (language) {
      PodcastLanguage.SWEDISH -> {
        val introExample = if (hostC != null) {
          "\"Välkomna till ett nytt avsnitt av Summer! Idag är det $formattedDate. Jag heter $hostA, och med mig har jag $hostB och $hostC...\""
        } else {
          "\"Välkomna till ett nytt avsnitt av Summer! Idag är det $formattedDate. Jag heter $hostA, och med mig har jag $hostB...\""
        }
        """
        MANDATORY OPENING PRESENTATION:
        - Start Turn 1 with a warm, natural welcome to another episode of "Summer", state today's date ($formattedDate), and have the hosts introduce themselves by name (for example: $introExample), with the co-host(s) greeting briefly before teasing today's overarching theme.
        """.trimIndent()
      }
      PodcastLanguage.ENGLISH -> {
        val introExample = if (hostC != null) {
          "\"Welcome back to another episode of Summer! Today is $formattedDate — I'm $hostA, and I'm joined today by $hostB and $hostC...\""
        } else {
          "\"Welcome back to another episode of Summer! Today is $formattedDate — I'm $hostA, and with me is $hostB...\""
        }
        """
        MANDATORY OPENING PRESENTATION:
        - Start Turn 1 with a warm, natural welcome to another episode of "Summer", state today's date ($formattedDate), and have the hosts introduce themselves by name (for example: $introExample), with the co-host(s) greeting briefly before teasing today's overarching theme.
        """.trimIndent()
      }
    }

    val languageInstruction = when (language) {
      PodcastLanguage.SWEDISH -> """
        CRITICAL LANGUAGE REQUIREMENT — SWEDISH (SVENSKA):
        - Write the ENTIRE podcast dialogue in natural, idiomatic, spoken SWEDISH (svenska).
        - Even when the source articles are in English (or any other language), translate and discuss all concepts, quotes, titles, and takeaways seamlessly in natural everyday Swedish podcast prose.
        - Avoid stiff word-for-word English translations; sound like native Swedish tech & culture podcast hosts.
      """.trimIndent()
      PodcastLanguage.ENGLISH -> """
        LANGUAGE:
        - Write the entire conversation in natural, engaging spoken English.
      """.trimIndent()
    }

    val speakerFormatLines = buildString {
      appendLine("  $hostA: <spoken words>")
      appendLine("  $hostB: <spoken words>")
      if (hostC != null) {
        appendLine("  $hostC: <spoken words>")
      }
    }.trimEnd()

    val targetTurns = if (hostC != null) "15 to 26" else "12 to 22"

    val prompt = """
      You are the showrunner and scriptwriter for the "Summer" daily catch-up podcast.
      Write an engaging, natural-sounding podcast episode between ${PodcastCastCatalog.formatHostList(cleanHosts, language)} covering the user's ${websites.size} saved article(s) below.
      
      $languageInstruction
      
      $hostRolesBlock
      
      $introRequirement
      
      CRITICAL GOAL — THE "RED LINE" ACROSS ARTICLES:
      - Do NOT treat the articles as disconnected clips or a mechanical checklist.
      - Find the overarching narrative thread ("red line"), shared themes, tensions, or surprising contrasts across these stories.
      - Every time you move from one article to the next, weave a natural conversational bridge so the entire episode flows like one unified discussion.
      - Write strictly for spoken audio: conversational rhythm, natural reactions, and clear pronunciation.
      - Format EVERY turn strictly on its own line starting with the speaker's exact name and a colon:
      $speakerFormatLines
      - Do NOT use markdown bold, bullet points, bracketed citations like [1], URLs, or stage directions like [music] or [laughs].
      - Aim for $targetTurns conversational turns total so every saved article is covered meaningfully at a brisk, lively pace.
      
      Articles to weave together:
      $itemsBlock
    """.trimIndent()

    generateWithSelectedBackend(prompt, settings)
  }

  /**
   * Probes the currently selected [AiBackend] with a tiny prompt (and lists enabled Gemini 3.x
   * models when using Direct Gemini API) so the user can verify access in Settings.
   */
  suspend fun verifyTextBackendAccess(settings: AppSettings): Result<String> = withContext(Dispatchers.IO) {
    val activeModel = when (settings.selectedBackend) {
      AiBackend.FIREBASE_AI -> settings.firebaseModel.trim().ifBlank { "gemini-3.8-flash" }
      AiBackend.DIRECT_GEMINI -> settings.directGeminiModel.trim().ifBlank { "gemini-3.8-flash" }
      AiBackend.GEMINI_NANO_ON_DEVICE -> "Gemini Nano (AICore)"
      AiBackend.OPENAI_COMPATIBLE -> settings.openAiModel.trim().ifBlank { "gpt-4o-mini" }
    }

    val startMs = System.currentTimeMillis()
    val probeResult = generateWithSelectedBackend(
      prompt = "Reply with the single word OK.",
      settings = settings,
      allowCapacityFallback = false
    )
    val elapsedMs = System.currentTimeMillis() - startMs

    probeResult.map { reply ->
      val extraModelsNote = if (settings.selectedBackend == AiBackend.DIRECT_GEMINI) {
        val available3x = listDirectGemini3Models(settings.directGeminiApiKey.trim())
        if (available3x.isNotEmpty()) {
          "\nGemini 3.x models on your key: ${available3x.joinToString(", ")}"
        } else {
          ""
        }
      } else {
        ""
      }
      "Verified ${settings.selectedBackend.displayName} using $activeModel (${elapsedMs}ms) — response: \"${reply.take(40)}\"$extraModelsNote"
    }
  }

  private fun listDirectGemini3Models(apiKey: String): List<String> {
    if (apiKey.isBlank()) return emptyList()
    return runCatching {
      val req = Request.Builder()
        .url("https://generativelanguage.googleapis.com/v1beta/models")
        .header("x-goog-api-key", apiKey)
        .get()
        .build()
      httpClient.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) return emptyList()
        val root = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
        root["models"]?.jsonArray.orEmpty()
          .mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content?.removePrefix("models/") }
          .filter { it.startsWith("gemini-3") && !it.contains("tts") && !it.contains("image") }
      }
    }.getOrDefault(emptyList())
  }

  internal suspend fun generateRawPromptForBenchmark(
    prompt: String,
    settings: AppSettings,
    allowCapacityFallback: Boolean = false
  ): Result<String> = withContext(Dispatchers.IO) {
    generateWithSelectedBackend(
      prompt = prompt,
      settings = settings,
      allowCapacityFallback = allowCapacityFallback
    )
  }

  private suspend fun generateWithSelectedBackend(
    prompt: String,
    settings: AppSettings,
    allowCapacityFallback: Boolean = true
  ): Result<String> {
    return try {
      val text = when (settings.selectedBackend) {
        AiBackend.FIREBASE_AI -> callFirebaseAiLogic(
          prompt = prompt,
          modelName = settings.firebaseModel,
          allowCapacityFallback = allowCapacityFallback
        )
        AiBackend.DIRECT_GEMINI -> callDirectGeminiApi(
          prompt = prompt,
          apiKey = settings.directGeminiApiKey.trim(),
          model = settings.directGeminiModel.trim().ifBlank { "gemini-3.8-flash" },
          allowCapacityFallback = allowCapacityFallback
        )
        AiBackend.GEMINI_NANO_ON_DEVICE -> callOnDeviceGeminiNano(prompt)
        AiBackend.OPENAI_COMPATIBLE -> callOpenAiCompatibleApi(
          prompt = prompt,
          baseUrl = settings.openAiBaseUrl.trim().trimEnd('/'),
          apiKey = settings.openAiApiKey.trim(),
          model = settings.openAiModel.trim().ifBlank { "gpt-4o-mini" }
        )
      }
      if (text.isBlank()) {
        Result.failure(IllegalStateException("AI provider returned an empty summary."))
      } else {
        Result.success(text.trim())
      }
    } catch (ce: CancellationException) {
      throw ce
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private suspend fun callFirebaseAiLogic(
    prompt: String,
    modelName: String,
    allowCapacityFallback: Boolean = true
  ): String {
    if (com.google.firebase.FirebaseApp.getApps(appContext).isEmpty()) {
      com.google.firebase.FirebaseApp.initializeApp(appContext)
    }
    val cleanModel = modelName.trim().ifBlank { "gemini-3.8-flash" }
    return try {
      val model = Firebase.ai.generativeModel(modelName = cleanModel)
      val response = model.generateContent(prompt)
      response.text ?: throw IllegalStateException("Empty response from Firebase AI Logic ($cleanModel)")
    } catch (ce: CancellationException) {
      throw ce
    } catch (e: Exception) {
      if (allowCapacityFallback && isTransientCapacityOrRateLimitError(e) && cleanModel != "gemini-3.5-flash") {
        val fallbackModel = Firebase.ai.generativeModel(modelName = "gemini-3.5-flash")
        val response = fallbackModel.generateContent(prompt)
        response.text ?: throw e
      } else {
        throw e
      }
    }
  }

  private fun callDirectGeminiApi(
    prompt: String,
    apiKey: String,
    model: String,
    allowCapacityFallback: Boolean = true
  ): String {
    if (apiKey.isBlank()) {
      throw IllegalStateException("Direct Gemini API key is missing. Add it in Settings or switch to Firebase AI Logic.")
    }
    return try {
      executeDirectGeminiHttpCall(prompt = prompt, apiKey = apiKey, model = model)
    } catch (e: Exception) {
      if (allowCapacityFallback && isTransientCapacityOrRateLimitError(e) && model != "gemini-3.5-flash") {
        executeDirectGeminiHttpCall(prompt = prompt, apiKey = apiKey, model = "gemini-3.5-flash")
      } else {
        throw e
      }
    }
  }

  private fun isTransientCapacityOrRateLimitError(e: Throwable): Boolean {
    if (e is java.net.SocketTimeoutException || e.cause is java.net.SocketTimeoutException) {
      return true
    }
    val combined = "${e.message.orEmpty()} ${e.cause?.message.orEmpty()}".lowercase()
    return combined.contains("503") ||
      combined.contains("429") ||
      combined.contains("unavailable") ||
      combined.contains("resource_exhausted") ||
      combined.contains("deadline_exceeded") ||
      combined.contains("high demand") ||
      combined.contains("overloaded") ||
      combined.contains("timed out")
  }

  private fun executeDirectGeminiHttpCall(prompt: String, apiKey: String, model: String): String {
    val endpoint =
      "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

    val requestPayload = buildJsonObject {
      put("contents", buildJsonArray {
        add(buildJsonObject {
          put("parts", buildJsonArray {
            add(buildJsonObject {
              put("text", JsonPrimitive(prompt))
            })
          })
        })
      })
    }.toString()

    val request = Request.Builder()
      .url(endpoint)
      .header("x-goog-api-key", apiKey)
      .post(requestPayload.toRequestBody("application/json; charset=utf-8".toMediaType()))
      .build()

    httpClient.newCall(request).execute().use { response ->
      val bodyStr = response.body?.string().orEmpty()
      if (!response.isSuccessful) {
        throw IllegalStateException("Gemini API HTTP ${response.code}: ${bodyStr.take(300)}")
      }
      val root = json.parseToJsonElement(bodyStr).jsonObject
      val candidates = root["candidates"]?.jsonArray
      val firstCandidate = candidates?.firstOrNull()?.jsonObject
      val parts = firstCandidate?.get("content")?.jsonObject?.get("parts")?.jsonArray
      return parts?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }
        ?.joinToString("\n")
        ?: throw IllegalStateException("Could not parse Gemini response.")
    }
  }

  private suspend fun callOnDeviceGeminiNano(prompt: String): String {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
      throw IllegalStateException("On-Device Gemini Nano requires Android 12 (API 31) or higher.")
    }
    return try {
      val config = generationConfig {
        context = appContext
        temperature = 0.4f
        topK = 40
        maxOutputTokens = 512
      }
      val localModel = GenerativeModel(generationConfig = config)
      val response = localModel.generateContent(prompt.take(6_000))
      response.text ?: throw IllegalStateException("Gemini Nano returned empty output.")
    } catch (ce: CancellationException) {
      throw ce
    } catch (e: Exception) {
      throw IllegalStateException(
        "On-Device Gemini Nano (AICore) error: ${e.message ?: "Requires supported Pixel/flagship device with AICore enabled"}. " +
          "You can switch to Firebase AI Logic or Direct Gemini API in Settings.",
        e
      )
    }
  }

  private fun callOpenAiCompatibleApi(
    prompt: String,
    baseUrl: String,
    apiKey: String,
    model: String
  ): String {
    if (apiKey.isBlank()) {
      throw IllegalStateException("API key is missing for OpenAI / Custom API. Please enter your API key in Settings.")
    }
    val isAnthropicNative = baseUrl.contains("anthropic.com") && !baseUrl.endsWith("/v1")
    val endpoint = when {
      isAnthropicNative -> "$baseUrl/v1/messages"
      baseUrl.endsWith("/chat/completions") -> baseUrl
      else -> "$baseUrl/chat/completions"
    }

    val payload = buildJsonObject {
      put("model", JsonPrimitive(model))
      if (isAnthropicNative) {
        put("max_tokens", JsonPrimitive(1024))
      }
      put("messages", buildJsonArray {
        add(buildJsonObject {
          put("role", JsonPrimitive("user"))
          put("content", JsonPrimitive(prompt))
        })
      })
    }.toString()

    val requestBuilder = Request.Builder()
      .url(endpoint)
      .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
      .header("Content-Type", "application/json")

    if (isAnthropicNative) {
      requestBuilder.header("x-api-key", apiKey)
      requestBuilder.header("anthropic-version", "2023-06-01")
    } else {
      requestBuilder.header("Authorization", "Bearer $apiKey")
    }

    httpClient.newCall(requestBuilder.build()).execute().use { response ->
      val bodyStr = response.body?.string().orEmpty()
      if (!response.isSuccessful) {
        throw IllegalStateException("API HTTP ${response.code}: ${bodyStr.take(300)}")
      }
      val root = json.parseToJsonElement(bodyStr).jsonObject
      val choices = root["choices"] as? JsonArray
      if (choices != null && choices.isNotEmpty()) {
        val message = choices[0].jsonObject["message"] as? JsonObject
        val content = message?.get("content")?.jsonPrimitive?.content
        if (!content.isNullOrBlank()) return content
      }
      val contentBlocks = root["content"] as? JsonArray
      if (contentBlocks != null && contentBlocks.isNotEmpty()) {
        val text = contentBlocks[0].jsonObject["text"]?.jsonPrimitive?.content
        if (!text.isNullOrBlank()) return text
      }
      throw IllegalStateException("Could not parse completion response.")
    }
  }
}
