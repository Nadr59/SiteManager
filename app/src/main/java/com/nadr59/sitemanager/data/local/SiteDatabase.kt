package com.nadr59.sitemanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SiteEntity::class,
        SiteAnalysisEntity::class,
        BrowserHistory::class,
        BrowserBookmark::class,
        TranslationCache::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SiteDatabase : RoomDatabase() {

    abstract fun siteDao(): SiteDao
    abstract fun browserDao(): BrowserDao

    companion object {
        @Volatile
        private var INSTANCE: SiteDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sites ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sites ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sites ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sites ADD COLUMN faviconUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sites ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sites ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sites ADD COLUMN lastAnalyzed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sites ADD COLUMN lastChecked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sites ADD COLUMN httpStatus INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sites ADD COLUMN pageTitle TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sites ADD COLUMN pageDescription TEXT NOT NULL DEFAULT ''")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS site_analyses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        siteId INTEGER NOT NULL,
                        analysisType TEXT NOT NULL,
                        result TEXT NOT NULL,
                        rating REAL NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (siteId) REFERENCES sites(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_site_analyses_siteId ON site_analyses(siteId)")
                db.execSQL("UPDATE sites SET createdAt = ${System.currentTimeMillis()} WHERE createdAt = 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ═══ جدول التاريخ ═══
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS browser_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        url TEXT NOT NULL,
                        title TEXT NOT NULL,
                        visitedAt INTEGER NOT NULL DEFAULT 0,
                        siteId INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // ═══ جدول الإشارات المرجعية ═══
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS browser_bookmarks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        url TEXT NOT NULL,
                        title TEXT NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        siteId INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // ═══ جدول ذاكرة الترجمة ═══
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS translation_cache (
                        id TEXT PRIMARY KEY NOT NULL,
                        originalText TEXT NOT NULL,
                        translatedText TEXT NOT NULL,
                        targetLanguage TEXT NOT NULL,
                        cachedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): SiteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SiteDatabase::class.java,
                    "site_manager_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
