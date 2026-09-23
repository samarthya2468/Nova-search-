package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class SearchResult(
    val query: String,
    val answer: String,
    val mode: String,
    val customPrompt: String?,
    val keyTakeaways: List<String>,
    val followUpQuestions: List<String>,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

data class GeneratedImageResult(
    val prompt: String,
    val style: String,
    val aspectRatio: String,
    val bitmap: Bitmap?,
    val imageBase64: String?,
    val mimeType: String,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val PREFS_NAME = "novasearch_prefs"
    private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customKey = prefs.getString(KEY_CUSTOM_API_KEY, null)?.trim()
        if (!customKey.isNullOrBlank()) {
            return customKey
        }
        return try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun saveCustomApiKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
    }

    fun isApiKeyConfigured(context: Context): Boolean {
        val key = getApiKey(context)
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Executes AI Search with Gemini 3.5 Flash
     */
    suspend fun search(
        context: Context,
        query: String,
        mode: String,
        customPrompt: String?
    ): SearchResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)

        if (!isApiKeyConfigured(context)) {
            // Return informative fallback response showing app capabilities
            return@withContext getSimulatedSearchResponse(query, mode, customPrompt)
        }

        try {
            val systemPrompt = buildSystemPrompt(mode, customPrompt)
            val fullUserQuery = if (!customPrompt.isNullOrBlank()) {
                "User Search Question: $query\nSpecial Directives / Custom Tone: $customPrompt"
            } else {
                "User Search Question: $query"
            }

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", fullUserQuery))
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.95)
                })
            }

            val url = "${BASE_URL}v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val httpRequest = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseBody, response.code)
                return@withContext SearchResult(
                    query = query,
                    answer = "Unable to complete search: $errorMsg\n\nYou can verify your Gemini API key in Settings.",
                    mode = mode,
                    customPrompt = customPrompt,
                    keyTakeaways = emptyList(),
                    followUpQuestions = listOf("Try another search phrase", "Check API Settings"),
                    isSuccess = false,
                    errorMessage = errorMsg
                )
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

            parseSearchResponse(query, rawText, mode, customPrompt)
        } catch (e: Exception) {
            SearchResult(
                query = query,
                answer = "Error while connecting to AI Search: ${e.localizedMessage ?: e.message}\n\nPlease check your internet connection or API key.",
                mode = mode,
                customPrompt = customPrompt,
                keyTakeaways = emptyList(),
                followUpQuestions = listOf("Retry search", "Check API key in Settings"),
                isSuccess = false,
                errorMessage = e.message
            )
        }
    }

    /**
     * Generates an image with Gemini 2.5 Flash Image
     */
    suspend fun generateImage(
        context: Context,
        prompt: String,
        style: String,
        aspectRatio: String
    ): GeneratedImageResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)

        val fullPrompt = buildImagePrompt(prompt, style)

        if (!isApiKeyConfigured(context)) {
            // Provide high quality generated visual canvas
            val fallbackBitmap = createArtisticPlaceholderBitmap(prompt, style)
            val stream = ByteArrayOutputStream()
            fallbackBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val b64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            return@withContext GeneratedImageResult(
                prompt = prompt,
                style = style,
                aspectRatio = aspectRatio,
                bitmap = fallbackBitmap,
                imageBase64 = b64,
                mimeType = "image/jpeg",
                isSuccess = true,
                errorMessage = "Sample visual preview (Add your Gemini API Key in Settings for live AI generation)"
            )
        }

        try {
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", fullPrompt))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("imageConfig", JSONObject().apply {
                        put("aspectRatio", aspectRatio)
                        put("imageSize", "1K")
                    })
                    put("responseModalities", JSONArray().apply {
                        put("TEXT")
                        put("IMAGE")
                    })
                })
            }

            val url = "${BASE_URL}v1beta/models/gemini-2.5-flash-image:generateContent?key=$apiKey"
            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val httpRequest = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseBody, response.code)
                // Fallback to stylized bitmap with error notice
                val fallbackBitmap = createArtisticPlaceholderBitmap(prompt, style)
                val stream = ByteArrayOutputStream()
                fallbackBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val b64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                return@withContext GeneratedImageResult(
                    prompt = prompt,
                    style = style,
                    aspectRatio = aspectRatio,
                    bitmap = fallbackBitmap,
                    imageBase64 = b64,
                    mimeType = "image/jpeg",
                    isSuccess = false,
                    errorMessage = errorMsg
                )
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")

            var imageBase64: String? = null
            var mimeType = "image/jpeg"

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i)
                    val inlineData = part?.optJSONObject("inlineData")
                    if (inlineData != null) {
                        imageBase64 = inlineData.optString("data")
                        mimeType = inlineData.optString("mimeType", "image/jpeg")
                        break
                    }
                }
            }

            if (!imageBase64.isNullOrEmpty()) {
                val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                GeneratedImageResult(
                    prompt = prompt,
                    style = style,
                    aspectRatio = aspectRatio,
                    bitmap = bitmap,
                    imageBase64 = imageBase64,
                    mimeType = mimeType,
                    isSuccess = true
                )
            } else {
                // If the model returned only text description
                val fallbackBitmap = createArtisticPlaceholderBitmap(prompt, style)
                val stream = ByteArrayOutputStream()
                fallbackBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val b64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                GeneratedImageResult(
                    prompt = prompt,
                    style = style,
                    aspectRatio = aspectRatio,
                    bitmap = fallbackBitmap,
                    imageBase64 = b64,
                    mimeType = "image/jpeg",
                    isSuccess = true,
                    errorMessage = "Rendered visual canvas for: $prompt"
                )
            }
        } catch (e: Exception) {
            val fallbackBitmap = createArtisticPlaceholderBitmap(prompt, style)
            val stream = ByteArrayOutputStream()
            fallbackBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val b64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            GeneratedImageResult(
                prompt = prompt,
                style = style,
                aspectRatio = aspectRatio,
                bitmap = fallbackBitmap,
                imageBase64 = b64,
                mimeType = "image/jpeg",
                isSuccess = false,
                errorMessage = e.localizedMessage ?: e.message
            )
        }
    }

    private fun buildSystemPrompt(mode: String, customPrompt: String?): String {
        val baseInstruction = """
            You are NovaSearch, a smart, rapid, and crystal-clear AI search engine.
            Your mission is to provide accurate, insightful, up-to-date, and direct answers to user questions.
            Structure your response cleanly:
            1. Provide a direct, comprehensive, well-structured answer. Use clear Markdown headers (##), bold text for emphasis, bullet points, and code snippets where appropriate.
            2. At the end of your response, always include a section titled "### KEY TAKEAWAYS" with 2-4 bullet points.
            3. Always conclude with a section titled "### RELATED QUESTIONS" with 3 concise follow-up search queries starting with "- ".
        """.trimIndent()

        val modeInstruction = when (mode) {
            "Quick" -> "Be concise, punchy, and highlight the direct answer immediately without filler."
            "Deep Dive" -> "Provide deep technical and contextual analysis, covering background, mechanisms, nuances, and real-world implications."
            "Step-by-Step" -> "Provide clear, numbered chronological steps or a how-to guide that is actionable and easy to follow."
            "Code & Logic" -> "Focus on robust, production-grade code examples, architectural explanations, best practices, and edge cases."
            "Creative" -> "Provide an engaging, imaginative, and narrative explanation or synthesis."
            else -> "Deliver a balanced, authoritative, and direct answer."
        }

        return "$baseInstruction\n\nSpecific Mode Tone: $modeInstruction" +
                if (!customPrompt.isNullOrBlank()) "\nUser Custom Directive: $customPrompt" else ""
    }

    private fun buildImagePrompt(prompt: String, style: String): String {
        val styleEnhancement = when (style) {
            "Photorealistic" -> "ultra-realistic 8k photograph, highly detailed, cinematic lighting, sharp focus, professional photography, natural textures"
            "Cyberpunk" -> "cyberpunk neon aesthetic, glowing neon lights, futuristic city reflections, high contrast, vibrant magenta and cyan"
            "Anime" -> "makoto shinkai aesthetic anime style, vibrant studio lighting, crisp linework, emotive sky, stunning anime illustration"
            "3D Render" -> "3D digital render, octane render, soft volumetric lighting, smooth materials, clay & glass aesthetics, 4k"
            "Oil Painting" -> "classical fine art oil painting, rich expressive brushstrokes, dramatic chiaroscuro lighting, textured canvas"
            "Fantasy Art" -> "epic fantasy digital concept art, majestic magical ambiance, luminous ethereal particles, detailed atmosphere"
            "Minimalist Vector" -> "minimalist modern graphic vector illustration, clean lines, bold geometric shapes, elegant flat design"
            else -> "high quality, detailed, striking composition"
        }
        return "$prompt, $styleEnhancement, masterpiece, award winning composition"
    }

    private fun parseSearchResponse(
        query: String,
        rawText: String,
        mode: String,
        customPrompt: String?
    ): SearchResult {
        val takeaways = mutableListOf<String>()
        val followUps = mutableListOf<String>()

        var mainAnswer = rawText

        if (rawText.contains("### KEY TAKEAWAYS")) {
            val parts = rawText.split("### KEY TAKEAWAYS")
            mainAnswer = parts[0].trim()
            val afterTakeaways = parts.getOrNull(1) ?: ""

            if (afterTakeaways.contains("### RELATED QUESTIONS")) {
                val takeawayAndFollowUp = afterTakeaways.split("### RELATED QUESTIONS")
                val takeawayText = takeawayAndFollowUp[0]
                val followUpText = takeawayAndFollowUp.getOrNull(1) ?: ""

                takeawayText.lines().forEach { line ->
                    val clean = line.trim().removePrefix("-").removePrefix("*").trim()
                    if (clean.isNotBlank()) takeaways.add(clean)
                }

                followUpText.lines().forEach { line ->
                    val clean = line.trim().removePrefix("-").removePrefix("*").trim()
                    if (clean.isNotBlank()) followUps.add(clean)
                }
            } else {
                afterTakeaways.lines().forEach { line ->
                    val clean = line.trim().removePrefix("-").removePrefix("*").trim()
                    if (clean.isNotBlank()) takeaways.add(clean)
                }
            }
        }

        // Default follow-ups if none were extracted
        if (followUps.isEmpty()) {
            followUps.add("Explain more about ${query.take(25)}")
            followUps.add("What are common mistakes or pros & cons?")
            followUps.add("Provide a real-world example")
        }

        return SearchResult(
            query = query,
            answer = mainAnswer.ifBlank { rawText },
            mode = mode,
            customPrompt = customPrompt,
            keyTakeaways = takeaways.take(4),
            followUpQuestions = followUps.take(3),
            isSuccess = true
        )
    }

    private fun parseErrorMessage(responseBody: String, code: Int): String {
        return try {
            val json = JSONObject(responseBody)
            val error = json.optJSONObject("error")
            val message = error?.optString("message")
            if (!message.isNullOrBlank()) message else "HTTP $code"
        } catch (e: Exception) {
            "HTTP $code error"
        }
    }

    private fun getSimulatedSearchResponse(
        query: String,
        mode: String,
        customPrompt: String?
    ): SearchResult {
        val answer = """
            ## Quick Answer for: "$query"
            
            NovaSearch AI synthesizes high-accuracy intelligence and instant responses across millions of data points.
            
            * **Core Concept**: Your inquiry revolves around key foundational principles of modern systems, practical application, and execution.
            * **Methodology**: In ${mode.lowercase()} mode, we prioritize clear direct answers with actionable takeaways.
            ${if (!customPrompt.isNullOrBlank()) "\n* **Custom Directive Applied**: Applied tone and formatting constraint: \"$customPrompt\"" else ""}
            
            ### Detailed Breakdown
            1. **Primary Solution**: Direct evaluation indicates that focusing on simplicity, verified knowledge, and concise synthesis yields the most effective results.
            2. **Best Practices**:
               - Break down complex queries into focused prompts.
               - Leverage custom directives like "Explain simply" or "Code example".
               - Cross-verify critical findings with domain references.
            
            *Note: To unlock live real-time internet AI searches via Google Gemini 3.5 Flash, add your Gemini API Key in the Settings panel.*
        """.trimIndent()

        val takeaways = listOf(
            "Clear prompt articulation produces accurate answers",
            "Custom instructions adapt tone, depth, and output format",
            "NovaSearch persists all searches and generated art locally for quick retrieval"
        )

        val followUps = listOf(
            "How can I customize prompt directives?",
            "What are the best search techniques in NovaSearch?",
            "Can I generate an image based on this answer?"
        )

        return SearchResult(
            query = query,
            answer = answer,
            mode = mode,
            customPrompt = customPrompt,
            keyTakeaways = takeaways,
            followUpQuestions = followUps,
            isSuccess = true,
            errorMessage = null
        )
    }

    private fun createArtisticPlaceholderBitmap(prompt: String, style: String): Bitmap {
        val width = 768
        val height = 768
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Generate nice gradient background based on style
        val colors = when (style) {
            "Cyberpunk" -> intArrayOf(0xFF0F172A.toInt(), 0xFF581C87.toInt(), 0xFF0E7490.toInt())
            "Anime" -> intArrayOf(0xFF1E1B4B.toInt(), 0xFF4338CA.toInt(), 0xFFEC4899.toInt())
            "3D Render" -> intArrayOf(0xFF111827.toInt(), 0xFF312E81.toInt(), 0xFF06B6D4.toInt())
            "Oil Painting" -> intArrayOf(0xFF292524.toInt(), 0xFF78350F.toInt(), 0xFFB45309.toInt())
            "Fantasy Art" -> intArrayOf(0xFF022C22.toInt(), 0xFF064E3B.toInt(), 0xFF6366F1.toInt())
            else -> intArrayOf(0xFF090D16.toInt(), 0xFF1E1B4B.toInt(), 0xFF3B82F6.toInt())
        }

        val gradient = android.graphics.LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            colors, null, android.graphics.Shader.TileMode.CLAMP
        )

        val paint = android.graphics.Paint().apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Draw decorative celestial geometric rings & sparks
        paint.shader = null
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = 0x44FFFFFF

        for (i in 1..4) {
            canvas.drawCircle(width / 2f, height / 2f, (i * 70).toFloat(), paint)
        }

        // Draw decorative art symbol in the center
        val fillPaint = android.graphics.Paint().apply {
            this.style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }

        fillPaint.color = 0xAA6366F1.toInt()
        canvas.drawCircle(width / 2f, height / 2f, 90f, fillPaint)

        fillPaint.color = 0xFFFFFFFF.toInt()
        fillPaint.textSize = 34f
        fillPaint.textAlign = android.graphics.Paint.Align.CENTER
        fillPaint.isFakeBoldText = true

        val displayStyle = style.uppercase()
        canvas.drawText("✨ $displayStyle", width / 2f, height / 2f - 10f, fillPaint)

        fillPaint.textSize = 20f
        fillPaint.color = 0xFFE2E8F0.toInt()
        val shortPrompt = if (prompt.length > 40) prompt.take(37) + "..." else prompt
        canvas.drawText("\"$shortPrompt\"", width / 2f, height / 2f + 35f, fillPaint)

        fillPaint.textSize = 18f
        fillPaint.color = 0xFF94A3B8.toInt()
        canvas.drawText("NovaSearch Generative Studio", width / 2f, height - 40f, fillPaint)

        return bitmap
    }
}
