// app/src/main/java/com/nadr59/sitemanager/di/TranslationModule.kt

package com.nadr59.sitemanager.di

import com.nadr59.sitemanager.domain.translator.DefaultTranslationEngine
import com.nadr59.sitemanager.domain.translator.TranslationEngine
import com.nadr59.sitemanager.domain.translator.WebPageTranslationCoordinator
import com.nadr59.sitemanager.domain.translator.WebPageTranslator
import com.nadr59.sitemanager.data.repository.TranslationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
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
    fun provideWebPageTranslator(): WebPageTranslator {
        return WebPageTranslator()
    }

    @Provides
    @Singleton
    fun provideWebPageTranslationCoordinator(
        translationRepository: TranslationRepository,
        webPageTranslator: WebPageTranslator
    ): WebPageTranslationCoordinator {
        return WebPageTranslationCoordinator(
            translationRepository = translationRepository,
            webPageTranslator = webPageTranslator
        )
    }
}
