package com.haramshield.data.db.dao

import androidx.room.*
import com.haramshield.data.db.entity.LockedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedAppDao {
    
    @Query("SELECT * FROM locked_apps WHERE lockUntil > :currentTime")
    fun getActiveLocksFlow(currentTime: Long = System.currentTimeMillis()): Flow<List<LockedApp>>
    
    @Query("SELECT * FROM locked_apps WHERE lockUntil > :currentTime")
    suspend fun getActiveLocks(currentTime: Long = System.currentTimeMillis()): List<LockedApp>
    
    @Query("SELECT * FROM locked_apps WHERE packageName = :packageName AND lockUntil > :currentTime LIMIT 1")
    suspend fun getActiveLock(packageName: String, currentTime: Long = System.currentTimeMillis()): LockedApp?
    
    @Query("SELECT * FROM locked_apps WHERE packageName = :packageName AND lockUntil > :currentTime LIMIT 1")
    fun getActiveLockFlow(packageName: String, currentTime: Long = System.currentTimeMillis()): Flow<LockedApp?>
    
    @Query("SELECT EXISTS(SELECT 1 FROM locked_apps WHERE packageName = :packageName AND lockUntil > :currentTime)")
    suspend fun isLocked(packageName: String, currentTime: Long = System.currentTimeMillis()): Boolean
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lockedApp: LockedApp)
    
    @Delete
    suspend fun delete(lockedApp: LockedApp)
    
    @Query("DELETE FROM locked_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
    
    @Query("DELETE FROM locked_apps WHERE lockUntil <= :currentTime")
    suspend fun deleteExpiredLocks(currentTime: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM locked_apps WHERE lockUntil > :currentTime")
    suspend fun getActiveLocksCount(currentTime: Long = System.currentTimeMillis()): Int
}
