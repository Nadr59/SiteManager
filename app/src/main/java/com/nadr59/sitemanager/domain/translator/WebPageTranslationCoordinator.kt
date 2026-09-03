package com.nadr59.sitemanager.domain.translator

import com.nadr59.sitemanager.data.model.PageTextNode
import com.nadr59.sitemanager.data.model.TranslatedNode
import com.nadr59.sitemanager.data.repository.TranslationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebPageTranslationCoordinator @Inject constructor(
    private val webPageTranslator: WebPageTranslator,
    private val translationRepository: TranslationRepository
) {

    sealed class JavascriptResult {
        data class Nodes(val nodes: List<PageTextNode>) : JavascriptResult()
        data class Selection(val text: String) : JavascriptResult()
        data object Unknown : JavascriptResult()
    }

    fun extractScript(): String {
        return webPageTranslator.buildExtractScript()
    }

    fun replaceScript(translations: List<TranslatedNode>): String {
        return webPageTranslator.buildReplaceScript(translations)
    }

    fun selectionScript(): String {
        return webPageTranslator.buildSelectionScript()
    }

    fun replaceSelectionScript(translatedText: String): String {
        return webPageTranslator.buildReplaceSelectionScript(translatedText)
    }

    fun installDynamicObserverScript(): String {
        return webPageTranslator.buildInstallDynamicObserverScript()
    }

    fun pollDynamicNodesScript(): String {
        return webPageTranslator.buildPollDynamicNodesScript()
    }

    fun decodeAndClassify(rawResult: String?): JavascriptResult {
        if (rawResult.isNullOrBlank()) {
            return JavascriptResult.Unknown
        }

        val decoded = webPageTranslator.decodeJavascriptResult(rawResult)

        if (decoded.isBlank()) {
            return JavascriptResult.Unknown
        }

        // ═══ محاولة تحليل كمصفوفة عقد ═══
        val nodes = webPageTranslator.parseExtractedNodes(decoded)
        if (nodes.isNotEmpty()) {
            return JavascriptResult.Nodes(nodes)
        }

        // ═══ محاولة تحليل كنص محدد ═══
        val selectionText = webPageTranslator.parseSelectionText(decoded)
        if (selectionText.isNotBlank()) {
            return JavascriptResult.Selection(selectionText)
        }

        return JavascriptResult.Unknown
    }

    suspend fun translatePage(
        nodes: List<PageTextNode>,
        targetLanguage: String,
        onProgress: (Float) -> Unit = {}
    ): Result<List<TranslatedNode>> {
        return translationRepository.translatePageNodes(
            nodes = nodes,
            targetLanguage = targetLanguage,
            onProgress = onProgress
        )
    }

    suspend fun translateSelection(
        text: String,
        targetLanguage: String
    ): Result<String> {
        return translationRepository.translateText(
            text = text,
            targetLanguage = targetLanguage
        )
    }
}
