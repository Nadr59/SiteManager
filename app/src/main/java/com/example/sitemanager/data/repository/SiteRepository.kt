package com.example.sitemanager.data.repository

import com.example.sitemanager.data.local.SiteDao
import com.example.sitemanager.data.local.SiteEntity
import kotlinx.coroutines.flow.Flow

class SiteRepository(private val dao: SiteDao) {

    fun getFavorites(): Flow<List<SiteEntity>> = dao.getFavorites()

    fun getSaved(): Flow<List<SiteEntity>> = dao.getSaved()

    fun search(query: String): Flow<List<SiteEntity>> = dao.search(query)

    suspend fun addSite(site: SiteEntity): Boolean {
        val existing = dao.getByUrl(site.url)
        if (existing != null) return false
        dao.insert(site)
        return true
    }

    suspend fun update(site: SiteEntity) = dao.update(site)

    suspend fun delete(site: SiteEntity) = dao.delete(site)

    suspend fun moveToTab(id: Long, newTabType: String) = dao.moveToTab(id, newTabType)

    suspend fun incrementClick(id: Long) = dao.incrementClickCount(id)

    suspend fun updateTitle(id: Long, title: String) = dao.updateTitle(id, title)
}
