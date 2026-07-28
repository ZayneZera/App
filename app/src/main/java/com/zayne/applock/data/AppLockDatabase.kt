package com.zayne.applock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AppLockConfigEntity::class], version = 1, exportSchema = false)
abstract class AppLockDatabase : RoomDatabase() {
    abstract fun appLockConfigDao(): AppLockConfigDao

    companion object {
        @Volatile
        private var instance: AppLockDatabase? = null

        fun getInstance(context: Context): AppLockDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppLockDatabase::class.java,
                    "app_lock.db"
                ).build().also { instance = it }
            }
        }
    }
}
