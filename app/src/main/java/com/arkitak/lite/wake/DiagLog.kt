package com.arkitak.lite.wake

import android.util.Log
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/** Small in-memory log (last 400 lines). Feeds the diagnostics popup and the crash reports. Never leaves the phone. */
object DiagLog {
    private const val MAX = 400
    private val lines = ArrayDeque<String>()
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun log(tag: String, msg: String, t: Throwable? = null) {
        val stamp = synchronized(fmt) { fmt.format(Date()) }
        val line = "$stamp [$tag] $msg" + (if (t != null) " | ${t.javaClass.simpleName}: ${t.message}" else "")
        synchronized(lines) {
            lines.addLast(line)
            while (lines.size > MAX) lines.removeFirst()
        }
        if (t != null) Log.w("ARKITAK-$tag", msg, t) else Log.i("ARKITAK-$tag", msg)
    }

    fun snapshot(n: Int = 200): List<String> = synchronized(lines) { lines.toList().takeLast(n) }

    /** Every service / engine callback runs inside this: an exception is logged, never thrown into the system. */
    inline fun safe(what: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            log("ERR", "$what failed", t)
        }
    }
}
