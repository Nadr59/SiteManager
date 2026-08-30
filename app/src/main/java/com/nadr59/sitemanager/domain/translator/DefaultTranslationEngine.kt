package com.nadr59.sitemanager.domain.translator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject

class DefaultTranslationEngine @Inject constructor() : TranslationEngine {

    override suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) return@withContext Result.success("")

            // ═══ المحاولة الأولى: Google Translate ═══
            val googleResult = translateViaGoogle(text, "auto", targetLanguage)
            if (googleResult.isSuccess && googleResult.getOrNull()?.isNotBlank() == true) {
                return@withContext googleResult
            }

            // ═══ المحاولة الثانية: MyMemory مع اكتشاف اللغة ═══
            val detectedLang = detectLanguage(text)
            val fallback = translateViaMyMemory(text, detectedLang, targetLanguage)
            if (fallback.isSuccess) return@withContext fallback

            Result.failure(Exception("فشل الترجمة من جميع المصادر"))
        } catch (e: Exception) {
            Result.failure(Exception("خطأ في الترجمة: ${e.message}"))
        }
    }

    override suspend fun translateBatch(
        texts: List<String>,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val results = mutableListOf<String>()
            for (text in texts) {
                if (text.isBlank()) {
                    results.add("")
                    continue
                }
                val result = translate(text, sourceLanguage, targetLanguage)
                result.fold(
                    onSuccess = { results.add(it) },
                    onFailure = { results.add(text) } // إبقاء النص الأصلي عند الفشل
                )
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══ اكتشاف اللغة تلقائياً ═══
    private fun detectLanguage(text: String): String {
        val sample = text.take(100)
        return when {
            // عربي
            sample.any { it in '\u0600'..'\u06FF' } -> "ar"
            // صيني
            sample.any { it in '\u4E00'..'\u9FFF' } -> "zh"
            // ياباني
            sample.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' } -> "ja"
            // كوري
            sample.any { it in '\uAC00'..'\uD7AF' } -> "ko"
            // روسي / سيريلك
            sample.any { it in '\u0400'..'\u04FF' } -> "ru"
            // فارسي
            sample.any { it in '\u0750'..'\u077F' } -> "fa"
            // افتراضي
            else -> "en"
        }
    }

    // ═══ Google Translate (يدعم auto) ═══
    private fun translateViaGoogle(
        text: String,
        source: String,
        target: String
    ): Result<String> {
        return try {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val urlStr = "https://translate.googleapis.com/translate_a/single" +
                "?client=gtx&sl=$source&tl=$target&dt=t&q=$encoded"

            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
                connectTimeout = 15000
                readTimeout = 15000
            }

            if (conn.responseCode != 200) {
                conn.disconnect()
                return Result.failure(Exception("HTTP ${conn.responseCode}"))
            }

            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val arr = JSONArray(response)
            val translations = arr.getJSONArray(0)
            val result = StringBuilder()

            for (i in 0 until translations.length()) {
                val sentence = translations.optJSONArray(i) ?: continue
                val translated = sentence.optString(0)
                if (translated.isNotBlank()) result.append(translated)
            }

            if (result.isNotBlank()) Result.success(result.toString())
            else Result.failure(Exception("ترجمة فارغة"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══ MyMemory (لا يدعم auto - نكتشف اللغة أولاً) ═══
    private fun translateViaMyMemory(
        text: String,
        source: String, // يجب أن يكون كود لغة حقيقي
        target: String
    ): Result<String> {
        return try {
            // MyMemory لا يقبل "auto" - نستخدم اللغة المكتشفة
            val validSource = if (source == "auto") detectLanguage(text) else source

            val encoded = URLEncoder.encode(text.take(500), "UTF-8")
            val langPair = "$validSource|$target"
            val urlStr = "https://api.mymemory.translated.net/get?q=$encoded&langpair=$langPair"

            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
            }

            if (conn.responseCode != 200) {
                conn.disconnect()
                return Result.failure(Exception("HTTP ${conn.responseCode}"))
            }

            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(response)
            val responseStatus = json.optInt("responseStatus", 0)

            if (responseStatus != 200) {
                val details = json.optString("responseDetails", "Unknown error")
                return Result.failure(Exception("MyMemory: $details"))
            }

            val translated = json.getJSONObject("responseData")
                .getString("translatedText")

            if (translated.isNotBlank()) Result.success(translated)
            else Result.failure(Exception("ترجمة فارغة"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
