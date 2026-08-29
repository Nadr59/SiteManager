package com.nadr59.sitemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val translationRepository: TranslationRepository,
    private val siteRepository: SiteRepository
) : ViewModel() {

    private val pageTranslator = WebPageTranslator()

    private val _uiState = MutableStateFlow(BrowserState())
    val uiState: StateFlow<BrowserState> = _uiState.asStateFlow()

    private val _extractedNodes = MutableStateFlow<List<PageTextNode>>(emptyList())
    val extractedNodes: StateFlow<List<PageTextNode>> = _extractedNodes.asStateFlow()

    private val _translatedNodes = MutableStateFlow<List<TranslatedNode>>(emptyList())
    val translatedNodes: StateFlow<List<TranslatedNode>> = _translatedNodes.asStateFlow()

    private val _selectedText = MutableStateFlow("")
    val selectedText: StateFlow<String> = _selectedText.asStateFlow()

    private val _selectedTranslation = MutableStateFlow("")
    val selectedTranslation: StateFlow<String> = _selectedTranslation.asStateFlow()

    private val _pendingJs = MutableStateFlow<String?>(null)
    val pendingJs: StateFlow<String?> = _pendingJs.asStateFlow()

    fun onJsExecuted() {
        _pendingJs.value = null
    }

    // ═══ تحميل الموقع من قاعدة البيانات ═══
    fun loadSite(siteId: Int) {
        viewModelScope.launch {
            try {
                val site = siteRepository.getSiteById(siteId)
                if (site != null) {
                    _uiState.update {
                        it.copy(
                            url = site.url,
                            title = site.name
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(error = "الموقع غير موجود")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "فشل تحميل الموقع: ${e.message}")
                }
            }
        }
    }

    fun loadUrl(url: String) {
        _uiState.update { it.copy(url = url) }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateProgress(progress: Int) {
       < 100
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

    fun startPageTranslation() {
        _uiState.update {
            it.copy(
                isTranslating = true,
                translationProgress = 0f,
                error = null,
                showTranslationSheet = false
            )
        }
        _pendingJs.value = pageTranslator.build _uiState.update {
            it.copy(
                progress = progress,
                isLoading = progress ExtractScript()
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

    fun translateSelectedText() {
        _pendingJs.value = pageTranslator.buildSelectionScript()
    }

    fun onTextSelected(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val text = json.optString("text", "")
            if (text.isBlank()) return

            _selectedText.value = text

            viewModelScope.launch {
                val result = translationRepository.translateText(
                    text = text,
                    targetLanguage = _uiState.value.targetLanguage
                )
                result.fold(
                    onSuccess = { translated ->
                        _selectedTranslation.value = translated
                        val replaceScript = pageTranslator.buildReplaceSelectionScript(translated)
                        _pendingJs.value = replaceScript
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(error = "فشل ترجمة النص: ${e.message}")
                        }
                    }
                )
            }
        } catch (_: Exception) {}
    }

    fun resetTranslation() {
        _uiState.update {
            it.copy(
                isTranslationMode = false,
                isTranslating = false,
                translationProgress = 0f
            )
        }
        _extractedNodes.value = emptyList()
        _translatedNodes.value = emptyList()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
                }
