package com.nadr59.sitemanager.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

data class SiteContent(
    val url: String,
    val title: String?,
    val description: String?,
    val rawContent: String,
    val isHttps: Boolean,
    val statusCode: Int,
    val headers: Map<String, String>
)

class WebScraper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun scrape(url: String): SiteContent = withContext(Dispatchers.IO) {
        val finalUrl = if (url.startsWith("http")) url else "https://$url"

        val request = Request.Builder()
            .url(finalUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Android 14) AppleWebKit/537.36")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        val statusCode = response.code
        val isHttps = finalUrl.startsWith("https")

        val headers = mutableMapOf<String, String>()
        response.headers.forEach { headers[it.first] = it.second }

        val doc = Jsoup.parse(body)

        val title = doc.title()?.take(500)
        val description = doc.select("meta[name=description]").attr("content")
            .ifEmpty { doc.select("meta[property=og:description]").attr("content") }
            .take(1000)

        // تنظيف المحتوى
        doc.select("script, style, nav, footer, header, aside, .ad, .advertisement").remove()
        val text = doc.body()?.text()?.take(8000) ?: ""

        SiteContent(
            url = finalUrl,
            title = title,
            description = description,
            rawContent = text,
            isHttps = isHttps,
            statusCode = statusCode,
            headers = headers
        )
    }

    suspend fun checkUrl(url: String): SiteContent? {
        return try {
            scrape(url)
        } catch (_: Exception) {
            null
        }
    }
}
