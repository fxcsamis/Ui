package com.arkitak.lite.wake

import android.service.voice.AlwaysOnHotwordDetector

/**
 * The callback of AlwaysOnHotwordDetector. On this phone it is an abstract CLASS (not an interface), so it has to be subclassed here
 * at compile time (the first version tried a reflection proxy and failed with exactly that message).
 *
 * UNVERIFIED on the machine that wrote this (no Android SDK there): the five abstract methods below are the ones the AOSP sources
 * list for API 24 to 34 (onAvailabilityChanged, onDetected, onError, onRecognitionPaused, onRecognitionResumed). If the build says
 * "does not implement abstract member X" or "overrides nothing", send me that line and I fix this one file.
 * Optional callbacks (onFailure, onRejected, ...) are left out on purpose: they have default bodies and differ between versions.
 */
class DspCallback(private val owner: DspHotwordEngine) : AlwaysOnHotwordDetector.Callback() {

    /** min API 24. Availability of the keyphrase: STATE_HARDWARE_UNAVAILABLE / UNSUPPORTED / UNENROLLED / ENROLLED. */
    override fun onAvailabilityChanged(status: Int) {
        DiagLog.safe("dsp onAvailabilityChanged") { owner.cbAvailability(status) }
    }

    /** min API 24. The real detection. Recognition stops by itself after this and must be started again. */
    override fun onDetected(eventPayload: AlwaysOnHotwordDetector.EventPayload) {
        DiagLog.safe("dsp onDetected") { owner.cbDetected() }
    }

    /** min API 24. */
    override fun onError() {
        DiagLog.safe("dsp onError") { owner.cbError("onError") }
    }

    override fun onRecognitionPaused() {
        DiagLog.safe("dsp onRecognitionPaused") { owner.cbInfo("onRecognitionPaused") }
    }

    override fun onRecognitionResumed() {
        DiagLog.safe("dsp onRecognitionResumed") { owner.cbInfo("onRecognitionResumed") }
    }
}
