package com.zayne.applock

import android.app.Application
import com.zayne.applock.data.AppRepository
import com.zayne.applock.security.PinManager

class AppLockApplication : Application() {

    lateinit var repository: AppRepository
        private set

    lateinit var pinManager: PinManager
        private set

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(this)
        pinManager = PinManager(this)
    }
}
