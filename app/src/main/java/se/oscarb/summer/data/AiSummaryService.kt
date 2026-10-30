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
      
      Format your response using clean bullet points:
      • Start with a 1-2 sentence TL;DR overview.
      • Follow with 3-5 key bullet points covering the most important facts, arguments, or insights.
      • Keep it scannable, accurate, and free of filler phrases.
      
      Title: ${website.title}
      URL: ${website.url}
      Domain: ${website.domain}
      
      Extracted Page Content:
      $contentSnippet
    """.trimIndent()

    generateWithSelectedBackend(prompt, settings)
  }

  suspend fun generateCatchMeUpDigest(
    websites: List<SavedWebsite>,
    settings: AppSettings
  ): Result<String> = withContext(Dispatchers.IO) {
    if (websites.isEmpty()) {
      return@withContext Result.failure(IllegalArgumentException("No unread websites to summarize."))
    }

    val itemsBlock = websites.mapIndexed { index, site ->
      val body = site.summary?.let { "Existing Summary:\n$it" }
        ?: site.extractedContent.take(3_500).ifBlank { "(Title/URL only)" }
      """
      ---
      ARTICLE [${index + 1}]
      Title: ${site.title}
      Domain: ${site.domain}
      URL: ${site.url}
      Content:
      $body
      """.trimIndent()
    }.joinToString("\n\n")

    val prompt = """
      You are the "Catch Me Up" daily briefing assistant inside the "Summer" Android app.
      The user has saved ${websites.size} website(s) from their feed to catch up on.
      
      Create a cohesive, easy-to-scan briefing across all ${websites.size} saved articles:
      1. Start with a brief "Big Picture" 1-2 sentence synthesis of the themes across their saved links.
      2. For each article (or logical theme group), provide a clear heading with its [Article #] and Title, followed by 2-3 punchy bullet points with the core takeaway.
      3. Reference articles using their index like [1], [2], etc. so the user can match them with the source cards below.
      
      Saved Articles:
      $itemsBlock
    """.trimIndent()

    generateWithSelectedBackend(prompt, settings)
  }

  /**
   * Generates a natural, spoken-word two-host mini-podcast script for a single saved article,
   * designed specifically for listening rather than reading bullet points.
   */
  suspend fun generateSingleArticlePodcastScript(
    website: SavedWebsite,
    settings: AppSettings
  ): Result<String> = withContext(Dispatchers.IO) {
    val hostA = settings.hostAName.trim().ifBlank { "Alex" }
    val hostB = settings.hostBName.trim().ifBlank { "Sam" }
    val contentSnippet = website.extractedContent.ifBlank {
      website.summary ?: "(Article body could not be fully extracted; discuss based on title and domain.)"
    }.take(10_000)

    val prompt = """
      You are writing a short, engaging 2-host audio summary script for the "Summer" podcast app.
      Instead of bullet points or written-style text, turn this article into a natural, lively 60-to-90 second conversation between two hosts: $hostA and $hostB.
      
      Guidelines for natural spoken audio:
      - $hostA is the curious lead host who introduces the story and asks sharp questions.
      - $hostB is the insightful analyst who unpacks the core takeaways, surprising details, and why it matters.
      - Write ONLY for the ear: use natural phrasing, contractions, and conversational flow. Never use bullet points, markdown formatting, URLs, or robotic transitions.
      - Format EVERY line strictly as "$hostA: <spoken words>" or "$hostB: <spoken words>" on its own line (6 to 10 turns total).
      - Do NOT include stage directions like [laughs] or (intro music).
      
      Article Title: ${website.title}
      Source Domain: ${website.domain}
      Existing Written Summary: ${website.summary ?: "None"}
      Article Content:
      $contentSnippet
    """.trimIndent()

    generateWithSelectedBackend(prompt, settings)
  }

  /**
   * Generates a cohesive 2-host "Catch Me Up" podcast episode script that weaves a narrative
   * "red line" (connecting thread) across all the articles in a digest.
   */
  suspend fun generateCatchMeUpPodcastScript(
    websites: List<SavedWebsite>,
    existingDigestMarkdown: String? = null,
    settings: AppSettings
  ): Result<String> = withContext(Dispatchers.IO) {
    if (websites.isEmpty() && existingDigestMarkdown.isNullOrBlank()) {
      return@withContext Result.failure(IllegalArgumentException("No articles available for podcast script."))
    }

    val hostA = settings.hostAName.trim().ifBlank { "Alex" }
    val hostB = settings.hostBName.trim().ifBlank { "Sam" }

    val itemsBlock = websites.mapIndexed { index, site ->
      val body = site.summary?.let { "Key Summary:\n$it" }
        ?: site.extractedContent.take(3_000).ifBlank { "(Title/URL only)" }
      """
      ARTICLE ${index + 1}: "${site.title}" (from ${site.domain})
      $body
      """.trimIndent()
    }.joinToString("\n\n")

    val digestContext = if (!existingDigestMarkdown.isNullOrBlank()) {
      "\nBriefing Overview:\n$existingDigestMarkdown\n"
    } else {
      ""
    }

    val prompt = """
      You are the showrunner and scriptwriter for the "Summer Catch Me Up" daily podcast.
      Write an engaging, natural-sounding two-host podcast episode between **$hostA** and **$hostB** covering the user's saved articles below.
      
      CRITICAL GOAL — THE "RED LINE" ACROSS ARTICLES:
      - Do NOT treat the articles as disconnected clips or a checklist.
      - Find the overarching narrative thread ("red line"), shared themes, tensions, or surprising contrasts across these stories.
      - Open with a warm, hooky 2-turn intro that highlights the big-picture thread connecting today's stories.
      - Every time you move from one article to the next, weave a natural conversational bridge (e.g., how one story's trend echoes, contrasts with, or sets up the next story) so the entire episode flows like a single unified conversation.
      - $hostA is the curious, engaging host who drives the narrative arc and connects the dots.
      - $hostB is the sharp, insightful co-host who brings out the most interesting facts, nuances, and "why it matters" takeaways.
      - Write strictly for spoken audio: conversational rhythm, contractions, and natural reactions ("Right, and here's the wild part...", "Wait, seriously?").
      - Format EVERY turn strictly as:
        $hostA: <spoken words>
        $hostB: <spoken words>
      - Do NOT use markdown bold, bullet points, bracketed citations like [1], or stage directions like [music] or [laughs].
      - Aim for 12 to 22 conversational turns total so it covers every article meaningfully while keeping a brisk, engaging pace.
      $digestContext
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
      // On-device Gemini Nano has a smaller context window; trim prompt safely
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
      // OpenAI format
      val choices = root["choices"] as? JsonArray
      if (choices != null && choices.isNotEmpty()) {
        val message = choices[0].jsonObject["message"] as? JsonObject
        val content = message?.get("content")?.jsonPrimitive?.content
        if (!content.isNullOrBlank()) return content
      }
      // Anthropic native format fallback
      val contentBlocks = root["content"] as? JsonArray
      if (contentBlocks != null && contentBlocks.isNotEmpty()) {
        val text = contentBlocks[0].jsonObject["text"]?.jsonPrimitive?.content
        if (!text.isNullOrBlank()) return text
      }
      throw IllegalStateException("Could not parse completion response.")
    }
  }
}
