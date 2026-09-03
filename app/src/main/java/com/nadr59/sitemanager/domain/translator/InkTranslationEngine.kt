package com.nadr59.sitemanager.domain.translator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InkTranslationEngine @Inject constructor() : TranslationEngine {

    companion object {
        private const val TIMEOUT = 15000
    }

    // ═══════════════════════════════════════════════
    // الواجهة الرئيسية
    // ═══════════════════════════════════════════════

    override suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext Result.success("")

        val source = if (sourceLanguage == "auto") {
            detectLanguage(text)
        } else {
            sourceLanguage
        }

        // ═══ تجربة المصادر بالترتيب ═══
        val engines = listOf(
            ::translateViaMyMemory,
            ::translateViaLingva,
            ::translateViaLibreTranslate,
            ::translateViaGoogleUnofficial
        )

        for (engine in engines) {
            val result = engine(text, source, targetLanguage)
            if (result.isSuccess) {
                val translated = result.getOrNull() ?: ""
                if (translated.isNotBlank() && translated != text) {
                    return@withContext Result.success(translated)
                }
            }
        }

        // إذا فشل الكل، أعد النص الأصلي
        Result.success(text)
    }

    override suspend fun translateBatch(
        texts: List<String>,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val results = texts.map { text ->
                if (text.isBlank()) {
                    ""
                } else {
                    translate(text, sourceLanguage, targetLanguage)
                        .getOrDefault(text)
                }
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════
    // المصدر 1: MyMemory (الأفضل - مجاني بدون مفتاح)
    // ═══════════════════════════════════════════════

    private fun translateViaMyMemory(
        text: String,
        source: String,
        target: String
    ): Result<String> {
        return try {
            val encodedText = URLEncoder.encode(
                text.take(500), "UTF-8"
            )
            val langPair = "$source|$target"
            val urlStr = "https://api.mymemory.translated.net/get" +
                "?q=$encodedText" +
                "&langpair=$langPair"

            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0"
                )
            }

            if (conn.responseCode != 200) {
                conn.disconnect()
                return Result.failure(
                    Exception("MyMemory HTTP ${conn.responseCode}")
                )
            }

            val response = conn.inputStream
                .bufferedReader()
                .readText()
            conn.disconnect()

            val json = JSONObject(response)
            val data = json.optJSONObject("responseData")
            val translated = data?.optString(
                "translatedText", ""
            ) ?: ""

            if (translated.isNotBlank()) {
                Result.success(translated)
            } else {
                Result.failure(Exception("MyMemory: ترجمة فارغة"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════
    // المصدر 2: Lingva Translate (واجهة Google مجانية)
    // ═══════════════════════════════════════════════

    private fun translateViaLingva(
        text: String,
        source: String,
        target: String
    ): Result<String> {
        return try {
            val encodedText = URLEncoder.encode(
                text.take(500), "UTF-8"
            )

            // يوجد عدة خوادم Lingva العامة
            val servers = listOf(
                "https://lingva.ml",
                "https://translate.plausibility.cloud",
                "https://lingva.thedaviddelta.com"
            )

            for (server in servers) {
                try {
                    val urlStr = "$server/api/v1/$source/$target/$encodedText"
                    val url = URL(urlStr)
                    val conn = (url.openConnection()
                        as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = TIMEOUT
                        readTimeout = TIMEOUT
                        setRequestProperty(
                            "User-Agent",
                            "Mozilla/5.0"
                        )
                    }

                    if (conn.responseCode == 200) {
                        val response = conn.inputStream
                            .bufferedReader()
                            .readText()
                        conn.disconnect()

                        val json = JSONObject(response)
                        val translated = json.optString(
                            "translation", ""
                        )

                        if (translated.isNotBlank()) {
                            return Result.success(translated)
                        }
                    } else {
                        conn.disconnect()
                    }
                } catch (_: Exception) {
                    // جرب الخادم التالي
                }
            }

            Result.failure(Exception("Lingva: فشل جميع الخوادم"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════
    // المصدر 3: LibreTranslate (خادم عام)
    // ═══════════════════════════════════════════════

    private fun translateViaLibreTranslate(
        text: String,
        source: String,
        target: String
    ): Result<String> {
        return try {
            val servers = listOf(
                "https://libretranslate.com",
                "https://translate.argosopentech.com",
                "https://translate.terraprint.co"
            )

            for (server in servers) {
                try {
                    val url = URL("$server/translate")
                    val conn = (url.openConnection()
                        as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty(
                            "Content-Type",
                            "application/json"
                        )
                        connectTimeout = TIMEOUT
                        readTimeout = TIMEOUT
                        doOutput = true
                    }

                    val body = JSONObject().apply {
                        put("q", text.take(500))
                        put("source", source)
                        put("target", target)
                        put("format", "text")
                    }

                    conn.outputStream.use { os ->
                        os.write(
                            body.toString()
                                .toByteArray(Charsets.UTF_8)
                        )
                    }

                    if (conn.responseCode == 200) {
                        val response = conn.inputStream
                            .bufferedReader()
                            .readText()
                        conn.disconnect()

                        val json = JSONObject(response)
                        val translated = json.optString(
                            "translatedText", ""
                        )

                        if (translated.isNotBlank()) {
                            return Result.success(translated)
                        }
                    } else {
                        conn.disconnect()
                    }
                } catch (_: Exception) {
                    // جرب الخادم التالي
                }
            }

            Result.failure(Exception("LibreTranslate: فشل"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════
    // المصدر 4: Google Translate (غير رسمي)
    // ═══════════════════════════════════════════════

    private fun translateViaGoogleUnofficial(
        text: String,
        source: String,
        target: String
    ): Result<String> {
        return try {
            val encodedText = URLEncoder.encode(
                text.take(500), "UTF-8"
            )
            val urlStr = "https://translate.googleapis.com" +
                "/translate_a/single" +
                "?client=gtx" +
                "&sl=$source" +
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
                    "Mozilla/5.0 (Android 14)"
                )
            }

            if (conn.responseCode != 200) {
                conn.disconnect()
                return Result.failure(
                    Exception("Google HTTP ${conn.responseCode}")
                )
            }

            val response = conn.inputStream
                .bufferedReader()
                .readText()
            conn.disconnect()

            // ═══ تحليل استجابة Google ═══
            val translated = parseGoogleResponse(response)

            if (translated.isNotBlank()) {
                Result.success(translated)
            } else {
                Result.failure(Exception("Google: ترجمة فارغة"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseGoogleResponse(response: String): String {
        return try {
            val arr = JSONArray(response)
            val translations = arr.optJSONArray(0)
                ?: return ""

            val sb = StringBuilder()
            for (i in 0 until translations.length()) {
                val item = translations.optJSONArray(i)
                    ?: continue
                val part = item.optString(0, "")
                if (part.isNotBlank()) {
                    sb.append(part)
                }
            }

            sb.toString().trim()

        } catch (_: Exception) {
            ""
        }
    }

    // ═══════════════════════════════════════════════
    // اكتشاف اللغة
    // ═══════════════════════════════════════════════

    private fun detectLanguage(text: String): String {
        val sample = text.take(100)
        return when {
            sample.any { it in '\u0600'..'\u06FF' } -> "ar"
            sample.any { it in '\u4E00'..'\u9FFF' } -> "zh"
            sample.any {
                it in '\u3040'..'\u309F' ||
                it in '\u30A0'..'\u30FF'
            } -> "ja"
            sample.any { it in '\uAC00'..'\uD7AF' } -> "ko"
            sample.any { it in '\u0400'..'\u04FF' } -> "ru"
            sample.any { it in '\u0750'..'\u077F' } -> "fa"
            else -> "en"
        }
    }
}
