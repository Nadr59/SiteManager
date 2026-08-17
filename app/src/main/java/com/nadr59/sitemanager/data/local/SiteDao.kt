package com.nadr59.sitemanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {

    // ═══ عمليات CRUD الأساسية ═══
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: SiteEntity): Long

    @Update
    suspend fun updateSite(site: SiteEntity)

    @Delete
    suspend fun deleteSite(site: SiteEntity)

    @Query("DELETE FROM sites WHERE id = :id")
    suspend fun deleteSiteById(id: Int)

    // ═══ الاستعلامات ═══
    @Query("SELECT * FROM sites ORDER BY isPinned DESC, createdAt DESC")
    fun getAllSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE id = :id")
    suspend fun getSiteById(id: Int): SiteEntity?

    @Query("SELECT * FROM sites WHERE id = :id")
    fun getSiteByIdFlow(id: Int): Flow<SiteEntity?>

    @Query("SELECT * FROM sites WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavorites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE isPinned = 1 ORDER BY name ASC")
    fun getPinnedSites(): Flow<List<SiteEntity>>

    // ═══ البحث الذكي ═══
    @Query("""
        SELECT * FROM sites WHERE
            name LIKE '%' || :query || '%' OR
            url LIKE '%' || :query || '%' OR
            notes LIKE '%' || :query || '%' OR
            description LIKE '%' || :query || '%' OR
            tags LIKE '%' || :query || '%' OR
            category LIKE '%' || :query || '%' OR
            cachedOverview LIKE '%' || :query || '%' OR
            pageTitle LIKE '%' || :query || '%' OR
            pageDescription LIKE '%' || :query || '%'
        ORDER BY isPinned DESC, createdAt DESC
    """)
    fun searchSites(query: String): Flow<List<SiteEntity>>

    // ═══ الفرز ═══
    @Query("SELECT * FROM sites ORDER BY isPinned DESC, visitCount DESC")
    fun getMostVisited(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites ORDER BY isPinned DESC, lastVisited DESC")
    fun getRecentlyOpened(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites ORDER BY isPinned DESC, name ASC")
    fun getSortedByName(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites ORDER BY isPinned DESC, category ASC")
    fun getSortedByCategory(): Flow<List<SiteEntity>>

    // ═══ الإحصائيات ═══
    @Query("SELECT COUNT(*) FROM sites")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sites WHERE isFavorite = 1")
    fun getFavoriteCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT category) FROM sites")
    fun getCategoryCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sites WHERE visitCount > 0")
    fun getVisitedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sites WHERE cachedOverview != ''")
    fun getAnalyzedCount(): Flow<Int>

    @Query("SELECT category, COUNT(*) as count FROM sites GROUP BY category ORDER BY count DESC LIMIT 5")
    fun getTopCategories(): Flow<List<CategoryCount>>

    @Query("SELECT * FROM sites ORDER BY visitCount DESC LIMIT 5")
    fun getTopVisited(): Flow<List<SiteEntity>>

    // ═══ تحديث الحقول ═══
    @Query("UPDATE sites SET visitCount = visitCount + 1, lastVisited = :time WHERE id = :id")
    suspend fun incrementVisit(id: Int, time: Long = System.currentTimeMillis())

    @Query("UPDATE sites SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Int, isFavorite: Boolean)

    @Query("UPDATE sites SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Int, isPinned: Boolean)

    @Query("UPDATE sites SET lastChecked = :time, httpStatus = :status, pageTitle = :title, pageDescription = :desc WHERE id = :id")
    suspend fun updateCheckResult(id: Int, time: Long, status: Int, title: String, desc: String)

    // ═══ التحليلات ═══
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: SiteAnalysisEntity)

    @Query("SELECT * FROM site_analyses WHERE siteId = :siteId ORDER BY createdAt DESC")
    fun getAnalysesForSite(siteId: Int): Flow<List<SiteAnalysisEntity>>

    @Query("SELECT * FROM site_analyses WHERE siteId = :siteId AND analysisType = :type ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestAnalysis(siteId: Int, type: String): SiteAnalysisEntity?

    @Query("SELECT * FROM site_analyses WHERE siteId = :siteId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestAnalysisAny(siteId: Int): SiteAnalysisEntity?

    @Query("DELETE FROM site_analyses WHERE siteId = :siteId")
    suspend fun deleteAnalysesForSite(siteId: Int)

    // ═══ التصنيفات ═══
    @Query("SELECT DISTINCT category FROM sites ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    // ═══ فحص التكرار ═══
    @Query("SELECT COUNT(*) FROM sites WHERE url = :url")
    suspend fun countByUrl(url: String): Int
}

data class CategoryCount(
    val category: String,
    val count: Int
)
