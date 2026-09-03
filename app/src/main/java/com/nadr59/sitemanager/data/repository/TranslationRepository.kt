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

    suspend fun translateText(
        text: String,
        targetLanguage: String
    ): Result<String> {
        if (text.isBlank()) return Result.success("")

        val cached = browserDao.getCachedTranslation(
            text, targetLanguage
        )
        if (cached != null) {
            return Result.success(cached.translatedText)
        }

        val result = translationEngine.translate(
            text = text,
            sourceLanguage = "auto",
            targetLanguage = targetLanguage
        )

        result.onSuccess { translated ->
            saveCache(text, translated, targetLanguage)
        }

        return result
    }

    suspend fun translatePageNodes(
        nodes: List<PageTextNode>,
        targetLanguage: String,
        onProgress: (Float) -> Unit = {}
    ): Result<List<TranslatedNode>> {
        return try {
            if (nodes.isEmpty()) {
                return Result.success(emptyList())
            }

            // ═══ تنظيف الكاش القديم ═══
            val weekAgo = System.currentTimeMillis() -
                    (7 * 24 * 60 * 60 * 1000L)
            browserDao.clearOldCache(weekAgo)

            onProgress(0.05f)

            // ═══ فصل المخزن عن غير المخزن ═══
            val cachedMap = mutableMapOf<String, String>()
            val uncachedNodes = mutableListOf<PageTextNode>()

            for (node in nodes) {
                val cached = browserDao.getCachedTranslation(
                    node.text, targetLanguage
                )
                if (cached != null) {
                    cachedMap[node.id] = cached.translatedText
                } else {
                    uncachedNodes.add(node)
                }
            }

            onProgress(0.1f)

            // ═══ ترجمة الدفعات الكبيرة (50 نص/دفعة) ═══
            val translatedMap = mutableMapOf<String, String>()
            translatedMap.putAll(cachedMap)

            if (uncachedNodes.isNotEmpty()) {
                // دفعات أكبر = أسرع
                val batchSize = 50
                val batches = uncachedNodes.chunked(batchSize)
                val totalBatches = batches.size

                batches.forEachIndexed { batchIndex, batch ->
                    val texts = batch.map { it.text }

                    val result = translationEngine.translateBatch(
                        texts = texts,
                        sourceLanguage = "auto",
                        targetLanguage = targetLanguage
                    )

                    result.fold(
                        onSuccess = { translations ->
                            batch.forEachIndexed { i, node ->
                                val translated = translations
                                    .getOrElse(i) { node.text }
                                translatedMap[node.id] = translated

                                // حفظ في الكاش
                                if (translated != node.text) {
                                    saveCache(
                                        node.text,
                                        translated,
                                        targetLanguage
                                    )
                                }
                            }
                        },
                        onFailure = {
                            // احتفظ بالنص الأصلي عند الفشل
                            batch.forEach { node ->
                                translatedMap[node.id] = node.text
                            }
                        }
                    )

                    // تحديث التقدم
                    val progress = 0.1f + (0.9f *
                            (batchIndex + 1).toFloat() /
                            totalBatches)
                    onProgress(progress.coerceIn(0f, 1f))
                }
            } else {
                onProgress(1f)
            }

            // ═══ بناء النتائج ═══
            val results = nodes.map { node ->
                TranslatedNode(
                    id = node.id,
                    originalText = node.text,
                    translatedText = translatedMap[node.id]
                        ?: node.text
                )
            }

            Result.success(results)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveCache(
        original: String,
        translated: String,
        targetLanguage: String
    ) {
        try {
            val hash = generateHash(original + targetLanguage)
            browserDao.insertTranslationCacheSync(
                TranslationCache(
                    id = hash,
                    originalText = original,
                    translatedText = translated,
                    targetLanguage = targetLanguage
                )
            )
        } catch (_: Exception) {}
    }

    private fun generateHash(input: String): String {
        val bytes = MessageDigest
            .getInstance("MD5")
            .digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
