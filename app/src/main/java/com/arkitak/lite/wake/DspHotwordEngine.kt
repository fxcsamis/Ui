package com.arkitak.lite.wake

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.service.voice.VoiceInteractionService
import com.arkitak.lite.Personas
import com.arkitak.lite.Prefs
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.util.Locale

/**
 * ENGINE A: hotword detection by the phone's own DSP / system sound-trigger, through the voice interaction framework.
 * The audio is fed by the system, so there is no microphone indicator when the platform supports this path.
 *
 * HONEST SUMMARY OF WHAT ANDROID ALLOWS (read this before judging the result):
 *  - AlwaysOnHotwordDetector works only while this app is the DEFAULT DIGITAL ASSISTANT (VoiceInteractionService.isActiveService).
 *  - It listens for ONE keyphrase at a time (creating a second detector invalidates the first), so DSP can wake on "Hey Olivia"
 *    OR "Hey ARKITAK", not both. Choose which one on the Wake screen.
 *  - A custom keyphrase needs a keyphrase sound model enrolled with the system. Normally only the phone maker / Google can
 *    provide one, so third-party apps usually get STATE_KEYPHRASE_UNSUPPORTED. That is a platform limit, not a bug here.
 *  - startRecognition() needs RECORD_AUDIO plus CAPTURE_AUDIO_HOTWORD (a signature|privileged permission) according to the AOSP
 *    sources; a normal app cannot hold it. This could not be tested on a real phone by the author: the Wake screen will show the
 *    exact result on your phone.
 *  - HotwordDetectionService (system-fed audio with an app-supplied detector) is NOT used: it needs privileged permissions and an
 *    OEM / privileged install, so it cannot be used by a normal app. Not implemented on purpose; nothing is faked.
 *
 * IMPLEMENTATION NOTE: the callback is a real subclass (DspCallback.kt: on this phone it is an abstract class). The detector itself
 * is created and started by reflection over public SDK members, so a signature that differs between Android versions cannot break
 * the build; if Android hides a member from apps, the lookup fails and the reason is shown in the popup.
 */
class DspHotwordEngine(private val ctx: Context, private val sink: WakeSink) : WakeEngine {

    override val id = "dsp"
    override val title = "A · DSP hotword (system-fed audio)"

    companion object {
        private const val TAG = "DSP"
        // Min API 24, public class. Callback is an interface on the API levels this reflection path supports (Android 13+).
        private const val AOHD = "android.service.voice.AlwaysOnHotwordDetector"
    }

    private val prefs = Prefs(ctx)

    @Volatile private var detector: Any? = null
    @Volatile private var callbackRef: Any? = null
    @Volatile private var availabilityCode: Int? = null
    @Volatile private var stateNames: Map<Int, String> = emptyMap()
    @Volatile private var running = false
    @Volatile private var recognizing = false
    @Volatile private var lastError: String? = null

    fun keyphrase(): String = if (prefs.dspPersona == Personas.ARKITAK) "Hey ARKITAK" else "Hey Olivia"

    private fun hasMic() = ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** VoiceInteractionService.isActiveService: public static, min API 21, no special permission. */
    private fun isDefaultAssistant(): Boolean = try {
        VoiceInteractionService.isActiveService(ctx, ComponentName(ctx, ArkitakVoiceInteractionService::class.java))
    } catch (t: Throwable) { false }

    // ------------------------------------------------------------------ system callbacks

    /** Called by [ArkitakVoiceInteractionService.onReady] (min API 21): only happens when ARKITAK is the default assistant. */
    fun onServiceReady(svc: VoiceInteractionService) {
        DiagLog.safe("dsp onServiceReady") {
            DiagLog.log(TAG, "VoiceInteractionService.onReady: ARKITAK is the active assistant service")
            loadStateNames()
            // Query availability at start-up (also logs the exact state). Recognition itself starts only if this engine is ON.
            if (hasMic()) createDetector(svc)
            sink.onStatusChanged(id)
        }
    }

    fun onServiceShutdown() {
        DiagLog.safe("dsp onServiceShutdown") {
            DiagLog.log(TAG, "VoiceInteractionService.onShutdown")
            detector = null
            callbackRef = null
            availabilityCode = null
            recognizing = false
            sink.onStatusChanged(id)
        }
    }

    // ------------------------------------------------------------------ engine API

    override fun start(): StartResult {
        lastError = null
        if (!hasMic()) {
            sink.onStatusChanged(id)
            return StartResult(false, "Microphone permission is missing")
        }
        if (!isDefaultAssistant()) {
            sink.onStatusChanged(id)
            return StartResult(false, "ARKITAK Lite is not the default digital assistant")
        }
        val svc = ArkitakVoiceInteractionService.instance
        if (svc == null) {
            lastError = "The assistant service is not running yet (onReady was not called). Toggle the default assistant off and on, or reboot."
            sink.onStatusChanged(id)
            return StartResult(false, lastError!!)
        }
        running = true
        recognizing = false
        // A new detector invalidates the old one, so a keyphrase change takes effect here.
        createDetector(svc)
        // If the availability was already reported, act on it now; otherwise onAvailabilityChanged will.
        if (availabilityCode != null) actOnAvailability()
        sink.onStatusChanged(id)
        return StartResult(true, "checking availability")
    }

