// app/src/main/java/com/nadr59/sitemanager/data/model/BrowserState.kt

package com.nadr59.sitemanager.data.model

data class BrowserState(
    val url: String = "",
    val title: String = "",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val error: String? = null,

    // Translation
    val isTranslating: Boolean = false,
    val isTranslationMode: Boolean = false,
    val translationProgress: Float = 0f,
    val targetLanguage: String = "ar",
    val showTranslationSheet: Boolean = false,

    // Features
    val isBookmarked: Boolean = false,
    val isReaderMode: Boolean = false,
    val isSummarizing: Boolean = false,
    val isAiThinking: Boolean = false,
    val hasNotes: Boolean = false,
    val screenshotPath: String? = null
)
