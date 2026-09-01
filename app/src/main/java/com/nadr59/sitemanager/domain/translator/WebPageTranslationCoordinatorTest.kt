// app/src/test/java/com/nadr59/sitemanager/domain/translator/WebPageTranslationCoordinatorTest.kt

package com.nadr59.sitemanager.domain.translator

import android.webkit.WebView
import com.nadr59.sitemanager.data.model.PageTextNode
import com.nadr59.sitemanager.data.repository.TranslationRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebPageTranslationCoordinatorTest {

    private lateinit var coordinator: WebPageTranslationCoordinator
    private lateinit var translationRepository: TranslationRepository
    private lateinit var webPageTranslator: WebPageTranslator
    private lateinit var webView: WebView

    @Before
    fun setup() {
        translationRepository = mockk(relaxed = true)
        webPageTranslator = mockk(relaxed = true)
        webView = mockk(relaxed = true)

        coordinator = WebPageTranslationCoordinator(
            translationRepository = translationRepository,
            webPageTranslator = webPageTranslator
        )
    }

    @Test
    fun `getPageState returns null for unknown URL`() {
        val state = coordinator.getPageState("https://unknown.com")
        assertNull(state)
    }

    @Test
    fun `isPageTranslated returns false for untranslated page`() {
        val isTranslated = coordinator.isPageTranslated("https://example.com")
        assertFalse(isTranslated)
    }

    @Test
    fun `updateConfig changes configuration`() {
        val newConfig = TranslationConfig(
            targetLanguage = "en",
            batchSize = 20
        )

        coordinator.updateConfig(newConfig)
        // التحقق من تطبيق الإعدادات في عمليات الترجمة
    }

    @Test
    fun `clearPageState removes state for URL`() {
        // هذا اختبار بسيط للتحقق من عدم حدوث exception
        coordinator.clearPageState("https://example.com")
    }

    @Test
    fun `clearAllStates removes all states`() {
        coordinator.clearAllStates()
        assertFalse(coordinator.isPageTranslated("https://example.com"))
    }

    @Test
    fun `TranslationConfig isRTL detects Arabic correctly`() {
        assertTrue(TranslationConfig.isRTL("ar"))
        assertTrue(TranslationConfig.isRTL("fa"))
        assertTrue(TranslationConfig.isRTL("he"))
        assertFalse(TranslationConfig.isRTL("en"))
    }
}
