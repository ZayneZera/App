package com.zayne.applock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_lock_config")
data class AppLockConfigEntity(
    @PrimaryKey val packageName: String,
    val isLocked: Boolean = false,
    val isException: Boolean = false,
    // null = globalen Standardwert aus GlobalSettingsStore verwenden
    val customAuthMode: String? = null,
    val customGraceMs: Long? = null
)
