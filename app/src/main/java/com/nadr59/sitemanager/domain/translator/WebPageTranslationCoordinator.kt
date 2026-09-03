package com.nadr59.sitemanager.domain.translator

import com.nadr59.sitemanager.data.model.PageTextNode
import com.nadr59.sitemanager.data.model.TranslatedNode
import com.nadr59.sitemanager.data.repository.TranslationRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Coordinates WebView JavaScript, extraction, translation and reinsertion. */
@Singleton
class WebPageTranslationCoordinator @Inject constructor(
    private val repository: TranslationRepository
) {
    private val translator = WebPageTranslator()

    sealed interface JavascriptResult {
        data class Nodes(val nodes: List<PageTextNode>) : JavascriptResult
        data class Selection(val text: String) : JavascriptResult
        data object Empty : JavascriptResult
        data class Other(val value: String) : JavascriptResult
    }

    fun extractScript(): String = translator.buildExtractScript()

    fun selectionScript(): String = translator.buildSelectionScript()

    fun decodeAndClassify(rawResult: String?): JavascriptResult {
        val value = translator.decodeJavascriptResult(rawResult) ?: return JavascriptResult.Empty
        val trimmed = value.trim()
        if (trimmed.startsWith("[")) {
            return JavascriptResult.Nodes(translator.parseExtractedNodes(trimmed))
        }
        if (trimmed.startsWith("{")) {
            val text = translator.parseSelectionText(trimmed)
            if (!text.isNullOrBlank()) return JavascriptResult.Selection(text)
        }
        return JavascriptResult.Other(value)
    }
    fun pollDynamicNodesScript(): String {
    return """
        (function() {
            try {
                if (!window.__siteManagerTranslationQueue) {
                    return JSON.stringify([]);
                }

                const queue = window.__siteManagerTranslationQueue;
                window.__siteManagerTranslationQueue = [];

                return JSON.stringify(queue);
            } catch (e) {
                return JSON.stringify([]);
            }
        })();
    """.trimIndent()
    }

    suspend fun translatePage(
        nodes: List<PageTextNode>,
        targetLanguage: String,
        onProgress: (Float) -> Unit = {}
    ): Result<List<TranslatedNode>> = repository.translatePageNodes(
        nodes = nodes,
        targetLanguage = targetLanguage,
        onProgress = onProgress
    )

    suspend fun translateSelection(
        text: String,
        targetLanguage: String
    ): Result<String> = repository.translateText(text, targetLanguage)

    fun replaceScript(nodes: List<TranslatedNode>): String =
        translator.buildReplaceScript(nodes)

    fun replaceSelectionScript(translatedText: String): String =
        translator.buildReplaceSelectionScript(translatedText)
}
