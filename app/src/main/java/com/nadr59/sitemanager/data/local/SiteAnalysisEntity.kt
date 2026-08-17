package com.nadr59.sitemanager.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "site_analyses",
    foreignKeys = [
        ForeignKey(
            entity = SiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["siteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["siteId"])]
)
data class SiteAnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val siteId: Int,
    val analysisType: String,
    val result: String,
    val rating: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
)
