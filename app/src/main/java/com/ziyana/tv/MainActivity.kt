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
    private val purple = 0xFFB88AFF.toInt()
    private val muted = 0xFFD8D0EE.toInt()
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout
    private lateinit var clock: TextView
    private var selected = "Home"
    private val prefs by lazy { getSharedPreferences("ziyana_launcher", MODE_PRIVATE) }
    private fun favorites() = prefs.getStringSet("favorites", emptySet())?.toMutableSet() ?: mutableSetOf()
    private fun recent() = (prefs.getString("recent", "") ?: "").split("|").filter { it.isNotBlank() }
    private fun rememberLaunch(pkg: String) {
        val items = (listOf(pkg) + recent().filter { it != pkg }).take(12)
        prefs.edit().putString("recent", items.joinToString("|")).apply()
    }
    private fun toggleFavorite(pkg: String) {
        val items = favorites()
        if (!items.add(pkg)) items.remove(pkg)
        prefs.edit().putStringSet("favorites", items).apply()
        toast(if (pkg in items) "Added to favorites" else "Removed from favorites")
        if (selected == "Favorites") showFavoritesPage()
    }
    private val handler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            if (::clock.isInitialized) clock.text = SimpleDateFormat("h:mm a  •  EEE, MMM d", Locale.getDefault()).format(Date())
            handler.postDelayed(this, 60_000)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        val root = FrameLayout(this)
        val wallpaper = ImageView(this).apply {
            setImageResource(R.drawable.anime_sakura_wallpaper)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Ziyana TV night theme background"
        }
        root.addView(wallpaper, FrameLayout.LayoutParams(-1, -1))
        root.addView(View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xF006071B.toInt(), 0xB30B0A25.toInt(), 0x99090A20.toInt()))
        }, FrameLayout.LayoutParams(-1, -1))

        val shell = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        nav = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(18), dp(10), dp(18))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xF20B0C25.toInt(), 0xE9181536.toInt(), 0xE82B1D49.toInt()))
                .apply { cornerRadius = dp(26).toFloat(); setStroke(dp(1), 0x447F6BB2) }
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            isFocusable = false
            setPadding(0, 0, 0, dp(8))
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(26), dp(18), dp(30), dp(30))
        }
        scroll.addView(content)
        shell.addView(nav, LinearLayout.LayoutParams(dp(158), -1))
        shell.addView(scroll, LinearLayout.LayoutParams(0, -1, 1f))
        root.addView(shell, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        buildNav()
        showHome()
        nav.getChildAt(1)?.requestFocus()
        handler.post(clockTick)
    }

    override fun onDestroy() {
        handler.removeCallbacks(clockTick)
        super.onDestroy()
    }

    private fun label(textValue: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = textValue
            textSize = size
            setTextColor(color)
            typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD)
                else Typeface.create("sans-serif", Typeface.NORMAL)
            gravity = Gravity.CENTER_VERTICAL
            includeFontPadding = true
        }

    private fun panel(color: Int, radius: Int, strokeColor: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (strokeColor != 0) setStroke(dp(1), strokeColor)
        }

    private fun focusStyle(view: View, radius: Int, normal: Int, focused: Int) {
        view.setOnFocusChangeListener { v, hasFocus ->
            v.background = panel(if (hasFocus) focused else normal, radius,
                if (hasFocus) 0xFFE5C7FF.toInt() else 0x557C68B5)
            v.animate().scaleX(if (hasFocus) 1.045f else 1f)
                .scaleY(if (hasFocus) 1.045f else 1f).setDuration(130).start()
        }
    }

    private fun buildNav() {
        nav.removeAllViews()
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5), dp(8), 0, dp(27))
        }
        brand.addView(label("✦", 27f, 0xFFE4A7FF.toInt(), true))
        brand.addView(label(" ZIYANA", 17f, Color.WHITE, true))
        brand.addView(label(" TV", 17f, purple, true))
        nav.addView(brand)

        val items = listOf(
            "⌂" to "Home", "⌕" to "Search", "▦" to "Apps", "♡" to "Favorites",
            "▣" to "Live TV", "◉" to "Movies", "▤" to "Series",
            "▶" to "YouTube", "⚙" to "Settings"
        )
        items.forEach { (symbol, name) ->
            val item = TextView(this).apply {
                text = "$symbol    $name"
                textSize = 14f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(13), 0, dp(5), 0)
                isFocusable = true
                isClickable = true
                background = panel(if (selected == name) 0xFF59458C.toInt() else 0x332A2449, 17,
                    if (selected == name) 0xFFD9B5FF.toInt() else 0)
                setOnClickListener { navigate(name) }
                setOnFocusChangeListener { v, hasFocus ->
                    v.background = panel(if (hasFocus || selected == name) 0xFF59458C.toInt() else 0x332A2449,
                        17, if (hasFocus || selected == name) 0xFFD9B5FF.toInt() else 0)
                    v.animate().scaleX(if (hasFocus) 1.04f else 1f)
                        .scaleY(if (hasFocus) 1.04f else 1f).setDuration(120).start()
                }
            }
            val lp = LinearLayout.LayoutParams(-1, dp(47))
            lp.bottomMargin = dp(5)
            nav.addView(item, lp)
        }
        nav.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))
    }

    private fun navigate(name: String) {
        selected = name
        buildNav()
        when (name) {
            "Home" -> showHome()
            "Apps" -> showAppsPage()
            "Favorites" -> showFavoritesPage()
            "Settings" -> try {
                startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
            } catch (_: Exception) { toast("Settings unavailable") }
            "Live TV" -> launchFirstMatching(listOf("tv", "iptv", "live"))
            "Movies", "Series" -> showMediaInfo(name)
            "YouTube" -> launchPackage("com.google.android.youtube.tv")
            "Search" -> showSearch()
        }
    }

    private fun heading(title: String, subtitle: String? = null) {
        val block = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        block.addView(label(title, 20f, Color.WHITE, true))
        if (subtitle != null) block.addView(label(subtitle, 11f, muted))
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.topMargin = dp(21)
        lp.bottomMargin = dp(10)
        content.addView(block, lp)
    }

    private fun showHome() {
        content.removeAllViews()
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(12))
        }
        top.addView(label("ZIYANA", 18f, Color.WHITE, true))
        top.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        top.addView(actionButton("⌕", false) { navigate("Search") },
            LinearLayout.LayoutParams(dp(44), dp(40)))
        clock = label("", 12f, 0xFFF4EFFF.toInt(), true)
        top.addView(clock.apply { setPadding(dp(16), 0, 0, 0) })
        content.addView(top)
        heading("Favorites")
        addPackageRow(favorites().toList())
        heading("Recently opened")
        addPackageRow(recent())
        heading("All installed apps")
        addInstalledAppsRow()
        content.addView(actionButton("VIEW ALL APPS  ›", true) { navigate("Apps") },
            LinearLayout.LayoutParams(dp(170), dp(42)).apply { topMargin = dp(18) })
    }

    private fun heroCard(): View {
        val hero = FrameLayout(this).apply {
            background = panel(0xFF17132F.toInt(), 25, 0x88D8B7FF.toInt())
            clipToOutline = true
        }
        val artwork = ImageView(this).apply {
            setImageResource(R.drawable.anime_sakura_wallpaper)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Anime girl under a full moon and cherry blossoms"
        }
        hero.addView(artwork, FrameLayout.LayoutParams(-1, -1))
        hero.addView(View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xF20A0A20.toInt(), 0xC20D0A28.toInt(), 0x220D0A28))
        }, FrameLayout.LayoutParams(-1, -1))

        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(25), dp(22), dp(10), dp(18))
        }
        copy.addView(label("ZIYANA  •  ANIME NIGHT  •  4K", 10f, 0xFFF0BFFF.toInt(), true))
        copy.addView(label("Moonlit\nSakura", 34f, Color.WHITE, true).apply {
            setLineSpacing(0f, 0.92f)
            setPadding(0, dp(5), 0, dp(1))
        })
        copy.addView(label("Anime atmosphere. Your favourite apps. One screen.", 12f, 0xFFE4D8F7.toInt()))
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(13), 0, 0)
        }
        buttons.addView(actionButton("▦  All Apps", true) { navigate("Apps") },
            LinearLayout.LayoutParams(dp(132), dp(42)))
        buttons.addView(actionButton("⌕  Search", false) { navigate("Search") },
            LinearLayout.LayoutParams(dp(112), dp(42)).apply { marginStart = dp(9) })
        copy.addView(buttons)
        hero.addView(copy, FrameLayout.LayoutParams(-1, -1))
        return hero
    }

    private fun actionButton(textValue: String, primary: Boolean, action: () -> Unit): TextView =
        TextView(this).apply {
            text = textValue
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = panel(if (primary) 0xFF8056C9.toInt() else 0x553D315F, 18,
                if (primary) 0xFFDDBEFF.toInt() else 0x887F6EB1.toInt())
            isFocusable = true
            isClickable = true
            focusStyle(this, 18, if (primary) 0xFF8056C9.toInt() else 0x553D315F,
                if (primary) 0xFF9A70E3.toInt() else 0xFF51417C.toInt())
            setOnClickListener { action() }
        }

    private fun addInstalledAppsRow() {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val hsv = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFocusable = false
            clipToPadding = false
            addView(row)
        }
        getApps().forEach { info ->
            val card = appCard(info)
            val lp = LinearLayout.LayoutParams(dp(132), dp(112))
            lp.marginEnd = dp(12)
            row.addView(card, lp)
        }
        if (row.childCount == 0) row.addView(label("No apps found. Install apps from Google Play.", 13f, muted))
        content.addView(hsv, LinearLayout.LayoutParams(-1, dp(118)))
    }

    private fun appCard(info: android.content.pm.ResolveInfo): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = panel(0xE91B1A38.toInt(), 17, 0x667D6AAE)
            isFocusable = true
            isClickable = true
            focusStyle(this, 17, 0xE91B1A38.toInt(), 0xFF55428F.toInt())
            setOnClickListener { rememberLaunch(info.activityInfo.packageName); launchActivity(info) }
            setOnLongClickListener { toggleFavorite(info.activityInfo.packageName); true }
        }
        card.addView(ImageView(this).apply {
            setImageDrawable(info.loadIcon(packageManager))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }, LinearLayout.LayoutParams(dp(47), dp(47)))
        card.addView(label(info.loadLabel(packageManager).toString(), 11f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            setPadding(dp(4), dp(5), dp(4), 0)
        })
        return card
    }

    private fun addAnimeAppsRow() {
        val preferred = listOf("crunchyroll", "netflix", "prime video", "disney", "youtube", "plex", "tubi", "jellyfin")
        val installed = getApps()
        val matches = installed.filter { app ->
            preferred.any { key -> app.loadLabel(packageManager).toString().contains(key, true) }
        }
        val apps = if (matches.isNotEmpty()) matches else installed.take(6)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val hsv = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFocusable = false
            clipToPadding = false
            addView(row)
        }
        apps.forEach { info ->
            val lp = LinearLayout.LayoutParams(dp(132), dp(112))
            lp.marginEnd = dp(12)
            row.addView(appCard(info), lp)
        }
        if (apps.isEmpty()) row.addView(label("Install streaming apps from Google Play Store to see their icons here.", 12f, muted))
        content.addView(hsv, LinearLayout.LayoutParams(-1, dp(118)))
    }

    private fun addFeatureTiles(names: List<String>) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val colors = intArrayOf(0xFF583477.toInt(), 0xFF214D72.toInt(), 0xFF6A315F.toInt(), 0xFF3D397A.toInt())
        val symbols = listOf("✿", "▣", "▶", "♡")
        names.forEachIndexed { i, name ->
            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(13), dp(9), dp(8), dp(8))
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    intArrayOf(colors[i], 0xFF15152F.toInt()))
                    .apply { cornerRadius = dp(17).toFloat(); setStroke(dp(1), 0x557C6CB0) }
                isFocusable = true
                isClickable = true
                focusStyle(this, 17, colors[i], 0xFF58458C.toInt())
                addView(label(symbols[i], 22f, 0xFFF0D6FF.toInt(), true))
                addView(label(name, 13f, Color.WHITE, true))
                setOnClickListener {
                    when (name) {
                        "Watchlist" -> showMediaInfo(name)
                        else -> showMediaInfo(name)
                    }
                }
            }
            val lp = LinearLayout.LayoutParams(0, dp(83), 1f)
            lp.marginEnd = dp(10)
            row.addView(tile, lp)
        }
        content.addView(row)
    }

    private fun addQuickActions() {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Apps" to "▦", "Live TV" to "▣", "YouTube" to "▶", "Settings" to "⚙").forEach { (name, symbol) ->
            row.addView(actionButton("$symbol  $name", false) { navigate(name) },
                LinearLayout.LayoutParams(dp(125), dp(43)).apply { marginEnd = dp(10) })
        }
        content.addView(row)
    }

    private fun clearPage(title: String, subtitle: String? = null) {
        content.removeAllViews()
        content.addView(label(title, 28f, Color.WHITE, true).apply {
            setPadding(0, dp(8), 0, dp(4))
        })
        if (subtitle != null) content.addView(label(subtitle, 12f, muted))
    }

    private fun showAppsPage() {
        clearPage("All apps  ·  ${getApps().size}")
        val apps = getApps()
        if (apps.isEmpty()) {
            content.addView(label("No TV apps are visible. Install apps from Google Play Store.", 15f, muted))
            return
        }
        addAppGrid(apps)
    }

    private fun newAppRow() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

    private fun addPackageRow(packages: List<String>) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val byPackage = getApps().associateBy { it.activityInfo.packageName }
        packages.distinct().mapNotNull { byPackage[it] }.forEach { info ->
            row.addView(appCard(info), LinearLayout.LayoutParams(dp(132), dp(112)).apply { marginEnd = dp(12) })
        }
        if (row.childCount == 0) content.addView(label("Long-press an app to add it to Favorites.", 12f, muted))
        else content.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; isFocusable = false; addView(row) }, LinearLayout.LayoutParams(-1, dp(120)))
    }

    private fun showFavoritesPage() {
        clearPage("Favorites", "Long-press an app card to add or remove it")
        val apps = getApps().filter { it.activityInfo.packageName in favorites() }
        if (apps.isEmpty()) content.addView(label("No favorites yet. Open Apps and long-press an app.", 14f, muted))
        else addAppGrid(apps)
    }

    private fun addAppGrid(apps: List<android.content.pm.ResolveInfo>) {
        var row = newAppRow()
        apps.forEachIndexed { index, info ->
            if (index > 0 && index % 5 == 0) { content.addView(row); row = newAppRow() }
            row.addView(appCard(info), LinearLayout.LayoutParams(0, dp(116), 1f).apply { setMargins(0, dp(12), dp(12), 0) })
        }
        if (row.childCount > 0) {
            while (row.childCount < 5) row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
            content.addView(row)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::content.isInitialized) when (selected) {
            "Home" -> showHome()
            "Apps" -> showAppsPage()
            "Favorites" -> showFavoritesPage()
        }
    }

    @Suppress("DEPRECATION")
    private fun getApps(): List<android.content.pm.ResolveInfo> {
        // Query both Android TV and standard Android launchable activities.
        // QUERY_ALL_PACKAGES prevents Android 11+ package visibility hiding apps.
        val leanback = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)
        val standard = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        val combined = (leanback + standard)
            .filter { it.activityInfo != null && it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName + "/" + it.activityInfo.name }
        return combined.sortedBy {
            it.loadLabel(packageManager).toString().trim().lowercase(Locale.getDefault())
        }
    }

    private fun launchActivity(info: android.content.pm.ResolveInfo) {
        try {
            val pkg = info.activityInfo.packageName
            val launch = packageManager.getLeanbackLaunchIntentForPackage(pkg)
                ?: packageManager.getLaunchIntentForPackage(pkg)
                ?: Intent(Intent.ACTION_MAIN).setClassName(pkg, info.activityInfo.name)
            startActivity(launch)
        } catch (_: Exception) { toast("Could not open app") }
    }

    private fun launchPackage(pkg: String) {
        try {
            val intent = packageManager.getLeanbackLaunchIntentForPackage(pkg)
                ?: packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) startActivity(intent) else toast("App is not installed")
        } catch (_: Exception) { toast("Could not open app") }
    }

    private fun launchFirstMatching(words: List<String>) {
        val found = getApps().firstOrNull { info ->
            words.any { info.loadLabel(packageManager).toString().contains(it, true) }
        }
        if (found != null) launchActivity(found) else showAppsPage()
    }

    private fun showSearch() {
        val input = EditText(this).apply {
            hint = "Search installed apps"
            setSingleLine(true)
        }
        AlertDialog.Builder(this).setTitle("Ziyana Search").setView(input)
            .setPositiveButton("Search") { _, _ ->
                val query = input.text.toString().trim()
                val match = getApps().firstOrNull {
                    it.loadLabel(packageManager).toString().contains(query, true)
                }
                if (match != null) launchActivity(match)
                else { showAppsPage(); toast("No matching app found") }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showMediaInfo(section: String) {
        AlertDialog.Builder(this).setTitle(section)
            .setMessage("This is a launcher section. Open a compatible streaming app to watch your own content.")
            .setPositiveButton("Open Apps") { _, _ -> navigate("Apps") }
            .setNegativeButton("Close", null).show()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) { navigate("Apps"); return true }
        if (keyCode == KeyEvent.KEYCODE_SEARCH) { navigate("Search"); return true }
        if (keyCode == KeyEvent.KEYCODE_BACK && selected != "Home") {
            navigate("Home")
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}