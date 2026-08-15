package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class CloudihubMusicWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_PLAY_PAUSE = "com.example.widget.ACTION_MUSIC_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.example.widget.ACTION_MUSIC_NEXT"
        const val ACTION_WIDGET_PREV = "com.example.widget.ACTION_MUSIC_PREV"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, CloudihubMusicWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            val intent = Intent(context, CloudihubMusicWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_WIDGET_PLAY_PAUSE -> {
                // Play / Pause sound directly from the widget in the background without opening the app!
                WidgetMusicManager.togglePlayPause(context)
            }
            ACTION_WIDGET_NEXT -> {
                // Switch to next track directly in background
                WidgetMusicManager.next(context)
            }
            ACTION_WIDGET_PREV -> {
                // Switch to previous track directly in background
                WidgetMusicManager.previous(context)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetMusicManager.initialize(context)
        val currentTrack = WidgetMusicManager.getCurrentTrack()
        val isPlaying = WidgetMusicManager.isPlaying

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_cloudihub_music)

            // 1. Update Track Info & Playing State
            views.setTextViewText(R.id.widget_music_title, currentTrack.title)
            views.setTextViewText(R.id.widget_music_subtitle, currentTrack.artist)
            views.setTextViewText(
                R.id.widget_music_status_text,
                if (isPlaying) "PLAYING ♫" else "STANDBY"
            )

            // 2. Update Play/Pause Icon State
            val playPauseIconRes = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            views.setImageViewResource(R.id.btn_music_play_pause, playPauseIconRes)

            // 3. BROADCAST PendingIntents for Audio Controls (Stand-alone without opening App!)
            fun getBroadcastPendingIntent(actionStr: String, requestCode: Int): PendingIntent {
                val broadcastIntent = Intent(context, CloudihubMusicWidgetProvider::class.java).apply {
                    action = actionStr
                }
                return PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    broadcastIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            views.setOnClickPendingIntent(
                R.id.btn_music_play_pause,
                getBroadcastPendingIntent(ACTION_WIDGET_PLAY_PAUSE, 1001)
            )
            views.setOnClickPendingIntent(
                R.id.btn_music_prev,
                getBroadcastPendingIntent(ACTION_WIDGET_PREV, 1002)
            )
            views.setOnClickPendingIntent(
                R.id.btn_music_next,
                getBroadcastPendingIntent(ACTION_WIDGET_NEXT, 1003)
            )

            // 4. ACTIVITY PendingIntents for Search, Voice, Sites, & Full App Hub
            fun getActivityPendingIntent(actionTarget: String, requestCode: Int): PendingIntent {
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("WIDGET_TARGET", actionTarget)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                return PendingIntent.getActivity(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            views.setOnClickPendingIntent(R.id.btn_music_header, getActivityPendingIntent("MUSIC", 2001))
            views.setOnClickPendingIntent(R.id.widget_music_status_badge, getActivityPendingIntent("MUSIC", 2002))
            views.setOnClickPendingIntent(R.id.widget_music_art, getActivityPendingIntent("MUSIC", 2003))
            views.setOnClickPendingIntent(R.id.btn_music_search, getActivityPendingIntent("MUSIC_SEARCH", 2004))
            views.setOnClickPendingIntent(R.id.btn_music_voice, getActivityPendingIntent("VOICE", 2005))
            views.setOnClickPendingIntent(R.id.btn_music_site, getActivityPendingIntent("SITE", 2006))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
