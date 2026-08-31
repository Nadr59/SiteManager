// data/local/BrowserBookmark.kt
package com.nadr59.sitemanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "browser_bookmarks")
data class BrowserBookmark(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val url: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val siteId: Int = 0
)
