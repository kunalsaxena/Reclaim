package com.example.focuslock

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Stats {
    private fun prefs(c: Context) = c.getSharedPreferences("stats", Context.MODE_PRIVATE)

    private fun cal(daysAgo: Int) = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
    private fun key(daysAgo: Int) = SimpleDateFormat("yyyyMMdd", Locale.US).format(cal(daysAgo).time)
    private fun day() = key(0)

    fun dayLetter(daysAgo: Int): String =
        SimpleDateFormat("EEEEE", Locale.getDefault()).format(cal(daysAgo).time)

    private fun bump(c: Context, key: String): Int {
        val p = prefs(c)
        val n = p.getInt(key, 0) + 1
        p.edit().putInt(key, n).apply()
        return n
    }

    /** Returns how many times this app triggered the block today. */
    fun recordUrge(c: Context, pkg: String): Int {
        bump(c, "${day()}_urges")
        return bump(c, "${day()}_app_$pkg")
    }

    /** Unlocked after waiting out the countdown. */
    fun recordUnlock(c: Context) {
        bump(c, "${day()}_unlocked")
    }

    /** Work pass: also counts as unlocked, plus its own per-app and total counters. */
    fun recordPass(c: Context, pkg: String) {
        bump(c, "${day()}_unlocked")
        bump(c, "${day()}_pass_$pkg")
        bump(c, "${day()}_passes")
    }

    fun passesUsed(c: Context, pkg: String) = prefs(c).getInt("${day()}_pass_$pkg", 0)
    fun passesToday(c: Context) = prefs(c).getInt("${day()}_passes", 0)

    fun appCount(c: Context, pkg: String) = prefs(c).getInt("${day()}_app_$pkg", 0)
    fun urges(c: Context) = urgesAgo(c, 0)
    fun unlocked(c: Context) = unlockedAgo(c, 0)
    fun skipped(c: Context) = (urges(c) - unlocked(c)).coerceAtLeast(0)

    fun urgesAgo(c: Context, d: Int) = prefs(c).getInt("${key(d)}_urges", 0)
    fun unlockedAgo(c: Context, d: Int) = prefs(c).getInt("${key(d)}_unlocked", 0)
}
