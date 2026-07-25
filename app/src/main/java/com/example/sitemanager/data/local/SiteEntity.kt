package com.example.sitemanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val tabType: String,        // "favorites" أو "saved"
    val faviconUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val clickCount: Int = 0
)