    override fun stop() {
        running = false
        DiagLog.safe("dsp stop") {
            val d = detector
            if (d != null && recognizing) {
                // min API 24; needs the same privileged access as startRecognition.
                invoke(d, "stopRecognition")
            }
            recognizing = false
        }
        sink.onStatusChanged(id)
    }

    override fun pauseForSession() { /* system-fed audio: the app's own AudioRecord does not conflict, nothing to release */ }

    override fun resumeAfterSession() {
        // A detection ends one-shot recognition, so arm it again.
        if (running && availabilityCode != null) DiagLog.safe("dsp resume") { actOnAvailability() }
    }

    override fun status(): EngineStatus {
        if (!hasMic()) return EngineStatus.PermissionMissing("Microphone permission is not granted", "Grant it with the button below")
        if (!isDefaultAssistant()) {
            return EngineStatus.Unsupported(
                "ARKITAK Lite is not the default digital assistant. Android binds the voice interaction service (and allows AlwaysOnHotwordDetector) only for the default assistant.",
                "Settings > Default digital assistant app > ARKITAK Lite (button below)"
            )
        }
        lastError?.let { return EngineStatus.Failed(it, fixFor(it)) }
        val code = availabilityCode
        val name = nameOf(code)
        if (recognizing) return EngineStatus.Running("listening for '${keyphrase()}' on the DSP ($name)")
        if (code == null) {
            return when {
                running -> EngineStatus.Checking("waiting for Android to report keyphrase availability")
                ArkitakVoiceInteractionService.instance == null ->
                    EngineStatus.Failed("The assistant service has not started yet", "Toggle the default assistant off and on, or reboot the phone")
                else -> EngineStatus.Stopped("no availability reported yet")
            }
        }
        return when {
            name.contains("HARDWARE_UNAVAILABLE") -> EngineStatus.HardwareUnavailable(
                "$name (code $code): this phone has no usable always-on listening hardware for apps.",
                "Use engine B (Vosk)"
            )
            name.contains("UNSUPPORTED") -> EngineStatus.Unsupported(
                "$name (code $code): this phone cannot listen for the custom phrase '${keyphrase()}' on the DSP. Normally only the maker's own phrase can be enrolled.",
                "Use engine B (Vosk) as the working wake word"
            )
            name.contains("UNENROLLED") -> EngineStatus.Unenrolled(
                "$name (code $code): '${keyphrase()}' is supported but not enrolled yet.",
                "Press Enroll below (needs a system enrollment screen; if none exists the phone does not offer enrollment)"
            )
            name.contains("ENROLLED") ->
                if (running) EngineStatus.Checking("$name (code $code): starting recognition")
                else EngineStatus.Supported("$name (code $code): ready, switch this engine on")
            name.contains("ERROR") -> EngineStatus.Failed("$name (code $code)", "Toggle the default assistant, or reboot")
            else -> EngineStatus.Failed("unknown availability value $code ($name)", "Send the report so it can be mapped")
        }
    }

    /** Exact state for the diagnostics popup. */
    fun stateReport(): String {
        val code = availabilityCode
        return "keyphrase='${keyphrase()}' locale=en-US; availability=${nameOf(code)}" + (if (code != null) " (code $code)" else "") +
            "; defaultAssistant=${isDefaultAssistant()}; serviceBound=${ArkitakVoiceInteractionService.instance != null}; " +
            "detectorCreated=${detector != null}; recognizing=$recognizing" + (lastError?.let { "; error=$it" } ?: "")
    }

    /**
     * Enrollment (only if Android reports UNENROLLED). AlwaysOnHotwordDetector.createEnrollIntent(), min API 24, same access rules.
     * Returns null if the platform gives no enrollment screen.
     */
    fun enrollIntent(): Intent? = try {
        val d = detector
        if (d == null) null else d.javaClass.getMethod("createEnrollIntent").invoke(d) as? Intent
    } catch (t: Throwable) {
        DiagLog.log(TAG, "no enrollment intent", t)
        null
    }

    // ------------------------------------------------------------------ detector (reflection over public SDK members)

    private fun loadStateNames() {
        try {
            val map = HashMap<Int, String>()
            for (f in Class.forName(AOHD).fields) {
                if (Modifier.isStatic(f.modifiers) && f.type == Int::class.java && f.name.startsWith("STATE_")) map[f.getInt(null)] = f.name
            }
            stateNames = map
            DiagLog.log(TAG, "platform state constants: $map")
        } catch (t: Throwable) {
            DiagLog.log(TAG, "could not read the STATE_ constants", t)
        }
    }

