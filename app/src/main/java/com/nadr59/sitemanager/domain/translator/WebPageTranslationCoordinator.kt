// app/src/main/java/com/nadr59/sitemanager/domain/translator/WebPageTranslationCoordinator.kt

package com.nadr59.sitemanager.domain.translator

import android.webkit.WebView
import com.nadr59.sitemanager.data.model.PageTextNode
import com.nadr59.sitemanager.data.repository.TranslationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * منسق ترجمة صفحات الويب
 * يدير العملية الكاملة لترجمة محتوى WebView
 */
@Singleton
class WebPageTranslationCoordinator @Inject constructor(
    private val translationRepository: TranslationRepository,
    private val webPageTranslator: WebPageTranslator
) {
    // حالة الترجمة لكل صفحة (URL -> State)
    private val _translationStates = MutableStateFlow<Map<String, PageTranslationState>>(emptyMap())
    val translationStates: StateFlow<Map<String, PageTranslationState>> = _translationStates.asStateFlow()

    // الإعدادات الافتراضية
    private var config = TranslationConfig.DEFAULT

    /**
     * تحديث إعدادات الترجمة
     */
    fun updateConfig(newConfig: TranslationConfig) {
        config = newConfig
    }

    /**
     * الحصول على حالة ترجمة صفحة معينة
     */
    fun getPageState(url: String): PageTranslationState? {
        return _translationStates.value[url]
    }

    /**
     * التحقق من وجود ترجمة للصفحة
     */
    fun isPageTranslated(url: String): Boolean {
        return _translationStates.value[url]?.isTranslated == true
    }

    /**
     * ترجمة صفحة كاملة
     */
    suspend fun translatePage(
        webView: WebView,
        url: String,
        targetLanguage: String = config.targetLanguage,
        onProgress: (TranslationOperation) -> Unit
    ): Result<PageTranslationState> = withContext(Dispatchers.Main) {
        try {
            // 1️⃣ التحقق من وجود ترجمة سابقة
            val existingState = _translationStates.value[url]
            if (existingState?.isTranslated == true && 
                existingState.targetLanguage == targetLanguage) {
                
                Timber.d("استخدام ترجمة محفوظة للصفحة: $url")
                onProgress(TranslationOperation.Success(existingState))
                return@withContext Result.success(existingState)
            }

            // 2️⃣ استخراج النصوص من الصفحة
            onProgress(TranslationOperation.Progress(0, 0, 0f))
            
            val extractedNodes = extractNodesFromPage(webView)
            
            if (extractedNodes.isEmpty()) {
                val error = "لم يتم العثور على نصوص قابلة للترجمة"
                Timber.w(error)
                onProgress(TranslationOperation.Failure(error))
                return@withContext Result.failure(Exception(error))
            }

            Timber.d("تم استخراج ${extractedNodes.size} عقدة نصية")

            // 3️⃣ تصفية النصوص
            val filteredNodes = filterNodes(extractedNodes)
            Timber.d("بعد التصفية: ${filteredNodes.size} عقدة")

            if (filteredNodes.isEmpty()) {
                val error = "لا توجد نصوص صالحة للترجمة بعد التصفية"
                Timber.w(error)
                onProgress(TranslationOperation.Failure(error))
                return@withContext Result.failure(Exception(error))
            }

            // 4️⃣ إنشاء حالة أولية
            val initialState = PageTranslationState(
                url = url,
                targetLanguage = targetLanguage,
                originalNodes = filteredNodes,
                isTranslated = false
            )
            updateState(url, initialState)

            // 5️⃣ ترجمة النصوص
            val translatedMap = translateNodes(
                nodes = filteredNodes,
                targetLanguage = targetLanguage,
                onProgress = { current, total ->
                    val percentage = (current.toFloat() / total.toFloat()) * 100f
                    onProgress(TranslationOperation.Progress(current, total, percentage))
                    
                    // تحديث الحالة مع التقدم
                    val updatedState = initialState.copy(
                        progress = percentage / 100f,
                        translatedNodes = translatedMap
                    )
                    updateState(url, updatedState)
                }
            )

            // 6️⃣ استبدال النصوص في الصفحة
            replaceNodesInPage(webView, translatedMap, targetLanguage)

            // 7️⃣ تحديث الحالة النهائية
            val finalState = PageTranslationState(
                url = url,
                targetLanguage = targetLanguage,
                originalNodes = filteredNodes,
                translatedNodes = translatedMap,
                isTranslated = true,
                progress = 1f
            )
            updateState(url, finalState)

            Timber.d("تمت ترجمة الصفحة بنجاح: ${translatedMap.size} عقدة")
            onProgress(TranslationOperation.Success(finalState))
            
            Result.success(finalState)

        } catch (e: Exception) {
            Timber.e(e, "فشل في ترجمة الصفحة: $url")
            val errorMessage = e.message ?: "خطأ غير معروف"
            onProgress(TranslationOperation.Failure(errorMessage, e))
            Result.failure(e)
        }
    }

    /**
     * ترجمة نص محدد من الصفحة
     */
    suspend fun translateSelection(
        webView: WebView,
        targetLanguage: String = config.targetLanguage
    ): Result<Pair<String, String>> = withContext(Dispatchers.Main) {
        try {
            // 1️⃣ استخراج النص المحدد
            val selectedText = extractSelectedText(webView)
            
            if (selectedText.isBlank()) {
                return@withContext Result.failure(Exception("لا يوجد نص محدد"))
            }

            Timber.d("نص محدد: ${selectedText.take(50)}...")

            // 2️⃣ ترجمة النص
            val translatedText = translationRepository.translateText(
                text = selectedText,
                targetLanguage = targetLanguage
            ).getOrThrow()

            // 3️⃣ استبدال النص المحدد
            replaceSelectedText(webView, translatedText, targetLanguage)

            Timber.d("تمت ترجمة النص المحدد")
            Result.success(selectedText to translatedText)

        } catch (e: Exception) {
            Timber.e(e, "فشل في ترجمة النص المحدد")
            Result.failure(e)
        }
    }

    /**
     * استعادة النص الأصلي للصفحة
     */
    suspend fun restoreOriginalPage(
        webView: WebView,
        url: String
    ): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val state = _translationStates.value[url]
            
            if (state == null || !state.isTranslated) {
                return@withContext Result.failure(Exception("الصفحة غير مترجمة"))
            }

            // استعادة النصوص الأصلية
            val originalMap = state.originalNodes.associate { node ->
                node.id to node.text
            }

            replaceNodesInPage(webView, originalMap, "original")

            // تحديث الحالة
            val restoredState = state.copy(
                isTranslated = false,
                progress = 0f
            )
            updateState(url, restoredState)

            Timber.d("تمت استعادة النص الأصلي للصفحة")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "فشل في استعادة النص الأصلي")
            Result.failure(e)
        }
    }

    /**
     * مسح حالة ترجمة صفحة
     */
    fun clearPageState(url: String) {
        _translationStates.value = _translationStates.value.toMutableMap().apply {
            remove(url)
        }
    }

    /**
     * مسح جميع حالات الترجمة
     */
    fun clearAllStates() {
        _translationStates.value = emptyMap()
    }

    // ==================== Private Methods ====================

    /**
     * استخراج العقد النصية من WebView
     */
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
                    Timber.e(e, "فشل في تحليل العقد المستخرجة")
                    continuation.resume(emptyList())
                }
            }
        }
    }

    /**
     * تصفية العقد النصية
     */
    private fun filterNodes(nodes: List<PageTextNode>): List<PageTextNode> {
        return nodes.filter { node ->
            val text = node.text.trim()
            
            // تصفية النصوص القصيرة جداً
            if (text.length < config.minTextLength) return@filter false
            
            // تصفية النصوص الطويلة جداً
            if (text.length > config.maxTextLength) return@filter false
            
            // تصفية النصوص التي تحتوي على أرقام فقط
            if (text.all { it.isDigit() || it.isWhitespace() }) return@filter false
            
            // تصفية URLs
            if (text.startsWith("http://") || text.startsWith("https://")) return@filter false
            
            // تصفية الأيميلات
            if (text.contains("@") && text.contains(".")) return@filter false
            
            true
        }.distinctBy { it.text.trim() } // إزالة التكرار
    }

    /**
     * ترجمة مجموعة من العقد
     */
    private suspend fun translateNodes(
        nodes: List<PageTextNode>,
        targetLanguage: String,
        onProgress: (Int, Int) -> Unit
    ): Map<String, String> = withContext(Dispatchers.IO) {
        
        val translatedMap = mutableMapOf<String, String>()
        val batches = nodes.chunked(config.batchSize)
        var processedCount = 0

        for ((batchIndex, batch) in batches.withIndex()) {
            try {
                // ترجمة الدفعة
                val batchResults = translationRepository.translatePageNodes(
                    nodes = batch,
                    targetLanguage = targetLanguage,
                    onProgress = { _ -> } // نتجاهل تقدم الدفعة الفردية
                ).getOrThrow()

                // إضافة النتائج
                batchResults.forEach { translated ->
                    translatedMap[translated.id] = translated.translatedText
                }

                processedCount += batch.size
                onProgress(processedCount, nodes.size)

                Timber.d("دفعة ${batchIndex + 1}/${batches.size}: تمت ترجمة ${batch.size} عقدة")

            } catch (e: Exception) {
                Timber.e(e, "فشل في ترجمة الدفعة ${batchIndex + 1}")
                // الاستمرار في الدفعات التالية
            }
        }

        translatedMap
    }

    /**
     * استبدال النصوص في WebView
     */
    private suspend fun replaceNodesInPage(
        webView: WebView,
        translationMap: Map<String, String>,
        targetLanguage: String
    ) = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            val isRTL = TranslationConfig.isRTL(targetLanguage)
            val replaceScript = webPageTranslator.buildReplaceScript(
                translations = translationMap.map { (id, text) ->
                    com.nadr59.sitemanager.data.model.TranslatedNode(
                        id = id,
                        originalText = "", // لا نحتاجه في الاستبدال
                        translatedText = text
                    )
                },
                applyRTL = isRTL && targetLanguage != "original"
            )

            webView.evaluateJavascript(replaceScript) { result ->
                Timber.d("تم تنفيذ سكريبت الاستبدال")
                continuation.resume(Unit)
            }
        }
    }

    /**
     * استخراج النص المحدد
     */
    private suspend fun extractSelectedText(webView: WebView): String {
        return suspendCoroutine { continuation ->
            val script = webPageTranslator.buildSelectionScript()
            
            webView.evaluateJavascript(script) { result ->
                try {
                    val selectedText = webPageTranslator.parseSelectionResult(result)
                    continuation.resume(selectedText)
                } catch (e: Exception) {
                    Timber.e(e, "فشل في تحليل النص المحدد")
                    continuation.resume("")
                }
            }
        }
    }

    /**
     * استبدال النص المحدد
     */
    private suspend fun replaceSelectedText(
        webView: WebView,
        translatedText: String,
        targetLanguage: String
    ) = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            val isRTL = TranslationConfig.isRTL(targetLanguage)
            val script = webPageTranslator.buildSelectionReplaceScript(
                translatedText = translatedText,
                applyRTL = isRTL
            )

            webView.evaluateJavascript(script) { result ->
                Timber.d("تم استبدال النص المحدد")
                continuation.resume(Unit)
            }
        }
    }

    /**
     * تحديث حالة صفحة
     */
    private fun updateState(url: String, state: PageTranslationState) {
        _translationStates.value = _translationStates.value.toMutableMap().apply {
            put(url, state)
        }
    }
}
