package com.nadr59.sitemanager.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class ApiAskResponse(
    val success: Boolean = false,
    val response: String = "",
    val remaining: Int = 0,
    val provider: String = "",
    val error: String = ""
)

class ApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val gson = Gson()

    suspend fun ask(prompt: String): ApiAskResponse = withContext(Dispatchers.IO) {
        try {
            val body = gson.toJson(mapOf(
                "appId" to ApiConfig.APP_ID,
                "prompt" to prompt
            ))

            val request = Request.Builder()
                .url(ApiConfig.ASK_URL)
                .addHeader("Content-Type", "application/json; charset=UTF-8")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            when (response.code) {
                200 -> {
                    gson.fromJson(responseBody, ApiAskResponse::class.java)
                }
                403 -> {
                    ApiAskResponse(error = "التطبيق غير مصرح له بالوصول")
                }
                429 -> {
                    ApiAskResponse(error = "تم تجاوز الحد اليومي للطلبات")
                }
                503 -> {
                    ApiAskResponse(error = "لا توجد مفاتيح متاحة حالياً")
                }
                else -> {
                    ApiAskResponse(error = "خطأ ${response.code}: ${responseBody.take(150)}")
                }
            }
        } catch (e: java.net.UnknownHostException) {
            ApiAskResponse(error = "لا يوجد اتصال بالإنترنت")
        } catch (e: java.net.SocketTimeoutException) {
            ApiAskResponse(error = "انتهت مهلة الاتصال — حاول مرة أخرى")
        } catch (e: java.net.ConnectException) {
            ApiAskResponse(error = "فشل الاتصال بالخادم")
        } catch (e: Exception) {
            ApiAskResponse(error = e.message ?: "خطأ غير معروف")
        }
    }
}
