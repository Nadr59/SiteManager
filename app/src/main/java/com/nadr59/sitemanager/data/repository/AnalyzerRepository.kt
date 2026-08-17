package com.nadr59.sitemanager.data.repository

import com.nadr59.sitemanager.data.local.AnalysisType
import com.nadr59.sitemanager.data.local.SiteAnalysisEntity
import com.nadr59.sitemanager.data.local.SiteDao
import com.nadr59.sitemanager.data.remote.AiConfig
import com.nadr59.sitemanager.data.remote.AiService
import com.nadr59.sitemanager.data.remote.AnalysisResult
import com.nadr59.sitemanager.data.remote.WebScraper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyzerRepository @Inject constructor(
    private val scraper: WebScraper,
    private val ai: AiService,
    private val dao: SiteDao
) {
    suspend fun analyze(
        siteId: Int,
        config: AiConfig,
        analysisType: AnalysisType = AnalysisType.EXPLAIN,
        customQuestion: String = "",
        forceRefresh: Boolean = false
    ): Result<AnalysisResult> {
        return try {
            val site = dao.getSiteById(siteId)
                ?: return Result.failure(Exception("الموقع غير موجود"))

            val content = scraper.scrape(site.url)
            val result = ai.analyzeSite(content, config, analysisType, customQuestion)

            // حفظ التحليل
            dao.insertAnalysis(
                SiteAnalysisEntity(
                    siteId = siteId,
                    analysisType = analysisType.key,
                    result = result.rawMarkdown,
                    rating = result.rating
                )
            )

            // ✅ تحديث فحص الموقع مع time
            dao.updateCheckResult(
                id = siteId,
                time = System.currentTimeMillis(),
                status = content.statusCode,
                title = content.title ?: "",
                desc = content.description ?: ""
            )

            // تحديث بيانات الموقع
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
                // ✅ تحديث فحص الموقع مع time
                dao.updateCheckResult(
                    id = siteId,
                    time = System.currentTimeMillis(),
                    status = content.statusCode,
                    title = content.title ?: "",
                    desc = content.description ?: ""
                )
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    // ✅ مسح ذاكرة التخزين المؤقت
    suspend fun clearCache(siteId: Int) {
        dao.deleteAnalysesForSite(siteId)
        val site = dao.getSiteById(siteId) ?: return
        dao.updateSite(
            site.copy(
                cachedOverview = "",
                aiRating = 0f,
                lastAnalyzed = 0L
            )
        )
    }
}
