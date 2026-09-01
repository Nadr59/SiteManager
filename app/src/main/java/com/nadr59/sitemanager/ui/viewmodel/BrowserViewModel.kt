// app/src/main/java/com/nadr59/sitemanager/ui/viewmodel/BrowserViewModel.kt

package com.nadr59.sitemanager.ui.viewmodel

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nadr59.sitemanager.data.local.BrowserBookmark
import com.nadr59.sitemanager.data.local.BrowserHistory
import com.nadr59.sitemanager.data.local.PageNote
import com.nadr59.sitemanager.data.model.BrowserState
import com.nadr59.sitemanager.data.remote.ApiClient
import com.nadr59.sitemanager.data.remote.WebScraper
import com.nadr59.sitemanager.data.repository.SiteRepository
import com.nadr59.sitemanager.data.repository.TranslationRepository
import com.nadr59.sitemanager.domain.translator.TranslationOperation
import com.nadr59.sitemanager.domain.translator.WebPageTranslationCoordinator
import com.nadr59.sitemanager.domain.translator.WebPageTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val translationRepository: TranslationRepository,
    private val translationCoordinator: WebPageTranslationCoordinator
) : ViewModel() {

    // الخدمات القديمة (نبقيها للتوافق)
    private val pageTranslator = WebPageTranslator()
    private val apiClient = ApiClient()
    private val webScraper = WebScraper()

    // WebView reference
    private var currentWebView: WebView? = null

    // حالة UI
    private val _uiState = MutableStateFlow(BrowserState())
    val uiState: StateFlow<BrowserState> = _uiState.asStateFlow()

    // JavaScript معلق
    private val _pendingJs = MutableStateFlow<String?>(null)
    val pendingJs: StateFlow<String?> = _pendingJs.asStateFlow()

    // ملاحظات الصفحة
    private val _pageNotes = MutableStateFlow<List<PageNote>>(emptyList())
    val pageNotes: StateFlow<List<PageNote>> = _pageNotes.asStateFlow()

    // ملخص الصفحة
    private val _pageSummary = MutableStateFlow<String?>(null)
    val pageSummary: StateFlow<String?> = _pageSummary.asStateFlow()

    // محادثة AI
    private val _aiMessages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val aiMessages: StateFlow<List<Pair<String, String>>> = _aiMessages.asStateFlow()

    // لقطة الشاشة
    private val _screenshot = MutableStateFlow<android.graphics.Bitmap?>(null)
    val screenshot: StateFlow<android.graphics.Bitmap?> = _screenshot.asStateFlow()

    // History
    private val _history = MutableStateFlow<List<BrowserHistory>>(emptyList())
    val history: StateFlow<List<BrowserHistory>> = _history.asStateFlow()

    // Bookmarks
    private val _bookmarks = MutableStateFlow<List<BrowserBookmark>>(emptyList())
    val bookmarks: StateFlow<List<BrowserBookmark>> = _bookmarks.asStateFlow()

    // Reader Mode
    private val _readerModeContent = MutableStateFlow<String?>(null)
    val readerModeContent: StateFlow<String?> = _readerModeContent.asStateFlow()

    // معرف الموقع الحالي
    private var currentSiteId: Int? = null

    init {
        loadHistory()
        loadBookmarks()
    }

    // ==================== WebView Management ====================

    fun registerWebView(webView: WebView) {
        currentWebView = webView
        Timber.d("تم تسجيل WebView")
    }

    fun unregisterWebView() {
        currentWebView = null
        Timber.d("تم إلغاء تسجيل WebView")
    }

    // ==================== Site Loading ====================

    fun loadSite(siteId: Int) {
        currentSiteId = siteId

        viewModelScope.launch {
            try {
                val site = siteRepository.getSiteById(siteId)

                if (site != null) {
                    _uiState.update { it.copy(url = site.url, title = site.name) }
                    siteRepository.incrementVisitCount(siteId)
                    loadPageNotes(site.url)
                }
            } catch (e: Exception) {
                Timber.e(e, "فشل في تحميل الموقع")
            }
        }
    }

    // ==================== Translation ====================

    fun startPageTranslationWithCoordinator() {
        val webView = currentWebView
        if (webView == null) {
            Timber.e("WebView غير مسجل")
            _uiState.update { it.copy(error = "خطأ داخلي: WebView غير متوفر") }
            return
        }

        val currentUrl = _uiState.value.url
        if (currentUrl.isBlank()) {
            Timber.e("لا يوجد URL للترجمة")
            return
        }

        _uiState.update {
            it.copy(
                isTranslating = true,
                translationProgress = 0f,
                error = null,
                showTranslationSheet = false
            )
        }

        viewModelScope.launch {
            try {
                translationCoordinator.translatePage(
                    webView = webView,
                    url = currentUrl,
                    targetLanguage = _uiState.value.targetLanguage,
                    onProgress = { operation ->
                        when (operation) {
                            is TranslationOperation.Progress -> {
                                _uiState.update {
                                    it.copy(
                                        translationProgress = operation.percentage / 100f
                                    )
                                }
                                Timber.d("تقدم الترجمة: ${operation.current}/${operation.total}")
                            }
                            is TranslationOperation.Success -> {
                                _uiState.update {
                                    it.copy(
                                        isTranslating = false,
                                        isTranslationMode = true,
                                        translationProgress = 1f,
                                        error = null
                                    )
                                }
                                Timber.d("نجحت الترجمة: ${operation.state.translatedCount} عقدة")
                            }
                            is TranslationOperation.Failure -> {
                                _uiState.update {
                                    it.copy(
                                        isTranslating = false,
                                        error = operation.error
                                    )
                                }
                                Timber.e("فشلت الترجمة: ${operation.error}")
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "خطأ في ترجمة الصفحة")
                _uiState.update {
                    it.copy(
                        isTranslating = false,
                        error = e.message ?: "فشل في ترجمة الصفحة"
                    )
                }
            }
        }
    }

    fun translateSelectionWithCoordinator() {
        val webView = currentWebView
        if (webView == null) {
            Timber.e("WebView غير مسجل")
            return
        }

        _uiState.update { it.copy(isTranslating = true) }

        viewModelScope.launch {
            try {
                val result = translationCoordinator.translateSelection(
                    webView = webView,
                    targetLanguage = _uiState.value.targetLanguage
                )

                if (result.isSuccess) {
                    val (original, translated) = result.getOrThrow()
                    Timber.d("تمت ترجمة النص المحدد: ${original.take(30)}... -> ${translated.take(30)}...")
                } else {
                    Timber.e("فشل في ترجمة النص المحدد")
                }

                _uiState.update { it.copy(isTranslating = false) }

            } catch (e: Exception) {
                Timber.e(e, "خطأ في ترجمة النص المحدد")
                _uiState.update {
                    it.copy(
                        isTranslating = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun restoreOriginalWithCoordinator() {
        val webView = currentWebView
        if (webView == null) {
            Timber.e("WebView غير مسجل")
            return
        }

        val currentUrl = _uiState.value.url
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            try {
                translationCoordinator.restoreOriginalPage(
                    webView = webView,
                    url = currentUrl
                ).onSuccess {
                    _uiState.update {
                        it.copy(
                            isTranslationMode = false,
                            translationProgress = 0f
                        )
                    }
                    Timber.d("تمت استعادة النص الأصلي")
                }.onFailure { e ->
                    Timber.e(e, "فشل في استعادة النص الأصلي")
                }
            } catch (e: Exception) {
                Timber.e(e, "خطأ في استعادة النص الأصلي")
            }
        }
    }

    // دوال للتوافق مع الكود القديم
    fun startPageTranslation() = startPageTranslationWithCoordinator()
    fun translateSelectedText() = translateSelectionWithCoordinator()
    fun resetTranslation() = restoreOriginalWithCoordinator()

    // ==================== UI State Updates ====================

    fun updateUrl(url: String) {
        _uiState.update { it.copy(url = url) }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateLoadingState(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    fun updateProgress(progress: Int) {
        _uiState.update { it.copy(progress = progress) }
    }

    fun updateNavigationState(canGoBack: Boolean, canGoForward: Boolean) {
        _uiState.update { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
    }

    fun showTranslationSheet() {
        _uiState.update { it.copy(showTranslationSheet = true) }
    }

    fun hideTranslationSheet() {
        _uiState.update { it.copy(showTranslationSheet = false) }
    }

    fun setTargetLanguage(language: String) {
        _uiState.update { it.copy(targetLanguage = language) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun consumePendingJs() {
        _pendingJs.value = null
    }

    // ==================== History ====================

    private fun loadHistory() {
        viewModelScope.launch {
            translationRepository.getAllHistory().collect { historyList ->
                _history.value = historyList
            }
        }
    }

    fun addToHistory(url: String, title: String) {
        viewModelScope.launch {
            try {
                val history = BrowserHistory(
                    url = url,
                    title = title,
                    visitedAt = System.currentTimeMillis(),
                    siteId = currentSiteId
                )
                translationRepository.insertHistory(history)
            } catch (e: Exception) {
                Timber.e(e, "فشل في إضافة التاريخ")
            }
        }
    }

    fun deleteHistory(history: BrowserHistory) {
        viewModelScope.launch {
            translationRepository.deleteHistory(history)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            translationRepository.clearAllHistory()
        }
    }

    // ==================== Bookmarks ====================

    private fun loadBookmarks() {
        viewModelScope.launch {
            translationRepository.getAllBookmarks().collect { bookmarkList ->
                _bookmarks.value = bookmarkList
            }
        }
    }

    fun addBookmark(url: String, title: String) {
        viewModelScope.launch {
            try {
                val bookmark = BrowserBookmark(
                    url = url,
                    title = title,
                    createdAt = System.currentTimeMillis(),
                    siteId = currentSiteId
                )
                translationRepository.insertBookmark(bookmark)
            } catch (e: Exception) {
                Timber.e(e, "فشل في إضافة الإشارة المرجعية")
            }
        }
    }

    fun deleteBookmark(bookmark: BrowserBookmark) {
        viewModelScope.launch {
            translationRepository.deleteBookmark(bookmark)
        }
    }

    // ==================== Page Notes ====================

    private fun loadPageNotes(url: String) {
        viewModelScope.launch {
            translationRepository.getPageNotes(url).collect { notes ->
                _pageNotes.value = notes
            }
        }
    }

    fun addPageNote(url: String, title: String, note: String) {
        viewModelScope.launch {
            try {
                val pageNote = PageNote(
                    url = url,
                    title = title,
                    note = note,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                translationRepository.insertPageNote(pageNote)
            } catch (e: Exception) {
                Timber.e(e, "فشل في إضافة الملاحظة")
            }
        }
    }

    fun deletePageNote(note: PageNote) {
        viewModelScope.launch {
            translationRepository.deletePageNote(note)
        }
    }

    // ==================== AI Features ====================

    fun summarizePage() {
        viewModelScope.launch {
            try {
                val url = _uiState.value.url
                val content = webScraper.scrapeWebsite(url)

                val prompt = """
                    لخص هذه الصفحة بشكل مختصر ومفيد:
                    
                    ${content.take(3000)}
                """.trimIndent()

                val summary = apiClient.sendPrompt(prompt)
                _pageSummary.value = summary

            } catch (e: Exception) {
                Timber.e(e, "فشل في تلخيص الصفحة")
                _uiState.update { it.copy(error = "فشل في تلخيص الصفحة") }
            }
        }
    }

    fun askAI(question: String) {
        viewModelScope.launch {
            try {
                val url = _uiState.value.url
                val content = webScraper.scrapeWebsite(url)

                val prompt = """
                    بناءً على محتوى هذه الصفحة:
                    ${content.take(2000)}
                    
                    السؤال: $question
                """.trimIndent()

                val answer = apiClient.sendPrompt(prompt)
                _aiMessages.value = _aiMessages.value + (question to answer)

            } catch (e: Exception) {
                Timber.e(e, "فشل في الحصول على إجابة AI")
            }
        }
    }

    // ==================== Reader Mode ====================

    fun enableReaderMode() {
        viewModelScope.launch {
            try {
                val url = _uiState.value.url
                val content = webScraper.scrapeWebsite(url)
                _readerModeContent.value = content
            } catch (e: Exception) {
                Timber.e(e, "فشل في تفعيل وضع القراءة")
            }
        }
    }

    fun disableReaderMode() {
        _readerModeContent.value = null
    }

    // ==================== Screenshot ====================

    fun captureScreenshot(bitmap: android.graphics.Bitmap) {
        _screenshot.value = bitmap
    }

    fun clearScreenshot() {
        _screenshot.value = null
    }

    // ==================== Lifecycle ====================

    override fun onCleared() {
        super.onCleared()
        unregisterWebView()
    }
}