    private fun nameOf(code: Int?): String = when {
        code == null -> "none yet"
        else -> stateNames[code] ?: "unknown code $code"
    }

    private fun createDetector(svc: VoiceInteractionService) {
        recognizing = false
        detector = null
        callbackRef = null
        availabilityCode = null
        if (stateNames.isEmpty()) loadStateNames()
        val cb: Any = try {
            DspCallback.create(this)
        } catch (t: Throwable) {
            lastError = t.message ?: "could not create the hotword callback"
            DiagLog.log(TAG, lastError!!, t)
            return
        }
        // VoiceInteractionService.createAlwaysOnHotwordDetector(String, Locale, Callback): min API 24, only for the active assistant service.
        val m = svc.javaClass.methods.firstOrNull {
            it.name == "createAlwaysOnHotwordDetector" && it.parameterTypes.size == 3 &&
                it.parameterTypes[0] == String::class.java && it.parameterTypes[1] == Locale::class.java
        }
        if (m == null) {
            lastError = "createAlwaysOnHotwordDetector(String, Locale, Callback) is not exposed to apps on this Android version"
            DiagLog.log(TAG, lastError!!)
            return
        }
        try {
            callbackRef = cb
            detector = m.invoke(svc, keyphrase(), Locale.US, cb)
            DiagLog.log(TAG, "detector created for '${keyphrase()}' (en-US); waiting for onAvailabilityChanged")
        } catch (e: InvocationTargetException) {
            val c = e.targetException
            lastError = "createAlwaysOnHotwordDetector threw ${c.javaClass.simpleName}: ${c.message}"
            DiagLog.log(TAG, lastError!!)
        } catch (t: Throwable) {
            lastError = "createAlwaysOnHotwordDetector failed: ${t.javaClass.simpleName}: ${t.message}"
            DiagLog.log(TAG, lastError!!, t)
        }
    }

    // ---- called by DspCallback (system callbacks) ----

    fun cbAvailability(code: Int) {
        availabilityCode = code
        DiagLog.log(TAG, "onAvailabilityChanged: ${nameOf(code)} (code=$code)")
        if (running) actOnAvailability()
        sink.onStatusChanged(id)
    }

    fun cbDetected() {
        recognizing = false
        DiagLog.log(TAG, "onDetected: the system heard '${keyphrase()}'")
        sink.onWake(id, prefs.dspPersona, "keyphrase '${keyphrase()}' (system hotword detection)")
        sink.onStatusChanged(id)
    }

    fun cbError(what: String) {
        lastError = "$what reported by the system"
        recognizing = false
        DiagLog.log(TAG, lastError!!)
        sink.onStatusChanged(id)
    }

    fun cbInfo(what: String) { DiagLog.log(TAG, "callback $what") }

    /** ENROLLED and the engine is ON: start recognition. Other states are reported by [status]. */
    private fun actOnAvailability() {
        val name = nameOf(availabilityCode)
        if (name.contains("UNENROLLED") || !name.contains("ENROLLED")) return
        val d = detector ?: return
        try {
            // startRecognition(int flags): min API 24. AOSP requires RECORD_AUDIO + CAPTURE_AUDIO_HOTWORD (signature|privileged).
            val ok = d.javaClass.getMethod("startRecognition", Int::class.java).invoke(d, 0) as? Boolean ?: false
            if (ok) {
                recognizing = true
                lastError = null
                DiagLog.log(TAG, "startRecognition OK")
            } else {
                lastError = "startRecognition returned false"
                DiagLog.log(TAG, lastError!!)
            }
        } catch (e: InvocationTargetException) {
            val c = e.targetException
            lastError = "startRecognition threw ${c.javaClass.simpleName}: ${c.message}" +
                (if (c is SecurityException) ". Android requires CAPTURE_AUDIO_HOTWORD, a privileged permission that normal apps cannot get." else "")
            DiagLog.log(TAG, lastError!!)
        } catch (t: Throwable) {
            lastError = "startRecognition failed: ${t.javaClass.simpleName}: ${t.message}"
            DiagLog.log(TAG, lastError!!, t)
        }
    }

    private fun invoke(target: Any, name: String) {
        try { target.javaClass.getMethod(name).invoke(target) } catch (t: Throwable) { DiagLog.log(TAG, "$name failed", t) }
    }

    private fun fixFor(error: String): String = when {
        error.contains("CAPTURE_AUDIO_HOTWORD") || error.contains("SecurityException") ->
            "This is a platform limit for normal apps. Use engine B (Vosk)."
        error.contains("not running") || error.contains("not bound") -> "Toggle the default assistant off and on, or reboot the phone"
        error.contains("abstract class") || error.contains("hidden API") -> "This Android version hides the API from apps. Use engine B (Vosk)."
        error.contains("not exposed") -> "This Android version hides the API from apps. Use engine B (Vosk)."
        else -> "Copy the report and send it, or use engine B (Vosk)"
    }
}
