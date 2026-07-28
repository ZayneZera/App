package com.zayne.applock.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLockConfigDao {
    @Query("SELECT * FROM app_lock_config")
    fun observeAll(): Flow<List<AppLockConfigEntity>>

    @Upsert
    suspend fun upsert(entity: AppLockConfigEntity)

    @Delete
    suspend fun delete(entity: AppLockConfigEntity)

    @Query("DELETE FROM app_lock_config WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)
}
