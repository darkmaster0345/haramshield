package com.haramshield.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an app that is currently locked due to a violation
 */
@Entity(tableName = "locked_apps")
data class LockedApp(
    @PrimaryKey
    val packageName: String,
    
    /** Timestamp when the lock expires (millis since epoch) */
    val lockUntil: Long,
    
    /** The category that triggered the lock */
    val category: String,
    
    /** Confidence score of the detection that triggered the lock */
    val confidence: Float,
    
    /** Timestamp when the app was locked */
    val lockedAt: Long = System.currentTimeMillis()
)
