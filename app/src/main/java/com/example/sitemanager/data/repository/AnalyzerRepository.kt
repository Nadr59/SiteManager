package com.nadr59.sitemanager.data.repository

import com.nadr59.sitemanager.data.local.SiteDao
import com.nadr59.sitemanager.data.remote.AiConfig
import com.nadr59.sitemanager.data.remote.AiService
import com.nadr59.sitemanager.data.remote.AnalysisResult
import com.nadr59.sitemanager.data.remote.WebScraper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyzerRepository @Inject constructor(
    private val webScraper: WebScraper,
    private val aiService: AiService,
    private val siteDao: SiteDao
) {
    suspend fun analyze(siteId: Int, config: AiConfig): Result<AnalysisResult> {
        return try {
            val site = siteDao.getSiteById(siteId)
                ?: return Result.failure(Exception("الموقع غير موجود"))

            val content = webScraper.scrape(site.url)
            val result = aiService.analyzeSite(content, config)

            siteDao.saveAnalysis(
                siteId = siteId,
                siteType = content.type.name,
                timestamp = System.currentTimeMillis(),
                overview = result.toCachedOverview(),
                techStack = result.toCachedTechStack(),
                features = result.toCachedFeatures(),
                rating = result.rating
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(Exception("خطأ في التحليل: ${e.message}"))
        }
    }

    suspend fun getCachedAnalysis(siteId: Int): AnalysisResult? {
        val site = siteDao.getSiteById(siteId) ?: return null
        if (site.lastAnalyzed == 0L || site.cachedOverview.isBlank()) return null
        return AnalysisResult.fromCache(
            overview = site.cachedOverview,
            techStack = site.cachedTechStack,
            features = site.cachedFeatures,
            rating = site.aiRating
        )
    }

    suspend fun isAnalysisStale(siteId: Int): Boolean {
        val site = siteDao.getSiteById(siteId) ?: return true
        if (site.lastAnalyzed == 0L) return true
        val dayMs = 24 * 60 * 60 * 1000
        return (System.currentTimeMillis() - site.lastAnalyzed) > dayMs
    }

    suspend fun clearCache(siteId: Int) {
        siteDao.clearAnalysisCache(siteId)
    }
}
