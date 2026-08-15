package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class CloudihubSearchWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_cloudihub_search)

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

            views.setOnClickPendingIntent(R.id.widget_search_logo, getPendingIntent("HOME"))
            views.setOnClickPendingIntent(R.id.btn_compact_search_bar, getPendingIntent("HOME_SEARCH"))
            views.setOnClickPendingIntent(R.id.btn_compact_voice_mic, getPendingIntent("VOICE"))
            views.setOnClickPendingIntent(R.id.btn_compact_downloads, getPendingIntent("DOWNLOADS"))

            // Video Cards Below Search Bar
            views.setOnClickPendingIntent(R.id.btn_card_1, getPendingIntent("VIDEO"))
            views.setOnClickPendingIntent(R.id.btn_card_2, getPendingIntent("MUSIC"))
            views.setOnClickPendingIntent(R.id.btn_card_3, getPendingIntent("HOME"))
            views.setOnClickPendingIntent(R.id.btn_card_4, getPendingIntent("MUSIC"))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
