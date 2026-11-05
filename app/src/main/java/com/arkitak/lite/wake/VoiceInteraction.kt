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
import android.widget.TextView

/**
 * The voice interaction service. Android binds it only when ARKITAK Lite is chosen as the default digital assistant
 * (Settings > Apps > Default apps > Digital assistant app). min API 21.
 */
class ArkitakVoiceInteractionService : VoiceInteractionService() {

    companion object {
        @Volatile var instance: ArkitakVoiceInteractionService? = null
    }

    /** min API 21: the system is ready, the service may now create hotword detectors. */
    override fun onReady() {
        super.onReady()
        instance = this
        DiagLog.safe("VIS onReady") {
            WakeEngineManager.init(applicationContext)
            WakeEngineManager.dsp.onServiceReady(this)
        }
    }

    override fun onShutdown() {
        DiagLog.safe("VIS onShutdown") {
            WakeEngineManager.dsp.onServiceShutdown()
            instance = null
        }
        super.onShutdown()
    }
}

class ArkitakVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = ArkitakVoiceInteractionSession(this)
}

/**
 * Manual trigger through the default-assistant gesture / power button (engine C). onShow starts the conversation;
 * onHide stops it only if THIS session started it ([owns]), so a conversation you started another way is never cut.
 */
class ArkitakVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    private var owns = false

    override fun onCreateContentView(): View {
        return TextView(context).apply {
            text = "ARKITAK is listening...  (go back to stop)"
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xCC000000.toInt())
        }
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        DiagLog.safe("session onShow") {
            owns = WakeEngineManager.onTrigger("assistant gesture / power button")
            DiagLog.log("SESSION", "onShow (flags=$showFlags) ownsConversation=$owns")
        }
    }

    override fun onHide() {
        DiagLog.safe("session onHide") {
            if (owns) {
                DiagLog.log("SESSION", "onHide: stopping the conversation this session started")
                WakeEngineManager.stopConversationFromTrigger()
            }
            owns = false
        }
        super.onHide()
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
