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

    private val pageTranslator = WebPageTranslator()
    private val apiClient = ApiClient()
    private val webScraper = WebScraper()

    private var currentWebView: WebView? = null

    private val _uiState = MutableStateFlow(BrowserState())
    val uiState: StateFlow<BrowserState> = _uiState.asStateFlow()

    private val _pendingJs = MutableStateFlow<String?>(null)
    val pendingJs: StateFlow<String?> = _pendingJs.asStateFlow()

    private val _pageNotes = MutableStateFlow<List<PageNote>>(emptyList())
    val pageNotes: StateFlow<List<PageNote>> = _pageNotes.asStateFlow()

    private val _pageSummary = MutableStateFlow<String?>(null)
    val pageSummary: StateFlow<String?> = _pageSummary.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val aiMessages: StateFlow<List<Pair<String, String>>> = _aiMessages.asStateFlow()

    private val _screenshot = MutableStateFlow<android.graphics.Bitmap?>(null)
    val screenshot: StateFlow<android.graphics.Bitmap?> = _screenshot.asStateFlow()

    private val _history = MutableStateFlow<List<BrowserHistory>>(emptyList())
    val history: StateFlow<List<BrowserHistory>> = _history.asStateFlow()

    // alias for BrowserScreen compatibility
    val browserHistory: StateFlow<List<BrowserHistory>> = _history.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<BrowserBookmark>>(emptyList())
    val bookmarks: StateFlow<List<BrowserBookmark>> = _bookmarks.asStateFlow()

    private val _readerModeContent = MutableStateFlow<String?>(null)
    val readerModeContent: StateFlow<String?> = _readerModeContent.asStateFlow()

    private var currentSiteId: Int? = null

    init {
        loadHistory()
        loadBookmarks()
    }

    // ==================== WebView ====================

    fun registerWebView(webView: WebView) {
        currentWebView = webView
        Timber.d("تم تسجيل WebView")
    }

    fun unregisterWebView() {
        currentWebView = null
        Timber.d("تم إلغاء تسجيل WebView")
    }

    // ==================== Site ====================

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

    // ==================== UI State ====================

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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun consumePendingJs() {
        _pendingJs.value = null
    }

    fun onJsExecuted() {
        consumePendingJs()
    }

    // ==================== Translation Sheet ====================

    fun showTranslationSheet() {
        _uiState.update { it.copy(showTranslationSheet = true) }
    }

    fun hideTranslationSheet() {
        _uiState.update { it.copy(showTranslationSheet = false) }
    }

    fun setTargetLanguage(language: String) {
        _uiState.update { it.copy(targetLanguage = language) }
    }

    // ==================== Translation ====================

    fun startPageTranslationWithCoordinator() {
        val webView = currentWebView ?: run {
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
                                    it.copy(translationProgress = operation.percentage / 100f)
                                }
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
                            }
                            is TranslationOperation.Failure -> {
                                _uiState.update {
                                    it.copy(
                                        isTranslating = false,
                                        error = operation.error
                                    )
                                }
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
        val webView = currentWebView ?: run {
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
                    Timber.d("ترجمة النص: ${original.take(30)} -> ${translated.take(30)}")
                }
                _uiState.update { it.copy(isTranslating = false) }
            } catch (e: Exception) {
                Timber.e(e, "خطأ في ترجمة النص المحدد")
                _uiState.update { it.copy(isTranslating = false, error = e.message) }
            }
        }
    }

    fun restoreOriginalWithCoordinator() {
        val webView = currentWebView ?: run {
            Timber.e("WebView غير مسجل")
            return
        }

        val currentUrl = _uiState.value.url
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            try {
                translationCoordinator.restoreOriginalPage(webView, currentUrl)
                    .onSuccess {
                        _uiState.update {
                            it.copy(isTranslationMode = false, translationProgress = 0f)
                        }
                    }
                    .onFailure { e ->
                        Timber.e(e, "فشل في استعادة النص الأصلي")
                    }
            } catch (e: Exception) {
                Timber.e(e, "خطأ في استعادة النص الأصلي")
            }
        }
    }

    // دوال للتوافق
    fun startPageTranslation() = startPageTranslationWithCoordinator()
    fun translateSelectedText() = translateSelectionWithCoordinator()
    fun resetTranslation() = restoreOriginalWithCoordinator()

    // ==================== Bookmark ====================

    fun toggleBookmark() {
        val currentUrl = _uiState.value.url
        val currentTitle = _uiState.value.title
        val isBookmarked = _uiState.value.isBookmarked

        if (isBookmarked) {
            viewModelScope.launch {
                val bookmark = _bookmarks.value.find { it.url == currentUrl }
                if (bookmark != null) {
                    translationRepository.deleteBookmark(bookmark)
                    _uiState.update { it.copy(isBookmarked = false) }
                }
            }
        } else {
            addBookmark(currentUrl, currentTitle)
            _uiState.update { it.copy(isBookmarked = true) }
        }
    }

    // ==================== Reader Mode ====================

    fun toggleReaderMode() {
        if (_uiState.value.isReaderMode) {
            disableReaderMode()
        } else {
            enableReaderMode()
        }
    }

    fun enableReaderMode() {
        viewModelScope.launch {
            try {
                val url = _uiState.value.url
                val content = webScraper.scrapeWebsite(url)
                _readerModeContent.value = content
                _uiState.update { it.copy(isReaderMode = true) }
            } catch (e: Exception) {
                Timber.e(e, "فشل في تفعيل وضع القراءة")
            }
        }
    }

    fun disableReaderMode() {
        _readerModeContent.value = null
        _uiState.update { it.copy(isReaderMode = false) }
    }

    // ==================== Screenshot ====================

    fun takeScreenshot() {
        val webView = currentWebView ?: return
        try {
            val bitmap = android.graphics.Bitmap.createBitmap(
                webView.width, webView.height,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            webView.draw(canvas)
            _screenshot.value = bitmap
            Timber.d("تم التقاط الشاشة")
        } catch (e: Exception) {
            Timber.e(e, "فشل في التقاط الشاشة")
        }
    }

    fun captureScreenshot(bitmap: android.graphics.Bitmap) {
        _screenshot.value = bitmap
    }

    fun clearScreenshot() {
        _screenshot.value = null
    }

    // ==================== AI / Summary ====================

    fun summarizePage() {
        _uiState.update { it.copy(isSummarizing = true) }
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
            } finally {
                _uiState.update { it.copy(isSummarizing = false) }
            }
        }
    }

    fun askAI(question: String) {
        _uiState.update { it.copy(isAiThinking = true) }
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
            } finally {
                _uiState.update { it.copy(isAiThinking = false) }
            }
        }
    }

    // ==================== Nodes / Selection Callbacks ====================

    fun onNodesExtracted(nodesJson: String) {
        Timber.d("تم استخراج العقد: ${nodesJson.take(100)}")
    }

    fun onTextSelected(selectedText: String) {
        Timber.d("تم تحديد النص: ${selectedText.take(50)}")
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
                val historyItem = BrowserHistory(
                    url = url,
                    title = title,
                    visitedAt = System.currentTimeMillis(),
                    siteId = currentSiteId
                )
                translationRepository.insertHistory(historyItem)
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
                val currentUrl = _uiState.value.url
                if (currentUrl.isNotBlank()) {
                    _uiState.update {
                        it.copy(isBookmarked = bookmarkList.any { b -> b.url == currentUrl })
                    }
                }
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
                _uiState.update { it.copy(hasNotes = notes.isNotEmpty()) }
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

    // ==================== Lifecycle ====================

    override fun onCleared() {
        super.onCleared()
        unregisterWebView()
    }
}
