package com.nadr59.sitemanager.data.remote

data class SiteContent(
    val url: String,
    val title: String = "",
    val description: String = "",
    val type: SiteType = SiteType.WEBSITE,
    val rawContent: String = "",
    val metadata: Map<String, String> = emptyMap()
)

enum class SiteType(val label: String, val icon: String) {
    GITHUB_REPO("مستودع GitHub", "🐙"),
    GITHUB_PROFILE("ملف GitHub", "👤"),
    WEBSITE("موقع ويب", "🌐"),
    API_DOCS("وثائق API", "📡"),
    BLOG("مدونة", "📝"),
    DOCUMENTATION("وثائق", "📚"),
    PACKAGE("حزمة/مكتبة", "📦"),
    UNKNOWN("غير معروف", "❓")
}

data class AnalysisResult(
    val overview: String = "",
    val purpose: String = "",
    val features: List<String> = emptyList(),
    val howToUse: String = "",
    val techStack: List<String> = emptyList(),
    val examples: String = "",
    val prosAndCons: ProsAndCons = ProsAndCons(),
    val rating: Float = 0f,
    val rawMarkdown: String = ""
) {
    fun toCachedOverview(): String = buildString {
        appendLine(overview)
        if (purpose.isNotBlank()) appendLine("\n$purpose")
    }.trim()

    fun toCachedTechStack(): String = techStack.joinToString(",")

    fun toCachedFeatures(): String = features.joinToString("|")

    companion object {
        fun fromCache(
            overview: String,
            techStack: String,
            features: String,
            rating: Float
        ): AnalysisResult {
            return AnalysisResult(
                overview = overview,
                techStack = techTechStack.split(",").filter { it.isNotBlank() },
                features = features.split("|").filter { it.isNotBlank() },
                rating = rating
            )
        }
    }
}

data class ProsAndCons(
    val pros: List<String> = emptyList(),
    val cons: List<String> = emptyList()
)

data class AiConfig(
    val provider: String = "groq",
    val apiKey: String = "",
    val model: String = "",
    val baseUrl: String = ""
)
