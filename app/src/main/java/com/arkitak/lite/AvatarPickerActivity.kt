package com.arkitak.lite

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Opens after the floating-window permission is allowed. Shows every glass orb design with a live, animated preview
 * (it keeps cycling idle / listening / thinking / speaking). Tap a design, then "Use this orb": it becomes the floating avatar.
 */
class AvatarPickerActivity : Activity() {

    private val REQ_OVERLAY = 21
    private val REQ_MIC = 22

    private lateinit var prefs: Prefs
    private lateinit var orb: GlassOrbView
    private lateinit var nameView: TextView
    private lateinit var aboutView: TextView
    private val chipDots = mutableMapOf<String, View>()
    private val stateButtons = mutableMapOf<String, TextView>()
    private var selected = GlassDesigns.DEFAULT_ID
    private var activeState = "auto"

    private fun px(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        selected = GlassDesigns.byId(prefs.avatarDesign).id

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(px(16), px(32), px(16), px(24))
        }

        col.addView(TextView(this).apply {
            text = "Choose your avatar orb"
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
        })
        col.addView(TextView(this).apply {
            text = "Live preview. This orb becomes your floating avatar."
            setTextColor(Color.GRAY)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, px(4), 0, px(8))
        })

        val screenW = resources.displayMetrics.widthPixels
        val previewSize = minOf(screenW - px(32), px(360))
        orb = GlassOrbView(this, picker = true, design = selected)
        col.addView(orb, LinearLayout.LayoutParams(previewSize, previewSize))

        nameView = TextView(this).apply { setTextColor(Color.WHITE); textSize = 20f; gravity = Gravity.CENTER }
        aboutView = TextView(this).apply { setTextColor(Color.GRAY); textSize = 13f; gravity = Gravity.CENTER }
        col.addView(nameView)
        col.addView(aboutView)

        // design chips
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(px(4), px(16), px(4), px(8)) }
        for (d in GlassDesigns.ALL) {
            val dot = View(this)
            chipDots[d.id] = dot
            val label = TextView(this).apply { text = d.name; setTextColor(Color.LTGRAY); textSize = 11f; gravity = Gravity.CENTER; setPadding(0, px(4), 0, 0) }
            val chip = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(px(8), 0, px(8), 0)
                addView(dot, LinearLayout.LayoutParams(px(52), px(52)))
                addView(label)
                setOnClickListener { select(d.id) }
            }
            chips.addView(chip)
        }
        col.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(chips) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // state buttons (live animation switching)
        val states = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(px(4), px(8), px(4), px(8)) }
        for ((key, label) in listOf("auto" to "Auto", "idle" to "Idle", "listening" to "Listening", "thinking" to "Thinking", "speaking" to "Speaking")) {
            val b = TextView(this).apply {
                text = label
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(px(14), px(8), px(14), px(8))
                setOnClickListener { setPreviewState(key) }
            }
            stateButtons[key] = b
            states.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(px(4), 0, px(4), 0) })
        }
        col.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(states) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        col.addView(Button(this).apply {
            text = "Use this orb as floating avatar"
            setOnClickListener { useOrb() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(16) })

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(ScrollView(this).apply { addView(col) })
        setContentView(root)

        refreshChips()
        refreshStateButtons()
        orb.setAuto(true)
    }

    private fun select(id: String) {
        selected = id
        orb.setDesign(id)
        refreshChips()
    }

    private fun setPreviewState(key: String) {
        activeState = key
        if (key == "auto") orb.setAuto(true) else { orb.setAuto(false); orb.setState(key) }
        refreshStateButtons()
    }

    private fun refreshChips() {
        val d = GlassDesigns.byId(selected)
        nameView.text = d.name
        aboutView.text = d.about
        for (design in GlassDesigns.ALL) {
            val dot = chipDots[design.id] ?: continue
            dot.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(design.colorA, design.colorB)).apply {
                shape = GradientDrawable.OVAL
                setStroke(px(if (design.id == selected) 3 else 1), if (design.id == selected) Color.WHITE else 0x66FFFFFF)
            }
        }
    }

    private fun refreshStateButtons() {
        for ((key, b) in stateButtons) {
            val on = key == activeState
            b.setTextColor(if (on) Color.BLACK else Color.WHITE)
            b.background = GradientDrawable().apply { cornerRadius = px(18).toFloat(); setColor(if (on) Color.WHITE else 0x33FFFFFF) }
        }
    }

    // ---------------------------------------------------------------- use

    private fun useOrb() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_MIC)
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Turn on \"Display over other apps\", then come back", Toast.LENGTH_LONG).show()
            startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), REQ_OVERLAY)
            return
        }
        prefs.avatarDesign = selected
        if (FloatingOrbService.running) FloatingOrbService.refreshDesign() else FloatingOrbService.start(this)
        Toast.makeText(this, "${GlassDesigns.byId(selected).name} orb is your floating avatar", Toast.LENGTH_SHORT).show()
        moveTaskToBack(true) // put the app behind so the floating orb is what you see
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) useOrb()
            else Toast.makeText(this, "Permission not granted, floating avatar is off", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_MIC && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) useOrb()
    }

    override fun onStart() {
        super.onStart()
        FloatingOrbService.activityStarted() // the floating orb hides while this screen is open
    }

    override fun onResume() { super.onResume(); orb.onResume() }
    override fun onPause() { orb.onPause(); super.onPause() }

    override fun onStop() {
        FloatingOrbService.activityStopped()
        super.onStop()
    }

    override fun onDestroy() {
        orb.destroy()
        super.onDestroy()
    }
}
