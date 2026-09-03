package com.nadr59.sitemanager.data.repository

import com.nadr59.sitemanager.data.local.BrowserDao
import com.nadr59.sitemanager.data.local.TranslationCache
import com.nadr59.sitemanager.data.model.PageTextNode
import com.nadr59.sitemanager.data.model.TranslatedNode
import com.nadr59.sitemanager.domain.translator.TranslationEngine
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationRepository @Inject constructor(
    private val translationEngine: TranslationEngine,
    private val browserDao: BrowserDao
) {

    // ═══ ترجمة نص مع ذاكرة مؤقتة ═══
    suspend fun translateText(
        text: String,
        targetLanguage: String
    ): Result<String> {
        if (text.isBlank()) return Result.success("")

        // ═══ البحث في الذاكرة المؤقتة ═══
        val cached = browserDao.getCachedTranslation(text, targetLanguage)
        if (cached != null) {
            return Result.success(cached.translatedText)
        }

        // ═══ الترجمة الفعلية ═══
        val result = translationEngine.translate(
            text = text,
            sourceLanguage = "auto",
            targetLanguage = targetLanguage
        )

        // ═══ حفظ في الذاكرة المؤقتة ═══
        result.onSuccess { translated ->
            browserDao.insertTranslationCache(
                TranslationCache(
                    id = generateHash(text + targetLanguage),
                    originalText = text,
                    translatedText = translated,
                    targetLanguage = targetLanguage
                )
            )
        }

        return result
    }

    // ═══ ترجمة عقد الصفحة مع ذاكرة مؤقتة ═══
    suspend fun translatePageNodes(
        nodes: List<PageTextNode>,
        targetLanguage: String,
        onProgress: (Float) -> Unit = {}
    ): Result<List<TranslatedNode>> {
        try {
            val total = nodes.size
            val results = mutableListOf<TranslatedNode>()
            var completed = 0

            // ═══ تنظيف الذاكرة القديمة (أكثر من 7 أيام) ═══
            val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            browserDao.clearOldCache(weekAgo)

            val batches = nodes.chunked(10)

            for (batch in batches) {
                val cachedResults = mutableMapOf<Int, String>()
                val uncachedIndices = mutableListOf<Int>()

                // ═══ فحص الذاكرة المؤقتة لكل عقدة ═══
                batch.forEachIndexed { index, node ->
                    val cached = browserDao.getCachedTranslation(node.text, targetLanguage)
                    if (cached != null) {
                        cachedResults[index] = cached.translatedText
                    } else {
                        uncachedIndices.add(index)
                    }
                }

                // ═══ ترجمة العقد غير المخزنة ═══
                if (uncachedIndices.isNotEmpty()) {
                    val uncachedTexts = uncachedIndices.map { batch[it].text }
                    val translated = translationEngine.translateBatch(
                        texts = uncachedTexts,
                        sourceLanguage = "auto",
                        targetLanguage = targetLanguage
                    )

                    translated.fold(
                        onSuccess = { translations ->
                            uncachedIndices.forEachIndexed { i, batchIndex ->
                                val translatedText = translations.getOrElse(i) { batch[batchIndex].text }
                                cachedResults[batchIndex] = translatedText

                                // ═══ حفظ في الذاكرة المؤقتة ═══
                                if (translatedText != batch[batchIndex].text) {
                                    browserDao.insertTranslationCache(
                                        TranslationCache(
                                            id = generateHash(batch[batchIndex].text + targetLanguage),
                                            originalText = batch[batchIndex].text,
                                            translatedText = translatedText,
                                            targetLanguage = targetLanguage
                                        )
                                    )
                                }
                            }
                        },
                        onFailure = {
                            uncachedIndices.forEach { batchIndex ->
                                cachedResults[batchIndex] = batch[batchIndex].text
                            }
                        }
                    )
                }

                // ═══ بناء النتائج بالترتيب ═══
                batch.forEachIndexed { index, node ->
                    results.add(
                        TranslatedNode(
                            id = node.id,
                            originalText = node.text,
                            translatedText = cachedResults[index] ?: node.text
                        )
                    )
                }

                completed += batch.size
                onProgress(completed.toFloat() / total)
            }

            return Result.success(results)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun generateHash(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
