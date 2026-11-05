package com.arkitak.lite.wake

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.service.voice.VoiceInteractionService
import com.arkitak.lite.Prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Builds the text shown in the diagnostics popup (and copied / shared). Everything here is read from the phone, nothing is guessed. */
object Diagnostics {

    fun device(): String =
        "${Build.MANUFACTURER} ${Build.MODEL} (brand ${Build.BRAND}, device ${Build.DEVICE}); " +
            "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}); security patch ${Build.VERSION.SECURITY_PATCH}"

    fun hasMic(ctx: Context) = ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** Android 13+ needs this to show the foreground-service notification. Below 13 it is granted. */
    fun hasNotif(ctx: Context) = Build.VERSION.SDK_INT < 33 || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ignoringBattery(ctx: Context): Boolean = try {
        (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)
    } catch (_: Throwable) { false }

    fun overlayOk(ctx: Context): Boolean = try { Settings.canDrawOverlays(ctx) } catch (_: Throwable) { false }

    /** VoiceInteractionService.isActiveService: public API since API 21. True only when ARKITAK is the chosen digital assistant. */
    fun isDefaultAssistant(ctx: Context): Boolean = try {
        VoiceInteractionService.isActiveService(ctx, ComponentName(ctx, ArkitakVoiceInteractionService::class.java))
    } catch (_: Throwable) { false }

    /** The system's own value, for display only (the key name is not an SDK constant, so it may be empty on some phones). */
    fun secureSetting(ctx: Context, key: String): String = try {
        Settings.Secure.getString(ctx.contentResolver, key) ?: "(empty)"
    } catch (t: Throwable) { "(not readable: ${t.javaClass.simpleName})" }

    fun report(ctx: Context): String {
        WakeEngineManager.init(ctx)
        val prefs = Prefs(ctx)
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sb = StringBuilder()
        sb.appendLine("ARKITAK wake diagnostics, ${time.format(Date())}")
        sb.appendLine("Device: ${device()}")
        sb.appendLine("OEM: ${Build.MANUFACTURER} (battery and background rules differ a lot between makers)")
        sb.appendLine()
        sb.appendLine("Wake mode: ${if (prefs.wakeEnabled) "ON" else "OFF"}; chosen engine: ${prefs.wakeEngine}; running now: ${WakeEngineManager.describeActive()}")
        sb.appendLine("Conversation silence timeout: ${prefs.silenceSeconds} s")
        WakeEngineManager.lastFailure?.let { sb.appendLine("Last failure: $it") }
        sb.appendLine()
        for (e in WakeEngineManager.engines) {
            val st = e.status()
            sb.appendLine("== ${e.title}")
            sb.appendLine("Status: ${st.code}" + (if (st.detail.isNotEmpty()) " - ${st.detail}" else ""))
            if (st.fix.isNotEmpty()) sb.appendLine("How to fix: ${st.fix}")
            if (e.id == "dsp") sb.appendLine("DSP state: ${WakeEngineManager.dsp.stateReport()}")
            if (e.id == "vosk") {
                sb.appendLine("Model: ${VoskModelStore.statusText(ctx)}")
                sb.appendLine("Service: ${VoskWakeService.state}" + (VoskWakeService.lastError?.let { " ($it)" } ?: ""))
                if (VoskWakeService.lastHeard.isNotEmpty()) sb.appendLine("Last heard: ${VoskWakeService.lastHeard}")
            }
            val last = prefs.lastTest(e.id)?.let { TestResult.decode(e.id, it) }
            sb.appendLine("Last test: ${last?.let { it.line() + " at " + time.format(Date(it.at)) } ?: "never run"}")
            sb.appendLine()
        }
        sb.appendLine("== Permissions and system")
        sb.appendLine("Microphone: ${if (hasMic(ctx)) "granted" else "MISSING"}")
        sb.appendLine("Notifications: ${if (hasNotif(ctx)) "granted" else "MISSING (the listening notification would be hidden)"}")
        sb.appendLine("Default assistant: ${if (isDefaultAssistant(ctx)) "ARKITAK Lite" else "not ARKITAK Lite"}")
        sb.appendLine("  system setting assistant = ${secureSetting(ctx, "assistant")}")
        sb.appendLine("  system setting voice_interaction_service = ${secureSetting(ctx, "voice_interaction_service")}")
        sb.appendLine("Battery optimization: ${if (ignoringBattery(ctx)) "not restricted" else "restricted (may stop the wake engine in the background)"}")
        sb.appendLine("Draw over other apps: ${if (overlayOk(ctx)) "allowed" else "not allowed (the app cannot open itself from the background)"}")
        sb.appendLine()
        sb.appendLine("Mic indicator note (typed by you): ${prefs.micNote.ifBlank { "(empty)" }}")
        sb.appendLine()
        sb.appendLine("== Recent log")
        DiagLog.snapshot(40).forEach { sb.appendLine(it) }
        return sb.toString()
    }
}
