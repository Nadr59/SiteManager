package com.nadr59.sitemanager.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nadr59.sitemanager.data.local.BrowserBookmark
import com.nadr59.sitemanager.data.local.BrowserHistory
import com.nadr59.sitemanager.data.local.PageNote
import com.nadr59.sitemanager.data.local.SiteDatabase
import com.nadr59.sitemanager.data.model.BrowserState
import com.nadr59.sitemanager.data.model.PageTextNode
import com.nadr59.sitemanager.data.model.TranslatedNode
import com.nadr59.sitemanager.data.remote.ApiClient
import com.nadr59.sitemanager.data.remote.WebScraper
import com.nadr59.sitemanager.data.repository.SiteRepository
import com.nadr59.sitemanager.domain.translator.WebPageTranslationCoordinator
import com.nadr59.sitemanager.domain.translator.WebPageTranslationCoordinator.JavascriptResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    application: Application,
    private val translationCoordinator: WebPageTranslationCoordinator
) : AndroidViewModel(application) {

    private val database = SiteDatabase.getDatabase(application)
    private val dao = database.siteDao()
    private val browserDao = database.browserDao()
    private val siteRepository = SiteRepository(dao)
    private val apiClient = ApiClient()
    private val webScraper = WebScraper()

    private val _uiState = MutableStateFlow(BrowserState())
    val uiState: StateFlow<BrowserState> = _uiState.asStateFlow()

    private val _extractedNodes = MutableStateFlow<List<PageTextNode>>(emptyList())
    val extractedNodes: StateFlow<List<PageTextNode>> = _extractedNodes.asStateFlow()

    private val _translatedNodes = MutableStateFlow<List<TranslatedNode>>(emptyList())
    val translatedNodes: StateFlow<List<TranslatedNode>> = _translatedNodes.asStateFlow()

    private val _pendingJs = MutableStateFlow<String?>(null)
    val pendingJs: StateFlow<String?> = _pendingJs.asStateFlow()

    private val _pageNotes = MutableStateFlow<List<PageNote>>(emptyList())
    val pageNotes: StateFlow<List<PageNote>> = _pageNotes.asStateFlow()

    private val _hasNotes = MutableStateFlow(false)
    val hasNotes: StateFlow<Boolean> = _hasNotes.asStateFlow()

    private val _pageSummary = MutableStateFlow<String?>(null)
    val pageSummary: StateFlow<String?> = _pageSummary.asStateFlow()

    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing: StateFlow<Boolean> = _isSummarizing.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<AiMessage>>(emptyList())
    val aiMessages: StateFlow<List<AiMessage>> = _aiMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _screenshotPath = MutableStateFlow<String?>(null)
    val screenshotPath: StateFlow<String?> = _screenshotPath.asStateFlow()

    val browserHistory = browserDao.getAllHistory()
    val bookmarks = browserDao.getAllBookmarks()

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private val _isReaderMode = MutableStateFlow(false)
    val isReaderMode: StateFlow<Boolean> = _isReaderMode.asStateFlow()

    private var currentSiteId = 0
    private var currentPageContent = ""
    private var dynamicTranslationPolling = false
    private var pendingJavascriptPurpose: JavascriptPurpose =
    JavascriptPurpose.None

private enum class JavascriptPurpose {
    None,
    InitialExtraction,
    DynamicPolling,
    Replace
}
    
    fun pollDynamicTranslation() {
    if (!_uiState.value.isTranslationMode) return
    if (_uiState.value.isTranslating) return
    if (_pendingJs.value != null) return
    if (dynamicTranslationPolling) return

    dynamicTranslationPolling = true
    pendingJavascriptPurpose = JavascriptPurpose.DynamicPolling
    _pendingJs.value = translationCoordinator.pollDynamicNodesScript()
    }
    fun onDynamicJavascriptResult(rawResult: String?) {
    dynamicTranslationPolling = false

    when (val result = translationCoordinator.decodeAndClassify(rawResult)) {
        is JavascriptResult.Nodes -> {
            if (result.nodes.isNotEmpty()) {
                handleDynamicNodes(result.nodes)
            }
        }

        else -> Unit
    }
    }

    private fun handleDynamicNodes(nodes: List<PageTextNode>) {
    if (nodes.isEmpty()) return
    if (_uiState.value.isTranslating) return

    _uiState.update {
        it.copy(
            isTranslating = true,
            translationProgress = 0f,
            error = null
        )
    }

    viewModelScope.launch {
        val result = translationCoordinator.translatePage(
            nodes = nodes,
            targetLanguage = _uiState.value.targetLanguage,
            onProgress = { progress ->
                _uiState.update {
                    it.copy(
                        translationProgress = progress.coerceIn(0f, 1f)
                    )
                }
            }
        )

        result.fold(
            onSuccess = { translated ->
                if (translated.isNotEmpty()) {
                    _translatedNodes.update { existing ->
                        existing + translated
                    }

                    _pendingJs.value =
                        translationCoordinator.replaceScript(translated)
                }

                _uiState.update {
                    it.copy(
                        isTranslating = false,
                        translationProgress = 1f
                    )
                }
            },

            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isTranslating = false,
                        translationProgress = 0f,
                        error = "فشل ترجمة المحتوى الجديد: ${
                            error.message ?: "خطأ غير معروف"
                        }"
                    )
                }
            }
        )
    }
    }
    

    fun onJsExecuted() {
    _pendingJs.value = null
    }

    fun loadSite(siteId: Int) {
        currentSiteId = siteId
        viewModelScope.launch {
            try {
                val site = siteRepository.getSiteById(siteId)
                if (site != null) {
                    _uiState.update { it.copy(url = site.url, title = site.name) }
                } else {
                    _uiState.update { it.copy(error = "الموقع غير موجود") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "فشل التحميل: ${e.message}") }
            }
        }
    }

    fun loadUrl(url: String) {
        _uiState.update { it.copy(url = url) }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
        checkBookmarkStatus()
        loadNotesForCurrentUrl()
    }

    fun updateProgress(progress: Int) {
        _uiState.update { it.copy(progress = progress, isLoading = progress < 100) }
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    fun updateNavigation(canGoBack: Boolean, canGoForward: Boolean) {
        _uiState.update { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
    }

    fun setTargetLanguage(language: String) {
        _uiState.update { it.copy(targetLanguage = language) }
    }

    fun showTranslationSheet() {
        _uiState.update { it.copy(showTranslationSheet = true) }
    }

    fun hideTranslationSheet() {
        _uiState.update { it.copy(showTranslationSheet = false) }
    }

    // ═══ لقطة الشاشة ═══
    fun takeScreenshot(webView: WebView) {
        viewModelScope.launch {
            try {
                val bitmap = Bitmap.createBitmap(
                    webView.width, webView.height, Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                webView.draw(canvas)

                val fileName = "screenshot_${System.currentTimeMillis()}.png"
                val file = File(
                    getApplication<Application>().getExternalFilesDir(null), fileName
                )
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
                _screenshotPath.value = file.absolutePath
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "فشل التقاط الشاشة: ${e.message}") }
            }
        }
    }

    fun clearScreenshot() {
        _screenshotPath.value = null
    }

    // ═══ ملخص الصفحة ═══
    fun summarizePage() {
        val url = _uiState.value.url
        if (url.isBlank()) return

        _isSummarizing.value = true
        _pageSummary.value = null

        viewModelScope.launch {
            try {
                val content = webScraper.scrape(url)
                currentPageContent = content.rawContent

                val prompt = """أنت مساعد ذكي مختصر.
لخّص هذه الصفحة في نقاط واضحة:
الرابط: ${content.url}
العنوان: ${content.title ?: ""}
الوصف: ${content.description ?: ""}
المحتوى:
${content.rawContent.take(3000)}
قواعد:
- لخّص في 5 نقاط رئيسية كحد أقصى
- كن مختصراً ومفيداً
- أجب بالعربية
- استخدم نقاط (•) للتنظيم"""

                val response = apiClient.ask(prompt)
                if (response.success) {
                    _pageSummary.value = response.response
                } else {
                    _uiState.update { it.copy(error = response.error) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "فشل التلخيص: ${e.message}") }
            } finally {
                _isSummarizing.value = false
            }
        }
    }

    fun clearSummary() {
        _pageSummary.value = null
    }

    // ═══ مساعد AI ═══
    fun askAiAboutPage(question: String) {
        if (question.isBlank()) return

        val url = _uiState.value.url
        val title = _uiState.value.title

        _aiMessages.update { it + AiMessage(text = question, isUser = true) }
        _isAiThinking.value = true

        viewModelScope.launch {
            try {
                if (currentPageContent.isBlank()) {
                    try {
                        val content = webScraper.scrape(url)
                        currentPageContent = content.rawContent
                    } catch (_: Exception) {}
                }

                val prompt = """أنت مساعد ذكي يساعد المستخدم في فهم محتوى صفحة ويب.
الصفحة الحالية:
- الرابط: $url
- العنوان: $title
${if (currentPageContent.isNotBlank()) "- المحتوى:\n${currentPageContent.take(3000)}" else ""}
سؤال المستخدم: $question
أجب بشكل مختصر ومفيد بالعربية."""

                val response = apiClient.ask(prompt)
                if (response.success) {
                    _aiMessages.update {
                        it + AiMessage(text = response.response, isUser = false)
                    }
                } else {
                    _aiMessages.update {
                        it + AiMessage(text = "عذراً، حدث خطأ: ${response.error}", isUser = false, isError = true)
                    }
                }
            } catch (e: Exception) {
                _aiMessages.update {
                    it + AiMessage(text = "خطأ: ${e.message}", isUser = false, isError = true)
                }
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    fun clearAiMessages() {
        _aiMessages.value = emptyList()
    }

    // ═══ ملاحظات الصفحة ═══
    private fun loadNotesForCurrentUrl() {
        val url = _uiState.value.url
        if (url.isBlank()) return
        viewModelScope.launch {
            browserDao.getNotesForUrl(url).collect { notes ->
                _pageNotes.value = notes
                _hasNotes.value = notes.isNotEmpty()
            }
        }
    }

    fun addNote(noteText: String) {
        if (noteText.isBlank()) return
        val state = _uiState.value
        viewModelScope.launch {
            browserDao.insertNote(
                PageNote(url = state.url, title = state.title.ifBlank { state.url }, note = noteText)
            )
        }
    }

    fun updateNote(note: PageNote, newText: String) {
        viewModelScope.launch {
            browserDao.updateNote(note.copy(note = newText, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch { browserDao.deleteNote(noteId) }
    }

    // ═══ التاريخ والإشارات ═══
    fun saveToHistory() {
        val state = _uiState.value
        if (state.url.isBlank()) return
        viewModelScope.launch {
            browserDao.insertHistory(
                BrowserHistory(url = state.url, title = state.title.ifBlank { state.url }, siteId = currentSiteId)
            )
        }
    }

    fun toggleBookmark() {
        val state = _uiState.value
        if (state.url.isBlank()) return
        viewModelScope.launch {
            val existing = browserDao.getBookmarkByUrl(state.url)
            if (existing != null) {
                browserDao.deleteBookmark(existing.id)
                _isBookmarked.value = false
            } else {
                browserDao.insertBookmark(
                    BrowserBookmark(url = state.url, title = state.title.ifBlank { state.url }, siteId = currentSiteId)
                )
                _isBookmarked.value = true
            }
        }
    }

    private fun checkBookmarkStatus() {
        viewModelScope.launch {
            val url = _uiState.value.url
            if (url.isNotBlank()) {
                _isBookmarked.value = browserDao.isBookmarked(url) > 0
            }
        }
    }

    // ═══ وضع القراءة ═══
    fun toggleReaderMode() {
        val newMode = !_isReaderMode.value
        _isReaderMode.value = newMode
        _pendingJs.value = if (newMode) buildEnhancedReaderModeScript()
        else "window.location.reload();"
    }

    private fun buildEnhancedReaderModeScript(): String {
        return """
        (function() {
            var removeSelectors = [
                'header:not(article header)', 'footer', 'nav', 'aside',
                '.ad', '.ads', '.advertisement', '.banner',
                '.sidebar', '.widget', '.popup', '.modal',
                '.cookie-notice', '.newsletter-signup',
                '[class*="ad-"]', '[id*="ad-"]',
                '[class*="social"]', '[class*="share"]',
                '[class*="related"]', '[class*="recommended"]',
                'script', 'style', 'iframe',
                '.comments', '#comments',
                '.sticky', '[class*="sticky"]'
            ];
            removeSelectors.forEach(function(s) {
                try { document.querySelectorAll(s).forEach(function(el) { el.remove(); }); } catch(e) {}
            });
            var mainContent =
                document.querySelector('article') ||
                document.querySelector('main') ||
                document.querySelector('[role="main"]') ||
                document.querySelector('.content') ||
                document.querySelector('.post-content') ||
                document.querySelector('.entry-content') ||
                document.body;
            var readerContainer = document.createElement('div');
            readerContainer.id = 'reader-mode-container';
            readerContainer.innerHTML = mainContent ? mainContent.innerHTML : document.body.innerHTML;
            document.body.innerHTML = '';
            document.body.appendChild(readerContainer);
            var style = document.createElement('style');
            style.textContent = `
                * { box-sizing: border-box; }
                body { background: #FAFAFA !important; margin: 0 !important; padding: 0 !important; }
                #reader-mode-container {
                    max-width: 720px !important; margin: 0 auto !important;
                    padding: 24px 20px 48px !important;
                    font-family: 'Georgia', serif !important; font-size: 19px !important;
                    line-height: 1.9 !important; color: #2C2C2C !important;
                    background: #FAFAFA !important;
                }
                #reader-mode-container h1, #reader-mode-container h2, #reader-mode-container h3 {
                    font-family: sans-serif !important; color: #1A1A1A !important;
                    margin-top: 1.5em !important; line-height: 1.4 !important;
                }
                #reader-mode-container h1 { font-size: 28px !important; }
                #reader-mode-container h2 { font-size: 23px !important; }
                #reader-mode-container p { margin-bottom: 1.2em !important; }
                #reader-mode-container img {
                    max-width: 100% !important; height: auto !important;
                    border-radius: 8px !important; margin: 16px 0 !important;
                }
                #reader-mode-container a { color: #1565C0 !important; text-decoration: underline !important; }
                #reader-mode-container blockquote {
                    border-right: 4px solid #1565C0 !important; border-left: none !important;
                    padding: 8px 16px !important; margin: 16px 0 !important;
                    background: #F0F4FF !important; border-radius: 0 8px 8px 0 !important;
                }
                #reader-mode-container pre, #reader-mode-container code {
                    background: #F5F5F5 !important; border-radius: 4px !important;
                    padding: 2px 6px !important; font-family: monospace !important;
                }
                #reader-mode-container pre { padding: 16px !important; overflow-x: auto !important; }
            `;
            document.head.appendChild(style);
            document.documentElement.setAttribute('dir', 'auto');
            return 'reader_mode_enabled';
        })();
        """.trimIndent()
    }

    // ═══ الترجمة ═══
    fun startPageTranslation() {
        if (_uiState.value.url.isBlank() || _uiState.value.isTranslating) return
        _uiState.update {
            it.copy(isTranslating = true, translationProgress = 0f, error = null, showTranslationSheet = false)
        }
        _extractedNodes.value = emptyList()
        _translatedNodes.value = emptyList()
        _pendingJs.value = translationCoordinator.extractScript()
      
         pendingJavascriptPurpose = JavascriptPurpose.InitialExtraction
    }

    fun onJavascriptResult(rawResult: String?) {
    when (pendingJavascriptPurpose) {

        JavascriptPurpose.DynamicPolling -> {
            dynamicTranslationPolling = false

            when (
                val result =
                    translationCoordinator.decodeAndClassify(rawResult)
            ) {
                is JavascriptResult.Nodes -> {
                    if (result.nodes.isNotEmpty()) {
                        handleDynamicNodes(result.nodes)
                    }
                }

                else -> Unit
            }
        }

        JavascriptPurpose.InitialExtraction -> {
            when (
                val result =
                    translationCoordinator.decodeAndClassify(rawResult)
            ) {
                is JavascriptResult.Nodes -> {
                    handleExtractedNodes(result.nodes)
                }

                is JavascriptResult.Selection -> {
                    handleSelectedText(result.text)
                }

                else -> Unit
            }
        }

        else -> {
            when (
                val result =
                    translationCoordinator.decodeAndClassify(rawResult)
            ) {
                is JavascriptResult.Nodes -> {
                    handleExtractedNodes(result.nodes)
                }

                is JavascriptResult.Selection -> {
                    handleSelectedText(result.text)
                }

                else -> Unit
            }
        }
    }

    pendingJavascriptPurpose = JavascriptPurpose.None
    }

    fun onNodesExtracted(jsonString: String) {
        val result = translationCoordinator.decodeAndClassify(jsonString)
        if (result is JavascriptResult.Nodes) handleExtractedNodes(result.nodes)
    }

    private fun handleExtractedNodes(nodes: List<PageTextNode>) {
        _extractedNodes.value = nodes
        if (nodes.isEmpty()) {
            _uiState.update {
                it.copy(
                    isTranslating = false,
                    translationProgress = 0f,
                    error = "لم يتم العثور على نص قابل للترجمة في الصفحة"
                )
            }
            return
        }

        viewModelScope.launch {
            val result = translationCoordinator.translatePage(
                nodes = nodes,
                targetLanguage = _uiState.value.targetLanguage,
                onProgress = { progress ->
                    _uiState.update { it.copy(translationProgress = progress.coerceIn(0f, 1f)) }
                }
            )
            result.fold(
                onSuccess = { translated ->
                    _translatedNodes.value = translated
                    _pendingJs.value = translationCoordinator.replaceScript(translated)
                    _uiState.update {
                        it.copy(isTranslating = false, isTranslationMode = true, translationProgress = 1f)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isTranslating = false,
                            translationProgress = 0f,
                            error = "فشل ترجمة الصفحة: ${error.message ?: "خطأ غير معروف"}"
                        )
                    }
                }
            )
        }
    }

    fun translateSelectedText() {
        if (_uiState.value.isTranslating) return
        _pendingJs.value = translationCoordinator.selectionScript()
    }

    fun onTextSelected(rawJson: String) {
        val result = translationCoordinator.decodeAndClassify(rawJson)
        if (result is JavascriptResult.Selection) handleSelectedText(result.text)
    }

    private fun handleSelectedText(text: String) {
        viewModelScope.launch {
            val result = translationCoordinator.translateSelection(
                text, _uiState.value.targetLanguage
            )
            result.fold(
                onSuccess = { translated ->
                    _pendingJs.value = translationCoordinator.replaceSelectionScript(translated)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = "فشل ترجمة النص المحدد: ${error.message ?: "خطأ غير معروف"}")
                    }
                }
            )
        }
    }

    fun resetTranslation() {
    dynamicTranslationPolling = false
    pendingJavascriptPurpose = JavascriptPurpose.None

    _uiState.update {
        it.copy(
            isTranslationMode = false,
            isTranslating = false,
            translationProgress = 0f,
            error = null
        )
    }

    _extractedNodes.value = emptyList()
    _translatedNodes.value = emptyList()
    _pendingJs.value = "window.location.reload();"
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun deleteHistory(id: Int) {
        viewModelScope.launch { browserDao.deleteHistory(id) }
    }

    fun clearAllHistory() {
        viewModelScope.launch { browserDao.clearAllHistory() }
    }

    fun deleteBookmark(id: Int) {
        viewModelScope.launch { browserDao.deleteBookmark(id) }
    }
}

data class AiMessage(
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
