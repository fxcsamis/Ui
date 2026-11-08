package com.arkitak.lite.wake

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.arkitak.lite.Personas
import com.arkitak.lite.Prefs
import java.io.File

/**
 * The one Wake screen: two engine cards (Vosk, Manual), the system rows, the Vosk settings, and the
 * "Test & diagnostics" popup. If something does not work, the reason and the fix are written here, and the report can be copied.
 */
class WakeSettingsActivity : Activity(), WakeEngineManager.Listener {

    private val REQ_MIC = 51
    private val REQ_NOTIF = 52

    private lateinit var prefs: Prefs
    private val dp by lazy { resources.displayMetrics.density }
    private fun px(v: Int) = (v * dp).toInt()

    private class Card(val radio: RadioButton, val status: TextView, val detail: TextView, val last: TextView, val extra: LinearLayout)

    private val cards = LinkedHashMap<String, Card>()
    private lateinit var activeLine: TextView
    private lateinit var failureLine: TextView
    private lateinit var wakeSwitch: Switch
    private lateinit var rows: LinearLayout
    private lateinit var voskBox: LinearLayout
    private var refreshing = false
    private val testNotes = HashMap<String, String>()
    private var diagDialog: AlertDialog? = null
    private var diagReport: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        WakeEngineManager.init(applicationContext)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(16), px(32), px(16), px(40))
        }
        col.addView(text("Wake word", 24f, Color.WHITE))
        col.addView(text("Say \"Hey Olivia\", \"Hey ARKITAK\" or just the name. Pick ONE engine, test it, and keep the one that works on your phone.", 13f, Color.GRAY).pad(0, 4, 0, 8))

        wakeSwitch = Switch(this).apply {
            text = "Wake mode (the conversation sleeps after silence; the engine listens for the name)"
            setTextColor(Color.WHITE)
            setOnCheckedChangeListener { _, on ->
                if (refreshing) return@setOnCheckedChangeListener
                if (on && !Diagnostics.hasMic(this@WakeSettingsActivity)) {
                    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_MIC)
                    refresh()
                    return@setOnCheckedChangeListener
                }
                WakeEngineManager.setEnabled(on)
                refresh()
            }
        }
        col.addView(wakeSwitch)
        activeLine = text("", 14f, 0xFF9BE7FF.toInt()).pad(0, 8, 0, 0)
        failureLine = text("", 13f, 0xFFFF8A80.toInt()).pad(0, 4, 0, 8)
        col.addView(activeLine)
        col.addView(failureLine)

        addCard(col, WakeEngineManager.vosk, "Says the name to wake up. Works on any phone, uses the microphone (the indicator shows).")
        addCard(col, WakeEngineManager.manual, "Nothing listens. Call it: assistant gesture / power button, Quick Settings tile, home widget.")

        col.addView(text("System checks", 16f, Color.WHITE).pad(0, 20, 0, 4))
        rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(rows)

        col.addView(text("Conversation silence timeout", 13f, 0xFF9BA6D8.toInt()).pad(0, 12, 0, 2))
        col.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@WakeSettingsActivity, android.R.layout.simple_spinner_dropdown_item, listOf("5 seconds", "8 seconds (default)", "15 seconds"))
            setSelection(when (prefs.silenceSeconds) { 5 -> 0; 15 -> 2; else -> 1 })
            onItemSelectedListener = picked { prefs.silenceSeconds = listOf(5, 8, 15)[it] }
        })

        voskBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        voskBox.addView(text("Vosk settings", 16f, Color.WHITE).pad(0, 16, 0, 4))
        voskBox.addView(text("Sensitivity (noise gate on the loudness)", 13f, 0xFF9BA6D8.toInt()))
        voskBox.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@WakeSettingsActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Kam (RMS 600)", "Medium (RMS 300)", "Zyada (RMS 120)"))
            setSelection(prefs.voskSensitivity)
            onItemSelectedListener = picked { prefs.voskSensitivity = it }
        })
        voskBox.addView(Switch(this).apply {
            text = "Activation sound"
            setTextColor(Color.WHITE)
            isChecked = prefs.voskSound
            setOnCheckedChangeListener { _, on -> prefs.voskSound = on }
        })
        col.addView(voskBox)

        col.addView(text("Android and the phone maker limit what can listen on the lock screen and in the background. No hidden APIs, no Accessibility tricks, no device-admin tricks are used.", 12f, Color.GRAY).pad(0, 16, 0, 8))
        col.addView(Button(this).apply {
            text = "Test & diagnostics popup"
            setOnClickListener { showDiagnostics() }
        })

        setContentView(ScrollView(this).apply { setBackgroundColor(Color.BLACK); addView(col) })

        if (CrashReporter.list(this).isNotEmpty()) window.decorView.post { showDiagnostics() } // a crash happened last time: show it
    }

    override fun onResume() {
        super.onResume()
        WakeEngineManager.addListener(this)
        refresh() // also after coming back from a system Settings screen
    }

    override fun onPause() {
        WakeEngineManager.removeListener(this)
        super.onPause()
    }

    override fun onWakeStateChanged() { refresh() }

    // ------------------------------------------------------------------ building blocks

    private fun text(t: String, size: Float, color: Int) = TextView(this).apply { text = t; textSize = size; setTextColor(color) }

    private fun TextView.pad(l: Int, t: Int, r: Int, b: Int): TextView { setPadding(px(l), px(t), px(r), px(b)); return this }

    private fun rounded(color: Int) = GradientDrawable().apply { setColor(color); cornerRadius = px(14).toFloat() }

    private fun picked(onPick: (Int) -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { if (!refreshing) onPick(position) }
        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }

    private fun addCard(parent: LinearLayout, e: WakeEngine, blurb: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(0x22FFFFFF)
            setPadding(px(14), px(10), px(14), px(12))
        }
        val radio = RadioButton(this).apply {
            text = e.title
            setTextColor(Color.WHITE)
            textSize = 16f
            setOnClickListener { WakeEngineManager.select(e.id); refresh() }
        }
        val status = text("", 14f, Color.WHITE).pad(0, 4, 0, 0)
        val detail = text("", 12f, 0xFFCCCCCC.toInt())
        val last = text("", 12f, 0xFF9BE7FF.toInt()).pad(0, 4, 0, 0)
        val extra = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(radio)
        box.addView(text(blurb, 12f, Color.GRAY))
        box.addView(status)
        box.addView(detail)
        box.addView(last)
        box.addView(extra)
        box.addView(Button(this).apply {
            text = "Test this engine"
            setOnClickListener { runSingleTest(e.id) }
        })
        parent.addView(box, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(12) })
        cards[e.id] = Card(radio, status, detail, last, extra)
    }

    private fun addRow(label: String, value: String, ok: Boolean, buttonText: String? = null, onClick: (() -> Unit)? = null) {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, px(6), 0, px(6)) }
        val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        left.addView(text(label, 14f, Color.WHITE))
        left.addView(text(value, 12f, if (ok) 0xFF4CD964.toInt() else 0xFFFF8A80.toInt()))
        r.addView(left, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (buttonText != null && onClick != null) r.addView(Button(this).apply { text = buttonText; setOnClickListener { onClick() } })
        rows.addView(r)
    }

    private fun openSettings(action: String, withPackage: Boolean = false) {
        try {
            val i = Intent(action)
            if (withPackage) i.data = Uri.parse("package:$packageName")
            startActivity(i)
        } catch (t: Throwable) {
            DiagLog.log("UI", "could not open $action", t)
            Toast.makeText(this, "This phone has no screen for that setting.", Toast.LENGTH_LONG).show()
        }
    }

    // ------------------------------------------------------------------ refresh

    private fun statusColor(s: EngineStatus): Int = when {
        s is EngineStatus.Running -> 0xFF4CD964.toInt()
        s is EngineStatus.Checking -> 0xFFFFD54F.toInt()
        s.isBlocking -> 0xFFFF8A80.toInt()
        else -> 0xFFCCCCCC.toInt()
    }

    private fun refresh() {
        refreshing = true
        try {
            val sel = WakeEngineManager.selectedId
            wakeSwitch.isChecked = WakeEngineManager.isEnabled
            activeLine.text = "Keep active: " + WakeEngineManager.describeActive() +
                (if (WakeEngineManager.isEnabled) "   (wake mode ON, chosen: $sel)" else "   (wake mode OFF)")
            val f = WakeEngineManager.lastFailure
            failureLine.text = if (f != null) "Last problem: $f" else ""
            failureLine.visibility = if (f != null) View.VISIBLE else View.GONE

            for ((id, c) in cards) {
                val e = WakeEngineManager.engine(id) ?: continue
                val st = e.status()
                c.radio.isChecked = id == sel
                c.status.text = "Status: ${st.code}"
                c.status.setTextColor(statusColor(st))
                c.detail.text = st.detail + (if (st.fix.isNotEmpty()) "\nFix: ${st.fix}" else "")
                c.detail.visibility = if (c.detail.text.isEmpty()) View.GONE else View.VISIBLE
                val testing = WakeEngineManager.testingEngine == id
                val last = prefs.lastTest(id)?.let { TestResult.decode(id, it) }
                c.last.text = when {
                    testing -> "TESTING... " + (testNotes[id] ?: "")
                    last != null -> "Last test: " + last.line()
                    else -> "Last test: never run"
                }
            }
            voskBox.visibility = if (sel == "vosk") View.VISIBLE else View.GONE

            rows.removeAllViews()
            val mic = Diagnostics.hasMic(this)
            addRow("Microphone permission", if (mic) "granted" else "MISSING", mic, "Grant") { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_MIC) }
            if (Build.VERSION.SDK_INT >= 33) {
                val n = Diagnostics.hasNotif(this)
                addRow("Notification permission", if (n) "granted" else "MISSING (the listening notification is hidden)", n, "Grant") {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
                }
            }
            val da = Diagnostics.isDefaultAssistant(this)
            addRow("Default assistant", if (da) "ARKITAK Lite" else "not ARKITAK Lite (needed for the swipe-up / power-button gesture)", da, "Open") { openSettings(Settings.ACTION_VOICE_INPUT_SETTINGS) }
            val bat = Diagnostics.ignoringBattery(this)
            addRow("Battery optimization", if (bat) "not restricted" else "restricted: may stop listening in the background", bat, "Open") { openSettings(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) }
            val ov = Diagnostics.overlayOk(this)
            addRow("Draw over other apps (floating orb; lets a wake word open the app)", if (ov) "allowed" else "not allowed", ov, "Open") { openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, true) }
            val vm = VoskModelStore.isReady(this) || VoskModelStore.inAssets(this)
            addRow("Vosk model", VoskModelStore.statusText(this), vm)
        } finally {
            refreshing = false
        }
    }

    // ------------------------------------------------------------------ tests

    private fun hintFor(id: String) = when (id) {
        "vosk" -> "say \"Hey Olivia\" or \"Hey ARKITAK\" now (the first run loads the model first)"
        else -> "trigger it now: Quick Settings tile, widget or assistant gesture"
    }

    private fun runSingleTest(id: String) {
        testNotes[id] = hintFor(id)
        WakeEngineManager.runTest(id) { r ->
            testNotes.remove(id)
            Toast.makeText(this, "${id.uppercase()}: ${r.outcome}", Toast.LENGTH_LONG).show()
            refresh()
            diagReport?.text = Diagnostics.report(this)
        }
        refresh()
    }

    // ------------------------------------------------------------------ the popup

    private fun clip(label: String, text: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun share(subject: String, text: String) {
        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, text)
        try { startActivity(Intent.createChooser(i, subject)) } catch (t: Throwable) { Toast.makeText(this, "Nothing can share text here", Toast.LENGTH_SHORT).show() }
    }

    private fun showDiagnostics() {
        if (diagDialog?.isShowing == true) return
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(px(16), px(8), px(16), px(8)) }
        val report = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 11f
            setTextColor(Color.WHITE)
            setTextIsSelectable(true)
            text = Diagnostics.report(this@WakeSettingsActivity)
        }
        diagReport = report
        val progress = text("", 13f, 0xFF9BE7FF.toInt())
        val note = EditText(this).apply {
            hint = "Mic indicator: did you see the green dot / privacy indicator? With which engine?"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 13f
            setText(prefs.micNote)
        }
        fun refreshReport() { prefs.micNote = note.text.toString(); report.text = Diagnostics.report(this) }

        val runAll = Button(this).apply { text = "Run all tests" }
        runAll.setOnClickListener {
            runAll.isEnabled = false
            val ids = listOf("vosk", "manual")
            fun next(i: Int) {
                if (i >= ids.size) { progress.text = "All tests finished."; runAll.isEnabled = true; refreshReport(); refresh(); return }
                progress.text = "Testing ${ids[i].uppercase()}: ${hintFor(ids[i])}"
                testNotes[ids[i]] = hintFor(ids[i])
                WakeEngineManager.runTest(ids[i]) { r ->
                    testNotes.remove(ids[i])
                    progress.text = "${ids[i].uppercase()}: ${r.line()}"
                    refreshReport(); refresh()
                    next(i + 1)
                }
                refresh()
            }
            next(0)
        }
        val copy = Button(this).apply { text = "Copy report"; setOnClickListener { refreshReport(); clip("ARKITAK wake report", report.text.toString()) } }
        val shareBtn = Button(this).apply { text = "Share report"; setOnClickListener { refreshReport(); share("ARKITAK wake report", report.text.toString()) } }
        val close = Button(this).apply { text = "Close"; setOnClickListener { diagDialog?.dismiss() } }

        col.addView(report)
        col.addView(text("Your note", 13f, 0xFF9BA6D8.toInt()).pad(0, 10, 0, 0))
        col.addView(note)
        col.addView(progress)
        val r1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        r1.addView(runAll, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        r1.addView(copy, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val r2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        r2.addView(shareBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        r2.addView(close, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        col.addView(r1)
        col.addView(r2)

        val crashBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(crashBox)
        fillCrashBox(crashBox)

        diagDialog = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Test & diagnostics")
            .setView(ScrollView(this).apply { addView(col) })
            .create()
        diagDialog?.setOnDismissListener { prefs.micNote = note.text.toString(); diagReport = null }
        diagDialog?.show()
    }

    private fun fillCrashBox(box: LinearLayout) {
        box.removeAllViews()
        val files: List<File> = CrashReporter.list(this)
        if (files.isEmpty()) return
        box.addView(text("Crash reports (stay on this phone)", 14f, 0xFFFF8A80.toInt()).pad(0, 14, 0, 4))
        for (f in files) {
            val body = CrashReporter.read(f)
            val headline = body.lines().firstOrNull { it.contains("Exception") || it.contains("Error") } ?: "(no exception line)"
            box.addView(text("${f.name}\n$headline", 12f, Color.WHITE))
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(Button(this).apply { text = "Copy"; setOnClickListener { clip("ARKITAK crash", body) } })
            row.addView(Button(this).apply { text = "Share"; setOnClickListener { share("ARKITAK crash report", body) } })
            row.addView(Button(this).apply { text = "Delete"; setOnClickListener { CrashReporter.delete(f); fillCrashBox(box) } })
            box.addView(row)
        }
    }

    // ------------------------------------------------------------------ results

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        DiagLog.log("UI", "activity result $requestCode -> $resultCode")
        refresh()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refresh()
    }
}
