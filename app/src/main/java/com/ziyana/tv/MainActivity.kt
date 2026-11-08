package com.ziyana.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val bg = Color.rgb(7, 9, 28)
    private val panel = 0xD9161738.toInt()
    private val purple = 0xFF9B65FF.toInt()
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout
    private var selected = "Home"

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF090B27.toInt(), 0xFF17113B.toInt(), 0xFF080B20.toInt()))
        }
        nav = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(20), dp(12), dp(20))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xF00B0B25.toInt(), 0xEE17132F.toInt(), 0xCC38234F.toInt())).apply { cornerRadius = dp(28).toFloat() }
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(18), dp(28), dp(26)) }
        scroll.addView(content)
        shell.addView(nav, LinearLayout.LayoutParams(dp(142), -1))
        shell.addView(scroll, LinearLayout.LayoutParams(0, -1, 1f))
        setContentView(shell)
        buildNav()
        showHome()
    }

    private fun text(s: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = s; textSize = size; setTextColor(color)
            typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun buildNav() {
        nav.removeAllViews()
        val brand = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(4), dp(10), 0, dp(28)) }
        brand.addView(text("▶", 28f, 0xFFCB4EFF.toInt(), true))
        brand.addView(text(" Ziyana", 18f, Color.WHITE, true))
        brand.addView(text(" TV", 18f, purple, true))
        nav.addView(brand)
        val items = listOf("⌂" to "Home", "⌕" to "Search", "▦" to "Apps", "▣" to "Live TV", "◉" to "Movies", "▣" to "Series", "▶" to "YouTube", "⚙" to "Settings")
        items.forEach { (symbol, name) ->
            val item = TextView(this).apply {
                text = "$symbol   $name"; textSize = 15f; setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), 0, dp(6), 0)
                background = rounded(if (selected == name) 0xFF6255C8.toInt() else Color.TRANSPARENT, 22)
                isFocusable = true
                setOnClickListener { selected = name; buildNav(); when(name) {
                    "Home" -> showHome()
                    "Apps" -> showAppsPage()
                    "Settings" -> startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                    "Live TV" -> showMessage("Open your installed Live TV app from Apps.")
                    "Movies", "Series" -> showMessage("$name section")
                    "YouTube" -> launchPackage("com.google.android.youtube.tv")
                    "Search" -> showAppsPage()
                } }
                setOnFocusChangeListener { v, f -> v.background = rounded(if (f || selected == name) 0xFF7669E8.toInt() else Color.TRANSPARENT, 22) }
            }
            val lp = LinearLayout.LayoutParams(-1, dp(52)); lp.bottomMargin = dp(4)
            nav.addView(item, lp)
        }
        val spacer = View(this); nav.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))
        nav.addView(text("4K  •  GOOGLE TV", 10f, 0xFFAAA8D0.toInt(), true))
    }

    private fun rounded(color: Int, radius: Int, stroke: Int = 0): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); if (stroke > 0) setStroke(dp(2), purple) }

    private fun clear(title: String) {
        content.removeAllViews()
        content.addView(text(title, 28f, Color.WHITE, true).apply { setPadding(0, 0, 0, dp(18)) })
    }

    private fun showHome() {
        content.removeAllViews()
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(text(" ", 12f))
        val gap = View(this); top.addView(gap, LinearLayout.LayoutParams(0, 1, 1f))
        top.addView(text("⌕     ◉     ⚙      9:45 PM", 15f, 0xFFE4E0FF.toInt(), true))
        content.addView(top)
        content.addView(text("Welcome Back!", 38f, Color.WHITE, true).apply { setPadding(0, dp(46), 0, 0) })
        content.addView(text("Relax. Watch. Enjoy.", 22f, 0xFFD5C9FF.toInt(), true))
        content.addView(text("▶   Continue Watching      ›", 17f, Color.WHITE, true).apply {
            background = rounded(0xCC302B62.toInt(), 30, 1); setPadding(dp(22), dp(12), dp(22), dp(12))
            isFocusable = true; setOnClickListener { showAppsPage() }
        }.also { it.setPadding(dp(22), dp(12), dp(22), dp(12)) })
        content.addView(text("Your Apps", 21f, Color.WHITE, true).apply { setPadding(0, dp(24), 0, dp(10)) })
        addInstalledAppsRow()
        content.addView(text("Continue Watching", 21f, Color.WHITE, true).apply { setPadding(0, dp(22), 0, dp(10)) })
        addContentTiles(listOf("Anime", "Recently Added", "Watchlist", "Discover"))
        content.addView(text("Trending Now", 21f, Color.WHITE, true).apply { setPadding(0, dp(22), 0, dp(10)) })
        addContentTiles(listOf("Popular", "New Episodes", "Recommended", "Explore"))
    }

    private fun addInstalledAppsRow() {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val hsv = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(row) }
        val apps = getApps().take(12)
        apps.forEach { ri ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                background = rounded(0xDD242344.toInt(), 18, 1); isFocusable = true
                setOnFocusChangeListener { v, f -> v.background = rounded(if (f) 0xFF4A3E87.toInt() else 0xDD242344.toInt(), 18, if (f) 2 else 1); v.scaleX = if(f) 1.06f else 1f; v.scaleY = if(f) 1.06f else 1f }
                setOnClickListener { launchActivity(ri.activityInfo.packageName, ri.activityInfo.name) }
            }
            val icon = ImageView(this).apply { setImageDrawable(ri.loadIcon(packageManager)); scaleType = ImageView.ScaleType.FIT_CENTER }
            card.addView(icon, LinearLayout.LayoutParams(dp(54), dp(54)))
            card.addView(text(ri.loadLabel(packageManager).toString(), 12f, Color.WHITE, true).apply { gravity = Gravity.CENTER; maxLines = 1 })
            val lp = LinearLayout.LayoutParams(dp(132), dp(112)); lp.marginEnd = dp(12); row.addView(card, lp)
        }
        content.addView(hsv, LinearLayout.LayoutParams(-1, dp(120)))
    }

    private fun addContentTiles(names: List<String>) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        names.forEachIndexed { i, name ->
            val tile = TextView(this).apply {
                text = name; textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.BOTTOM or Gravity.START
                setPadding(dp(14), dp(12), dp(10), dp(12)); background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    intArrayOf(listOf(0xFF33235D.toInt(),0xFF173D69.toInt(),0xFF49245B.toInt(),0xFF172B55.toInt())[i], 0xFF10152F.toInt())).apply { cornerRadius = dp(16).toFloat() }
                isFocusable = true; setOnFocusChangeListener { v, f -> v.scaleX = if(f) 1.04f else 1f; v.scaleY = if(f) 1.04f else 1f; v.background = rounded(if(f) 0xFF453878.toInt() else 0xFF20203F.toInt(),16,if(f) 2 else 0) }
                setOnClickListener { showMessage("$name will appear here when a media source is connected.") }
            }
            val lp = LinearLayout.LayoutParams(0, dp(104), 1f); lp.marginEnd = dp(12); row.addView(tile, lp)
        }
        content.addView(row)
    }

    private fun showAppsPage() {
        clear("Apps")
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        getApps().forEach { ri ->
            val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; background = rounded(0xDD242344.toInt(),18,1); isFocusable = true
                setOnFocusChangeListener { v,f -> v.background = rounded(if(f) 0xFF4A3E87.toInt() else 0xDD242344.toInt(),18,if(f) 2 else 1) }
                setOnClickListener { launchActivity(ri.activityInfo.packageName,ri.activityInfo.name) } }
            card.addView(ImageView(this).apply { setImageDrawable(ri.loadIcon(packageManager)) }, LinearLayout.LayoutParams(dp(52),dp(52)))
            card.addView(text(ri.loadLabel(packageManager).toString(),12f,Color.WHITE,true).apply { gravity = Gravity.CENTER })
            val lp = LinearLayout.LayoutParams(dp(135),dp(110)); lp.setMargins(0,0,dp(12),dp(12)); row.addView(card,lp)
            if (row.childCount % 5 == 0) { content.addView(row); }
        }
        if (row.parent == null) content.addView(row)
    }

    private fun getApps() = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)
        .filter { it.activityInfo.packageName != packageName }.distinctBy { it.activityInfo.packageName }
        .sortedBy { it.loadLabel(packageManager).toString().lowercase() }

    private fun launchPackage(pkg: String) { try { startActivity(packageManager.getLaunchIntentForPackage(pkg)) } catch (_: Exception) { showAppsPage() } }
    private fun launchActivity(pkg: String, cls: String) { try { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(pkg,cls)) } catch (_: Exception) { launchPackage(pkg) } }
    private fun showMessage(s: String) { Toast.makeText(this,s,Toast.LENGTH_SHORT).show() }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) { selected = "Apps"; buildNav(); showAppsPage(); return true }
        return super.onKeyDown(keyCode,event)
    }
}
