package com.haramshield.data.db.dao

import androidx.room.*
import com.haramshield.data.db.entity.WhitelistedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistDao {
    
    @Query("SELECT * FROM whitelisted_apps ORDER BY appLabel ASC")
    fun getAllWhitelistedAppsFlow(): Flow<List<WhitelistedApp>>
    
    @Query("SELECT * FROM whitelisted_apps ORDER BY appLabel ASC")
    suspend fun getAllWhitelistedApps(): List<WhitelistedApp>
    
    @Query("SELECT packageName FROM whitelisted_apps")
    suspend fun getWhitelistedPackageNames(): List<String>
    
    @Query("SELECT packageName FROM whitelisted_apps")
    fun getWhitelistedPackageNamesFlow(): Flow<List<String>>
    
    @Query("SELECT EXISTS(SELECT 1 FROM whitelisted_apps WHERE packageName = :packageName)")
    suspend fun isWhitelisted(packageName: String): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM whitelisted_apps WHERE packageName = :packageName)")
    fun isWhitelistedFlow(packageName: String): Flow<Boolean>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(whitelistedApp: WhitelistedApp)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<WhitelistedApp>)
    
    @Delete
    suspend fun delete(whitelistedApp: WhitelistedApp)
    
    @Query("DELETE FROM whitelisted_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
    
    @Query("SELECT COUNT(*) FROM whitelisted_apps")
    suspend fun getWhitelistCount(): Int
    
    @Query("SELECT COUNT(*) FROM whitelisted_apps")
    fun getWhitelistCountFlow(): Flow<Int>
}
