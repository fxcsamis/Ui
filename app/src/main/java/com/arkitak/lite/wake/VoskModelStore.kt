package com.arkitak.lite.wake

import android.content.Context
import android.content.res.AssetManager
import java.io.File
import java.io.FileOutputStream

/**
 * The Vosk model lives in the APK as assets/model-hi/ (the CONTENTS of vosk-model-small-hi-0.22: am, conf, graph, ivector, README).
 * Vosk cannot read assets, so on the first run the model is copied to filesDir/vosk-model-hi. A marker file is written only after
 * a complete copy, so an interrupted copy is detected and redone. Always call [ensure] on a background thread.
 */
object VoskModelStore {
    const val ASSET_DIR = "model-hi"
    private const val MARK = ".arkitak_copy_ok"
    private const val VERSION = "hi-0.22-v1"
    private val REQUIRED = listOf("am/final.mdl", "conf/mfcc.conf", "graph/Gr.fst", "graph/HCLr.fst", "ivector/final.ie")

    fun targetDir(ctx: Context) = File(ctx.filesDir, "vosk-model-hi")

    fun inAssets(ctx: Context): Boolean = try {
        ctx.assets.list(ASSET_DIR)?.isNotEmpty() == true
    } catch (_: Throwable) { false }

    fun isReady(ctx: Context): Boolean {
        val m = File(targetDir(ctx), MARK)
        return m.exists() && (try { m.readText().trim() == VERSION } catch (_: Throwable) { false })
    }

    fun statusText(ctx: Context): String = when {
        isReady(ctx) -> "ready (copied to ${targetDir(ctx).absolutePath})"
        inAssets(ctx) -> "found in the APK, will be copied on first start (about 80 MB)"
        else -> "MISSING: put the contents of vosk-model-small-hi-0.22 in app/src/main/assets/model-hi/ and rebuild"
    }

    @Synchronized
    fun ensure(ctx: Context): File {
        val dir = targetDir(ctx)
        if (isReady(ctx)) return dir
        if (!inAssets(ctx)) throw IllegalStateException("Vosk model not found in the APK (assets/$ASSET_DIR is empty)")
        DiagLog.log("VOSK", "copying the model from assets to ${dir.absolutePath}")
        if (dir.exists()) dir.deleteRecursively()
        dir.mkdirs()
        copyAssetDir(ctx.assets, ASSET_DIR, dir)
        for (r in REQUIRED) {
            if (!File(dir, r).exists()) throw IllegalStateException("model copy incomplete: $r is missing (check assets/$ASSET_DIR)")
        }
        File(dir, MARK).writeText(VERSION)
        DiagLog.log("VOSK", "model copied")
        return dir
    }

    private fun copyAssetDir(am: AssetManager, assetPath: String, dest: File) {
        val children = am.list(assetPath) ?: emptyArray()
        if (children.isEmpty()) { // a file
            dest.parentFile?.mkdirs()
            am.open(assetPath).use { input -> FileOutputStream(dest).use { out -> input.copyTo(out, 64 * 1024) } }
            return
        }
        dest.mkdirs()
        for (name in children) copyAssetDir(am, "$assetPath/$name", File(dest, name))
    }
}
