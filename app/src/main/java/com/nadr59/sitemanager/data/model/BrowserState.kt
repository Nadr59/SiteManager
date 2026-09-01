package com.nadr59.sitemanager.data.model

data class BrowserState(
    val url: String = "",
    val title: String = "",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isTranslationMode: Boolean = false,
    val targetLanguage: String = "ar",
    val isTranslating: Boolean = false,
    val translationProgress: Float = 0f,
    val showTranslationSheet: Boolean = false,
    val error: String? = null
)


