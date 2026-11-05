package com.arkitak.lite.wake

import android.app.Application
import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes a crash report to filesDir/crash_reports/ (stack trace, time, device, active wake engine, last 200 log lines), then
 * hands the crash to the previous handler. Reports stay on the phone; the diagnostics popup shows them with Copy / Share / Delete.
 */
object CrashReporter {
    private const val DIR = "crash_reports"
    private const val KEEP = 10

    fun install(app: Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try { write(app, thread, error) } catch (_: Throwable) {}
            previous?.uncaughtException(thread, error)
        }
    }

    private fun write(ctx: Context, thread: Thread, error: Throwable) {
        val dir = File(ctx.filesDir, DIR).also { it.mkdirs() }
        val now = Date()
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(now)
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val active = try { WakeEngineManager.describeActive() } catch (_: Throwable) { "unknown" }
        val text = buildString {
            appendLine("ARKITAK crash report")
            appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(now)}")
            appendLine("Thread: ${thread.name}")
            appendLine("Device: ${Diagnostics.device()}")
            appendLine("Active wake engine: $active")
            appendLine()
            appendLine(trace)
            appendLine("---- last 200 log lines ----")
            DiagLog.snapshot(200).forEach { appendLine(it) }
        }
        File(dir, "crash-$stamp.txt").writeText(text)
        dir.listFiles()?.sortedBy { it.name }?.dropLast(KEEP)?.forEach { it.delete() }
    }

    fun list(ctx: Context): List<File> =
        File(ctx.filesDir, DIR).listFiles()?.filter { it.name.endsWith(".txt") }?.sortedByDescending { it.name } ?: emptyList()

    fun read(f: File): String = try { f.readText() } catch (t: Throwable) { "(could not read ${f.name}: ${t.message})" }

    fun delete(f: File) { try { f.delete() } catch (_: Throwable) {} }
}
