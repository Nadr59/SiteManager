// app/src/main/java/com/nadr59/sitemanager/domain/translator/WebPageTranslationCoordinator.kt

package com.nadr59.sitemanager.domain.translator

import android.webkit.WebView
import com.nadr59.sitemanager.data.model.PageTextNode
import com.nadr59.sitemanager.data.model.TranslatedNode
import com.nadr59.sitemanager.data.repository.TranslationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * منسق ترجمة صفحات الويب
 */
@Singleton
class WebPageTranslationCoordinator @Inject constructor(
    private val translationRepository: TranslationRepository,
    private val webPageTranslator: WebPageTranslator
) {
    companion object {
        private const val TAG = "TranslationCoordinator"
    }

    private val _translationStates = MutableStateFlow<Map<String, PageTranslationState>>(emptyMap())
    val translationStates: StateFlow<Map<String, PageTranslationState>> = _translationStates.asStateFlow()

    private var config = TranslationConfig.DEFAULT

    fun updateConfig(newConfig: TranslationConfig) {
        config = newConfig
    }

    fun getPageState(url: String): PageTranslationState? {
        return _translationStates.value[url]
    }

    fun isPageTranslated(url: String): Boolean {
        return _translationStates.value[url]?.isTranslated == true
    }

    suspend fun translatePage(
        webView: WebView,
        url: String,
        targetLanguage: String = config.targetLanguage,
        onProgress: (TranslationOperation) -> Unit
    ): Result<PageTranslationState> = withContext(Dispatchers.Main) {
        try {
            val existingState = _translationStates.value[url]
            if (existingState?.isTranslated == true && 
                existingState.targetLanguage == targetLanguage) {
                
                Log.d(TAG, "استخدام ترجمة محفوظة للصفحة: $url")
                onProgress(TranslationOperation.Success(existingState))
                return@withContext Result.success(existingState)
            }

            onProgress(TranslationOperation.Progress(0, 0, 0f))
            
            val extractedNodes = extractNodesFromPage(webView)
            
            if (extractedNodes.isEmpty()) {
                val error = "لم يتم العثور على نصوص قابلة للترجمة"
                Log.w(TAG, error)
                onProgress(TranslationOperation.Failure(error))
                return@withContext Result.failure(Exception(error))
            }

            Log.d(TAG, "تم استخراج ${extractedNodes.size} عقدة نصية")

            val filteredNodes = filterNodes(extractedNodes)
            Log.d(TAG, "بعد التصفية: ${filteredNodes.size} عقدة")

            if (filteredNodes.isEmpty()) {
                val error = "لا توجد نصوص صالحة للترجمة بعد التصفية"
                Log.w(TAG, error)
                onProgress(TranslationOperation.Failure(error))
                return@withContext Result.failure(Exception(error))
            }

            val initialState = PageTranslationState(
                url = url,
                targetLanguage = targetLanguage,
                originalNodes = filteredNodes,
                isTranslated = false
            )
            updateState(url, initialState)

            val translatedMap = mutableMapOf<String, String>()
            
            val translationResult = translateNodes(
                nodes = filteredNodes,
                targetLanguage = targetLanguage,
                onProgress = { current, total ->
                    val percentage = (current.toFloat() / total.toFloat()) * 100f
                    onProgress(TranslationOperation.Progress(current, total, percentage))
                },
                translatedMap = translatedMap
            )

            replaceNodesInPage(webView, translatedMap, targetLanguage)

            val finalState = PageTranslationState(
                url = url,
                targetLanguage = targetLanguage,
                originalNodes = filteredNodes,
                translatedNodes = translatedMap,
                isTranslated = true,
                progress = 1f
            )
            updateState(url, finalState)

            Log.d(TAG, "تمت ترجمة الصفحة بنجاح: ${translatedMap.size} عقدة")
            onProgress(TranslationOperation.Success(finalState))
            
            Result.success(finalState)

        } catch (e: Exception) {
            Log.e(TAG, "فشل في ترجمة الصفحة: $url", e)
            val errorMessage = e.message ?: "خطأ غير معروف"
            onProgress(TranslationOperation.Failure(errorMessage, e))
            Result.failure(e)
        }
    }

    suspend fun translateSelection(
        webView: WebView,
        targetLanguage: String = config.targetLanguage
    ): Result<Pair<String, String>> = withContext(Dispatchers.Main) {
        try {
            val selectedText = extractSelectedText(webView)
            
            if (selectedText.isBlank()) {
                return@withContext Result.failure(Exception("لا يوجد نص محدد"))
            }

            Log.d(TAG, "نص محدد: ${selectedText.take(50)}...")

            val translatedText = translationRepository.translateText(
                text = selectedText,
                targetLanguage = targetLanguage
            ).getOrThrow()

            replaceSelectedText(webView, translatedText, targetLanguage)

            Log.d(TAG, "تمت ترجمة النص المحدد")
            Result.success(selectedText to translatedText)

        } catch (e: Exception) {
            Log.e(TAG, "فشل في ترجمة النص المحدد", e)
            Result.failure(e)
        }
    }

    suspend fun restoreOriginalPage(
        webView: WebView,
        url: String
    ): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val state = _translationStates.value[url]
            
            if (state == null || !state.isTranslated) {
                return@withContext Result.failure(Exception("الصفحة غير مترجمة"))
            }

            val originalMap = state.originalNodes.associate { node ->
                node.id to node.text
            }

            replaceNodesInPage(webView, originalMap, "original")

            val restoredState = state.copy(
                isTranslated = false,
                progress = 0f
            )
            updateState(url, restoredState)

            Log.d(TAG, "تمت استعادة النص الأصلي للصفحة")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "فشل في استعادة النص الأصلي", e)
            Result.failure(e)
        }
    }

    fun clearPageState(url: String) {
        _translationStates.value = _translationStates.value.toMutableMap().apply {
            remove(url)
        }
    }

    fun clearAllStates() {
        _translationStates.value = emptyMap()
    }

    private suspend fun extractNodesFromPage(webView: WebView): List<PageTextNode> {
        return suspendCoroutine { continuation ->
            val extractScript = webPageTranslator.buildExtractScript()
            
            webView.evaluateJavascript(extractScript) { result ->
                try {
                    val nodes = if (result != null && result != "null") {
                        webPageTranslator.parseExtractedNodes(result)
                    } else {
                        emptyList()
                    }
                    continuation.resume(nodes)
                } catch (e: Exception) {
                    Log.e(TAG, "فشل في تحليل العقد المستخرجة", e)
                    continuation.resume(emptyList())
                }
            }
        }
    }

    private fun filterNodes(nodes: List<PageTextNode>): List<PageTextNode> {
        return nodes.filter { node ->
            val text = node.text.trim()
            
            if (text.length < config.minTextLength) return@filter false
            if (text.length > config.maxTextLength) return@filter false
            if (text.all { it.isDigit() || it.isWhitespace() }) return@filter false
            if (text.startsWith("http://") || text.startsWith("https://")) return@filter false
            if (text.contains("@") && text.contains(".")) return@filter false
            
            true
        }.distinctBy { it.text.trim() }
    }

    private suspend fun translateNodes(
        nodes: List<PageTextNode>,
        targetLanguage: String,
        onProgress: (Int, Int) -> Unit,
        translatedMap: MutableMap<String, String>
    ): Unit = withContext(Dispatchers.IO) {
        
        val batches = nodes.chunked(config.batchSize)
        var processedCount = 0

        for ((batchIndex, batch) in batches.withIndex()) {
            try {
                val batchResults = translationRepository.translatePageNodes(
                    nodes = batch,
                    targetLanguage = targetLanguage,
                    onProgress = { _ -> }
                ).getOrThrow()

                batchResults.forEach { translated ->
                    translatedMap[translated.id] = translated.translatedText
                }

                processedCount += batch.size
                onProgress(processedCount, nodes.size)

                Log.d(TAG, "دفعة ${batchIndex + 1}/${batches.size}: تمت ترجمة ${batch.size} عقدة")

            } catch (e: Exception) {
                Log.e(TAG, "فشل في ترجمة الدفعة ${batchIndex + 1}", e)
            }
        }
    }

    private suspend fun replaceNodesInPage(
        webView: WebView,
        translationMap: Map<String, String>,
        targetLanguage: String
    ) = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            val isRTL = TranslationConfig.isRTL(targetLanguage)
            val translatedNodes = translationMap.map { (id, text) ->
                TranslatedNode(
                    id = id,
                    originalText = "",
                    translatedText = text
                )
            }
            
            val replaceScript = webPageTranslator.buildReplaceScript(translatedNodes)

            webView.evaluateJavascript(replaceScript) { _ ->
                Log.d(TAG, "تم تنفيذ سكريبت الاستبدال")
                continuation.resume(Unit)
            }
        }
    }

    private suspend fun extractSelectedText(webView: WebView): String {
        return suspendCoroutine { continuation ->
            val script = webPageTranslator.buildSelectionScript()
            
            webView.evaluateJavascript(script) { result ->
                try {
                    val jsonResult = result?.trim('"') ?: "{}"
                    val selectedText = webPageTranslator.parseSelectionJson(jsonResult)
                    continuation.resume(selectedText)
                } catch (e: Exception) {
                    Log.e(TAG, "فشل في تحليل النص المحدد", e)
                    continuation.resume("")
                }
            }
        }
    }

    private suspend fun replaceSelectedText(
        webView: WebView,
        translatedText: String,
        targetLanguage: String
    ) = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            val isRTL = TranslationConfig.isRTL(targetLanguage)
            val escapedText = translatedText
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\"", "\\\"")
            
            val direction = if (isRTL) "rtl" else "ltr"
            val textAlign = if (isRTL) "right" else "left"
            
            val script = """
                (function() {
                    try {
                        var selection = window.getSelection();
                        if (selection.rangeCount > 0) {
                            var range = selection.getRangeAt(0);
                            range.deleteContents();
                            
                            var span = document.createElement('span');
                            span.textContent = '$escapedText';
                            span.style.direction = '$direction';
                            span.style.textAlign = '$textAlign';
                            span.style.backgroundColor = '#FFEB3B';
                            span.style.padding = '2px 4px';
                            span.style.borderRadius = '3px';
                            
                            range.insertNode(span);
                            selection.removeAllRanges();
                        }
                    } catch(e) {
                        console.error('Error replacing selection:', e);
                    }
                })();
            """.trimIndent()

            webView.evaluateJavascript(script) { _ ->
                Log.d(TAG, "تم استبدال النص المحدد")
                continuation.resume(Unit)
            }
        }
    }

    private fun updateState(url: String, state: PageTranslationState) {
        _translationStates.value = _translationStates.value.toMutableMap().apply {
            put(url, state)
        }
    }
}
