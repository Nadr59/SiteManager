package com.nadr59.sitemanager.data.remote

data class AnalysisResult(
    val overview: String = "",
    val purpose: String = "",
    val features: List<String> = emptyList(),
    val howToUse: String = "",
    val techStack: List<String> = emptyList(),
    val examples: String = "",
    val prosAndCons: ProsAndCons = ProsAndCons(),
    val rating: Float = 0f,
    val rawMarkdown: String = "",
    val analysisType: String = "explain"
)

data class ProsAndCons(
    val pros: List<String> = emptyList(),
    val cons: List<String> = emptyList()
)
