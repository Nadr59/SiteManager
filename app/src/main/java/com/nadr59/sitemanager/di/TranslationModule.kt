package com.nadr59.sitemanager.di

import com.nadr59.sitemanager.domain.translator.InkTranslationEngine
import com.nadr59.sitemanager.domain.translator.TranslationEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TranslationModule {

    @Binds
    @Singleton
    abstract fun bindTranslationEngine(
        inkTranslationEngine: InkTranslationEngine
    ): TranslationEngine
}
