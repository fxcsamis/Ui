package com.arkitak.lite.wake

import android.content.Context
import android.os.Build
import com.arkitak.lite.Prefs

/** ENGINE B: Vosk wake word in a microphone foreground service (see [VoskWakeService]). Indicator visible, works on any phone. */
class VoskEngine(private val ctx: Context, private val sink: WakeSink) : WakeEngine {
    override val id = "vosk"
    override val title = "B · Vosk (offline, mic indicator visible)"

    override fun start(): StartResult {
        if (!Diagnostics.hasMic(ctx)) { sink.onStatusChanged(id); return StartResult(false, "Microphone permission is missing") }
        if (!VoskModelStore.isReady(ctx) && !VoskModelStore.inAssets(ctx)) {
            sink.onStatusChanged(id)
            return StartResult(false, "Vosk model missing: ${VoskModelStore.statusText(ctx)}")
        }
        VoskWakeService.lastError = null
        val svc = VoskWakeService.instance
        val r = if (svc != null) { svc.resumeListening(); StartResult(true, "resumed") } else VoskWakeService.start(ctx)
        sink.onStatusChanged(id)
        return r
    }

    override fun stop() {
        VoskWakeService.stop(ctx)
        sink.onStatusChanged(id)
    }

    override fun pauseForSession() { VoskWakeService.instance?.pauseListening() }

    override fun resumeAfterSession() {
        val svc = VoskWakeService.instance
        if (svc != null) svc.resumeListening() else DiagLog.log("VOSK", "service is gone after the conversation; start the engine again from the Wake screen")
    }

    override fun status(): EngineStatus {
        if (!Diagnostics.hasMic(ctx)) return EngineStatus.PermissionMissing("Microphone permission is not granted", "Grant it with the button below")
        if (!VoskModelStore.isReady(ctx) && !VoskModelStore.inAssets(ctx)) {
            return EngineStatus.Failed(
                "Vosk model not found in the APK",
                "Put the contents of vosk-model-small-hi-0.22 in app/src/main/assets/model-hi/ and rebuild (see README)"
            )
        }
        val err = VoskWakeService.lastError
        return when (VoskWakeService.state) {
            VoskWakeService.S_LISTENING -> EngineStatus.Running("listening for Olivia / ARKITAK (mic indicator visible)")
            VoskWakeService.S_LOADING -> EngineStatus.Checking("loading the model (first run copies about 80 MB, can take a while)")
            VoskWakeService.S_PAUSED -> EngineStatus.Running("paused while a conversation uses the microphone")
            VoskWakeService.S_ERROR -> EngineStatus.Failed(err ?: "unknown error", fixFor(err ?: ""))
            else -> EngineStatus.Stopped(if (Build.VERSION.SDK_INT >= 33 && !Diagnostics.hasNotif(ctx)) "notification permission is off, the listening notification is hidden" else "")
        }
    }

    private fun fixFor(e: String): String = when {
        e.contains("blocked") -> "Open the app (Wake screen) and switch it on from there"
        e.contains("another app") || e.contains("microphone") -> "Close other apps that use the microphone, then switch it on again"
        e.contains("model") -> "Check assets/model-hi (see README), rebuild, reinstall"
        else -> "Copy the report and send it"
    }
}
