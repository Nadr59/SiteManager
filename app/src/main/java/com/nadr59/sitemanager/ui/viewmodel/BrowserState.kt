package com.nadr59.sitemanager.viewmodel

data class PageTextNode(
    val id: String,
    val text: String
)

data class TranslatedNode(
    val id: String,
    val originalText: String,
    val translatedText: String
)

data class BrowserState(
    val title: String = "",
    val url: String = "",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val isTranslationMode: Boolean = false,
    val isTranslating: Boolean = false,
    val translationProgress: Float = 0f,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val showTranslationSheet: Boolean = false,
    val targetLanguage: String = "en",
    val error: String? = null
)
