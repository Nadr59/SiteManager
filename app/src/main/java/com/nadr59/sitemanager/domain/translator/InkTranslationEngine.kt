package com.nadr59.sitemanager.domain.translator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InkTranslationEngine @Inject constructor() : TranslationEngine {

    companion object {
        private const val API_URL = "https://inktranslator.com/api/translate"
        private const val ACCESS_KEY = "FREE"
        private const val TIMEOUT = 20000
    }

    override suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) return@withContext Result.success("")

            // ═══ تحويل "auto" إلى لغة مكتشفة ═══
            val source = if (sourceLanguage == "auto") {
                detectLanguage(text)
            } else {
                sourceLanguage
            }

            translateViaInk(text, source, targetLanguage)

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
                    onFailure = { results.add(text) }
                )
            }

            Result.success(results)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══ الترجمة عبر InkTranslator API ═══
    private fun translateViaInk(
        text: String,
        source: String,
        target: String
    ): Result<String> {
        return try {
            val url = URL(API_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                doOutput = true
            }

            // ═══ بناء JSON Body ═══
            val jsonBody = JSONObject().apply {
                put("accessKey", ACCESS_KEY)
                put("text", text)
                put("sourceLang", source)
                put("targetLang", target)
            }

            // ═══ إرسال الطلب ═══
            conn.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }

            // ═══ قراءة الاستجابة ═══
            if (conn.responseCode != 200) {
                val error = conn.errorStream?.bufferedReader()?.readText() 
                    ?: "HTTP ${conn.responseCode}"
                conn.disconnect()
                return Result.failure(Exception(error))
            }

            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            // ═══ استخراج النص المترجم ═══
            val json = JSONObject(response)
            val translated = json.optString("translatedText", "")

            if (translated.isNotBlank()) {
                Result.success(translated)
            } else {
                Result.failure(Exception("ترجمة فارغة من InkTranslator"))
            }

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
}
