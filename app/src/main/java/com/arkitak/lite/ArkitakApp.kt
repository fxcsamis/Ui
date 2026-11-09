package com.arkitak.lite

import android.app.Application
import com.arkitak.lite.wake.DiagLog

class ArkitakApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DiagLog.log("APP", "started")
    }
}
