package com.haramshield.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a logged violation event
 */
@Entity(
    tableName = "violation_logs",
    indices = [androidx.room.Index(value = ["timestamp"])]
)
data class ViolationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Package name of the app where violation occurred */
    val packageName: String,
    
    /** The detection category (NSFW, ALCOHOL, TOBACCO, GAMBLING) */
    val category: String,
    
    /** Confidence score of the detection */
    val confidence: Float,
    
    /** Timestamp of the violation */
    val timestamp: Long = System.currentTimeMillis(),
    
    /** Brief description or app label */
    val appLabel: String? = null,
    
    /** Whether the user was locked out as a result */
    val lockedOut: Boolean = true
)
