// data/local/TranslationCache.kt
package com.nadr59.sitemanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_cache")
data class TranslationCache(
    @PrimaryKey
    val id: String = "",  // hash of text + language
    val originalText: String,
    val translatedText: String,
    val targetLanguage: String,
    val cachedAt: Long = System.currentTimeMillis()
)
