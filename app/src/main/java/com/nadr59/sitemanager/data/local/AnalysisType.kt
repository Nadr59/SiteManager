package com.nadr59.sitemanager.data.local

enum class AnalysisType(
    val key: String,
    val displayName: String,
    val emoji: String,
    val promptPrefix: String
) {
    EXPLAIN(
        "explain",
        "شرح الموقع",
        "📖",
        "اشرح وظيفة هذا الموقع وما يقدمه للمستخدم. كن واضحاً ومختصراً."
    ),
    TECHNICAL(
        "technical",
        "تحليل تقني",
        "⚙️",
        "حلل الموقع من الناحية التقنية: نوع الخدمة، التقنيات المستخدمة، طبيعة الموقع، مدى ملاءمته للهاتف."
    ),
    CONTENT(
        "content",
        "تحليل المحتوى",
        "📝",
        "حلل طبيعة المحتوى في الموقع والموضوعات الرئيسية التي يتناولها."
    ),
    SECURITY(
        "security",
        "الأمان والخصوصية",
        "🔒",
        """حلل الموقع من ناحية الأمان والخصوصية:
- هل يستخدم HTTPS؟
- سياسة الخصوصية إن كانت متاحة
- طلب الصلاحيات أو البيانات
- مؤشرات الثقة الظاهرة
- أي ملاحظات أمنية
لا تدّعِ أن الموقع آمن أو خطير 100% بدون أدلة كافية."""
    ),
    USEFULNESS(
        "usefulness",
        "تحليل الفائدة",
        "💡",
        """حلل فائدة الموقع:
- لمن يناسب؟
- أهم استخداماته؟
- أبرز مميزاته؟
- القيود أو العيوب المحتملة؟"""
    ),
    SUMMARY(
        "summary",
        "تلخيص سريع",
        "📋",
        "ألخص هذا الموقع في فقرة قصيرة يمكن قراءتها بسرعة."
    ),
    CUSTOM(
        "custom",
        "سؤال مخصص",
        "❓",
        ""
    );

    companion object {
        fun fromKey(key: String): AnalysisType {
            return entries.find { it.key == key } ?: EXPLAIN
        }
    }
}
