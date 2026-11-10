package com.example.focuslock

import android.content.Context
import java.util.Calendar

object Config {
    const val DELAY_MS = 5 * 60 * 1000L      // wait before unlock is allowed
    const val UNLOCK_MS = 10 * 60 * 1000L    // how long the app stays open after unlocking

    const val PASS_MS = 10 * 60 * 1000L
    const val PASSES_PER_DAY = 2

    const val MORNING_START_HOUR = 5 // 5 AM
    const val MORNING_END_HOUR = 9   // 9 AM
    const val PREF_MORNING_BLOCK_ENABLED = "morning_block_enabled"

    val BLOCKED = setOf(
        "com.google.android.youtube",
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.snapchat.android",
        "com.reddit.frontpage",
        "com.linkedin.android"
        // add more package names here
    )

    fun isMorningBlockEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_MORNING_BLOCK_ENABLED, true)
    }

    fun setMorningBlockEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_MORNING_BLOCK_ENABLED, enabled).apply()
    }

    fun isMorningHours(context: Context, cal: Calendar = Calendar.getInstance()): Boolean {
        if (!isMorningBlockEnabled(context)) return false
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour in MORNING_START_HOUR until MORNING_END_HOUR
    }

    fun getMsUntilMorningEnds(cal: Calendar = Calendar.getInstance()): Long {
        val endCal = cal.clone() as Calendar
        endCal.set(Calendar.HOUR_OF_DAY, MORNING_END_HOUR)
        endCal.set(Calendar.MINUTE, 0)
        endCal.set(Calendar.SECOND, 0)
        endCal.set(Calendar.MILLISECOND, 0)
        return (endCal.timeInMillis - cal.timeInMillis).coerceAtLeast(0L)
    }
}

