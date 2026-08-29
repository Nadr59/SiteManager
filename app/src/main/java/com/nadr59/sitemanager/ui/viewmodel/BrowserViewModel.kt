package com.nadr59.sitemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nadr59.sitemanager.data.model.BrowserState
import com.nadr59.sitemanager.data.model.PageTextNode
import com.n val pageTranslator = WebPageTranslator()

    // ═adr59.sitemanager.data.model.TranslatedNode
import com.nadr59.sitemanager.data.repository.TranslationRepository
import com.nadr59.sitemanager.domain.translator.WebPageTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val translationRepository: TranslationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserState())
    val uiState: StateFlow<BrowserState> = _uiState.asStateFlow()

   ══ عقد الصفحة المستخرجة ═══
    private val _extractedNodes = MutableStateFlow<List<PageTextNode>>(emptyList())
    val extractedNodes: StateFlow<List<PageTextNode>> = _extractedNodes.asStateFlow()

    // ═══ النتائج المترجمة ═══
    private val _translatedNodes = MutableStateFlow<List<TranslatedNode>>(emptyList())
    val translatedNodes: StateFlow<List<TranslatedNode>> = _translatedNodes.asStateFlow()

    // ═══ النص المحدد ═══
    private val _selectedText = MutableStateFlow("")
    val selectedText: StateFlow<String> = _selectedText.asStateFlow()

    private val _selectedTranslation = MutableStateFlow("")
    val selectedTranslation: StateFlow<String> = _selectedTranslation.asStateFlow()

    // ═══ طلب WebView ═══
    private val _pendingJs = MutableStateFlow<String?>(null)
    val pendingJs: StateFlow<String?> = _pendingJs.asStateFlow()

    fun onJsExecuted() {
        _pendingJs.value = null
    }

    // ═══ تحميل الموقع ═══
    fun loadUrl(url: String) {
        _uiState.update { it.copy(url = url) }
    }

    fun loadSite(siteId: Int) {
        // TODO: جلب من قاعدة البيانات
        // viewModelScope.launch {
        //     val site = siteRepository.getSiteById(siteId)
        //     _uiState.update { it.copy(url = site.url, title = site.name) }
        // }
    }

    // ═══ تحديثات المتصفح ═══
    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateProgress(progress: Int) {
        _uiState.update {
            it.copy(
                progress = progress,
                isLoading = progress < 100
            )
        }
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    fun updateNavigation(canGoBack: Boolean, canGoForward: Boolean) {
        _uiState.update {
            it.copy(canGoBack = canGoBack, canGoForward = canGoForward)
        }
    }

    // ═══ الترجمة ═══
    fun setTargetLanguage(language: String) {
        _uiState.update { it.copy(targetLanguage = language) }
    }

    fun showTranslationSheet() {
        _uiState.update { it.copy(showTranslationSheet = true) }
    }

    fun hideTranslationSheet() {
        _uiState.update { it.copy(showTranslationSheet = false) }
    }

    // ═══ ترجمة الصفحة كاملة ═══
    fun startPageTranslation() {
        _uiState.update {
            it.copy(
                isTranslating = true,
                translationProgress = 0f,
                error = null,
                showTranslationSheet = false
            )
        }

        // أمر استخراج النصوص
        _pendingJs.value = pageTranslator.buildExtractScript()
    }

    fun onNodesExtracted(jsonString: String) {
        val nodes = pageTranslator.parseExtractedNodes(jsonString)
        _extractedNodes.value = nodes

        if (nodes.isEmpty()) {
            _uiState.update {
                it.copy(
                    isTranslating = false,
                    error = "لم يتم العثور على نصوص للترجمة"
                )
            }
            return
        }

        viewModelScope.launch {
            val result = translationRepository.translatePageNodes(
                nodes = nodes,
                targetLanguage = _uiState.value.targetLanguage,
                onProgress = { progress ->
                    _uiState.update { it.copy(translationProgress = progress) }
                }
            )

            result.fold(
                onSuccess = { translated ->
                    _translatedNodes.value = translated
                    val replaceScript = pageTranslator.buildReplaceScript(translated)
                    _pendingJs.value = replaceScript
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
                        it.copy(
                            isTranslating = false,
                            error = "فشل الترجمة: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    // ═══ ترجمة النص المحدد ═══
    fun translateSelectedText() {
        _pendingJs.value = pageTranslator.buildSelectionScript()
    }

    fun onTextSelected(jsonString: String) {
        try {
            val json = org.json.JSONObject(jsonString)
            val text = json.getString("text")
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
        }
