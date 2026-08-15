package com.nadr59.sitemanager.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nadr59.sitemanager.data.local.SiteDao
import com.nadr59.sitemanager.data.local.SiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SiteDatabase {
        return SiteDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideSiteDao(database: SiteDatabase): SiteDao {
        return database.siteDao()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()
}
