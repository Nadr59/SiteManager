package com.nadr59.sitemanager.di

import com.nadr59.sitemanager.domain.translator.DefaultTranslationEngine
import com.nadr59.sitemanager.domain.translator.TranslationEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TranslationModule {

    @Binds
    abstract fun bindTranslationEngine(
        implementation: DefaultTranslationEngine
    ): TranslationEngine
}
