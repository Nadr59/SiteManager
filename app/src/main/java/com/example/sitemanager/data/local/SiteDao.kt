package com.example.sitemanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {

    @Query("SELECT * FROM sites WHERE tabType = 'favorites' ORDER BY clickCount DESC")
    fun getFavorites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE tabType = 'saved' ORDER BY createdAt DESC")
    fun getSaved(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY clickCount DESC")
    fun search(query: String): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): SiteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(site: SiteEntity): Long

    @Update
    suspend fun update(site: SiteEntity)

    @Delete
    suspend fun delete(site: SiteEntity)

    @Query("UPDATE sites SET tabType = :newTabType WHERE id = :id")
    suspend fun moveToTab(id: Long, newTabType: String)

    @Query("UPDATE sites SET clickCount = clickCount + 1 WHERE id = :id")
    suspend fun incrementClickCount(id: Long)

    @Query("UPDATE sites SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)
}
