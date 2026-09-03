package com.nadr59.sitemanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserDao {

    // ═══ التاريخ ═══
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: BrowserHistory)

    @Query("SELECT * FROM browser_history ORDER BY visitedAt DESC LIMIT 100")
    fun getAllHistory(): Flow<List<BrowserHistory>>

    @Query("SELECT * FROM browser_history WHERE siteId = :siteId ORDER BY visitedAt DESC")
    fun getHistoryForSite(siteId: Int): Flow<List<BrowserHistory>>

    @Query("DELETE FROM browser_history WHERE id = :id")
    suspend fun deleteHistory(id: Int)

    @Query("DELETE FROM browser_history")
    suspend fun clearAllHistory()

    // ═══ الإشارات المرجعية ═══
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BrowserBookmark)

    @Query("SELECT * FROM browser_bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BrowserBookmark>>

    @Query("SELECT * FROM browser_bookmarks WHERE url = :url LIMIT 1")
    suspend fun getBookmarkByUrl(url: String): BrowserBookmark?

    @Query("DELETE FROM browser_bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Int)

    @Query("SELECT COUNT(*) FROM browser_bookmarks WHERE url = :url")
    suspend fun isBookmarked(url: String): Int

    // ═══ ذاكرة الترجمة ═══
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslationCache(cache: TranslationCache)

    @Query(
        "SELECT * FROM translation_cache " +
        "WHERE originalText = :text AND targetLanguage = :lang LIMIT 1"
    )
    suspend fun getCachedTranslation(text: String, lang: String): TranslationCache?

    @Query("DELETE FROM translation_cache WHERE cachedAt < :before")
    suspend fun clearOldCache(before: Long)

    // ═══ ملاحظات الصفحة ═══
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: PageNote)

    @Update
    suspend fun updateNote(note: PageNote)

    @Query("DELETE FROM page_notes WHERE id = :id")
    suspend fun deleteNote(id: Int)

    @Query("SELECT * FROM page_notes WHERE url = :url ORDER BY updatedAt DESC")
    fun getNotesForUrl(url: String): Flow<List<PageNote>>

    @Query("SELECT * FROM page_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<PageNote>>

    @Query("SELECT COUNT(*) FROM page_notes WHERE url = :url")
    suspend fun getNoteCountForUrl(url: String): Int

    // ═══════════════════════════════════════
    // الصفحات المحفوظة للقراءة لاحقاً
    // ═══════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPage(page: SavedPage)

    @Query("SELECT * FROM saved_pages ORDER BY savedAt DESC")
    fun getAllSavedPages(): Flow<List<SavedPage>>

    @Query("SELECT * FROM saved_pages WHERE url = :url LIMIT 1")
    suspend fun getSavedPageByUrl(url: String): SavedPage?

    @Query("SELECT COUNT(*) FROM saved_pages WHERE url = :url")
    suspend fun isPageSaved(url: String): Int

    @Query("DELETE FROM saved_pages WHERE id = :id")
    suspend fun deleteSavedPage(id: Int)

    @Query("SELECT COUNT(*) FROM saved_pages")
    suspend fun getSavedPagesCount(): Int
}
