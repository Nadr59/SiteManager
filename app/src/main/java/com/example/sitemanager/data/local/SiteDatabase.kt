package com.nadr59.sitemanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SiteEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SiteDatabase : RoomDatabase() {

    abstract fun siteDao(): SiteDao

    companion object {
        @Volatile
        private var INSTANCE: SiteDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sites ADD COLUMN siteType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sites ADD COLUMN lastAnalyzed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sites ADD COLUMN cachedOverview TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sites ADD COLUMN cachedTechStack TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sites ADD COLUMN cachedFeatures TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sites ADD COLUMN aiRating REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sites ADD COLUMN analysisCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): SiteDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    SiteDatabase::class.java,
                    "site_manager_db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
