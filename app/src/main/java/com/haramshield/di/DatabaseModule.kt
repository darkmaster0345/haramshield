package com.haramshield.di

import android.content.Context
import androidx.room.Room
import com.haramshield.data.db.ShieldDatabase
import com.haramshield.data.db.dao.LockedAppDao
import com.haramshield.data.db.dao.ViolationLogDao
import com.haramshield.data.db.dao.WhitelistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ShieldDatabase {
        return Room.databaseBuilder(
            context,
            ShieldDatabase::class.java,
            ShieldDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideLockedAppDao(database: ShieldDatabase): LockedAppDao {
        return database.lockedAppDao()
    }
    
    @Provides
    @Singleton
    fun provideViolationLogDao(database: ShieldDatabase): ViolationLogDao {
        return database.violationLogDao()
    }
    
    @Provides
    @Singleton
    fun provideWhitelistDao(database: ShieldDatabase): WhitelistDao {
        return database.whitelistDao()
    }
}
