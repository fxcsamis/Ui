package com.arkitak.lite.wake

import android.content.Context

/**
 * ENGINE C: nothing listens. The microphone is only used when YOU invoke the assistant:
 *  - default-assistant gesture / power button (VoiceInteractionSession.onShow), needs ARKITAK as default assistant
 *  - the ASSIST / VOICE_COMMAND activity (ArkitakAssistActivity)
 *  - the Quick Settings tile (add it from the tile editor)
 *  - the home-screen widget "Talk to ARKITAK"
 * All of them call [WakeEngineManager.onTrigger], which works whichever engine is selected.
 */
class ManualTriggerEngine(private val ctx: Context, private val sink: WakeSink) : WakeEngine {
    override val id = "manual"
    override val title = "C · Manual trigger (mic only when you call)"

    @Volatile private var armed = false

    override fun start(): StartResult {
        armed = true
        sink.onStatusChanged(id)
        return StartResult(true, "armed")
    }

    override fun stop() {
        armed = false
        sink.onStatusChanged(id)
    }

    override fun status(): EngineStatus {
        val assistant = Diagnostics.isDefaultAssistant(ctx)
        val how = "Ways to call: assistant gesture/power button" + (if (assistant) "" else " (needs default assistant)") +
            ", Quick Settings tile, home widget"
        return if (armed) EngineStatus.Running(how) else EngineStatus.Stopped(how)
    }
}
