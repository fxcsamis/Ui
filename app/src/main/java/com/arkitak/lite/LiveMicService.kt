package com.arkitak.lite

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.util.Log

/**
 * "Mic always on". A foreground service of type microphone: while it runs, Android lets the live conversation keep using the
 * microphone when the app is not on screen or the screen is off. Android requires the small "listening" notification.
 * Started by [AssistantEngine] when a conversation starts and Mic always on is enabled; stopped when the conversation stops.
 */
class LiveMicService : Service() {

    companion object {
        @Volatile var running = false
        private const val TAG = "LiveMicService"
        private const val CHANNEL_ID = "live_mic"
        private const val NOTIF_ID = 8
        private const val ACTION_STOP = "com.arkitak.lite.STOP_LIVE_MIC"

        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, LiveMicService::class.java))
            } catch (e: Exception) {
                // Android refuses to start a microphone service from the background. The conversation still works while the app is visible.
                Log.w(TAG, "could not start the always-on service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try { context.stopService(Intent(context, LiveMicService::class.java)) } catch (_: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            AssistantEngine.get(this).stop()
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            else startForeground(NOTIF_ID, notification)
            running = true
        } catch (e: Exception) {
            Log.w(TAG, "startForeground failed: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Mic always on", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 2, Intent(this, LiveMicService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "Stop listening", stopIntent
        ).build()
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("ARKITAK is listening")
            .setContentText("Mic always on. Tap to open, or stop listening.")
            .setContentIntent(open)
            .addAction(stopAction)
            .setOngoing(true)
            .build()
    }
}
