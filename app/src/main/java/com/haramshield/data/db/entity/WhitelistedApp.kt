package com.haramshield.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an app that is whitelisted (exempt from monitoring)
 */
@Entity(tableName = "whitelisted_apps")
data class WhitelistedApp(
    @PrimaryKey
    val packageName: String,
    
    /** User-friendly app label */
    val appLabel: String,
    
    /** Timestamp when the app was whitelisted */
    val addedAt: Long = System.currentTimeMillis()
)
