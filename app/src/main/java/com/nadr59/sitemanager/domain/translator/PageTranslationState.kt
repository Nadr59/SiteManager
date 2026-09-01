// app/src/main/java/com/nadr59/sitemanager/domain/translator/PageTranslationState.kt

package com.nadr59.sitemanager.domain.translator

import com.nadr59.sitemanager.data.model.PageTextNode

/**
 * حالة ترجمة الصفحة
 */
data class PageTranslationState(
    val url: String,
    val targetLanguage: String,
    val isTranslated: Boolean = false,
    val originalNodes: List<PageTextNode> = emptyList(),
    val translatedNodes: Map<String, String> = emptyMap(), // nodeId -> translatedText
    val progress: Float = 0f,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * التحقق من وجود ترجمة لعقدة معينة
     */
    fun hasTranslation(nodeId: String): Boolean {
        return translatedNodes.containsKey(nodeId)
    }

    /**
     * الحصول على الترجمة لعقدة معينة
     */
    fun getTranslation(nodeId: String): String? {
        return translatedNodes[nodeId]
    }

    /**
     * عدد العقد المترجمة
     */
    val translatedCount: Int
        get() = translatedNodes.size

    /**
     * عدد العقد الإجمالي
     */
    val totalCount: Int
        get() = originalNodes.size

    /**
     * نسبة الإكمال
     */
    val completionRate: Float
        get() = if (totalCount > 0) {
            translatedCount.toFloat() / totalCount.toFloat()
        } else {
            0f
        }
}

/**
 * نتيجة عملية الترجمة
 */
sealed class TranslationOperation {
    data class Success(val state: PageTranslationState) : TranslationOperation()
    data class Progress(val current: Int, val total: Int, val percentage: Float) : TranslationOperation()
    data class Failure(val error: String, val cause: Throwable? = null) : TranslationOperation()
}

/**
 * إعدادات الترجمة
 */
data class TranslationConfig(
    val targetLanguage: String = "ar",
    val batchSize: Int = 10,
    val retryAttempts: Int = 2,
    val cacheEnabled: Boolean = true,
    val minTextLength: Int = 3,
    val maxTextLength: Int = 5000,
    val excludeSelectors: List<String> = listOf(
        "code", "pre", "script", "style", "svg"
    ),
    val preserveFormatting: Boolean = true
) {
    companion object {
        val DEFAULT = TranslationConfig()
        
        val RTL_LANGUAGES = setOf("ar", "fa", "he", "ur")
        
        fun isRTL(language: String): Boolean {
            return language in RTL_LANGUAGES
        }
    }
}
