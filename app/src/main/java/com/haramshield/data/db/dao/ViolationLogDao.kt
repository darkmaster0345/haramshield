package com.haramshield.data.db.dao

import androidx.room.*
import com.haramshield.data.db.entity.ViolationLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ViolationLogDao {
    
    @Query("SELECT * FROM violation_logs ORDER BY timestamp DESC")
    fun getAllViolationsFlow(): Flow<List<ViolationLog>>
    
    @Query("SELECT * FROM violation_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentViolationsFlow(limit: Int = 50): Flow<List<ViolationLog>>
    
    @Query("SELECT * FROM violation_logs WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getViolationsByDateRange(startTime: Long, endTime: Long): List<ViolationLog>
    
    @Query("SELECT * FROM violation_logs WHERE packageName = :packageName ORDER BY timestamp DESC")
    suspend fun getViolationsForApp(packageName: String): List<ViolationLog>
    
    @Query("SELECT * FROM violation_logs WHERE category = :category ORDER BY timestamp DESC")
    suspend fun getViolationsByCategory(category: String): List<ViolationLog>
    
    @Query("SELECT COUNT(*) FROM violation_logs")
    suspend fun getTotalViolationCount(): Int
    
    @Query("SELECT COUNT(*) FROM violation_logs")
    fun getTotalViolationCountFlow(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM violation_logs WHERE timestamp >= :startTime")
    suspend fun getViolationCountSince(startTime: Long): Int
    
    @Query("SELECT COUNT(DISTINCT packageName) FROM violation_logs")
    suspend fun getUniqueAppViolationCount(): Int
    
    @Query("SELECT category, COUNT(*) as count FROM violation_logs GROUP BY category")
    suspend fun getViolationCountByCategory(): List<CategoryCount>
    
    @Insert
    suspend fun insert(violationLog: ViolationLog): Long
    
    @Delete
    suspend fun delete(violationLog: ViolationLog)
    
    @Query("DELETE FROM violation_logs WHERE timestamp < :beforeTime")
    suspend fun deleteOlderThan(beforeTime: Long): Int
    
    @Query("DELETE FROM violation_logs")
    suspend fun deleteAll()
}

data class CategoryCount(
    val category: String,
    val count: Int
)
