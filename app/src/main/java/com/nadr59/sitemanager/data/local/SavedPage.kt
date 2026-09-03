 package com.nadr59.sitemanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_pages")
data class SavedPage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val url: String,
    val title: String,
    val content: String,                    // النص المنظف
    val translatedContent: String? = null,  // النص المترجم (nullable)
    val imageUrls: String = "[]",           // JSON array من روابط الصور
    val language: String = "unknown",       // اللغة الأصلية
    val isTranslated: Boolean = false,      // هل تم ترجمتها؟
    val savedAt: Long = System.currentTimeMillis(),
    val siteId: Int = 0                     // nullable - قد تكون صفحة خارجية
)
