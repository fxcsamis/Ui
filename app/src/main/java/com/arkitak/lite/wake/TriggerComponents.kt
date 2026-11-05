package com.arkitak.lite.wake

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.RemoteViews
import com.arkitak.lite.MainActivity
import com.arkitak.lite.R

/**
 * Transparent ASSIST / VOICE_COMMAND entry (and the target of the tile and the widget). Because this activity is on screen, Android
 * allows the microphone to start. It starts the conversation, opens the main screen and closes itself.
 */
class ArkitakAssistActivity : Activity() {

    private val reqMic = 41

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DiagLog.safe("assist onCreate") { go() }
    }

    private fun go() {
        WakeEngineManager.init(applicationContext)
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), reqMic)
            return
        }
        val source = intent?.getStringExtra("source") ?: (intent?.action ?: "assist activity")
        WakeEngineManager.onTrigger(source)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        DiagLog.safe("assist permission result") {
            if (requestCode == reqMic && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) go() else finish()
        }
    }
}

/** Quick Settings tile (API 24). Opens the assist activity so the microphone starts from a visible screen. */
class ArkitakTileService : TileService() {

    override fun onStartListening() {
        DiagLog.safe("tile onStartListening") {
            val t = qsTile ?: return
            t.state = Tile.STATE_INACTIVE
            t.label = "ARKITAK"
            t.icon = Icon.createWithResource(this, R.drawable.ic_launcher_orb)
            t.updateTile()
        }
    }

    override fun onClick() {
        DiagLog.safe("tile onClick") {
            val i = Intent(this, ArkitakAssistActivity::class.java)
                .putExtra("source", "quick settings tile")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= 34) {
                // startActivityAndCollapse(PendingIntent): API 34.
                startActivityAndCollapse(PendingIntent.getActivity(this, 5, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(i)
            }
        }
    }
}

/** Home-screen widget "Talk to ARKITAK". */
class ArkitakWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        DiagLog.safe("widget onUpdate") {
            val i = Intent(context, ArkitakAssistActivity::class.java)
                .putExtra("source", "home widget")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pi = PendingIntent.getActivity(context, 7, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val views = RemoteViews(context.packageName, R.layout.widget_trigger)
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, views)
        }
    }
}
