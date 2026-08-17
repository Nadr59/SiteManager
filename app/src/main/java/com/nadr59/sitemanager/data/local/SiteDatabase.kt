package com.nadr59.sitemanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SiteEntity::class, SiteAnalysisEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SiteDatabase : RoomDatabase() {

    abstract fun siteDao(): SiteDao

    companion object {
        @Volatile
        private var INSTANCE: SiteDatabase? = null

        // ═══ Migration من الإصدار 1 إلى 2 ═══
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // إضافة أعمدة جديدة لجدول sites
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

                // إنشاء جدول التحليلات
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

                // تحديث createdAt للمواقع الموجودة
                db.execSQL("UPDATE sites SET createdAt = ${System.currentTimeMillis()} WHERE createdAt = 0")
            }
        }

        fun getDatabase(context: Context): SiteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SiteDatabase::class.java,
                    "site_manager_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
