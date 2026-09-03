package com.nadr59.sitemanager.data.model

data class BrowserState(
    val url: String = "",
    val title: String = "",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isTranslationMode: Boolean = false,
    val targetLanguage: String = "ar",
    val isTranslating: Boolean = false,
    val translationProgress: Float = 0f,
    val showTranslationSheet: Boolean = false,
    val error: String? = null,

    // ═══ حالات القراءة الذكية ═══
    val isSmartReadMode: Boolean = false,      // هل وضع "قراءة بالعربية" مفعّل؟
    val smartReadStep: SmartReadStep = SmartReadStep.IDLE,  // الخطوة الحالية
    val isPageSaved: Boolean = false,          // هل الصفحة الحالية محفوظة؟
    val isSavingPage: Boolean = false          // هل جارٍ الحفظ؟
)

// ═══ خطوات وضع القراءة الذكية ═══
enum class SmartReadStep {
    IDLE,           // لا شيء يعمل
    EXTRACTING,     // استخراج المحتوى
    CLEANING,       // تنظيف الصفحة (Reader Mode)
    TRANSLATING,    // الترجمة
    DONE            // اكتمل
}
