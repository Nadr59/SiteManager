package com.nadr59.sitemanager.domain.translator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
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

            // ═══ نجرب Google Translate المجاني ═══
            val result = translateViaGoogle(text, sourceLanguage, targetLanguage)
            if (result.isSuccess) return@withContext result

            // ═══ احتياطي: MyMemory ═══
            val fallback = translateViaMyMemory(text, sourceLanguage, targetLanguage)
            if (fallback.isSuccess) return@withContext fallback

            Result.failure(Exception("فشل الترجمة"))
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

    // ═══ Google Translate (مجاني) ═══
    private fun translateViaGoogle(
        text: String,
        source: String,
        target: String
    ): Result<String> {
        return try {
            val encoded = URLEncoder.com/translate_a/single?" +
                "client=gtx&sl=$source&tl=$target&dt=t&q=$encoded"
            )

            val conn = url.openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
                connectTimeout = 15000
                readTimeout = 15000
            }

            if (conn.responseCode != 200) {
                conn.disconnect()
                return Result.failure(Exception("HTTP ${conn.responseCode}"))
            }

.encode(text, "UTF-8")
            val url = URL(
                "https://translate.googleapis            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val arr = JSONArray(response)
            val translations = arr.getJSONArray(0)
            val result = StringBuilder()

            for (i in 0 until translations.length()) {
                val sentence = translations.getJSONArray(i)
                result.append(sentence.getString(0))
            }

            if (result.isNotBlank()) {
                Result.success(result.toString())
            } else {
                Result.failure(Exception("ترجمة فارغة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══ MyMemory (مجاني — احتياطي) ═══
    private fun translateViaMyMemory(
        text: String,
        source: String,
        target: String
    ): Result<String> {
        return try {
            val encoded = URLEncoder.encode(text.take(500), "UTF-8")
            val langPair = "${source}|${target}"
            val url = URL(
                "https://api.mymemory.translated.net/get?q=$encoded&langpair=$langPair"
            )

            val conn = url.openConnection() as HttpURLConnection
            conn.apply {
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
            val translated = json.getJSONObject("responseData")
                .getString("translatedText")

            if (translated.isNotBlank()) {
                Result.success(translated)
            } else {
                Result.failure(Exception("ترجمة فارغة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
              }
