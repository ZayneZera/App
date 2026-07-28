package com.zayne.applock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.zayne.applock.AppLockApplication
import com.zayne.applock.ui.LockOverlayActivity
import com.zayne.applock.ui.RecentsGuardActivity

/**
 * Erkennt per AccessibilityEvent, welche App gerade in den Vordergrund kommt (rein
 * event-basiert, kein Polling -> akkuschonend). Löst je nach Sperrzustand ein
 * Overlay über der App aus oder schützt den Task-Switcher (Recents).
 */
class LockAccessibilityService : AccessibilityService() {

    private lateinit var launcherPackageName: String

    override fun onServiceConnected() {
        super.onServiceConnected()
        launcherPackageName = resolveLauncherPackageName()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return // eigene App ignorieren

        val app = application as AppLockApplication
        val repository = app.repository

        if (pkg == SYSTEM_UI_PACKAGE || pkg == launcherPackageName) {
            val className = event.className?.toString().orEmpty()
            if (looksLikeRecentsScreen(className)) {
                maybeGuardRecents(repository.settings.value.recentsProtectionEnabled)
            }
            return
        }

        handleForegroundPackage(pkg, repository)
    }

    private fun handleForegroundPackage(
        pkg: String,
        repository: com.zayne.applock.data.AppRepository
    ) {
        val previous = LockStateTracker.lastForegroundPackage
        if (previous != null && previous != pkg && repository.isLocked(previous)) {
            val prevState = LockStateTracker.stateFor(previous)
            if (prevState.authenticated && prevState.backgroundedAt == null) {
                prevState.backgroundedAt = System.currentTimeMillis()
            }
        }
        LockStateTracker.lastForegroundPackage = pkg

        val isLocked = repository.isLocked(pkg)
        if (!isLocked) {
            LockStateTracker.lastForegroundNeedsAuth = false
            return
        }

        val graceMs = repository.graceMsFor(pkg)
        val state = LockStateTracker.stateFor(pkg)
        val bgAt = state.backgroundedAt
        val stillInGrace = state.authenticated &&
            (bgAt == null || (System.currentTimeMillis() - bgAt) < graceMs)

        if (stillInGrace) {
            state.backgroundedAt = null
            LockStateTracker.lastForegroundNeedsAuth = false
        } else {
            state.authenticated = false
            state.backgroundedAt = null
            LockStateTracker.lastForegroundNeedsAuth = true
            launchLockOverlay(pkg)
        }
    }

    private fun maybeGuardRecents(recentsProtectionEnabled: Boolean) {
        if (!recentsProtectionEnabled) return
        if (!LockStateTracker.lastForegroundNeedsAuth) return
        val intent = Intent(this, RecentsGuardActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent, noAnimationOptions())
    }

    private fun launchLockOverlay(targetPackage: String) {
        val intent = Intent(this, LockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(LockOverlayActivity.EXTRA_TARGET_PACKAGE, targetPackage)
        }
        startActivity(intent, noAnimationOptions())
    }

    // Verhindert die Standard-"neue App öffnet sich"-Slide-Animation, damit die Sperre
    // wie ein reines Overlay statt wie ein App-Wechsel wirkt.
    private fun noAnimationOptions(): android.os.Bundle =
        android.app.ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle()

    /**
     * Heuristik: Der Task-Switcher heißt je nach Android-Version/Hersteller unterschiedlich.
     * Auf Stock/Pixel läuft er meist in com.android.systemui, auf Samsung OneUI kann die
     * Übersicht auch Teil des Launcher-Pakets sein. Falls der Recents-Schutz auf einem
     * konkreten Gerät nicht auslöst, hier per "adb shell dumpsys window windows" die
     * tatsächliche Klasse prüfen und ergänzen.
     */
    private fun looksLikeRecentsScreen(className: String): Boolean {
        val lower = className.lowercase()
        return lower.contains("recent") || lower.contains("overview") || lower.contains("taskswitcher")
    }

    private fun resolveLauncherPackageName(): String {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, 0)
        return resolveInfo?.activityInfo?.packageName ?: SYSTEM_UI_PACKAGE
    }

    override fun onInterrupt() {}

    companion object {
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
