package com.nadr59.sitemanager.data.model

data class PageTextNode(
    val id: String,
    val text: String
)

data class TranslatedNode(
    val id: String,
    val originalText: String,
    val translatedText: String
)

data class TranslationResult(
    val nodes: List<TranslatedNode>,
    val sourceLanguage: String,
    val targetLanguage: String,
    val success: Boolean,
    val error: String? = null
)


