package com.nadr59.sitemanager.domain.translator

interface TranslationEngine {
    suspend fun translate(
        text: String,
        sourceLanguage: String = "auto",
        targetLanguage: String
    ): Result<String>

    suspend fun translateBatch(
        texts: List<String>,
        sourceLanguage: String = "auto",
        targetLanguage: String
    ): Result<List<String>>
}


