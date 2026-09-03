package com.nadr59.sitemanager.domain.translator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InkTranslationEngine @Inject constructor() : TranslationEngine {

    companion object {
        private const val TIMEOUT = 10000
        private const val BASE_URL =
            "https://translate.googleapis.com/translate_a/single"
    }

    override suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext Result.success("")
        translateGoogle(text, sourceLanguage, targetLanguage)
    }

    override suspend fun translateBatch(
        texts: List<String>,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        if (texts.isEmpty()) return@withContext Result.success(emptyList())

        // ═══ دمج كل النصوص في طلب واحد ═══
        val separator = "\n🔸\n"
        val combined = texts
            .filter { it.isNotBlank() }
            .joinToString(separator)

        val result = translateGoogle(
            combined,
            sourceLanguage,
            targetLanguage
        )

        result.fold(
            onSuccess = { translatedCombined ->
                val parts = translatedCombined
                    .split("🔸")
                    .map { it.trim() }

                // ═══ إعادة ربط النتائج بالنصوص الأصلية ═══
                val finalResults = mutableListOf<String>()
                var partIndex = 0

                for (original in texts) {
                    if (original.isBlank()) {
                        finalResults.add("")
                    } else {
                        finalResults.add(
                            parts.getOrElse(partIndex) { original }
                        )
                        partIndex++
                    }
                }

                Result.success(finalResults)
            },
            onFailure = {
                // ═══ Fallback: ترجمة فردية ═══
                val results = texts.map { text ->
                    if (text.isBlank()) ""
                    else translateGoogle(text, sourceLanguage, targetLanguage)
                        .getOrDefault(text)
                }
                Result.success(results)
            }
        )
    }

    private fun translateGoogle(
        text: String,
        source: String,
        target: String
    ): Result<String> {
        return try {
            val sl = if (source == "auto") "auto" else source
            val encodedText = URLEncoder.encode(text, "UTF-8")

            val urlStr = "$BASE_URL" +
                "?client=gtx" +
                "&sl=$sl" +
                "&tl=$target" +
                "&dt=t" +
                "&q=$encodedText"

            val url = URL(urlStr)
            val conn = (url.openConnection()
                    as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14)"
                )
                setRequestProperty(
                    "Accept",
                    "application/json"
                )
            }

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                conn.disconnect()
                return Result.failure(
                    Exception("HTTP $responseCode")
                )
            }

            val response = conn.inputStream
                .bufferedReader(Charsets.UTF_8)
                .readText()
            conn.disconnect()

            val translated = parseGoogleResponse(response)

            if (translated.isNotBlank()) {
                Result.success(translated)
            } else {
                Result.failure(Exception("ترجمة فارغة"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseGoogleResponse(response: String): String {
        return try {
            val outer = JSONArray(response)
            val sentences = outer.optJSONArray(0)
                ?: return ""

            val sb = StringBuilder()
            for (i in 0 until sentences.length()) {
                val item = sentences.optJSONArray(i)
                    ?: continue
                val part = item.optString(0, "")
                sb.append(part)
            }

            sb.toString().trim()
        } catch (_: Exception) {
            ""
        }
    }
}
