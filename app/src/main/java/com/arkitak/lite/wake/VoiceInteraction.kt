package com.arkitak.lite.wake

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.arkitak.lite.AssistantEngine
import com.arkitak.lite.DuoOrbView
import com.arkitak.lite.OrbView
import com.arkitak.lite.ParticleOrbHandle
import com.arkitak.lite.Personas

/**
 * The voice interaction service. Android binds it only when ARKITAK Lite is chosen as the default digital assistant
 * (Settings > Apps > Default apps > Digital assistant app). It exists only so that the swipe-up-from-the-corner gesture and the
 * power-button long press open ARKITAK (see [ArkitakVoiceInteractionSession]). min API 21.
 */
class ArkitakVoiceInteractionService : VoiceInteractionService() {

    companion object {
        @Volatile var instance: ArkitakVoiceInteractionService? = null
    }

    override fun onReady() {
        super.onReady()
        instance = this
        DiagLog.safe("VIS onReady") {
            WakeEngineManager.init(applicationContext)
            DiagLog.log("VIS", "ARKITAK is the active assistant service")
        }
    }

    override fun onShutdown() {
        instance = null
        super.onShutdown()
    }
}

class ArkitakVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = ArkitakVoiceInteractionSession(this)
}

/**
 * The screen of the default-assistant gesture / power button: a black screen with the SAME orb as the main screen (same
 * listening / thinking / speaking animations, driven by the same engine). When Olivia and ARKITAK talk to each other, the orb
 * splits in two with a line between them, exactly like in the app. Tap the orb or press back to close.
 * onShow starts the conversation; onHide stops it only if THIS session started it ([owns]).
 */
class ArkitakVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    private var owns = false
    private var root: FrameLayout? = null
    private var orb: OrbView? = null
    private var duo: DuoOrbView? = null
    private var listening = false

    private val engine: AssistantEngine get() = AssistantEngine.get(context.applicationContext)

    private val listener = object : AssistantEngine.Listener {
        override fun onState(state: String) { orb?.setState(state); duo?.setState(state) }
        override fun onLevel(level: Float) { orb?.pushAudio(level); duo?.pushAudio(level) }
        override fun onPersona(persona: String) { root?.post { syncDuo(); duo?.setActive(persona) } }
    }

    override fun onCreateContentView(): View {
        val frame = FrameLayout(context).apply { setBackgroundColor(Color.BLACK) }
        val o = OrbView(context, fullField = true)
        o.setTheme(OrbView.THEME_DARK)
        o.onOrbTap = { hide() }
        frame.addView(o, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        val hint = TextView(context).apply {
            text = "tap the orb to close"
            setTextColor(0x66FFFFFF)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 72)
        }
        frame.addView(hint, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        root = frame
        orb = o
        return frame
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        DiagLog.safe("session onShow") {
            WakeEngineManager.init(context.applicationContext)
            orb?.onResume()
            // This screen is already the UI: do not also open the main app behind it.
            owns = WakeEngineManager.onTrigger("assistant gesture / power button", openUi = false)
            DiagLog.log("SESSION", "onShow (flags=$showFlags) ownsConversation=$owns")
            if (!listening) { listening = true; engine.addListener(listener) }
            syncDuo()
        }
    }

    override fun onHide() {
        DiagLog.safe("session onHide") {
            if (listening) { listening = false; engine.removeListener(listener) }
            orb?.onPause()
            duo?.onPause()
            if (owns) {
                DiagLog.log("SESSION", "onHide: stopping the conversation this session started")
                WakeEngineManager.stopConversationFromTrigger()
            }
            owns = false
        }
        super.onHide()
    }

    override fun onDestroy() {
        DiagLog.safe("session onDestroy") {
            if (listening) { listening = false; engine.removeListener(listener) }
            duo?.destroy(); duo = null
            orb?.destroy(); orb = null
        }
        super.onDestroy()
    }

    /** Group chat on: the orb splits in two. Group chat off: they flow back together. */
    private fun syncDuo() {
        val frame = root ?: return
        val main = orb ?: return
        val want = engine.groupMode
        val d = duo
        if (want && d == null) {
            val nd = DuoOrbView(context) { p ->
                ParticleOrbHandle(context, if (p == Personas.ARKITAK) OrbView.THEME_DARK else OrbView.THEME_RED)
            }
            nd.onTap = { hide() }
            nd.setActive(engine.persona)
            nd.setState(engine.state)
            frame.addView(nd, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            duo = nd
            main.animate().alpha(0f).setDuration(300).start()
            main.onPause()
            nd.open()
        } else if (!want && d != null) {
            main.onResume()
            main.animate().alpha(1f).setDuration(450).start()
            duo = null
            d.close {
                frame.removeView(d)
                d.destroy()
                if (engine.groupMode) syncDuo()
            }
        } else if (want && d != null) {
            d.setActive(engine.persona)
        }
    }
}

/**
 * Stub. Android's assistant picker expects a voice interaction service to name a RecognitionService (this could not be verified
 * against the SDK here, so it is provided to be safe). It never recognizes anything: it answers every request with an error.
 */
class ArkitakRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: RecognitionService.Callback?) {
        try { listener?.error(SpeechRecognizer.ERROR_CLIENT) } catch (_: Throwable) {}
    }
    override fun onCancel(listener: RecognitionService.Callback?) {}
    override fun onStopListening(listener: RecognitionService.Callback?) {}
}
