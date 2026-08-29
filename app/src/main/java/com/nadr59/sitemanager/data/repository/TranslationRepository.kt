package com.nadr59.sitemanager.data.repository

import com.nadr59.sitemanager.data.model.PageTextNode
import com.nadr59.sitemanager.data.model.TranslatedNode
import com.nadr59.sitemanager.domain.translator.TextChunker
import com.nadr59.sitemanager.domain.translator.TranslationEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationRepository @Inject constructor(
    private val translationEngine: TranslationEngine
) {

    suspend fun translateText(
        text: String,
        targetLanguage: String
    ): Result<String> {
        return translationEngine.translate(
            text = text,
            sourceLanguage = "auto",
            targetLanguage = targetLanguage
        )
    }

    // ═══ ترجمة عقد الصفحة ═══
    suspend fun translatePageNodes(
        nodes: List<PageTextNode>,
        targetLanguage: String,
        onProgress: (Float) -> Unit = {}
    ): Result<List<TranslatedNode>> {
        try {
            val total = nodes.size
            val results = mutableListOf<TranslatedNode>()
            var completed = 0

            // ═══ تجميع النصوص الطويلة ═══
            val batches = nodes.chunked(10)

            for (batch in batches) {
                val texts = batch.map { it.text }
                val translated = translationEngine.translateBatch(
                    texts = texts,
                    sourceLanguage = "auto",
                    targetLanguage = targetLanguage
                )

                translated.fold(
                    onSuccess = { translations ->
                        for (i in batch.indices) {
                            results.add(
                                TranslatedNode(
                                    id = batch[i].id,
                                    originalText = batch[i].text,
                                    translatedText = translations.getOrElse(i) { batch[i].text }
                                )
                            )
                        }
                    },
                    onFailure = {
                        for (node in batch) {
                            results.add(
                                TranslatedNode(
                                    id = node.id,
                                    originalText = node.text,
                                    translatedText = node.text
                                )
                            )
                        }
                    }
                )

                completed += batch.size
                onProgress(completed.toFloat() / total)
            }

            return Result.success(results)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
