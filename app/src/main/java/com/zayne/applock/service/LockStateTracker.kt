package com.zayne.applock.service

import java.util.concurrent.ConcurrentHashMap

/**
 * Hält den Auth-Zustand pro App im Prozessspeicher. Beim Beenden des AccessibilityService
 * (z.B. durch das System) geht der Zustand verloren -> alle Apps gelten dann wieder als
 * gesperrt, was die sichere Default-Variante ist.
 */
object LockStateTracker {

    class PkgState {
        @Volatile var authenticated: Boolean = false
        @Volatile var backgroundedAt: Long? = null
    }

    private val states = ConcurrentHashMap<String, PkgState>()

    fun stateFor(packageName: String): PkgState = states.getOrPut(packageName) { PkgState() }

    @Volatile var lastForegroundPackage: String? = null

    // true = die zuletzt aktive App war (aktuell) gesperrt/nicht authentifiziert.
    // Wird vom Recents-Schutz ausgewertet.
    @Volatile var lastForegroundNeedsAuth: Boolean = false

    fun markAuthenticated(packageName: String) {
        val st = stateFor(packageName)
        st.authenticated = true
        st.backgroundedAt = null
        if (lastForegroundPackage == packageName) {
            lastForegroundNeedsAuth = false
        }
    }
}
