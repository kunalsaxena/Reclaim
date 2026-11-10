package com.example.focuslock

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.window.OnBackInvokedDispatcher

class BlockActivity : Activity() {

    private val bg = Color.parseColor("#0F172A")
    private val card = Color.parseColor("#1E293B")
    private val muted = Color.parseColor("#64748B")
    private val green = Color.parseColor("#4ADE80")
    private val amber = Color.parseColor("#FBBF24")

    private var timer: CountDownTimer? = null
    private var pkg = ""
    private var timerDone = false

    private lateinit var msg: TextView
    private lateinit var clock: TextView
    private lateinit var pass: Button
    private lateinit var unlock: Button

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)

        msg = TextView(this).apply {
            textSize = 20f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
        }
        clock = TextView(this).apply {
            textSize = 56f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            setPadding(0, dp(16), 0, dp(24))
        }
        val leave = pill("Never mind, go back") { goHome() }
        fill(leave, green, bg)
        pass = pill("Work pass") { usePass() }
        unlock = pill("Unlock") { unlockApp() }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(bg)
            setPadding(dp(24), dp(32), dp(24), dp(32))
            addView(msg); addView(clock)
            addView(leave)      // the healthy choice comes first
            addView(View(this@BlockActivity), LinearLayout.LayoutParams(1, dp(16)))
            addView(pass)
            addView(unlock)
        })

        begin(intent, count = b == null)

        // Predictive back (Android 13+) skips onBackPressed, so register a callback.
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) { goHome() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        begin(intent, count = true)
    }

    @Suppress("DEPRECATION")
    private fun begin(i: Intent, count: Boolean) {
        pkg = i.getStringExtra("pkg") ?: ""
        val recheck = i.getBooleanExtra("recheck", false)
        val label = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) { "this app" }

        val isMorning = Config.isMorningHours(this)
        msg.text = if (isMorning) {
            if (recheck) "Morning block in effect (5 AM - 9 AM).\nYour time in $label is up."
            else {
                val n = if (count) Stats.recordUrge(this, pkg) else Stats.appCount(this, pkg)
                "Morning block in effect (5 AM - 9 AM).\n${ordinal(n)} time opening $label today."
            }
        } else if (recheck) {
            "Your time in $label is up."
        } else {
            val n = if (count) Stats.recordUrge(this, pkg) else Stats.appCount(this, pkg)
            "${ordinal(n)} time opening $label today.\nDo you really need it?"
        }
        updatePass()
    }

    // Timer restarts every time the screen is left, so you can't wait it out elsewhere.
    override fun onStart() {
        super.onStart()
        timerDone = false
        updateUnlock()
        updatePass()
        val isMorning = Config.isMorningHours(this)
        val durationMs = if (isMorning) Config.getMsUntilMorningEnds() else Config.DELAY_MS
        timer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(ms: Long) {
                val s = ms / 1000
                val h = s / 3600
                val m = (s % 3600) / 60
                val sec = s % 60
                clock.text = if (isMorning && h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
            }
            override fun onFinish() {
                clock.text = "0:00"
                if (!isMorning) {
                    timerDone = true
                }
                updateUnlock()
                updatePass()
            }
        }.start()
    }

    override fun onStop() {
        timer?.cancel()
        super.onStop()
    }

    private fun updatePass() {
        if (Config.isMorningHours(this)) {
            pass.isEnabled = false
            pass.text = "Work passes disabled during morning hours"
            fill(pass, card, muted)
            return
        }
        val left = (Config.PASSES_PER_DAY - Stats.passesUsed(this, pkg)).coerceAtLeast(0)
        pass.isEnabled = left > 0
        pass.text = if (left > 0)
            "Work pass: ${Config.PASS_MS / 60000} min ($left left today)"
        else "No work passes left today"
        if (left > 0) fill(pass, Color.WHITE, bg) else fill(pass, card, muted)
    }

    private fun updateUnlock() {
        if (Config.isMorningHours(this)) {
            unlock.isEnabled = false
            unlock.text = "Unlocks disabled (5 AM - 9 AM)"
            fill(unlock, card, muted)
            return
        }
        unlock.isEnabled = timerDone
        unlock.text = if (timerDone) "Unlock for ${Config.UNLOCK_MS / 60000} min"
                      else "Unlock (wait for timer)"
        if (timerDone) fill(unlock, amber, bg) else fill(unlock, card, muted)
    }

    private fun usePass() {
        if (Config.isMorningHours(this)) return
        if (Stats.passesUsed(this, pkg) >= Config.PASSES_PER_DAY) return
        Stats.recordPass(this, pkg)
        grant(Config.PASS_MS)
    }

    private fun unlockApp() {
        if (Config.isMorningHours(this)) return
        if (!timerDone) return
        Stats.recordUnlock(this)
        grant(Config.UNLOCK_MS)
    }

    private fun grant(ms: Long) {
        getSharedPreferences("lock", MODE_PRIVATE).edit()
            .putLong("until_$pkg", System.currentTimeMillis() + ms).apply()
        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
        finish()
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    @Deprecated("Back also goes home")
    override fun onBackPressed() = goHome()

    // ---- small UI helpers ----

    private fun pill(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setPadding(dp(24), dp(12), dp(24), dp(12))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) }
        fill(this, card, Color.WHITE)
        setOnClickListener { onClick() }
    }

    private fun fill(b: Button, color: Int, textColor: Int) {
        b.setTextColor(textColor)
        b.background = GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(24).toFloat()
        }
    }

    private fun ordinal(n: Int): String {
        val s = if (n % 100 in 11..13) "th" else when (n % 10) {
            1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th"
        }
        return "$n$s"
    }
}
