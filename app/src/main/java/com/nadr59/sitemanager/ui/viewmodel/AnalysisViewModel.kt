package com.nadr59.sitemanager.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nadr59.sitemanager.data.remote.AiConfig
import com.nadr59.sitemanager.data.remote.AnalysisResult
import com.nadr59.sitemanager.data.remote.SiteType
import com.nadr59.sitemanager.data.repository.AnalyzerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalysisUiState(
    val isLoading: Boolean = false,
    val loadingStep: String = "",
    val result: AnalysisResult? = null,
    val cachedResult: AnalysisResult? = null,
    val siteType: SiteType? = null,
    val error: String? = null,
    val isFromCache: Boolean = false
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val analyzerRepo: AnalyzerRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private fun loadConfig(): AiConfig {
        return AiConfig(
            provider = prefs.getString("ai_provider", "groq") ?: "groq",
            apiKey = prefs.getString("ai_key", "") ?: "",
            model = prefs.getString("ai_model", "") ?: "",
            baseUrl = prefs.getString("ai_base_url", "") ?: ""
        )
    }

    fun loadCachedOrAnalyze(siteId: Int) {
        viewModelScope.launch {
            // محاولة جلب التحليل المحفوظ أولاً
            val cached = analyzerRepo.getCachedAnalysis(siteId)
            val isStale = analyzerRepo.isAnalysisStale(siteId)

            if (cached != null) {
                _uiState.value = _uiState.value.copy(
                    cachedResult = cached,
                    isFromCache = true,
                    siteType = try {
                        SiteType.valueOf(
                            com.nadr59.sitemanager.data.local.SiteEntity::class.java
                                .let { "" } // placeholder
                        )
                    } catch (_: Exception) { null }
                )

                // إذا كان التحليل حديثًا، استخدمه مباشرة
                if (!isStale) {
                    _uiState.value = _uiState.value.copy(result = cached)
                    return@launch
                }
            }

            // وإلا قم بتحليل جديد
            analyze(siteId, forceRefresh = false)
        }
    }

    fun analyze(siteId: Int, forceRefresh: Boolean = true) {
        val config = loadConfig()
        if (config.apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "أدخل مفتاح AI من الإعدادات أولاً"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingStep = "جارٍ جمع محتوى الموقع...",
                error = null
            )

            // تأخير بسيط لتحديث الواجهة
            kotlinx.coroutines.delay(500)

            _uiState.value = _uiState.value.copy(
                loadingStep = "جارٍ التحليل بالذكاء الاصطناعي..."
            )

            val result = analyzerRepo.analyze(siteId, config)

            result.fold(
                onSuccess = { analysis ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        result = analysis,
                        isFromCache = false,
                        loadingStep = ""
                    )
                },
                onFailure = { e ->
                    // محاولة استخدام التخزين المؤقت كخطة بديلة
                    val fallback = analyzerRepo.getCachedAnalysis(siteId)
                    if (fallback != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            result = fallback,
                            isFromCache = true,
                            loadingStep = "",
                            error = "تم عرض تحليل محفوظ (فشل الاتصال: ${e.message})"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            loadingStep = "",
                            error = e.message
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refreshAnalysis(siteId: Int) {
        viewModelScope.launch {
            analyzerRepo.clearCache(siteId)
        }
        analyze(siteId, forceRefresh = true)
    }
}
