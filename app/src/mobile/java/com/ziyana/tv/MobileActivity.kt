package com.ziyana.tv

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MobileActivity : Activity() {
    private val champagne = 0xFFE6D1A9.toInt()
    private val muted = 0xFFBEB7AD.toInt()
    private lateinit var page: LinearLayout
    private var selected = "Home"
    private var query = ""
    private val prefs by lazy { getSharedPreferences("ziyana_mobile_prefs", MODE_PRIVATE) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(15, 16, 20)
        window.navigationBarColor = Color.rgb(15, 16, 20)
        window.decorView.systemUiVisibility = 0

        val root = FrameLayout(this)
        root.addView(ImageView(this).apply {
            setImageResource(R.drawable.anime_sakura_wallpaper_hd)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Ziyana wallpaper"
        }, FrameLayout.LayoutParams(-1, -1))
        root.addView(View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xE90A0C12.toInt(), 0xC512141A.toInt(), 0xF20B0D12.toInt()))
        }, FrameLayout.LayoutParams(-1, -1))

        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(10))
        }
        val scroll = ScrollView(this).apply { clipToPadding = false; isFillViewport = false }
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(20))
        }
        scroll.addView(page)
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        shell.addView(bottomNavigation(), LinearLayout.LayoutParams(-1, dp(66)))
        root.addView(shell)
        setContentView(root)
        render()
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun surface(top: Int = 0xC52B2926.toInt(), bottom: Int = 0xE914151A.toInt(), radius: Int = 20): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bottom)).apply {
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), 0x667F7057)
        }

    private fun bottomNavigation(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        background = surface(0xE72B2926.toInt(), 0xF30D0F14.toInt(), 24)
        listOf("⌂" to "Home", "▦" to "Apps", "♡" to "Favorites", "⚙" to "Settings").forEach { (symbol, name) ->
            val item = LinearLayout(this@MobileActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                addView(text(symbol, 21f, if (selected == name) champagne else muted, true).apply { gravity = Gravity.CENTER })
                addView(text(name, 10f, if (selected == name) Color.WHITE else muted).apply { gravity = Gravity.CENTER })
                setOnClickListener { selected = name; render() }
            }
            addView(item, LinearLayout.LayoutParams(0, -1, 1f))
        }
    }

    private fun render() {
        page.removeAllViews()
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("✧  ZIYANA", 13f, champagne, true))
            addView(text(SimpleDateFormat("EEEE, d MMMM  •  h:mm a", Locale.getDefault()).format(Date()), 12f, muted))
            addView(text(when (selected) {
                "Home" -> "Your space, beautifully arranged"
                "Apps" -> "All your applications"
                "Favorites" -> "Your pinned favourites"
                else -> "Personalise your experience"
            }, 23f, Color.WHITE, true).apply { setPadding(0, dp(18), 0, dp(14)) })
        }
        page.addView(header)

        val search = EditText(this).apply {
            hint = "Search apps"
            setHintTextColor(muted)
            setTextColor(Color.WHITE)
            textSize = 14f
            singleLine = true
            setPadding(dp(16), 0, dp(16), 0)
            background = surface(0xB82A2A28.toInt(), 0xD817181D.toInt(), 16)
            setText(query)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    query = s?.toString().orEmpty()
                    renderGrid()
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
        page.addView(search, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(18) })

        if (selected == "Settings") {
            listOf("Appearance" to "Premium charcoal and champagne theme",
                "Favorites" to "Long-press an app card to add or remove",
                "About" to "Ziyana Mobile • Independent mobile edition").forEach { (title, detail) ->
                page.addView(LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = surface()
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                    addView(text(title, 15f, Color.WHITE, true))
                    addView(text(detail, 12f, muted))
                }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
            }
        } else {
            page.addView(text(if (selected == "Home") "Program" else selected, 19f, Color.WHITE, true).apply {
                setPadding(0, 0, 0, dp(12))
            })
            renderGrid()
        }
    }

    private fun renderGrid() {
        page.findViewWithTag<GridLayout>("app_grid")?.let { page.removeView(it) }
        page.findViewWithTag<TextView>("empty_apps")?.let { page.removeView(it) }
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(launchIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .filter { it.loadLabel(packageManager).toString().contains(query, ignoreCase = true) }
            .filter {
                selected != "Favorites" ||
                    prefs.getStringSet("favorites", emptySet())?.contains(it.activityInfo.packageName) == true
            }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }

        val grid = GridLayout(this).apply {
            tag = "app_grid"
            columnCount = 2
            useDefaultMargins = false
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }
        val cardWidth = (resources.displayMetrics.widthPixels - dp(36) - dp(10)) / 2
        apps.forEach { info ->
            val pkg = info.activityInfo.packageName
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = surface()
                setPadding(dp(10), dp(14), dp(10), dp(12))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
                        ?: toast("Unable to open app")
                }
                setOnLongClickListener {
                    val saved = prefs.getStringSet("favorites", emptySet())?.toMutableSet() ?: mutableSetOf()
                    val removed = saved.remove(pkg)
                    if (!removed) saved.add(pkg)
                    prefs.edit().putStringSet("favorites", saved).apply()
                    toast(if (removed) "Removed from Favorites" else "Added to Favorites")
                    if (selected == "Favorites") renderGrid()
                    true
                }
                addView(ImageView(this@MobileActivity).apply {
                    setImageDrawable(info.loadIcon(packageManager))
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }, LinearLayout.LayoutParams(dp(48), dp(48)))
                addView(text(info.loadLabel(packageManager).toString(), 12f, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER
                    maxLines = 2
                    setPadding(0, dp(8), 0, 0)
                })
            }
            grid.addView(card, GridLayout.LayoutParams().apply {
                width = cardWidth
                height = dp(112)
                setMargins(dp(3), dp(3), dp(3), dp(7))
            })
        }
        if (apps.isEmpty()) {
            page.addView(text(if (selected == "Favorites") "Long-press an app to add it to Favorites." else "No matching apps found.", 13f, muted).apply {
                tag = "empty_apps"
            })
        }
        page.addView(grid)
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
