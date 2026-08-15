package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class CloudihubAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_cloudihub_full)

            fun getPendingIntent(actionTarget: String): PendingIntent {
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("WIDGET_TARGET", actionTarget)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                return PendingIntent.getActivity(
                    context,
                    actionTarget.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            // Header & Search
            views.setOnClickPendingIntent(R.id.widget_header, getPendingIntent("HOME"))
            views.setOnClickPendingIntent(R.id.btn_widget_downloads, getPendingIntent("DOWNLOADS"))
            views.setOnClickPendingIntent(R.id.btn_widget_search_bar, getPendingIntent("HOME_SEARCH"))
            views.setOnClickPendingIntent(R.id.btn_widget_voice_mic, getPendingIntent("VOICE"))

            // Shortcuts Row (Hot Video, Music, Streams, Site)
            views.setOnClickPendingIntent(R.id.btn_widget_shortcut_video, getPendingIntent("VIDEO"))
            views.setOnClickPendingIntent(R.id.btn_widget_shortcut_music, getPendingIntent("MUSIC"))
            views.setOnClickPendingIntent(R.id.btn_widget_shortcut_trending, getPendingIntent("VIDEO"))
            views.setOnClickPendingIntent(R.id.btn_widget_shortcut_site, getPendingIntent("SITE"))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
