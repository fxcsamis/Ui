package com.arkitak.lite.wake

/** What an engine can say about itself. [detail] is the exact reason, [fix] is what you can do about it. */
sealed class EngineStatus(val code: String, val detail: String = "", val fix: String = "") {
    class Stopped(detail: String = "") : EngineStatus("STOPPED", detail)
    class Running(detail: String = "") : EngineStatus("RUNNING", detail)
    class Checking(detail: String = "") : EngineStatus("CHECKING", detail)
    class Supported(detail: String = "") : EngineStatus("SUPPORTED", detail)
    class Unsupported(detail: String, fix: String = "") : EngineStatus("UNSUPPORTED", detail, fix)
    class Unenrolled(detail: String, fix: String = "") : EngineStatus("UNENROLLED", detail, fix)
    class HardwareUnavailable(detail: String, fix: String = "") : EngineStatus("HARDWARE_UNAVAILABLE", detail, fix)
    class PermissionMissing(detail: String, fix: String = "") : EngineStatus("PERMISSION_MISSING", detail, fix)
    class Failed(detail: String, fix: String = "") : EngineStatus("FAILED", detail, fix)

    /** The engine cannot work as it is now. */
    val isBlocking: Boolean
        get() = this is Unsupported || this is Unenrolled || this is HardwareUnavailable || this is PermissionMissing || this is Failed

    /** The engine is on (listening, or starting up). */
    val isActive: Boolean get() = this is Running || this is Checking

    override fun toString(): String = if (detail.isEmpty()) code else "$code: $detail"
}

data class StartResult(val ok: Boolean, val message: String = "")

/** Outcome of a real detection test: Detected, Timed out, Unsupported or Failed. */
data class TestResult(val engineId: String, val outcome: String, val reason: String, val latencyMs: Long, val at: Long) {
    fun encode(): String = listOf(outcome, reason.replace("|", "/").replace("\n", " "), latencyMs.toString(), at.toString()).joinToString("|")

    fun line(): String =
        outcome + (if (reason.isNotEmpty()) " - $reason" else "") + (if (latencyMs > 0) " (${latencyMs} ms)" else "")

    companion object {
        fun decode(id: String, s: String): TestResult? {
            val p = s.split("|")
            if (p.size < 4) return null
            return TestResult(id, p[0], p[1], p[2].toLongOrNull() ?: 0L, p[3].toLongOrNull() ?: 0L)
        }
    }
}

/** How an engine reports back to [WakeEngineManager]. Always safe to call from any thread. */
interface WakeSink {
    /** A real detection. [persona] is "olivia" / "arkitak" when the engine knows which name was said, else null. */
    fun onWake(engineId: String, persona: String?, heard: String)

    /** The engine's status changed (listening, failed, ...). */
    fun onStatusChanged(engineId: String)
}

/** One way of waking the assistant. Exactly one engine is active at a time (see [WakeEngineManager]). */
interface WakeEngine {
    val id: String
    val title: String
    fun start(): StartResult
    fun stop()
    fun status(): EngineStatus

    /** A conversation is about to use the microphone: let go of it. */
    fun pauseForSession() {}

    /** The conversation ended: listen again (only called while wake mode is on). */
    fun resumeAfterSession() {}
}
