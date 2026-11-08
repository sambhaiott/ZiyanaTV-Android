package com.ziyana.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val purple = 0xFF9B65FF.toInt()
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout
    private lateinit var clock: TextView
    private var selected = "Home"
    private val handler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            if (::clock.isInitialized) clock.text = SimpleDateFormat("h:mm a   EEE, MMM d", Locale.getDefault()).format(Date())
            handler.postDelayed(this, 60_000)
        }
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val root = FrameLayout(this)
        val wallpaper = ImageView(this).apply {
            setImageResource(R.drawable.anime_sakura_wallpaper)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Ziyana TV anime sakura night wallpaper"
        }
        root.addView(wallpaper, FrameLayout.LayoutParams(-1, -1))
        val shade = View(this).apply { background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(0xA9080920.toInt(), 0x66090B25, 0x88080A20.toInt())) }
        root.addView(shade, FrameLayout.LayoutParams(-1, -1))
        val shell = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        nav = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(18), dp(11), dp(18))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xF00A0B25.toInt(), 0xEE17132F.toInt(), 0xE52A1C46.toInt())).apply { cornerRadius = dp(26).toFloat() }
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false; isFocusable = false }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(15), dp(26), dp(26)) }
        scroll.addView(content)
        shell.addView(nav, LinearLayout.LayoutParams(dp(145), -1))
        shell.addView(scroll, LinearLayout.LayoutParams(0, -1, 1f))
        root.addView(shell, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        buildNav()
        showHome()
        handler.post(clockTick)
    }

    override fun onDestroy() { handler.removeCallbacks(clockTick); super.onDestroy() }

    private fun label(s: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = s; textSize = size; setTextColor(color)
            typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun rounded(color: Int, radius: Int, stroke: Int = 0): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); if (stroke > 0) setStroke(dp(stroke), 0xFFD5B5FF.toInt()) }

    private fun buildNav() {
        nav.removeAllViews()
        val brand = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(4), dp(10), 0, dp(28)) }
        brand.addView(label("▶", 27f, 0xFFCB4EFF.toInt(), true))
        brand.addView(label(" Ziyana", 18f, Color.WHITE, true))
        brand.addView(label(" TV", 18f, purple, true))
        nav.addView(brand)
        val items = listOf("⌂" to "Home", "⌕" to "Search", "▦" to "Apps", "▣" to "Live TV", "◉" to "Movies", "▣" to "Series", "▶" to "YouTube", "⚙" to "Settings")
        items.forEach { (symbol, name) ->
            val item = TextView(this).apply {
                text = "$symbol   $name"; textSize = 14f; setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), 0, dp(5), 0)
                background = rounded(if (selected == name) 0xFF6255C8.toInt() else 0x22202040, 22, if (selected == name) 2 else 0)
                isFocusable = true
                setOnClickListener { navigate(name) }
                setOnFocusChangeListener { v, focused ->
                    v.background = rounded(if (focused || selected == name) 0xFF6255C8.toInt() else 0x22202040, 22, if (focused || selected == name) 2 else 0)
                    if (focused) v.animate().scaleX(1.04f).scaleY(1.04f).setDuration(120).start()
                    else v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }
            }
            val lp = LinearLayout.LayoutParams(-1, dp(49)); lp.bottomMargin = dp(4); nav.addView(item, lp)
        }
        nav.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))
        nav.addView(label("4K  •  GOOGLE TV", 9f, 0xFFAAA8D0.toInt(), true))
    }

    private fun navigate(name: String) {
        selected = name; buildNav()
        when (name) {
            "Home" -> showHome()
            "Apps" -> showAppsPage()
            "Settings" -> try { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) } catch (_: Exception) { toast("Settings unavailable") }
            "Live TV" -> launchFirstMatching(listOf("tv", "iptv", "live"))
            "Movies", "Series" -> showMediaInfo(name)
            "YouTube" -> launchPackage("com.google.android.youtube.tv")
            "Search" -> showSearch()
        }
    }

    private fun heading(s: String) {
        content.addView(label(s, 21f, Color.WHITE, true).apply { setPadding(0, dp(18), 0, dp(10)) })
    }

    private fun showHome() {
        content.removeAllViews()
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        top.addView(label("⌕     Wi-Fi     ⚙    ", 13f, 0xFFF0E9FF.toInt(), true).apply { isFocusable = true; setOnClickListener { navigate("Settings") } })
        clock = label("", 13f, 0xFFF0E9FF.toInt(), true)
        top.addView(clock)
        content.addView(top)
        content.addView(label("Welcome Back!", 36f, Color.WHITE, true).apply { setPadding(0, dp(38), 0, 0) })
        content.addView(label("Relax. Watch. Enjoy.", 21f, 0xFFE0D1FF.toInt(), true))
        content.addView(label("▶   Continue Watching      ›", 16f, Color.WHITE, true).apply {
            background = rounded(0xDD30265F.toInt(), 28, 1); setPadding(dp(20), dp(11), dp(22), dp(11))
            isFocusable = true; setOnClickListener { navigate("Apps") }
        })
        heading("Your Apps")
        addInstalledAppsRow()
        heading("Continue Watching")
        addFeatureTiles(listOf("Anime", "Recently Added", "Watchlist", "Discover"))
        heading("Trending Now")
        addFeatureTiles(listOf("Popular", "New Episodes", "Recommended", "Explore"))
    }

    private fun addInstalledAppsRow() {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val hsv = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; isFocusable = false; addView(row) }
        getApps().take(14).forEach { info ->
            val card = appCard(info)
            val lp = LinearLayout.LayoutParams(dp(124), dp(104)); lp.marginEnd = dp(11); row.addView(card, lp)
        }
        if (row.childCount == 0) row.addView(label("No TV apps found. Open Apps to refresh.", 14f, 0xFFD4C8EF.toInt()))
        content.addView(hsv, LinearLayout.LayoutParams(-1, dp(112)))
    }

    private fun appCard(info: android.content.pm.ResolveInfo): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = rounded(0xE51B1B3B.toInt(), 17, 1); isFocusable = true; isClickable = true
            setOnFocusChangeListener { v, focused ->
                v.background = rounded(if (focused) 0xFF55428F.toInt() else 0xE51B1B3B.toInt(), 17, if (focused) 3 else 1)
                v.animate().scaleX(if (focused) 1.07f else 1f).scaleY(if (focused) 1.07f else 1f).setDuration(130).start()
            }
            setOnClickListener { launchActivity(info) }
        }
        card.addView(ImageView(this).apply { setImageDrawable(info.loadIcon(packageManager)); scaleType = ImageView.ScaleType.FIT_CENTER }, LinearLayout.LayoutParams(dp(48), dp(48)))
        card.addView(label(info.loadLabel(packageManager).toString(), 11f, Color.WHITE, true).apply { gravity = Gravity.CENTER; maxLines = 1 })
        return card
    }

    private fun addFeatureTiles(names: List<String>) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        names.forEachIndexed { i, name ->
            val colors = intArrayOf(0xFF49316F.toInt(), 0xFF17436A.toInt(), 0xFF5A285F.toInt(), 0xFF263A70.toInt())
            val tile = TextView(this).apply {
                text = name; textSize = 14f; setTextColor(Color.WHITE); gravity = Gravity.BOTTOM or Gravity.START
                setPadding(dp(12), dp(10), dp(8), dp(10))
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(colors[i], 0xFF10152F.toInt())).apply { cornerRadius = dp(15).toFloat() }
                isFocusable = true; isClickable = true
                setOnFocusChangeListener { v, focused ->
                    v.background = rounded(if (focused) 0xFF51417F.toInt() else colors[i], 15, if (focused) 2 else 0)
                    v.animate().scaleX(if (focused) 1.04f else 1f).scaleY(if (focused) 1.04f else 1f).setDuration(120).start()
                }
                setOnClickListener { showMediaInfo(name) }
            }
            val lp = LinearLayout.LayoutParams(0, dp(82), 1f); lp.marginEnd = dp(9); row.addView(tile, lp)
        }
        content.addView(row)
    }

    private fun clearPage(title: String) {
        content.removeAllViews()
        content.addView(label(title, 27f, Color.WHITE, true).apply { setPadding(0, dp(8), 0, dp(16)) })
    }

    private fun showAppsPage() {
        clearPage("All Apps")
        val apps = getApps()
        if (apps.isEmpty()) {
            content.addView(label("No TV apps are visible. Install apps from Google Play Store.", 16f, 0xFFE0D7F7.toInt()))
            return
        }
        var row = newAppRow()
        apps.forEachIndexed { index, info ->
            if (index > 0 && index % 5 == 0) { content.addView(row); row = newAppRow() }
            val card = appCard(info)
            val lp = LinearLayout.LayoutParams(0, dp(112), 1f); lp.setMargins(0, 0, dp(12), dp(12))
            row.addView(card, lp)
        }
        if (row.childCount > 0) {
            while (row.childCount < 5) row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
            content.addView(row)
        }
    }

    private fun newAppRow() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

    @Suppress("DEPRECATION")
    private fun getApps(): List<android.content.pm.ResolveInfo> {
        val flags = android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
        val leanback = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), flags)
        val standard = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), flags)
        return (leanback + standard)
            .filter { it.activityInfo != null && it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }
    }

    private fun launchActivity(info: android.content.pm.ResolveInfo) {
        try { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setClassName(info.activityInfo.packageName, info.activityInfo.name)) }
        catch (_: Exception) { launchPackage(info.activityInfo.packageName) }
    }

    private fun launchPackage(pkg: String) {
        try {
            val intent = packageManager.getLeanbackLaunchIntentForPackage(pkg) ?: packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) startActivity(intent) else toast("App is not installed")
        } catch (_: Exception) { toast("Could not open app") }
    }

    private fun launchFirstMatching(words: List<String>) {
        val found = getApps().firstOrNull { info -> words.any { info.loadLabel(packageManager).toString().contains(it, true) } }
        if (found != null) launchActivity(found) else showAppsPage()
    }

    private fun showSearch() {
        val input = EditText(this).apply { hint = "Search installed apps"; setSingleLine(true) }
        AlertDialog.Builder(this).setTitle("Ziyana Search").setView(input)
            .setPositiveButton("Search") { _, _ ->
                val q = input.text.toString().trim()
                val match = getApps().firstOrNull { it.loadLabel(packageManager).toString().contains(q, true) }
                if (match != null) launchActivity(match) else { showAppsPage(); toast("No matching app found") }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showMediaInfo(section: String) {
        AlertDialog.Builder(this).setTitle(section)
            .setMessage("This is a launcher section. Connect or install a compatible streaming app to play your own content.")
            .setPositiveButton("Open Apps") { _, _ -> navigate("Apps") }
            .setNegativeButton("Close", null).show()
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) { navigate("Apps"); return true }
        if (keyCode == KeyEvent.KEYCODE_SEARCH) { navigate("Search"); return true }
        return super.onKeyDown(keyCode, event)
    }
}
