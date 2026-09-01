package com.nadr59.sitemanager.di

import com.nadr59.sitemanager.data.repository.TranslationRepository
import com.nadr59.sitemanager.domain.translator.DefaultTranslationEngine
import com.nadr59.sitemanager.domain.translator.TranslationEngine
import com.nadr59.sitemanager.domain.translator.WebPageTranslationCoordinator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TranslationModule {

    @Binds
    @Singleton
    abstract fun bindTranslationEngine(
        implementation: DefaultTranslationEngine
    ): TranslationEngine
}

@Module
@InstallIn(SingletonComponent::class)
object TranslationProviderModule {

    @Provides
    @Singleton
    fun provideWebPageTranslationCoordinator(
        repository: TranslationRepository
    ): WebPageTranslationCoordinator {
        return WebPageTranslationCoordinator(repository)
    }
}
