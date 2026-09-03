package com.nadr59.sitemanager.domain.translator

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DefaultTranslationEngine @Inject constructor() : TranslationEngine {

    companion object {
        private const val ENDPOINT =
            "https://translation.googleapis.com/language/translate/v2"

        /*
         * يتم إنشاء هذا الحقل في BuildConfig من local.properties.
         *
         * لا تضع المفتاح الحقيقي داخل Git.
         */
        private const val API_KEY =
           // BuildConfig.GOOGLE_TRANSLATION_API_KEY
        "FREE" // أو ضع مفتاحك مباشرة

        private const val MAX_BATCH_SIZE = 100
    }

    private val client =
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    private val gson = Gson()

    override suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<String> = withContext(Dispatchers.IO) {

        if (text.isBlank()) {
            return@withContext Result.success("")
        }

        translateBatch(
            texts = listOf(text),
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage
        ).mapCatching {
            it.firstOrNull() ?: ""
        }
    }

    override suspend fun translateBatch(
        texts: List<String>,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {

        if (texts.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        if (API_KEY.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException(
                    "مفتاح Google Cloud Translation غير مضبوط"
                )
            )
        }

        if (targetLanguage.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException(
                    "لغة الترجمة غير محددة"
                )
            )
        }

        try {

            val allResults =
                mutableListOf<String>()

            texts.chunked(MAX_BATCH_SIZE)
                .forEach { batch ->

                    val result =
                        translateBatchInternal(
                            texts = batch,
                            sourceLanguage = sourceLanguage,
                            targetLanguage = targetLanguage
                        )

                    if (result.isFailure) {
                        return@withContext Result.failure(
                            result.exceptionOrNull()
                                ?: Exception("فشل الترجمة")
                        )
                    }

                    allResults.addAll(
                        result.getOrThrow()
                    )
                }

            Result.success(allResults)

        } catch (e: Exception) {

            Result.failure(
                Exception(
                    "فشل الاتصال بخدمة Google Cloud Translation: ${e.message}",
                    e
                )
            )
        }
    }

    private fun translateBatchInternal(
        texts: List<String>,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<List<String>> {

        return try {

            val requestJson =
                JSONObject().apply {

                    val queries =
                        org.json.JSONArray()

                    texts.forEach { text ->
                        queries.put(text)
                    }

                    put("q", queries)
                    put("target", targetLanguage)

                    /*
                     * auto-detection:
                     * عندما تكون source = auto لا نرسل source.
                     */
                    if (
                        sourceLanguage.isNotBlank() &&
                        sourceLanguage != "auto"
                    ) {
                        put(
                            "source",
                            sourceLanguage
                        )
                    }

                    put("format", "text")
                    put("model", "nmt")
                }

            val body =
                requestJson
                    .toString()
                    .toRequestBody(
                        "application/json; charset=utf-8"
                            .toMediaType()
                    )

            val request =
                Request.Builder()
                    .url("$ENDPOINT?key=$API_KEY")
                    .post(body)
                    .addHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .build()

            client.newCall(request)
                .execute()
                .use { response ->

                    val responseBody =
                        response.body?.string()
                            .orEmpty()

                    if (!response.isSuccessful) {

                        return Result.failure(
                            Exception(
                                "Google Translation HTTP ${response.code}: " +
                                    extractGoogleError(responseBody)
                            )
                        )
                    }

                    parseResponse(responseBody)
                }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    private fun parseResponse(
        responseBody: String
    ): Result<List<String>> {

        return try {

            val root =
                JSONObject(responseBody)

            if (root.has("error")) {
                return Result.failure(
                    Exception(
                        root.getJSONObject("error")
                            .optString(
                                "message",
                                "Google Translation error"
                            )
                    )
                )
            }

            val translations =
                root
                    .getJSONObject("data")
                    .getJSONArray("translations")

            val result =
                mutableListOf<String>()

            for (i in 0 until translations.length()) {

                val item =
                    translations.getJSONObject(i)

                result.add(
                    item.optString(
                        "translatedText",
                        ""
                    )
                )
            }

            Result.success(result)

        } catch (e: Exception) {

            Result.failure(
                Exception(
                    "تعذر تحليل استجابة Google Translation",
                    e
                )
            )
        }
    }

    private fun extractGoogleError(
        responseBody: String
    ): String {

        return try {

            JSONObject(responseBody)
                .getJSONObject("error")
                .optString(
                    "message",
                    responseBody.take(300)
                )

        } catch (_: Exception) {

            responseBody.take(300)
        }
    }
}
