package com.nadr59.sitemanager.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebScraper @Inject constructor() {

companion object {
    private const val MAX_CONTENT = 6000    // ← قللت من 8000 لتقليل حجم الطلب
    private const val TIMEOUT = 20_000     // ← زدت من 15000
}

    suspend fun scrape(url: String): SiteContent = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36")
                .timeout(TIMEOUT)
                .followRedirects(true)
                .get()

            val type = detectType(url, doc)
            SiteContent(
                url = url,
                title = doc.title().ifBlank { extractDomain(url) },
                description = extractDescription(doc),
                type = type,
                rawContent = extractContent(doc, type),
                metadata = extractMetadata(doc)
            )
        } catch (e: Exception) {
            SiteContent(
                url = url,
                title = extractDomain(url),
                rawContent = "فشل تحميل المحتوى: ${e.message}",
                type = SiteType.UNKNOWN
            )
        }
    }

    private fun detectType(url: String, doc: Document): SiteType {
        val domain = extractDomain(url).lowercase()
        val path = url.lowercase()
        return when {
            domain.contains("github.com") && path.count { it == '/' } >= 4 ->
                SiteType.GITHUB_REPO
            domain.contains("github.com") && path.count { it == '/' } <= 2 ->
                SiteType.GITHUB_PROFILE
            path.contains("/api/") || path.contains("/docs/api") || path.contains("/swagger") ->
                SiteType.API_DOCS
            domain.contains("readthedocs") || domain.contains("docs.") || path.contains("/docs/") ->
                SiteType.DOCUMENTATION
            domain.contains("medium.com") || domain.contains("dev.to") || doc.select("article").isNotEmpty() ->
                SiteType.BLOG
            domain.contains("npmjs") || domain.contains("pypi.org") || domain.contains("crates.io") ->
                SiteType.PACKAGE
            else -> SiteType.WEBSITE
        }
    }

    private fun extractDescription(doc: Document): String {
        return doc.select("meta[name=description]").attr("content").ifBlank {
            doc.select("meta[property='og:description']").attr("content")
        }.take(500)
    }

    private fun extractContent(doc: Document, type: SiteType): String {
        return when (type) {
            SiteType.GITHUB_REPO -> extractGitHub(doc)
            SiteType.GITHUB_PROFILE -> extractGitHubProfile(doc)
            SiteType.API_DOCS -> extractApiDocs(doc)
            SiteType.DOCUMENTATION -> extractDocs(doc)
            SiteType.BLOG -> doc.select("article, .post-content, main").text()
            SiteType.PACKAGE -> extractPackage(doc)
            else -> {
                doc.select("script, style, nav, footer, header, aside").remove()
                doc.body().text()
            }
        }.take(MAX_CONTENT)
    }

    private fun extractGitHub(doc: Document): String = buildString {
        val readme = doc.select("article.markdown-body").text()
        if (readme.isNotBlank()) {
            appendLine("=== README ===")
            appendLine(readme.take(4000))
        }
        val desc = doc.select("p.f4.my-3").text()
        if (desc.isNotBlank()) appendLine("الوصف: $desc")
        val topics = doc.select("a.topic-tag").map { it.text().trim() }
        if (topics.isNotEmpty()) appendLine("المواضيع: ${topics.joinToString(", ")}")
        val langs = doc.select("span[itemprop='programmingLanguage']").map { it.text() }
        if (langs.isNotEmpty()) appendLine("اللغات: ${langs.joinToString(", ")}")
        val files = doc.select("div[role='rowheader'] a").take(20).map { it.text() }
        if (files.isNotEmpty()) {
            appendLine("\n=== الملفات ===")
            files.forEach { appendLine("  $it") }
        }
    }

    private fun extractGitHubProfile(doc: Document): String = buildString {
        appendLine("الاسم: ${doc.select("span.p-name").text()}")
        appendLine("السيرة: ${doc.select("div.p-note div").text()}")
        val repos = doc.select("span.repo").map { it.text() }.take(10)
        if (repos.isNotEmpty()) {
            appendLine("المستودعات:")
            repos.forEach { appendLine("  - $it") }
        }
    }

    private fun extractApiDocs(doc: Document): String = buildString {
        doc.select("pre, code").take(15).forEach {
            val text = it.text().take(400)
            if (text.isNotBlank()) { appendLine("---"); appendLine(text) }
        }
    }

    private fun extractDocs(doc: Document): String = buildString {
        val nav = doc.select("nav a, .sidebar a, .toctree a").take(15).map { it.text() }
        if (nav.isNotEmpty()) {
            appendLine("=== الفهرس ===")
            nav.forEach { appendLine("  - $it") }
        }
        appendLine(doc.select("main, article, .content, .body").text().take(5000))
    }

    private fun extractPackage(doc: Document): String = buildString {
        appendLine("الوصف: ${doc.select("meta[name=description]").attr("content")}")
        appendLine(doc.select(".markdown-body, .package-description").text().take(3000))
    }

    private fun extractMetadata(doc: Document): Map<String, String> {
        val meta = mutableMapOf<String, String>()
        doc.select("meta[property^='og:'], meta[name=author], meta[name=keywords]").forEach {
            val key = it.attr("property").ifBlank { it.attr("name") }
            val value = it.attr("content")
            if (key.isNotBlank() && value.isNotBlank()) meta[key] = value
        }
        return meta
    }

    private fun extractDomain(url: String): String = try {
        URL(url).host.removePrefix("www.")
    } catch (_: Exception) { url.take(50) }
}
