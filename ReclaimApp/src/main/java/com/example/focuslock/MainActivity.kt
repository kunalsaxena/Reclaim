package com.example.focuslock

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var hint: TextView
    private lateinit var btn: Button
    private lateinit var todayLabel: TextView
    private lateinit var nUrges: TextView
    private lateinit var nSkipped: TextView
    private lateinit var nUnlocked: TextView
    private lateinit var trigger: TextView
    private lateinit var week: LinearLayout
    private lateinit var morningStatus: TextView
    private lateinit var morningToggleBtn: Button

    private val bg = Color.parseColor("#0F172A")
    private val card = Color.parseColor("#1E293B")
    private val muted = Color.parseColor("#94A3B8")
    private val green = Color.parseColor("#4ADE80")
    private val amber = Color.parseColor("#FBBF24")

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)

        val title = TextView(this).apply {
            text = "Reclaim"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(6), dp(16), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16) }
        }
        hint = TextView(this).apply {
            textSize = 14f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(12), dp(8), 0)
        }
        btn = Button(this).apply {
            text = "Enable in Accessibility Settings"
            isAllCaps = false
            setTextColor(bg)
            setPadding(dp(32), dp(12), dp(32), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(28).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(20) }
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }

        // Today card
        val (c1, n1) = column("urges", Color.WHITE)
        val (c2, n2) = column("skipped", green)
        val (c3, n3) = column("unlocked", amber)
        nUrges = n1; nSkipped = n2; nUnlocked = n3
        todayLabel = sectionLabel("TODAY")
        trigger = TextView(this).apply {
            textSize = 14f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
        }
        val todayCard = cardView().apply {
            addView(todayLabel)
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(c1); addView(c2); addView(c3)
            })
            addView(trigger)
        }

        // Morning Block card
        morningStatus = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(12))
        }
        morningToggleBtn = Button(this).apply {
            isAllCaps = false
            textSize = 13f
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setOnClickListener {
                val current = Config.isMorningBlockEnabled(this@MainActivity)
                Config.setMorningBlockEnabled(this@MainActivity, !current)
                updateMorningCard()
            }
        }
        val morningCard = cardView().apply {
            addView(sectionLabel("MORNING BLOCK (5 AM - 9 AM)"))
            addView(morningStatus)
            addView(morningToggleBtn)
        }

        // 7-day card
        week = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val legend = TextView(this).apply {
            text = "green = skipped · amber = unlocked"
            textSize = 11f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        val weekCard = cardView().apply {
            addView(sectionLabel("LAST 7 DAYS"))
            addView(week)
            addView(legend)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(48), dp(20), dp(32))
            addView(title); addView(status); addView(hint); addView(btn)
            addView(todayCard); addView(morningCard); addView(weekCard)
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
            addView(root)
        })
    }

    private fun cardView() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(20))
        background = GradientDrawable().apply {
            setColor(card)
            cornerRadius = dp(20).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(16) }
    }

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text
        textSize = 12f
        letterSpacing = 0.15f
        setTextColor(muted)
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, dp(12))
    }

    private fun column(label: String, numColor: Int): Pair<LinearLayout, TextView> {
        val num = TextView(this).apply {
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(numColor)
            gravity = Gravity.CENTER
        }
        val lab = TextView(this).apply {
            text = label
            textSize = 12f
            setTextColor(muted)
            gravity = Gravity.CENTER
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(num); addView(lab)
        }
        return col to num
    }

    private fun seg(color: Int, h: Int) = View(this).apply {
        setBackgroundColor(color)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
    }

    private fun fillWeek() {
        week.removeAllViews()
        val max = (0..6).maxOf { Stats.urgesAgo(this, it) }.coerceAtLeast(1)
        val maxH = dp(72)
        for (d in 6 downTo 0) {
            val urges = Stats.urgesAgo(this, d)
            val unl = Stats.unlockedAgo(this, d).coerceAtMost(urges)
            val skip = urges - unl

            val bar = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM
                layoutParams = LinearLayout.LayoutParams(dp(16), maxH)
            }
            if (urges == 0) bar.addView(seg(Color.parseColor("#334155"), dp(3)))
            if (unl > 0) bar.addView(seg(amber, (maxH * unl / max).coerceAtLeast(dp(3))))
            if (skip > 0) bar.addView(seg(green, (maxH * skip / max).coerceAtLeast(dp(3))))

            val label = TextView(this).apply {
                text = Stats.dayLetter(d)
                textSize = 12f
                setTextColor(if (d == 0) Color.WHITE else muted)
                gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, 0)
            }
            week.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(bar); addView(label)
            })
        }
    }

    private fun updateMorningCard() {
        val enabled = Config.isMorningBlockEnabled(this)
        val activeNow = Config.isMorningHours(this)
        if (enabled) {
            if (activeNow) {
                morningStatus.text = "●  Active right now (Strict Block)"
                morningStatus.setTextColor(green)
            } else {
                morningStatus.text = "Enabled  ·  Strict block 5:00 AM – 9:00 AM"
                morningStatus.setTextColor(Color.WHITE)
            }
            morningToggleBtn.text = "Disable Morning Block"
            morningToggleBtn.setTextColor(Color.WHITE)
            morningToggleBtn.background = GradientDrawable().apply {
                setColor(Color.parseColor("#334155"))
                cornerRadius = dp(16).toFloat()
            }
        } else {
            morningStatus.text = "Disabled"
            morningStatus.setTextColor(muted)
            morningToggleBtn.text = "Enable Morning Block"
            morningToggleBtn.setTextColor(bg)
            morningToggleBtn.background = GradientDrawable().apply {
                setColor(green)
                cornerRadius = dp(16).toFloat()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val list = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val cn = ComponentName(this, BlockerService::class.java)
        val on = list.contains(cn.flattenToString()) || list.contains(cn.flattenToShortString())

        status.text = if (on) "●  Protection ON" else "●  Protection OFF"
        status.setTextColor(Color.parseColor(if (on) "#4ADE80" else "#F87171"))
        status.background = GradientDrawable().apply {
            setColor(Color.parseColor(if (on) "#14532D" else "#7F1D1D"))
            cornerRadius = dp(20).toFloat()
        }
        hint.text = if (on) "Blocked apps make you wait first."
                    else "Turn on Reclaim under Installed apps."
        btn.visibility = if (on) View.GONE else View.VISIBLE

        val urges = Stats.urges(this)
        val skipped = Stats.skipped(this)
        nUrges.text = urges.toString()
        nSkipped.text = skipped.toString()
        nUnlocked.text = Stats.unlocked(this).toString()
        todayLabel.text = if (urges > 0) "TODAY  ·  ${skipped * 100 / urges}% skipped" else "TODAY"

        trigger.text = "Work passes used today: ${Stats.passesToday(this)}"

        fillWeek()
        updateMorningCard()
    }
}
