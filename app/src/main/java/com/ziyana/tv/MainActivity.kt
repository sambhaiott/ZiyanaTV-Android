package com.ziyana.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.speech.RecognizerIntent
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
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val purple = 0xFFE0C99A.toInt()
    private val muted = 0xFFC5C2C9.toInt()
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout
    private lateinit var clock: TextView
    private var selected = "Home"
    private var appFilter = "All"
    private var searchQuery = ""
    private var pendingVoiceQuery = ""
    private val voiceRequestCode = 41
    private val prefs by lazy { getSharedPreferences("ziyana_launcher", MODE_PRIVATE) }
    private fun pinned() = prefs.getStringSet("pinned_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
    private fun favorites() = prefs.getStringSet("favorite_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
    private fun hiddenApps() = prefs.getStringSet("hidden_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
    private fun toggleHidden(pkg: String) {
        val items = hiddenApps()
        val hidden = if (items.contains(pkg)) { items.remove(pkg); false } else { items.add(pkg); true }
        prefs.edit().putStringSet("hidden_apps", items).apply()
        toast(if (hidden) "App hidden from launcher" else "App is visible again")
        when (selected) {
            "Home" -> showHome()
            "Favorites" -> showFavoritesPage()
            "Apps", "Entertainment", "Tools" -> showAppsPage()
        }
    }
    private fun manageHiddenApps() {
        val apps = getApps().sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }
        if (apps.isEmpty()) { toast("No installed apps found"); return }
        val hidden = hiddenApps()
        val names = apps.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        val checked = apps.map { it.activityInfo.packageName in hidden }.toBooleanArray()
        AlertDialog.Builder(this)
            .setTitle("Hidden apps")
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                val pkg = apps[which].activityInfo.packageName
                val current = hiddenApps()
                if (isChecked) current.add(pkg) else current.remove(pkg)
                prefs.edit().putStringSet("hidden_apps", current).apply()
            }
            .setPositiveButton("Done") { _, _ ->
                when (selected) {
                    "Home" -> showHome()
                    "Favorites" -> showFavoritesPage()
                    else -> showAppsPage()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun toggleFavorite(pkg: String) {
        val items = favorites()
        val added = if (items.contains(pkg)) { items.remove(pkg); false } else { items.add(pkg); true }
        prefs.edit().putStringSet("favorite_apps", items).apply()
        toast(if (added) "Added to Favorites" else "Removed from Favorites")
        if (selected == "Favorites") showFavoritesPage()
        else if (selected == "Apps" || selected == "Home" || selected == "Entertainment" || selected == "Tools") {
            if (selected == "Home") showHome() else showAppsPage()
        }
    }
    private fun togglePinned(pkg: String) {
        val items = pinned()
        val added = if (items.contains(pkg)) { items.remove(pkg); false } else { items.add(pkg); true }
        prefs.edit().putStringSet("pinned_apps", items).apply()
        toast(if (added) "App pinned" else "App unpinned")
        if (selected == "Apps" || selected == "Home" || selected == "Entertainment" || selected == "Tools") {
            if (selected == "Home") showHome() else showAppsPage()
        }
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
            setImageResource(R.drawable.anime_sakura_wallpaper_hd)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Ziyana TV night theme background"
        }
        root.addView(wallpaper, FrameLayout.LayoutParams(-1, -1))
        root.addView(View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xF0080A10.toInt(), 0xD0181718.toInt(), 0xA02A2118.toInt()))
        }, FrameLayout.LayoutParams(-1, -1))

        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }
        nav = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(18), dp(10), dp(18))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xF20B0C10.toInt(), 0xEA201B18.toInt(), 0xE82D251C.toInt()))
                .apply { cornerRadius = dp(26).toFloat(); setStroke(dp(1), 0x887F6A4A.toInt()) }
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            // Keep focused cards and their scale animation inside the content viewport.
            // Without this, focus shadows can draw over the left navigation rail.
            clipToPadding = true
            clipChildren = true
            isFocusable = false
            setPadding(0, 0, 0, dp(8))
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(26), dp(18), dp(30), dp(30))
            clipChildren = false
            clipToPadding = false
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

    // Use the same deep charcoal-to-warm-black palette as the left navigation rail.
    private fun panel(color: Int, radius: Int, strokeColor: Int = 0): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(color, 0xFF171513.toInt())).apply {
            cornerRadius = dp(radius).toFloat()
            if (strokeColor != 0) setStroke(dp(1), strokeColor)
        }

    // Respect each component's own colors so focus, cards and controls keep their
    // intended champagne/charcoal contrast instead of collapsing to one flat shade.
    private fun glossySurface(top: Int, bottom: Int, radius: Int, stroke: Int = 0x557D7564): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(top, bottom)).apply {
            cornerRadius = dp(radius).toFloat()
            if (stroke != 0) setStroke(dp(1), stroke)
        }

    private fun focusStyle(view: View, radius: Int, normal: Int, focused: Int) {
        view.isFocusable = true
        view.isClickable = true
        view.setOnFocusChangeListener { v, hasFocus ->
            val surface = if (hasFocus) focused else normal
            v.background = glossySurface(
                surface,
                if (hasFocus) 0xFF26231D.toInt() else 0xFF17191D.toInt(),
                radius,
                if (hasFocus) 0xFFE8D8B8.toInt() else 0x557D7564
            )
            v.animate().cancel()
            v.animate()
                .scaleX(if (hasFocus) 1.055f else 1f)
                .scaleY(if (hasFocus) 1.055f else 1f)
                .alpha(if (hasFocus) 1f else 0.97f)
                .translationZ(if (hasFocus) dp(10).toFloat() else 0f)
                .setDuration(215)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
            v.elevation = if (hasFocus) dp(10).toFloat() else 0f
        }
    }

    private fun buildNav() {
        nav.removeAllViews()
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5), dp(8), 0, dp(27))
        }
        brand.addView(label("✧", 27f, 0xFFE7D2A8.toInt(), true))
        brand.addView(label(" ZIYANA", 17f, Color.WHITE, true))
        brand.addView(label(" TV", 17f, purple, true))
        nav.addView(brand)

        val items = listOf(
            "⌂" to "Home", "⌕" to "Search", "▦" to "Apps", "♡" to "Favorites",
            "▷" to "Entertainment", "◈" to "Tools", "⚙" to "Settings"
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
                background = glossySurface(if (selected == name) 0xFF65543A.toInt() else 0x66413A30,
                    0xFF171513.toInt(), 17, if (selected == name) 0xFFE8D5AF.toInt() else 0x667D6749)
                setOnClickListener { navigate(name) }
                setOnFocusChangeListener { v, hasFocus ->
                    v.background = glossySurface(if (hasFocus || selected == name) 0xFF705B3B.toInt() else 0x66413A30,
                        0xFF171513.toInt(), 17,
                        if (hasFocus || selected == name) 0xFFE8D5AF.toInt() else 0x667D6749)
                    v.animate().scaleX(if (hasFocus) 1.035f else 1f)
                        .scaleY(if (hasFocus) 1.035f else 1f)
                        .setDuration(205)
                        .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
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
            "Apps" -> { appFilter = "All"; showAppsPage() }
            "Favorites" -> showFavoritesPage()
            "Entertainment" -> { appFilter = "Entertainment"; showAppsPage() }
            "Tools" -> { appFilter = "Tools"; showAppsPage() }
            "Settings" -> showSettings()
            "Search" -> showSearch()
        }
        content.alpha = 0f
        content.translationY = dp(9).toFloat()
        content.animate().alpha(1f).translationY(0f)
            .setDuration(245).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
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

    private fun addStatusHeader() {
        // Clock/date stays on the first line; compact Search and Settings icons sit below it.
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            setPadding(dp(2), dp(2), dp(2), dp(12))
        }

        val clockRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
        clock = label("", 13f, 0xFFFFFFFF.toInt(), true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = glossySurface(0xD03A342B.toInt(), 0xC0161512.toInt(), 15, 0x887D6749.toInt())
            elevation = dp(3).toFloat()
        }
        clockRow.addView(clock)
        header.addView(clockRow, LinearLayout.LayoutParams(-1, -2))

        val controlsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(0, dp(5), 0, 0)
        }
        val searchControl = statusIcon(R.drawable.ic_search, "Search apps") { navigate("Search") }
        controlsRow.addView(searchControl, LinearLayout.LayoutParams(dp(30), dp(27)).apply {
            marginEnd = dp(6)
        })

        val voiceControl = statusIcon(android.R.drawable.ic_btn_speak_now, "Voice search") { startVoiceSearch() }
        controlsRow.addView(voiceControl, LinearLayout.LayoutParams(dp(30), dp(27)).apply {
            marginEnd = dp(6)
        })

        val settingsControl = statusIcon(R.drawable.ic_settings, "Settings") { navigate("Settings") }
        controlsRow.addView(settingsControl, LinearLayout.LayoutParams(dp(30), dp(27)))
        header.addView(controlsRow, LinearLayout.LayoutParams(-1, -2))

        content.addView(header, LinearLayout.LayoutParams(-1, -2))
        clock.text = SimpleDateFormat("h:mm a  •  EEE, MMM d", Locale.getDefault()).format(Date())
    }

    private fun startVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak an app name")
        }
        try {
            startActivityForResult(intent, voiceRequestCode)
        } catch (_: Exception) {
            toast("Voice search is not available on this TV")
        }
    }

    @Deprecated("Deprecated by Android, retained for broad Android TV compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == voiceRequestCode && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) {
                pendingVoiceQuery = spoken
                navigate("Search")
            }
        }
    }

    private fun statusIcon(iconRes: Int, description: String, action: () -> Unit): ImageView =
        ImageView(this).apply {
            setImageResource(iconRes)
            setColorFilter(Color.WHITE)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = description
            background = glossySurface(0xD03A342B.toInt(), 0xC0161512.toInt(), 10, 0x887D6749.toInt())
            isFocusable = true
            isClickable = true
            setPadding(dp(7), dp(5), dp(7), dp(5))
            focusStyle(this, 10, 0xC016181C.toInt(), 0xFF5A5141.toInt())
            setOnClickListener { action() }
        }

    private fun showHome() {
        content.removeAllViews()
        addStatusHeader()

        // Keep one permanent full-screen anime wallpaper; no second hero image or slideshow.
        content.addView(View(this), LinearLayout.LayoutParams(1, dp(20)))

        heading("Program")
        addInstalledAppsRow()

        heading("Watching Continue")
        addWatchingContinueCard()

        heading("Explore")
        addFeatureTiles(listOf("Favorites", "Entertainment", "Tools", "Settings"))

        content.addView(actionButton("VIEW ALL APPLICATIONS  ›", true) { navigate("Apps") },
            LinearLayout.LayoutParams(dp(230), dp(44)).apply { topMargin = dp(14) })
    }

    // YouTube keeps watch history inside the user's Google account. The launcher
    // cannot read private history directly, so this card opens YouTube's own History page.
    private fun addWatchingContinueCard() {
        val youtube = getApps().firstOrNull {
            it.loadLabel(packageManager).toString().contains("YouTube", true)
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
            background = glossySurface(0xFF38332B.toInt(), 0xFF171513.toInt(), 20, 0x887D6749.toInt())
            isFocusable = true
            isClickable = true
            focusStyle(this, 20, 0xFF25231F.toInt(), 0xFF705B3B.toInt())
            setOnClickListener { openYouTubeHistory() }
        }
        val icon = ImageView(this).apply {
            if (youtube != null) setImageDrawable(youtube.loadIcon(packageManager))
            else setImageResource(android.R.drawable.ic_media_play)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        card.addView(icon, LinearLayout.LayoutParams(dp(54), dp(54)).apply { marginEnd = dp(18) })
        val textColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textColumn.addView(label("YouTube  ·  Watch history", 15f, Color.WHITE, true))
        textColumn.addView(label("Open your recently watched videos  ›", 11f, muted))
        card.addView(textColumn, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(card, LinearLayout.LayoutParams(-1, dp(86)))
    }

    private fun openYouTubeHistory() {
        val historyUri = android.net.Uri.parse("https://www.youtube.com/feed/history")
        val packages = listOf("com.google.android.youtube.tv", "com.google.android.youtube")
        for (pkg in packages) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, historyUri).setPackage(pkg)
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                    return
                }
            } catch (_: Exception) { }
        }
        val youtube = getApps().firstOrNull {
            it.loadLabel(packageManager).toString().contains("YouTube", true)
        }
        if (youtube != null) launchActivity(youtube)
        else toast("YouTube is not installed on this TV")
    }

    private fun actionButton(textValue: String, primary: Boolean, action: () -> Unit): TextView =
        TextView(this).apply {
            text = textValue
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = panel(if (primary) 0xFF786B50.toInt() else 0x55403D36, 18,
                if (primary) 0xFFE7D5AC.toInt() else 0x887C766A.toInt())
            isFocusable = true
            isClickable = true
            focusStyle(this, 18, if (primary) 0xFF786B50.toInt() else 0x55403D36,
                if (primary) 0xFF968363.toInt() else 0xFF5B5548.toInt())
            setOnClickListener { action() }
        }

    private fun addInstalledAppsRow() {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
            setPadding(dp(4), dp(8), dp(8), dp(8))
        }
        val hsv = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFocusable = false
            // Clip the horizontal rail to its own viewport. Focused items scroll
            // into view one at a time instead of drawing across the sidebar.
            clipChildren = true
            clipToPadding = true
            addView(row)
        }
        sortedApps().take(14).forEach { info ->
            val card = homeAppCard(info)
            val lp = LinearLayout.LayoutParams(dp(154), dp(136))
            lp.marginEnd = dp(16)
            row.addView(card, lp)
        }
        if (row.childCount == 0) row.addView(label("No apps found. Install apps from Google Play.", 13f, muted))
        content.addView(hsv, LinearLayout.LayoutParams(-1, dp(152)))
    }

    // Large, authentic installed-app icons in an Apple TV-inspired horizontal app rail.
    private fun homeAppCard(info: android.content.pm.ResolveInfo): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = glossySurface(0xE93B3D42.toInt(), 0xE916181C.toInt(), 22, 0x667D7769)
            isFocusable = true
            isClickable = true
            clipChildren = false
            focusStyle(this, 22, 0xE9161730.toInt(), 0xFF625846.toInt())
            setOnClickListener { launchActivity(info) }
            setOnLongClickListener { showAppOptions(info); true }
        }
        val iconPlate = FrameLayout(this).apply {
            background = glossySurface(0xFF41434A.toInt(), 0xFF1D1F24.toInt(), 18, 0x667D7769)
        }
        iconPlate.addView(ImageView(this).apply {
            setImageDrawable(info.loadIcon(packageManager))
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = info.loadLabel(packageManager).toString()
        }, FrameLayout.LayoutParams(dp(64), dp(64), Gravity.CENTER))
        card.addView(iconPlate, LinearLayout.LayoutParams(dp(78), dp(78)))
        card.addView(label(info.loadLabel(packageManager).toString(), 12f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(dp(5), dp(8), dp(5), 0)
        }, LinearLayout.LayoutParams(-1, dp(27)))
        if (info.activityInfo.packageName in favorites()) {
            card.addView(label("★", 10f, 0xFFFFD36D.toInt(), true))
        }
        return card
    }

    private fun appCard(info: android.content.pm.ResolveInfo): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = glossySurface(0xFF393630.toInt(), 0xFF171513.toInt(), 17, 0x667D6749)
            isFocusable = true
            isClickable = true
            clipChildren = false
            focusStyle(this, 17, 0xFF25231F.toInt(), 0xFF705B3B.toInt())
            setOnClickListener { launchActivity(info) }
            setOnLongClickListener { showAppOptions(info); true }
        }
        val iconFrame = FrameLayout(this)
        iconFrame.addView(ImageView(this).apply {
            setImageDrawable(info.loadIcon(packageManager))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }, FrameLayout.LayoutParams(dp(47), dp(47), Gravity.CENTER))
        if (info.activityInfo.packageName in favorites()) {
            iconFrame.addView(label("★", 14f, 0xFFFFD36D.toInt(), true),
                FrameLayout.LayoutParams(dp(22), dp(22), Gravity.END or Gravity.TOP))
        }
        card.addView(iconFrame, LinearLayout.LayoutParams(dp(54), dp(52)))
        card.addView(label(info.loadLabel(packageManager).toString(), 11f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(dp(4), dp(5), dp(4), 0)
        })
        return card
    }

    private fun showFavoritesPage() {
        content.removeAllViews()
        val apps = sortedApps().filter { it.activityInfo.packageName in favorites() }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }
        content.addView(label("Favorites", 27f, Color.WHITE, true).apply { setPadding(0, dp(8), 0, dp(4)) })
        content.addView(label("Your starred apps, ready to launch", 12f, muted))
        if (apps.isEmpty()) {
            content.addView(label("No favorites yet. Open Apps and long-press an app to add it here.", 14f, muted).apply {
                setPadding(0, dp(24), 0, 0)
            })
        } else addAppGrid(apps)
    }

    private fun showAppOptions(info: android.content.pm.ResolveInfo) {
        val pkg = info.activityInfo.packageName
        val isFavorite = pkg in favorites()
        val isPinned = pkg in pinned()
        val actions = arrayOf(
            if (isFavorite) "Remove from Favorites" else "Add to Favorites",
            if (isPinned) "Unpin from top" else "Pin to top",
            "Open app",
            "App information",
            if (pkg in hiddenApps()) "Show in launcher" else "Hide from launcher",
            "Uninstall app"
        )
        AlertDialog.Builder(this)
            .setTitle(info.loadLabel(packageManager).toString())
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> toggleFavorite(pkg)
                    1 -> togglePinned(pkg)
                    2 -> launchActivity(info)
                    3 -> openAppInfo(pkg)
                    4 -> toggleHidden(pkg)
                    5 -> uninstallApp(pkg)
                }
            }.show()
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
        val colors = intArrayOf(0xFF514633.toInt(), 0xFF38332B.toInt(), 0xFF34302B.toInt(), 0xFF40382E.toInt())
        val symbols = listOf("✿", "▣", "▶", "♡")
        names.forEachIndexed { i, name ->
            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(13), dp(9), dp(8), dp(8))
                background = glossySurface(colors[i], 0xFF17191D.toInt(), 17, 0x667D7564)
                isFocusable = true
                isClickable = true
                focusStyle(this, 17, colors[i], 0xFF806747.toInt())
                addView(label(symbols[i], 22f, 0xFFE9D6B4.toInt(), true))
                addView(label(name, 13f, Color.WHITE, true))
                setOnClickListener {
                    when (name) {
                        "Favorites" -> navigate("Favorites")
                        "Entertainment" -> navigate("Entertainment")
                        "Tools" -> navigate("Tools")
                        "Settings" -> navigate("Settings")
                        else -> navigate("Apps")
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
            row.addView(actionButton("$symbol  $name", false) {
                when (name) {
                    "Apps" -> navigate("Apps")
                    "Settings" -> navigate("Settings")
                    "YouTube" -> launchFirstMatching(listOf("YouTube"))
                    "Live TV" -> launchFirstMatching(listOf("Live TV", "Live Channels", "TV"))
                }
            },
                LinearLayout.LayoutParams(dp(125), dp(43)).apply { marginEnd = dp(10) })
        }
        content.addView(row)
    }

    private fun clearPage(title: String, subtitle: String? = null) {
        content.removeAllViews()
        addStatusHeader()
        content.addView(label(title, 28f, Color.WHITE, true).apply {
            setPadding(0, dp(8), 0, dp(4))
        })
        if (subtitle != null) content.addView(label(subtitle, 12f, muted))
    }

    private fun showAppsPage() {
        clearPage("Applications  ·  ${sortedApps().size}", "Choose a category or open any installed app")
        val filters = listOf("All", "Entertainment", "Tools", "System")
        val filterRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        filters.forEach { filter ->
            filterRow.addView(actionButton(filter, appFilter == filter) {
                appFilter = filter
                showAppsPage()
            }, LinearLayout.LayoutParams(dp(120), dp(40)).apply { marginEnd = dp(9) })
        }
        content.addView(filterRow, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(12) })
        val apps = sortedApps().filter { matchesFilter(it, appFilter) }
        heading(if (appFilter == "All") "All applications" else appFilter)
        if (apps.isEmpty()) {
            content.addView(label("No apps found in this category.", 14f, muted))
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
        if (row.childCount == 0) content.addView(label("Long-press an app to pin it to the top.", 12f, muted))
        else content.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; isFocusable = false; addView(row) }, LinearLayout.LayoutParams(-1, dp(120)))
    }

    private fun sortedApps(): List<android.content.pm.ResolveInfo> {
        val pins = pinned()
        val hidden = hiddenApps()
        return getApps().filter { it.activityInfo.packageName !in hidden }
            .sortedWith(compareByDescending<android.content.pm.ResolveInfo> {
                it.activityInfo.packageName in pins
            }.thenBy { it.loadLabel(packageManager).toString().trim().lowercase(Locale.getDefault()) })
    }

    private fun matchesFilter(info: android.content.pm.ResolveInfo, filter: String): Boolean {
        if (filter == "All") return true
        val name = info.loadLabel(packageManager).toString().lowercase(Locale.getDefault())
        val pkg = info.activityInfo.packageName.lowercase(Locale.getDefault())
        val media = listOf("youtube", "netflix", "prime", "video", "disney", "crunchy", "plex", "tubi", "jellyfin", "spotify", "music", "iptv", "tv", "movie", "media")
        val system = listOf("settings", "launcher", "system", "update", "input", "remote", "cast")
        return when (filter) {
            "Entertainment" -> media.any { name.contains(it) || pkg.contains(it) }
            "System" -> system.any { name.contains(it) || pkg.contains(it) }
            "Tools" -> !media.any { name.contains(it) || pkg.contains(it) } && !system.any { name.contains(it) || pkg.contains(it) }
            else -> true
        }
    }

    private fun addAppGrid(apps: List<android.content.pm.ResolveInfo>) {
        // Adapt the app drawer to the TV's actual logical screen width.
        val screenDp = (resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt()
        val availableDp = (screenDp - 158 - 56 - 36).coerceAtLeast(640)
        val columns = (availableDp / 154).coerceIn(4, 8)
        var row = newAppRow().apply { clipChildren = false; clipToPadding = false }
        apps.forEachIndexed { index, info ->
            if (index > 0 && index % columns == 0) {
                content.addView(row)
                row = newAppRow().apply { clipChildren = false; clipToPadding = false }
            }
            row.addView(appCard(info), LinearLayout.LayoutParams(0, dp(116), 1f)
                .apply { setMargins(0, dp(12), dp(12), 0) })
        }
        if (row.childCount > 0) {
            while (row.childCount < columns) row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
            content.addView(row)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::content.isInitialized) when (selected) {
            "Home" -> showHome()
            "Apps", "Entertainment", "Tools" -> showAppsPage()
            "Favorites" -> showFavoritesPage()
            "Search" -> showSearch()
            "Settings" -> showSettings()
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
            .distinctBy { it.activityInfo.packageName }
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
        val apps = getApps()
        // Respect requested priority: specific Live TV names before generic TV matches.
        val found = words.firstNotNullOfOrNull { word ->
            apps.firstOrNull { info ->
                info.loadLabel(packageManager).toString().contains(word, true)
            }
        }
        if (found != null) launchActivity(found) else showAppsPage()
    }

    private fun showSearch() {
        clearPage("Search", "Find and launch any installed application")
        val input = EditText(this).apply {
            hint = "Type an app name…"
            setSingleLine(true)
            textSize = 17f
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = panel(0xDD29231B.toInt(), 16, 0x998B7656.toInt())
            isFocusable = true
        }
        content.addView(input, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(18) })
        if (pendingVoiceQuery.isNotBlank()) {
            searchQuery = pendingVoiceQuery
            pendingVoiceQuery = ""
        }
        if (searchQuery.isNotBlank()) {
            input.setText(searchQuery)
            input.setSelection(input.text.length)
        }
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(results, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        fun render(query: String) {
            results.removeAllViews()
            val found = sortedApps().filter { it.loadLabel(packageManager).toString().contains(query.trim(), true) }
            results.addView(label(if (query.isBlank()) "${sortedApps().size} visible apps" else "${found.size} results", 12f, muted))
            if (found.isEmpty()) results.addView(label("No matching app found.", 14f, muted).apply { setPadding(0, dp(18), 0, 0) })
            else found.take(30).forEach { info ->
                val line = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    isFocusable = true
                    isClickable = true
                    background = panel(0x9940392E.toInt(), 13, 0x668B7655.toInt())
                    val icon = ImageView(this@MainActivity).apply { setImageDrawable(info.loadIcon(packageManager)) }
                    addView(icon, LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginEnd = dp(14) })
                    addView(label(info.loadLabel(packageManager).toString(), 14f, Color.WHITE, true), LinearLayout.LayoutParams(0, -1, 1f))
                    setOnClickListener { launchActivity(info) }
                    focusStyle(this, 13, 0x9940392E.toInt(), 0xFF806747.toInt())
                }
                results.addView(line, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(7) })
            }
        }
        render("")
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString() ?: ""
                render(searchQuery)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        input.requestFocus()
    }

    private fun openAppInfo(pkg: String) {
        try { startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$pkg"))) }
        catch (_: Exception) { toast("App information unavailable") }
    }

    private fun uninstallApp(pkg: String) {
        try { startActivity(Intent(Intent.ACTION_DELETE, android.net.Uri.parse("package:$pkg"))) }
        catch (_: Exception) { toast("Uninstall is not available for this app") }
    }

    private fun openSystemPanel(action: String) {
        try { startActivity(Intent(action)) }
        catch (_: Exception) { try { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) } catch (_: Exception) { toast("This setting is unavailable on this TV") } }
    }

    private fun showMediaInfo(section: String) {
        AlertDialog.Builder(this).setTitle(section)
            .setMessage("Open a compatible streaming app from Applications to watch your content.")
            .setPositiveButton("Open apps") { _, _ -> navigate("Apps") }
            .setNegativeButton("Close", null).show()
    }

    private fun showControlCenter() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(18))
            background = glossySurface(0xF02B2925.toInt(), 0xF00F1013.toInt(), 22, 0x99A58A63.toInt())
        }
        panel.addView(label("Control Center", 22f, Color.WHITE, true))
        panel.addView(label("Quick access to your TV controls", 12f, muted).apply {
            setPadding(0, dp(3), 0, dp(12))
        })
        val controls: List<Pair<String, () -> Unit>> = listOf(
            "Wi-Fi & Network" to { openSystemPanel(android.provider.Settings.ACTION_WIFI_SETTINGS) },
            "Display & Brightness" to { openSystemPanel(android.provider.Settings.ACTION_DISPLAY_SETTINGS) },
            "Sound & Audio" to { openSystemPanel(android.provider.Settings.ACTION_SOUND_SETTINGS) },
            "Date & Time" to { openSystemPanel(android.provider.Settings.ACTION_DATE_SETTINGS) },
            "Android System Settings" to { openSystemPanel(android.provider.Settings.ACTION_SETTINGS) },
            "Accessibility" to { openSystemPanel(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS) }
        )
        controls.forEach { (name, action) ->
            panel.addView(actionButton(name, false, action),
                LinearLayout.LayoutParams(dp(330), dp(48)).apply { topMargin = dp(7) })
        }
        val dialog = AlertDialog.Builder(this).setView(panel)
            .setNegativeButton("Close", null).create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun openPlayStore() {
        try {
            val intent = packageManager.getLeanbackLaunchIntentForPackage("com.android.vending")
                ?: packageManager.getLaunchIntentForPackage("com.android.vending")
            if (intent != null) startActivity(intent) else toast("Google Play Store is not available on this TV")
        } catch (_: Exception) { toast("Google Play Store is not available on this TV") }
    }

    private fun showSettings() {
        clearPage("Settings", "Launcher and device controls")
        val options: List<Pair<String, () -> Unit>> = listOf(
            "Control Center" to { showControlCenter() },
            "Google Play Store" to { openPlayStore() },
            "Android system settings" to { openSystemPanel(android.provider.Settings.ACTION_SETTINGS) },
            "Network / Wi-Fi settings" to { openSystemPanel(android.provider.Settings.ACTION_WIFI_SETTINGS) },
            "Display settings" to { openSystemPanel(android.provider.Settings.ACTION_DISPLAY_SETTINGS) },
            "Sound settings" to { openSystemPanel(android.provider.Settings.ACTION_SOUND_SETTINGS) },
            "Date and time" to { openSystemPanel(android.provider.Settings.ACTION_DATE_SETTINGS) },
            "Accessibility" to { openSystemPanel(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS) },
            "Manage hidden apps" to { manageHiddenApps() },
            "Refresh installed apps" to { when (selected) { "Home" -> showHome(); "Apps", "Entertainment", "Tools" -> showAppsPage(); "Favorites" -> showFavoritesPage(); "Search" -> showSearch(); else -> showSettings() }; toast("App list refreshed") },
            "Choose Home launcher" to { toast("Press the TV remote Home button and select Ziyana TV if your TV offers the choice.") },
            "About Ziyana TV" to {
                val version = try {
                    packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"
                } catch (_: Exception) { "Unknown" }
                AlertDialog.Builder(this).setTitle("Ziyana TV")
                    .setMessage("Premium Android TV launcher\nVersion $version")
                    .setPositiveButton("OK", null).show()
            }
        )
        options.forEach { (title, action) ->
            content.addView(actionButton(title, false, action), LinearLayout.LayoutParams(dp(310), dp(48)).apply { topMargin = dp(12) })
        }
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