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
    val tags: String = "",
    val notes: String = "",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val visitCount: Int = 0,
    val lastVisited: Long = 0L,
    val faviconUrl: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastAnalyzed: Long = 0L,
    val lastChecked: Long = 0L,
    val httpStatus: Int = 0,
    val pageTitle: String = "",
    val pageDescription: String = "",
    // ═══حقول موجودة سابقاً ═══
    val cachedOverview: String = "",
    val aiRating: Float = 0f,
    val cachedTechStack: String = "",
    val cachedFeatures: String = ""
)
