package com.nadr59.sitemanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val url: String,
    val category: String = "عام",
    val notes: String = "",
    val color: String = "#D4A853",
    val createdAt: Long = System.currentTimeMillis(),
    val visitCount: Int = 0,
    // حقول التحليل
    val siteType: String = "",
    val lastAnalyzed: Long = 0,
    val cachedOverview: String = "",
    val cachedTechStack: String = "",
    val cachedFeatures: String = "",
    val aiRating: Float = 0f,
    val analysisCount: Int = 0
)
