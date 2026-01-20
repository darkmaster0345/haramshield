package com.haramshield.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.haramshield.data.db.dao.LockedAppDao
import com.haramshield.data.db.dao.ViolationLogDao
import com.haramshield.data.db.dao.WhitelistDao
import com.haramshield.data.db.entity.LockedApp
import com.haramshield.data.db.entity.ViolationLog
import com.haramshield.data.db.entity.WhitelistedApp

@Database(
    entities = [
        LockedApp::class,
        ViolationLog::class,
        WhitelistedApp::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ShieldDatabase : RoomDatabase() {
    
    abstract fun lockedAppDao(): LockedAppDao
    abstract fun violationLogDao(): ViolationLogDao
    abstract fun whitelistDao(): WhitelistDao
    
    companion object {
        const val DATABASE_NAME = "haramshield_database"
    }
}
