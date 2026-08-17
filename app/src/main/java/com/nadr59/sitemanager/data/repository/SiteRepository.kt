package com.nadr59.sitemanager.data.repository

import com.nadr59.sitemanager.data.local.SiteAnalysisEntity
import com.nadr59.sitemanager.data.local.SiteDao
import com.nadr59.sitemanager.data.local.SiteEntity
import com.nadr59.sitemanager.data.local.AnalysisType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteRepository @Inject constructor(
    private val siteDao: SiteDao
) {
    // ═══ عمليات CRUD ═══
    suspend fun insertSite(site: SiteEntity): Long = siteDao.insertSite(site)
    suspend fun updateSite(site: SiteEntity) = siteDao.updateSite(site)
    suspend fun deleteSite(site: SiteEntity) = siteDao.deleteSite(site)
    suspend fun deleteSiteById(id: Int) = siteDao.deleteSiteById(id)
    suspend fun getSiteById(id: Int): SiteEntity? = siteDao.getSiteById(id)
    fun getSiteByIdFlow(id: Int): Flow<SiteEntity?> = siteDao.getSiteByIdFlow(id)

    // ═══ القوائم ═══
    fun getAllSites(): Flow<List<SiteEntity>> = siteDao.getAllSites()
    fun getFavorites(): Flow<List<SiteEntity>> = siteDao.getFavorites()

    // ═══ البحث ═══
    fun searchSites(query: String): Flow<List<SiteEntity>> = siteDao.searchSites(query)

    // ═══ الفرز ═══
    fun getMostVisited(): Flow<List<SiteEntity>> = siteDao.getMostVisited()
    fun getRecentlyOpened(): Flow<List<SiteEntity>> = siteDao.getRecentlyOpened()
    fun getSortedByName(): Flow<List<SiteEntity>> = siteDao.getSortedByName()
    fun getSortedByCategory(): Flow<List<SiteEntity>> = siteDao.getSortedByCategory()

    // ═══ الإجراءات ═══
    suspend fun incrementVisit(id: Int) = siteDao.incrementVisit(id)
    suspend fun setFavorite(id: Int, isFavorite: Boolean) = siteDao.setFavorite(id, isFavorite)
    suspend fun setPinned(id: Int, isPinned: Boolean) = siteDao.setPinned(id, isPinned)
    suspend fun countByUrl(url: String): Int = siteDao.countByUrl(url)

    suspend fun updateCheckResult(
        id: Int, status: Int, title: String, desc: String
    ) = siteDao.updateCheckResult(id, System.currentTimeMillis(), status, title, desc)

    // ═══ الإحصائيات ═══
    fun getTotalCount(): Flow<Int> = siteDao.getTotalCount()
    fun getFavoriteCount(): Flow<Int> = siteDao.getFavoriteCount()
    fun getCategoryCount(): Flow<Int> = siteDao.getCategoryCount()
    fun getVisitedCount(): Flow<Int> = siteDao.getVisitedCount()
    fun getAnalyzedCount(): Flow<Int> = siteDao.getAnalyzedCount()
    fun getTopCategories() = siteDao.getTopCategories()
    fun getTopVisited(): Flow<List<SiteEntity>> = siteDao.getTopVisited()

    // ═══ التصنيفات ═══
    fun getAllCategories(): Flow<List<String>> = siteDao.getAllCategories()

    // ═══ التحليلات ═══
    suspend fun insertAnalysis(analysis: SiteAnalysisEntity) = siteDao.insertAnalysis(analysis)
    fun getAnalysesForSite(siteId: Int): Flow<List<SiteAnalysisEntity>> = siteDao.getAnalysesForSite(siteId)
    suspend fun getLatestAnalysis(siteId: Int, type: String) = siteDao.getLatestAnalysis(siteId, type)
    suspend fun getLatestAnalysisAny(siteId: Int) = siteDao.getLatestAnalysisAny(siteId)
}
