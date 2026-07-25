package com.example.sitemanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SiteEntity::class], version = 1, exportSchema = false)
abstract class SiteDatabase : RoomDatabase() {

    abstract fun siteDao(): SiteDao

    companion object {
        @Volatile
        private var INSTANCE: SiteDatabase? = null

        fun getDatabase(context: Context): SiteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SiteDatabase::class.java,
                    "site_manager_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
