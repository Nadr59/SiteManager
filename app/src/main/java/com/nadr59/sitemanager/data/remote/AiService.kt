package com.nadr59.sitemanager.data.remote

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.nadr59.sitemanager.data.local.AnalysisType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AiService(private val gson: Gson) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun analyzeSite(
        content: SiteContent,
        config: AiConfig,
        analysisType: AnalysisType = AnalysisType.EXPLAIN,
        customQuestion: String = ""
    ): AnalysisResult = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(content, analysisType, customQuestion)

        var lastError: Exception? = null
        repeat(3) { attempt ->
            try {
                val raw = callAi(prompt, config)
                return@withContext parseResponse(raw, analysisType)
            } catch (e: Exception) {
                lastError = e
                if (attempt < 2) Thread.sleep(2000L * (attempt + 1))
            }
        }
        throw lastError ?: Exception("فشل التحليل بعد 3 محاولات")
    }

    private fun buildPrompt(
        content: SiteContent,
        type: AnalysisType,
        customQuestion: String
    ): String {
        val typeInstruction = when (type) {
            AnalysisType.CUSTOM -> customQuestion
            else -> type.promptPrefix
        }

        val contentSection = content.rawContent.take(4000)

        return """أنت مساعد ذكي لتحليل المواقع.
حلل: "${content.url}"
النوع: ${type.displayName}

$typeInstruction

معلومات إضافية عن الموقع:
${content.title?.let { "العنوان: $it" } ?: ""}
${content.description?.let { "الوصف: $it" } ?: ""}
${if (content.isHttps) "يستخدم HTTPS" else "لا يستخدم HTTPS"}

المحتوى:
$contentSection

أجب بتنسيق Markdown مع عناوين واضحة."""
    }

    private fun callAi(prompt: String, config: AiConfig): String {
        val url = buildApiUrl(config)
        val model = buildModel(config)

        val body = gson.toJson(mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf("role" to "user", "content" to prompt)
            ),
            "max_tokens" to 2000,
            "temperature" to 0.7
        ))

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json; charset=UTF-8")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        if (response.code == 429) throw Exception("تم تجاوز حد الطلبات")
        if (response.code == 401) throw Exception("مفتاح API غير صالح")
        if (response.code != 200) {
            throw Exception("خطأ الخادم ${response.code}: ${responseBody?.take(150)}")
        }
        if (responseBody.isNullOrBlank()) throw Exception("استجابة فارغة")

        return extractText(responseBody)
    }

    private fun buildApiUrl(config: AiConfig): String {
        return when (config.provider) {
            "groq"       -> "https://api.groq.com/openai/v1/chat/completions"
            "orcarouter" -> "https://api.orcarouter.ai/v1/chat/completions"
            "openrouter" -> "https://openrouter.ai/api/v1/chat/completions"
            "openai"     -> "https://api.openai.com/v1/chat/completions"
            "hcnsec"     -> "https://api.hcnsec.cn/v1/chat/completions"
            "gemini"     -> {
                val m = config.model.ifBlank { "gemini-2.0-flash" }
                "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=${config.apiKey}"
            }
            "custom"     -> "${config.baseUrl.trimEnd('/')}/v1/chat/completions"
            else -> throw Exception("مزود غير معروف: ${config.provider}")
        }
    }

    private fun buildModel(config: AiConfig): String {
        return config.model.ifBlank {
            when (config.provider) {
                "groq"       -> "llama-3.3-70b-versatile"
                "orcarouter" -> "orcarouter/free"
                "openrouter" -> "google/gemini-2.0-flash-exp"
                "openai"     -> "gpt-4o-mini"
                "hcnsec"     -> "auto"
                else         -> "auto"
            }
        }
    }

    private fun extractText(body: String): String {
        val json = JsonParser.parseString(body).asJsonObject

        json.getAsJsonArray("choices")?.let { choices ->
            if (choices.size() > 0) {
                choices[0].asJsonObject
                    .getAsJsonObject("message")
                    ?.get("content")?.asString?.trim()
                    ?.let { return it }
            }
        }

        json.getAsJsonArray("candidates")?.let { candidates ->
            if (candidates.size() > 0) {
                candidates[0].asJsonObject
                    .getAsJsonObject("content")
                    ?.getAsJsonArray("parts")
                    ?.let { parts ->
                        if (parts.size() > 0) {
                            parts[0].asJsonObject.get("text")?.asString?.trim()
                                ?.let { return it }
                        }
                    }
            }
        }

        throw Exception("تنسيق استجابة غير متوقع: ${body.take(200)}")
    }

    private fun parseResponse(raw: String, type: AnalysisType): AnalysisResult {
        val sections = mutableMapOf<String, StringBuilder>()
        var currentKey = "overview"
        val current = StringBuilder()

        raw.lines().forEach { line ->
            val header = Regex("^##?\\s+(.+)").find(line)
            if (header != null) {
                sections[currentKey] = current
                current.clear()
                currentKey = normalizeKey(header.groupValues[1].trim())
            } else {
                current.appendLine(line)
            }
        }
        sections[currentKey] = current

        val ratingText = sections["rating"]?.toString() ?: ""
        val ratingMatch = Regex("(\\d+\\.?\\d*)\\s*/\\s*10|من\\s*(\\d+\\.?\\d*)").find(ratingText)
        val rating = (ratingMatch?.groupValues?.getOrNull(1)?.toFloatOrNull()
            ?: ratingMatch?.groupValues?.getOrNull(2)?.toFloatOrNull()
            ?: 0f).coerceIn(0f, 10f)

        return AnalysisResult(
            overview = sections["overview"]?.toString()?.trim() ?: "",
            purpose = sections["purpose"]?.toString()?.trim() ?: "",
            features = extractBullets(sections["features"]?.toString() ?: ""),
            howToUse = sections["usage"]?.toString()?.trim() ?: "",
            techStack = extractBullets(sections["tech"]?.toString() ?: ""),
            examples = sections["examples"]?.toString()?.trim() ?: "",
            prosAndCons = ProsAndCons(
                pros = extractBullets(sections["pros"]?.toString() ?: ""),
                cons = extractBullets(sections["cons"]?.toString() ?: "")
            ),
            rating = rating,
            rawMarkdown = raw,
            analysisType = type.key
        )
    }

    private fun normalizeKey(name: String): String {
        val n = name.lowercase()
        return when {
            "نظرة" in n || "overview" in n -> "overview"
            "غرض" in n || "هدف" in n || "purpose" in n -> "purpose"
            "ميزة" in n || "feature" in n -> "features"
            "تقني" in n || "tech" in n -> "tech"
            "استخدام" in n || "بدء" in n || "usage" in n -> "usage"
            "مثال" in n || "example" in n -> "examples"
            "قوة" in n || "pros" in n -> "pros"
            "ضعف" in n || "cons" in n -> "cons"
            "تقييم" in n || "rating" in n -> "rating"
            else -> n.replace(" ", "_")
        }
    }

    private fun extractBullets(text: String): List<String> = text.lines()
        .map { it.trim() }
        .filter { it.startsWith("- ") || it.startsWith("* ") || it.startsWith("• ") }
        .map { it.removePrefix("- ").removePrefix("* ").removePrefix("• ").trim() }
        .filter { it.isNotBlank() }
}
