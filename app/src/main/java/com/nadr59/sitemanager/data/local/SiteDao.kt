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

    @Query("SELECT * FROM sites ORDER BY category, name")
    fun getAllSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE category = :category ORDER BY name")
    fun getSitesByCategory(category: String): Flow<List<SiteEntity>>

    @Query("SELECT DISTINCT category FROM sites ORDER BY category")
    fun getAllCategories(): Flow<List<String>>

    @Query("""
        SELECT * FROM sites
        WHERE name LIKE '%' || :query || '%'
           OR url LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
        ORDER BY name
    """)
    fun searchSites(query: String): Flow<List<SiteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: SiteEntity)

    @Update
    suspend fun updateSite(site: SiteEntity)
    // ═══ أضف هذا في DAO ═══
@Update
suspend fun updateSite(site: SiteEntity)

    @Delete
    suspend fun deleteSite(site: SiteEntity)

    @Query("SELECT * FROM sites WHERE id = :id")
    suspend fun getSiteById(id: Int): SiteEntity?

    @Query("SELECT * FROM sites WHERE url = :url LIMIT 1")
    suspend fun getSiteByUrl(url: String): SiteEntity?

    @Query("UPDATE sites SET visitCount = visitCount + 1 WHERE id = :id")
    suspend fun incrementVisitCount(id: Int)

    @Query("""
        UPDATE sites SET
            siteType = :siteType,
            lastAnalyzed = :timestamp,
            cachedOverview = :overview,
            cachedTechStack = :techStack,
            cachedFeatures = :features,
            aiRating = :rating,
            analysisCount = analysisCount + 1
        WHERE id = :siteId
    """)
    suspend fun saveAnalysis(
        siteId: Int,
        siteType: String,
        timestamp: Long,
        overview: String,
        techStack: String,
        features: String,
        rating: Float
    )

    @Query("SELECT * FROM sites WHERE lastAnalyzed > 0 ORDER BY lastAnalyzed DESC LIMIT 10")
    fun getRecentlyAnalyzed(): Flow<List<SiteEntity>>

    @Query("""
        UPDATE sites SET
            cachedOverview = '',
            cachedTechStack = '',
            cachedFeatures = '',
            lastAnalyzed = 0
        WHERE id = :siteId
    """)
    suspend fun clearAnalysisCache(siteId: Int)
}
