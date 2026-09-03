package com.nadr59.sitemanager.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.nadr59.sitemanager.data.local.BrowserBookmark
import com.nadr59.sitemanager.data.local.BrowserHistory
import com.nadr59.sitemanager.data.local.PageNote
import com.nadr59.sitemanager.data.local.SavedPage
import com.nadr59.sitemanager.data.local.SiteDatabase
import com.nadr59.sitemanager.data.model.BrowserState
import com.nadr59.sitemanager.data.model.PageTextNode
import com.nadr59.sitemanager.data.model.SmartReadStep
import com.nadr59.sitemanager.data.model.TranslatedNode
import com.nadr59.sitemanager.data.remote.ApiClient
import com.nadr59.sitemanager.data.remote.WebScraper
import com.nadr59.sitemanager.data.repository.SiteRepository
import com.nadr59.sitemanager.data.repository.TranslationRepository
import com.nadr59.sitemanager.domain.translator.WebPageTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    application: Application,
    private val translationRepository: TranslationRepository
) : AndroidViewModel(application) {

    private val database = SiteDatabase.getDatabase(application)
    private val dao = database.siteDao()
    private val browserDao = database.browserDao()
    private val siteRepository = SiteRepository(dao)
    private val pageTranslator = WebPageTranslator()
    private val apiClient = ApiClient()
    private val webScraper = WebScraper()
    private val gson = Gson()

    // ═══ الحالة الرئيسية ═══
    private val _uiState = MutableStateFlow(BrowserState())
    val uiState: StateFlow<BrowserState> = _uiState.asStateFlow()

    // ═══ العقد المستخرجة ═══
    private val _extractedNodes = MutableStateFlow<List<PageTextNode>>(emptyList())
    val extractedNodes: StateFlow<List<PageTextNode>> = _extractedNodes.asStateFlow()

    private val _translatedNodes = MutableStateFlow<List<TranslatedNode>>(emptyList())
    val translatedNodes: StateFlow<List<TranslatedNode>> = _translatedNodes.asStateFlow()

    // ═══ JavaScript ═══
    private val _pendingJs = MutableStateFlow<String?>(null)
    val pendingJs: StateFlow<String?> = _pendingJs.asStateFlow()

    // ═══ الملاحظات ═══
    private val _pageNotes = MutableStateFlow<List<PageNote>>(emptyList())
    val pageNotes: StateFlow<List<PageNote>> = _pageNotes.asStateFlow()

    private val _hasNotes = MutableStateFlow(false)
    val hasNotes: StateFlow<Boolean> = _hasNotes.asStateFlow()

    // ═══ ملخص الصفحة ═══
    private val _pageSummary = MutableStateFlow<String?>(null)
    val pageSummary: StateFlow<String?> = _pageSummary.asStateFlow()

    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing: StateFlow<Boolean> = _isSummarizing.asStateFlow()

    // ═══ مساعد AI ═══
    private val _aiMessages = MutableStateFlow<List<AiMessage>>(emptyList())
    val aiMessages: StateFlow<List<AiMessage>> = _aiMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    // ═══ لقطة الشاشة ═══
    private val _screenshotPath = MutableStateFlow<String?>(null)
    val screenshotPath: StateFlow<String?> = _screenshotPath.asStateFlow()

    // ═══ التاريخ والإشارات ═══
    val browserHistory = browserDao.getAllHistory()
    val bookmarks = browserDao.getAllBookmarks()

    // ═══ الصفحات المحفوظة ═══
    val savedPages = browserDao.getAllSavedPages()

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private val _isReaderMode = MutableStateFlow(false)
    val isReaderMode: StateFlow<Boolean> = _isReaderMode.asStateFlow()

    private var currentSiteId = 0
    private var currentPageContent = ""

    // ═══ بيانات SmartRead المؤقتة ═══
    private var smartReadPageContent = ""
    private var smartReadImageUrls = listOf<String>()
    private var smartReadDetectedLanguage = "unknown"
    private var isSmartReadFlow = false  // لتمييز الترجمة العادية عن SmartRead

    fun onJsExecuted() { _pendingJs.value = null }

    // ═══════════════════════════════════════
    // تحميل الموقع
    // ═══════════════════════════════════════
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

    fun loadUrl(url: String) { _uiState.update { it.copy(url = url) } }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
        checkBookmarkStatus()
        checkSavedStatus()
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

    // ═══════════════════════════════════════
    // لقطة الشاشة
    // ═══════════════════════════════════════
    fun takeScreenshot(webView: WebView) {
        viewModelScope.launch {
            try {
                val bitmap = Bitmap.createBitmap(
                    webView.width,
                    webView.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                webView.draw(canvas)

                val fileName = "screenshot_${System.currentTimeMillis()}.png"
                val file = File(
                    getApplication<Application>().getExternalFilesDir(null),
                    fileName
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

    fun clearScreenshot() { _screenshotPath.value = null }

    // ═══════════════════════════════════════
    // ملخص الصفحة بالذكاء الاصطناعي
    // ═══════════════════════════════════════
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

    fun clearSummary() { _pageSummary.value = null }

    // ═══════════════════════════════════════
    // مساعد AI للصفحة
    // ═══════════════════════════════════════
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
                        it + AiMessage(
                            text = "عذراً، حدث خطأ: ${response.error}",
                            isUser = false,
                            isError = true
                        )
                    }
                }
            } catch (e: Exception) {
                _aiMessages.update {
                    it + AiMessage(
                        text = "خطأ: ${e.message}",
                        isUser = false,
                        isError = true
                    )
                }
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    fun clearAiMessages() { _aiMessages.value = emptyList() }

    // ═══════════════════════════════════════
    // ملاحظات الصفحة
    // ═══════════════════════════════════════
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
                PageNote(
                    url = state.url,
                    title = state.title.ifBlank { state.url },
                    note = noteText
                )
            )
        }
    }

    fun updateNote(note: PageNote, newText: String) {
        viewModelScope.launch {
            browserDao.updateNote(
                note.copy(
                    note = newText,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch { browserDao.deleteNote(noteId) }
    }

    // ═══════════════════════════════════════
    // التاريخ والإشارات
    // ═══════════════════════════════════════
    fun saveToHistory() {
        val state = _uiState.value
        if (state.url.isBlank()) return
        viewModelScope.launch {
            browserDao.insertHistory(
                BrowserHistory(
                    url = state.url,
                    title = state.title.ifBlank { state.url },
                    siteId = currentSiteId
                )
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
                    BrowserBookmark(
                        url = state.url,
                        title = state.title.ifBlank { state.url },
                        siteId = currentSiteId
                    )
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

    // ═══════════════════════════════════════
    // وضع القراءة العادي
    // ═══════════════════════════════════════
    fun toggleReaderMode() {
        val newMode = !_isReaderMode.value
        _isReaderMode.value = newMode
        _pendingJs.value = if (newMode) buildEnhancedReaderModeScript()
        else "window.location.reload();"
    }

    // ═══════════════════════════════════════
    // ═══ القراءة الذكية — startSmartRead ═══
    // ═══════════════════════════════════════
    /**
     * يبدأ تدفق "قراءة بالعربية":
     * 1. تطبيق Reader Mode لتنظيف الصفحة
     * 2. استخراج العقد النصية
     * 3. ترجمتها إلى العربية
     * 4. استبدال النصوص في الصفحة
     */
    fun startSmartRead() {
        val state = _uiState.value
        if (state.isSmartReadMode) {
            // ═══ إلغاء وضع القراءة الذكية ═══
            resetSmartRead()
            return
        }

        isSmartReadFlow = true
        _uiState.update {
            it.copy(
                smartReadStep = SmartReadStep.CLEANING,
                isTranslating = false,
                translationProgress = 0f,
                error = null
            )
        }
        _isReaderMode.value = true

        // الخطوة 1: تطبيق Reader Mode أولاً
        // بعد تطبيقه، نستخرج العقد في onReaderModeApplied()
        _pendingJs.value = buildSmartReadReaderScript()
    }

    /**
     * يُستدعى بعد تطبيق Reader Mode في SmartRead
     * ينتقل إلى مرحلة استخراج النصوص
     */
    fun onReaderModeApplied() {
        if (!isSmartReadFlow) return

        _uiState.update {
            it.copy(smartReadStep = SmartReadStep.EXTRACTING)
        }
        // الخطوة 2: استخراج العقد بعد تنظيف الصفحة
        _pendingJs.value = pageTranslator.buildExtractScript()
    }

    fun resetSmartRead() {
        isSmartReadFlow = false
        smartReadPageContent = ""
        smartReadImageUrls = emptyList()
        _isReaderMode.value = false
        _uiState.update {
            it.copy(
                isSmartReadMode = false,
                smartReadStep = SmartReadStep.IDLE,
                isTranslationMode = false,
                isTranslating = false,
                translationProgress = 0f
            )
        }
        _extractedNodes.value = emptyList()
        _translatedNodes.value = emptyList()
        _pendingJs.value = "window.location.reload();"
    }

    // ═══════════════════════════════════════
    // الترجمة العادية
    // ═══════════════════════════════════════
    fun startPageTranslation() {
        isSmartReadFlow = false
        _uiState.update {
            it.copy(
                isTranslating = true,
                translationProgress = 0f,
                error = null,
                showTranslationSheet = false
            )
        }
        _pendingJs.value = pageTranslator.buildExtractScript()
    }

    /**
     * نقطة التقاء SmartRead والترجمة العادية
     * يُستدعى عند اكتمال استخراج العقد في كلا الحالتين
     */
    fun onNodesExtracted(jsonString: String) {
        val nodes: List<PageTextNode> = pageTranslator.parseExtractedNodes(jsonString)
        _extractedNodes.value = nodes

        if (nodes.isEmpty()) {
            if (isSmartReadFlow) {
                _uiState.update {
                    it.copy(
                        smartReadStep = SmartReadStep.DONE,
                        isSmartReadMode = true,
                        isTranslating = false,
                        error = "لا يوجد نص قابل للترجمة في هذه الصفحة"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isTranslating = false, error = "لا يوجد نص للترجمة")
                }
            }
            return
        }

        // تحديث الحالة للترجمة
        _uiState.update {
            it.copy(
                isTranslating = true,
                translationProgress = 0f,
                smartReadStep = if (isSmartReadFlow) SmartReadStep.TRANSLATING else it.smartReadStep
            )
        }

        viewModelScope.launch {
            val result = translationRepository.translatePageNodes(
                nodes = nodes,
                targetLanguage = _uiState.value.targetLanguage,
                onProgress = { p -> _uiState.update { it.copy(translationProgress = p) } }
            )
            result.fold(
                onSuccess = { translated: List<TranslatedNode> ->
                    _translatedNodes.value = translated
                    _pendingJs.value = pageTranslator.buildReplaceScript(translated)

                    if (isSmartReadFlow) {
                        _uiState.update {
                            it.copy(
                                isTranslating = false,
                                isSmartReadMode = true,
                                isTranslationMode = true,
                                translationProgress = 1f,
                                smartReadStep = SmartReadStep.DONE
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isTranslating = false,
                                isTranslationMode = true,
                                translationProgress = 1f
                            )
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isTranslating = false,
                            smartReadStep = if (isSmartReadFlow)
                                SmartReadStep.IDLE else it.smartReadStep,
                            error = "فشل الترجمة: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun translateSelectedText() {
        _pendingJs.value = pageTranslator.buildSelectionScript()
    }

    fun onTextSelected(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val text = json.optString("text", "")
            if (text.isBlank()) return
            viewModelScope.launch {
                val result = translationRepository.translateText(
                    text = text,
                    targetLanguage = _uiState.value.targetLanguage
                )
                result.fold(
                    onSuccess = { translated ->
                        _pendingJs.value =
                            pageTranslator.buildReplaceSelectionScript(translated)
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = "فشل الترجمة: ${e.message}") }
                    }
                )
            }
        } catch (_: Exception) {}
    }

    fun resetTranslation() {
        isSmartReadFlow = false
        _uiState.update {
            it.copy(
                isTranslationMode = false,
                isTranslating = false,
                translationProgress = 0f
            )
        }
        _extractedNodes.value = emptyList()
        _translatedNodes.value = emptyList()
        _pendingJs.value = "window.location.reload();"
    }

    // ═══════════════════════════════════════
    // حفظ الصفحة للقراءة لاحقاً
    // ═══════════════════════════════════════
    fun saveCurrentPage() {
        val state = _uiState.value
        if (state.url.isBlank() || state.isSavingPage) return

        _uiState.update { it.copy(isSavingPage = true) }

        viewModelScope.launch {
            try {
                // جلب المحتوى عبر WebScraper
                val scraped = webScraper.scrape(state.url)

                // استخراج روابط الصور من المحتوى
                val imageUrls = extractImageUrls(scraped.rawContent, state.url)
                val imageUrlsJson = gson.toJson(imageUrls)

                // تحديد النص المترجم إن وجد
                val translatedContent = if (state.isTranslationMode || state.isSmartReadMode) {
                    buildTranslatedContent(_translatedNodes.value)
                } else null

                // تحديد اللغة
                val language = if (state.isTranslationMode || state.isSmartReadMode) {
                    "translated_to_${state.targetLanguage}"
                } else {
                    detectPageLanguage(scraped.rawContent)
                }

                browserDao.insertSavedPage(
                    SavedPage(
                        url = state.url,
                        title = state.title.ifBlank { state.url },
                        content = scraped.rawContent.take(50000), // حد أقصى معقول
                        translatedContent = translatedContent,
                        imageUrls = imageUrlsJson,
                        language = language,
                        isTranslated = state.isTranslationMode || state.isSmartReadMode,
                        siteId = currentSiteId
                    )
                )

                _uiState.update {
                    it.copy(
                        isSavingPage = false,
                        isPageSaved = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingPage = false,
                        error = "فشل الحفظ: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteSavedPage(id: Int) {
        viewModelScope.launch {
            browserDao.deleteSavedPage(id)
            // تحديث حالة الصفحة الحالية إن كانت هي المحذوفة
            checkSavedStatus()
        }
    }

    private fun checkSavedStatus() {
        viewModelScope.launch {
            val url = _uiState.value.url
            if (url.isNotBlank()) {
                _uiState.update {
                    it.copy(isPageSaved = browserDao.isPageSaved(url) > 0)
                }
            }
        }
    }

    // ═══ بناء نص مترجم موحد من العقد ═══
    private fun buildTranslatedContent(nodes: List<TranslatedNode>): String? {
        if (nodes.isEmpty()) return null
        return nodes.joinToString("\n") { it.translatedText }
    }

    // ═══ استخراج روابط الصور من المحتوى الخام ═══
    private fun extractImageUrls(content: String, baseUrl: String): List<String> {
        val imgRegex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return imgRegex.findAll(content)
            .map { it.groupValues[1] }
            .filter { it.startsWith("http") }
            .take(20) // حد أقصى 20 صورة
            .toList()
    }

    // ═══ اكتشاف لغة بسيط ═══
    private fun detectPageLanguage(content: String): String {
        val sample = content.take(500)
        return when {
            sample.any { it in '\u0600'..'\u06FF' } -> "ar"
            sample.any { it in '\u4E00'..'\u9FFF' } -> "zh"
            sample.any { it in '\u3040'..'\u30FF' } -> "ja"
            sample.any { it in '\u0400'..'\u04FF' } -> "ru"
            else -> "en"
        }
    }

    // ═══════════════════════════════════════
    // JavaScript Scripts
    // ═══════════════════════════════════════

    /**
     * نسخة Reader Mode المخصصة لـ SmartRead
     * تُعيد قيمة "smart_reader_ready" عند الاكتمال
     */
    private fun buildSmartReadReaderScript(): String {
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
                try {
                    document.querySelectorAll(s).forEach(function(el) { el.remove(); });
                } catch(e) {}
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
            readerContainer.id = 'smart-reader-container';
            readerContainer.innerHTML = mainContent ? mainContent.innerHTML : document.body.innerHTML;

            document.body.innerHTML = '';
            document.body.appendChild(readerContainer);

            var style = document.createElement('style');
            style.textContent = `
                * { box-sizing: border-box; }
                body { background: #FAFAFA !important; margin: 0 !important; padding: 0 !important; }
                #smart-reader-container {
                    max-width: 720px !important;
                    margin: 0 auto !important;
                    padding: 24px 20px 48px !important;
                    font-family: 'Georgia', serif !important;
                    font-size: 19px !important;
                    line-height: 1.9 !important;
                    color: #2C2C2C !important;
                    background: #FAFAFA !important;
                }
                #smart-reader-container h1,h2,h3,h4 {
                    font-family: sans-serif !important;
                    color: #1A1A1A !important;
                    margin-top: 1.5em !important;
                }
                #smart-reader-container h1 { font-size: 28px !important; }
                #smart-reader-container h2 { font-size: 23px !important; }
                #smart-reader-container h3 { font-size: 19px !important; }
                #smart-reader-container p { margin-bottom: 1.2em !important; }
                #smart-reader-container img {
                    max-width: 100% !important; height: auto !important;
                    border-radius: 8px !important; margin: 16px 0 !important; display: block !important;
                }
                #smart-reader-container a { color: #1565C0 !important; text-decoration: underline !important; }
                #smart-reader-container blockquote {
                    border-right: 4px solid #1565C0 !important; border-left: none !important;
                    padding: 8px 16px !important; margin: 16px 0 !important;
                    background: #F0F4FF !important; border-radius: 0 8px 8px 0 !important;
                }
                #smart-reader-container ul, #smart-reader-container ol {
                    padding-right: 24px !important; padding-left: 0 !important;
                }
                #smart-reader-container li { margin-bottom: 8px !important; }
            `;
            document.head.appendChild(style);
            document.documentElement.setAttribute('dir', 'auto');

            return 'smart_reader_ready';
        })();
        """.trimIndent()
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
                try {
                    document.querySelectorAll(s).forEach(function(el) { el.remove(); });
                } catch(e) {}
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
                    max-width: 720px !important;
                    margin: 0 auto !important;
                    padding: 24px 20px 48px !important;
                    font-family: 'Georgia', serif !important;
                    font-size: 19px !important;
                    line-height: 1.9 !important;
                    color: #2C2C2C !important;
                    background: #FAFAFA !important;
                }
                #reader-mode-container h1 { font-size: 28px !important; }
                #reader-mode-container h2 { font-size: 23px !important; }
                #reader-mode-container h3 { font-size: 19px !important; }
                #reader-mode-container p {
                    margin-bottom: 1.2em !important;
                    text-align: justify !important;
                }
                #reader-mode-container img {
                    max-width: 100% !important; height: auto !important;
                    border-radius: 8px !important; margin: 16px 0 !important; display: block !important;
                }
                #reader-mode-container a { color: #1565C0 !important; }
                #reader-mode-container blockquote {
                    border-right: 4px solid #1565C0 !important; border-left: none !important;
                    padding: 8px 16px !important; background: #F0F4FF !important;
                }
                #reader-mode-container ul, #reader-mode-container ol {
                    padding-right: 24px !important; padding-left: 0 !important;
                }
            `;
            document.head.appendChild(style);
            document.documentElement.setAttribute('dir', 'auto');

            return 'reader_mode_enabled';
        })();
        """.trimIndent()
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }

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

// ═══ نموذج رسائل AI ═══
data class AiMessage(
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
