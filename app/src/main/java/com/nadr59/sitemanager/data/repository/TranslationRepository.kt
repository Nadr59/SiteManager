package com.nadr59.sitemanager.data.repository

import com.nadr59.sitemanager.data.local.BrowserDao
import com.nadr59.sitemanager.data.local.TranslationCache
import com.nadr59.sitemanager.data.model.PageTextNode
import com.nadr59.sitemanager.data.model.TranslatedNode
import com.nadr59.sitemanager.domain.translator.TranslationEngine
import com.nadr59.sitemanager.domain.translator.TranslationUsageTracker
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationRepository @Inject constructor(
    private val translationEngine: TranslationEngine,
    private val browserDao: BrowserDao,
    private val usageTracker: TranslationUsageTracker
) {

    suspend fun translateText(
        text: String,
        targetLanguage: String
    ): Result<String> {

        if (text.isBlank()) {
            return Result.success("")
        }

        val cached =
            browserDao.getCachedTranslation(
                text,
                targetLanguage
            )

        if (cached != null) {
            return Result.success(
                cached.translatedText
            )
        }

        val characterCount =
            text.length.toLong()

        if (
            !usageTracker.canTranslate(
                characterCount
            )
        ) {
            return Result.failure(
                TranslationLimitException(
                    "تم الوصول إلى حد الترجمة الشهري داخل التطبيق"
                )
            )
        }

        val result =
            translationEngine.translate(
                text = text,
                sourceLanguage = "auto",
                targetLanguage = targetLanguage
            )

        result.onSuccess { translated ->

            usageTracker.recordCharacters(
                characterCount
            )

            browserDao.insertTranslationCache(
                TranslationCache(
                    id = generateHash(
                        text + targetLanguage
                    ),
                    originalText = text,
                    translatedText = translated,
                    targetLanguage = targetLanguage
                )
            )
        }

        return result
    }

    suspend fun translatePageNodes(
        nodes: List<PageTextNode>,
        targetLanguage: String,
        onProgress: (Float) -> Unit = {}
    ): Result<List<TranslatedNode>> {

        if (nodes.isEmpty()) {
            return Result.success(emptyList())
        }

        return try {

            val total =
                nodes.size

            val results =
                mutableListOf<TranslatedNode>()

            var completed = 0

            val weekAgo =
                System.currentTimeMillis() -
                    (7L * 24 * 60 * 60 * 1000)

            browserDao.clearOldCache(
                weekAgo
            )

            /*
             * Google v2 يسمح حتى 128 نصًا.
             *
             * نستخدم 100 لترك هامش آمن.
             */
            val batches =
                nodes.chunked(100)

            for (batch in batches) {

                val cachedResults =
                    mutableMapOf<Int, String>()

                val uncachedIndices =
                    mutableListOf<Int>()

                batch.forEachIndexed { index, node ->

                    val cached =
                        browserDao.getCachedTranslation(
                            node.text,
                            targetLanguage
                        )

                    if (cached != null) {

                        cachedResults[index] =
                            cached.translatedText

                    } else {

                        uncachedIndices.add(index)
                    }
                }

                if (uncachedIndices.isNotEmpty()) {

                    val uncachedTexts =
                        uncachedIndices.map {
                            batch[it].text
                        }

                    val requestedCharacters =
                        uncachedTexts.sumOf {
                            it.length.toLong()
                        }

                    if (
                        !usageTracker.canTranslate(
                            requestedCharacters
                        )
                    ) {
                        return Result.failure(
                            TranslationLimitException(
                                "تجاوزت النصوص المتبقية حد الترجمة الشهري"
                            )
                        )
                    }

                    val translated =
                        translationEngine.translateBatch(
                            texts = uncachedTexts,
                            sourceLanguage = "auto",
                            targetLanguage = targetLanguage
                        )

                    translated.fold(

                        onSuccess = { translations ->

                            var successfulCharacters = 0L

                            uncachedIndices.forEachIndexed {
                                    i,
                                    batchIndex ->

                                val original =
                                    batch[batchIndex].text

                                val translatedText =
                                    translations.getOrElse(i) {
                                        original
                                    }

                                cachedResults[
                                    batchIndex
                                ] = translatedText

                                if (
                                    translatedText.isNotBlank() &&
                                    translatedText != original
                                ) {

                                    successfulCharacters +=
                                        original.length.toLong()

                                    browserDao.insertTranslationCache(
                                        TranslationCache(
                                            id = generateHash(
                                                original +
                                                    targetLanguage
                                            ),
                                            originalText = original,
                                            translatedText = translatedText,
                                            targetLanguage = targetLanguage
                                        )
                                    )
                                }
                            }

                            if (
                                successfulCharacters > 0
                            ) {
                                usageTracker.recordCharacters(
                                    successfulCharacters
                                )
                            }
                        },

                        onFailure = {
                            uncachedIndices.forEach {
                                batchIndex ->

                                cachedResults[
                                    batchIndex
                                ] =
                                    batch[batchIndex].text
                            }
                        }
                    )
                }

                batch.forEachIndexed { index, node ->

                    results.add(
                        TranslatedNode(
                            id = node.id,
                            originalText = node.text,
                            translatedText =
                                cachedResults[index]
                                    ?: node.text
                        )
                    )
                }

                completed += batch.size

                onProgress(
                    (
                        completed.toFloat() /
                            total
                    ).coerceIn(
                        0f,
                        1f
                    )
                )
            }

            Result.success(results)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    private fun generateHash(
        input: String
    ): String {

        val bytes =
            MessageDigest.getInstance("MD5")
                .digest(
                    input.toByteArray()
                )

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }
}

class TranslationLimitException(
    message: String
) : Exception(message)
