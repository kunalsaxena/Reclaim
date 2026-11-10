package com.example.focuslock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class BlockerService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("lock", MODE_PRIVATE) }

    private fun unlockedUntil(pkg: String) = prefs.getLong("until_$pkg", 0L)

    // Runs when an unlock window ends: if a blocked app is still on screen, block it again.
    private val recheck = Runnable {
        val pkg = rootInActiveWindow?.packageName?.toString() ?: return@Runnable
        if (pkg in Config.BLOCKED && System.currentTimeMillis() >= unlockedUntil(pkg)) {
            showBlock(pkg, recheck = true)
        }
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = e.packageName?.toString() ?: return
        if (pkg !in Config.BLOCKED) return

        if (Config.isMorningHours(this)) {
            handler.removeCallbacks(recheck)
            showBlock(pkg, recheck = false)
            return
        }

        val remaining = unlockedUntil(pkg) - System.currentTimeMillis()
        if (remaining > 0) {
            handler.removeCallbacks(recheck)
            handler.postDelayed(recheck, remaining + 500)
        } else {
            showBlock(pkg, recheck = false)
        }
    }

    private fun showBlock(pkg: String, recheck: Boolean) {
        startActivity(
            Intent(this, BlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("pkg", pkg)
                .putExtra("recheck", recheck)
        )
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacks(recheck)
        super.onDestroy()
    }
}
