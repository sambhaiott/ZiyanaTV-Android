package com.ziyana.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable

class MainActivity : Activity() {
    private val bg = Color.rgb(10, 13, 29)
    private val accent = Color.rgb(151, 117, 250)
    private lateinit var root: LinearLayout
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(52), dp(30), dp(52), dp(24)); setBackgroundColor(bg) }
        setContentView(root)
        buildHome()
    }
    private fun label(text: String, size: Float, color: Int = Color.WHITE): TextView =
        TextView(this).apply { this.text = text; textSize = size; setTextColor(color); typeface = Typeface.create("sans-serif", Typeface.NORMAL) }
    private fun buildHome() {
        root.removeAllViews()
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; orientation = LinearLayout.HORIZONTAL }
        top.addView(label("ZIYANA", 25f).apply { setTextColor(accent); typeface = Typeface.DEFAULT_BOLD })
        top.addView(label("   TV", 25f).apply { typeface = Typeface.DEFAULT_BOLD })
        val spacer = View(this); top.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))
        top.addView(label("HOME     •     APPS     •     SETTINGS", 13f, 0xFFB8BED5.toInt()))
        root.addView(top)
        root.addView(label("Good evening", 17f, 0xFFB8BED5.toInt()).apply { setPadding(0, dp(35), 0, dp(5)) })
        root.addView(label("Your screen. Your world.", 34f).apply { typeface = Typeface.DEFAULT_BOLD })
        root.addView(label("A calmer home for everything you watch.", 16f, 0xFFB8BED5.toInt()).apply { setPadding(0, dp(8), 0, dp(24)) })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        addCard(row, "▶   Open apps", "Browse installed TV apps") { showApps() }
        addCard(row, "⚙   Settings", "Open Android settings") { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
        addCard(row, "◉   Wi-Fi", "Network connections") { startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)) }
        root.addView(row)
        root.addView(label("Favourites", 21f).apply { typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(34), 0, dp(12)) })
        root.addView(label("Select Open apps to see apps installed on this TV.", 15f, 0xFFB8BED5.toInt()))
    }
    private fun addCard(row: LinearLayout, title: String, subtitle: String, action: () -> Unit) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(18), dp(18), dp(18))
            background = GradientDrawable().apply { setColor(0xFF202744.toInt()); cornerRadius = dp(18).toFloat(); setStroke(dp(1), 0xFF414A70.toInt()) }
            isFocusable = true; isFocusableInTouchMode = true
            setOnClickListener { action() }
            setOnFocusChangeListener { v, focused ->
                v.scaleX = if (focused) 1.045f else 1f; v.scaleY = if (focused) 1.045f else 1f
                background = GradientDrawable().apply { setColor(if (focused) 0xFF39305E.toInt() else 0xFF202744.toInt()); cornerRadius = dp(18).toFloat(); setStroke(dp(if (focused) 3 else 1), if (focused) accent else 0xFF414A70.toInt()) }
            }
        }
        card.addView(label(title, 19f).apply { typeface = Typeface.DEFAULT_BOLD })
        card.addView(label(subtitle, 13f, 0xFFB8BED5.toInt()).apply { setPadding(0, dp(8), 0, 0) })
        val lp = LinearLayout.LayoutParams(0, dp(116), 1f); lp.marginEnd = dp(14); row.addView(card, lp)
    }
    private fun showApps() {
        val pm = packageManager
        val intents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        )
        val apps = intents.flatMap { pm.queryIntentActivities(it, 0) }
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
        if (apps.isEmpty()) { AlertDialog.Builder(this).setTitle("No apps found").setMessage("Install TV apps from Google Play Store, then try again.").setPositiveButton("OK", null).show(); return }
        val names = apps.map { it.loadLabel(pm).toString() }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Your apps").setItems(names) { _, which ->
            val info = apps[which].activityInfo
            val launch = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER); setClassName(info.packageName, info.name); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            try { startActivity(launch) } catch (_: Exception) { try { startActivity(pm.getLaunchIntentForPackage(info.packageName)) } catch (_: Exception) {} }
        }.setNegativeButton("Close", null).show()
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) { showApps(); return true }
        return super.onKeyDown(keyCode, event)
    }
}
