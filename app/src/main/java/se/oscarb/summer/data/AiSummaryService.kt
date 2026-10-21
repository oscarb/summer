package se.oscarb.summer.data

import android.content.Context
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
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

  private suspend fun generateWithSelectedBackend(
    prompt: String,
    settings: AppSettings
  ): Result<String> {
    return try {
      val text = when (settings.selectedBackend) {
        AiBackend.FIREBASE_AI -> callFirebaseAiLogic(prompt, settings.firebaseModel)
        AiBackend.DIRECT_GEMINI -> callDirectGeminiApi(
          prompt = prompt,
          apiKey = settings.directGeminiApiKey.trim(),
          model = settings.directGeminiModel.trim().ifBlank { "gemini-2.5-flash" }
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
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private suspend fun callFirebaseAiLogic(prompt: String, modelName: String): String {
    val cleanModel = modelName.trim().ifBlank { "gemini-2.5-flash" }
    val model = Firebase.ai.generativeModel(modelName = cleanModel)
    val response = model.generateContent(prompt)
    return response.text ?: throw IllegalStateException("Empty response from Firebase AI Logic ($cleanModel)")
  }

  private fun callDirectGeminiApi(prompt: String, apiKey: String, model: String): String {
    if (apiKey.isBlank()) {
      throw IllegalStateException("Direct Gemini API key is missing. Add it in Settings or switch to Firebase AI Logic.")
    }
    val endpoint =
      "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

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
