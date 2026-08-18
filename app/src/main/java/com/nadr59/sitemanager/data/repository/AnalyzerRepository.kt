package com.nadr59.sitemanager.data.repository

import com.nadr59.sitemanager.data.local.AnalysisType
import com.nadr59.sitemanager.data.local.SiteAnalysisEntity
import com.nadr59.sitemanager.data.local.SiteDao
import com.nadr59.sitemanager.data.remote.ApiClient
import com.nadr59.sitemanager.data.remote.AnalysisResult
import com.nadr59.sitemanager.data.remote.ProsAndCons
import com.nadr59.sitemanager.data.remote.WebScraper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyzerRepository @Inject constructor(
    private val scraper: WebScraper,
    private val client: ApiClient,
    private val dao: SiteDao
) {
    suspend fun analyze(
        siteId: Int,
        analysisType: AnalysisType = AnalysisType.EXPLAIN,
        customQuestion: String = ""
    ): Result<AnalysisResult> {
        return try {
            val site = dao.getSiteById(siteId)
                ?: return Result.failure(Exception("الموقع غير موجود"))

            val content = scraper.scrape(site.url)

            val prompt = buildPrompt(
                url = content.url,
                title = content.title,
                description = content.description,
                rawContent = content.rawContent,
                isHttps = content.isHttps,
                type = analysisType,
                customQuestion = customQuestion
            )

            val response = client.ask(prompt)

            if (!response.success) {
                return Result.failure(Exception(response.error))
            }

            val result = parseResponse(response.response, analysisType)

            dao.insertAnalysis(
                SiteAnalysisEntity(
                    siteId = siteId,
                    analysisType = analysisType.key,
                    result = response.response,
                    rating = result.rating
                )
            )

            dao.updateCheckResult(
                id = siteId,
                time = System.currentTimeMillis(),
                status = content.statusCode,
                title = content.title ?: "",
                desc = content.description ?: ""
            )

            dao.updateSite(
                site.copy(
                    lastAnalyzed = System.currentTimeMillis(),
                    cachedOverview = result.overview,
                    aiRating = result.rating,
                    pageTitle = content.title ?: "",
                    pageDescription = content.description ?: "",
                    lastChecked = System.currentTimeMillis(),
                    httpStatus = content.statusCode,
                    description = content.description ?: ""
                )
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    
        private fun buildPrompt(
        url: String,
        title: String?,
        description: String?,
        rawContent: String,
        isHttps: Boolean,
        type: AnalysisType,
        customQuestion: String
    ): String {
        val typeInstruction = when (type) {
            AnalysisType.CUSTOM -> customQuestion
            else -> type.promptPrefix
        }

        // تنظيف المحتوى وإزالة التكرار
        val cleanContent = rawContent
            .replace(Regex("\\s+"), " ")
            .replace(Regex("(.{50,})\\1+"), "$1")  // إزالة التكرار
            .take(3000)
            .trim()

        return """أنت محلل مواقع ذكي ومختصر.

حلل هذا الموقع: "$url"
${title?.let { "عنوان الصفحة: $it" } ?: ""}
${description?.let { "وصف الصفحة: $it" } ?: ""}
${if (isHttps) "الموقع يستخدم HTTPS (اتصال آمن)" else "الموقع لا يستخدم HTTPS"}

نوع التحليل المطلوب: ${type.displayName}

$typeInstruction

قواعد مهمة:
- لا تكرر نفس الجملة أو الفكرة أكثر من مرة
- كن دقيقاً ولا تختلق معلومات غير موجودة
- إذا لم تجد معلومة قل "غير متوفر" بدل الافتراض
- أجب بالعربية
- استخدم تنسيق Markdown مع عناوين واضحة
- كن مختصراً ومفيداً

محتوى الموقع (مقتطع):
$cleanContent"""
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

    suspend fun getCachedAnalysis(siteId: Int): AnalysisResult? {
        val entity = dao.getLatestAnalysisAny(siteId) ?: return null
        return AnalysisResult(
            overview = entity.result.take(500),
            rawMarkdown = entity.result,
            rating = entity.rating,
            analysisType = entity.analysisType
        )
    }

    suspend fun getCachedAnalysis(siteId: Int, type: AnalysisType): AnalysisResult? {
        val entity = dao.getLatestAnalysis(siteId, type.key) ?: return null
        return AnalysisResult(
            overview = entity.result.take(500),
            rawMarkdown = entity.result,
            rating = entity.rating,
            analysisType = entity.analysisType
        )
    }

    suspend fun checkSiteStatus(siteId: Int): Boolean {
        val site = dao.getSiteById(siteId) ?: return false
        return try {
            val content = scraper.checkUrl(site.url)
            if (content != null) {
                dao.updateCheckResult(
                    id = siteId,
                    time = System.currentTimeMillis(),
                    status = content.statusCode,
                    title = content.title ?: "",
                    desc = content.description ?: ""
                )
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun clearCache(siteId: Int) {
        dao.deleteAnalysesForSite(siteId)
        val site = dao.getSiteById(siteId) ?: return
        dao.updateSite(site.copy(cachedOverview = "", aiRating = 0f, lastAnalyzed = 0L))
    }
}
