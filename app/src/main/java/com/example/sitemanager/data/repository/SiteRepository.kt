package com.nadr59.sitemanager.data.repository

import com.nadr59.sitemanager.data.local.SiteDao
import com.nadr59.sitemanager.data.local.SiteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteRepository @Inject constructor(
    private val siteDao: SiteDao
) {
    fun getAllSites(): Flow<List<SiteEntity>> = siteDao.getAllSites()

    fun getSitesByCategory(category: String): Flow<List<SiteEntity>> =
        siteDao.getSitesByCategory(category)

    fun getAllCategories(): Flow<List<String>> = siteDao.getAllCategories()

    fun searchSites(query: String): Flow<List<SiteEntity>> = siteDao.searchSites(query)

    suspend fun insertSite(site: SiteEntity) = siteDao.insertSite(site)

    suspend fun updateSite(site: SiteEntity) = siteDao.updateSite(site)

    suspend fun deleteSite(site: SiteEntity) = siteDao.deleteSite(site)

    suspend fun getSiteById(id: Int): SiteEntity? = siteDao.getSiteById(id)

    suspend fun incrementVisit(id: Int) = siteDao.incrementVisitCount(id)
}
