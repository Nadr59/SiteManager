package com.nadr59.sitemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nadr59.sitemanager.data.local.BrowserBookmark
import com.nadr59.sitemanager.data.local.BrowserHistory
import com.nadr59.sitemanager.data.local.SiteDatabase
import com.nadr59.sitemanager.data.model.BrowserState
import com.nadr59.sitemanager.data.model.PageTextNode
import com.nadr59.sitemanager.data.model.TranslatedNode
import com.nadr59.sitemanager.data.repository.SiteRepository
import com.nadr59.sitemanager.data.repository.TranslationRepository
import com.nadr59.sitemanager.domain.translator.WebPageTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
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

    private val _uiState = MutableStateFlow(BrowserState())
    val uiState: StateFlow<BrowserState> = _uiState.asStateFlow()

    private val _extractedNodes = MutableStateFlow<List<PageTextNode>>(emptyList())
    val extractedNodes: StateFlow<List<PageTextNode>> = _extractedNodes.asStateFlow()

    private val _translatedNodes = MutableStateFlow<List<TranslatedNode>>(emptyList())
    val translatedNodes: StateFlow<List<TranslatedNode>> = _translatedNodes.asStateFlow()

    private val _pendingJs = MutableStateFlow<String?>(null)
    val pendingJs: StateFlow<String?> = _pendingJs.asStateFlow()

    // ═══ التاريخ والإشارات ═══
    val browserHistory = browserDao.getAllHistory()
    val bookmarks = browserDao.getAllBookmarks()

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private val _isReaderMode = MutableStateFlow(false)
    val isReaderMode: StateFlow<Boolean> = _isReaderMode.asStateFlow()

    private var currentSiteId = 0

    fun onJsExecuted() { _pendingJs.value = null }

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

    // ═══ حفظ في التاريخ ═══
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

    // ═══ إضافة/إزالة إشارة مرجعية ═══
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

    // ═══ وضع القراءة ═══
    fun toggleReaderMode() {
        val newMode = !_isReaderMode.value
        _isReaderMode.value = newMode
        if (newMode) {
            _pendingJs.value = buildReaderModeScript()
        } else {
            _pendingJs.value = "window.location.reload();"
        }
    }

    private fun buildReaderModeScript(): String {
        return """
        (function() {
            // إزالة الإعلانات والعناصر الزائدة
            var selectors = [
                'header', 'footer', 'nav', 'aside',
                '.ad', '.ads', '.advertisement', '.banner',
                '.sidebar', '.cookie', '.popup', '.modal',
                '[class*="ad-"]', '[id*="ad-"]',
                '[class*="social"]', '[class*="share"]',
                'script', 'style', 'iframe', 'video'
            ];
            selectors.forEach(function(s) {
                document.querySelectorAll(s).forEach(function(el) {
                    el.remove();
                });
            });

            // تجميل المحتوى
            document.body.style.cssText = `
                max-width: 800px !important;
                margin: 0 auto !important;
                padding: 20px !important;
                font-family: 'Georgia', serif !important;
                font-size: 18px !important;
                line-height: 1.8 !important;
                color: #333 !important;
                background: #fafafa !important;
            `;

            // تحسين الصور
            document.querySelectorAll('img').forEach(function(img) {
                img.style.maxWidth = '100%';
                img.style.height = 'auto';
            });

            return 'reader_mode_enabled';
        })();
        """.trimIndent()
    }

    fun startPageTranslation() {
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

    fun onNodesExtracted(jsonString: String) {
        val nodes: List<PageTextNode> = pageTranslator.parseExtractedNodes(jsonString)
        _extractedNodes.value = nodes

        if (nodes.isEmpty()) {
            _uiState.update { it.copy(isTranslating = false, error = "لا يوجد نص للترجمة") }
            return
        }

        viewModelScope.launch {
            val result = translationRepository.translatePageNodes(
                nodes = nodes,
                targetLanguage = _uiState.value.targetLanguage,
                onProgress = { p ->
                    _uiState.update { it.copy(translationProgress = p) }
                }
            )
            result.fold(
                onSuccess = { translated: List<TranslatedNode> ->
                    _translatedNodes.value = translated
                    _pendingJs.value = pageTranslator.buildReplaceScript(translated)
                    _uiState.update {
                        it.copy(
                            isTranslating = false,
                            isTranslationMode = true,
                            translationProgress = 1f
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isTranslating = false, error = "فشل الترجمة: ${e.message}")
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
                        _pendingJs.value = pageTranslator.buildReplaceSelectionScript(translated)
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = "فشل الترجمة: ${e.message}") }
                    }
                )
            }
        } catch (_: Exception) {}
    }

    fun resetTranslation() {
        _uiState.update {
            it.copy(isTranslationMode = false, isTranslating = false, translationProgress = 0f)
        }
        _extractedNodes.value = emptyList()
        _translatedNodes.value = emptyList()
        _pendingJs.value = "window.location.reload();"
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
