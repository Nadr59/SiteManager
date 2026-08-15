package com.nadr59.sitemanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // ═══ البيانات الأساسية (موجودة مسبقًا) ═══
    val name: String,
    val url: String,
    val category: String = "عام",
    val notes: String = "",
    val color: String = "#D4A853",
    val createdAt: Long = System.currentTimeMillis(),
    val visitCount: Int = 0,

    // ═══ بيانات التحليل الجديدة ═══
    val siteType: String = "",          // GITHUB_REPO, WEBSITE, API_DOCS...
    val lastAnalyzed: Long = 0,         // آخر مرة تم تحليلها
    val cachedOverview: String = "",     // نظرة عامة محفوظة
    val cachedTechStack: String = "",    // التقنيات المستخدمة (مفصولة بفواصل)
    val cachedFeatures: String = "",     // الميزات (مفصولة بـ |)
    val aiRating: Float = 0f,           // تقييم AI من 10
    val analysisCount: Int = 0          // عدد مرات التحليل
)
