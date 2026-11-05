package com.arkitak.lite

import android.app.Application
import com.arkitak.lite.wake.CrashReporter
import com.arkitak.lite.wake.DiagLog
import com.arkitak.lite.wake.WakeEngineManager

class ArkitakApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this) // first, so even a crash during start-up is written down
        DiagLog.safe("wake init") { WakeEngineManager.init(this) }
        DiagLog.log("APP", "started; ${com.arkitak.lite.wake.Diagnostics.device()}")
    }
}
