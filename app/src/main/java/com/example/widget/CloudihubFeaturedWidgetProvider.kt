package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

import com.example.ui.components.createWaveBallLoaderBitmap

class CloudihubFeaturedWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_cloudihub_featured)

            try {
                val waveBitmap = createWaveBallLoaderBitmap(context, 36)
                views.setImageViewBitmap(R.id.img_featured_ai_icon, waveBitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }

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

            views.setOnClickPendingIntent(R.id.btn_featured_brand, getPendingIntent("HOME"))
            views.setOnClickPendingIntent(R.id.btn_featured_downloads, getPendingIntent("DOWNLOADS"))
            views.setOnClickPendingIntent(R.id.btn_featured_search, getPendingIntent("HOME_SEARCH"))
            views.setOnClickPendingIntent(R.id.btn_featured_voice, getPendingIntent("VOICE"))

            // Categories
            views.setOnClickPendingIntent(R.id.btn_featured_hot, getPendingIntent("VIDEO"))
            views.setOnClickPendingIntent(R.id.btn_featured_music, getPendingIntent("MUSIC"))
            views.setOnClickPendingIntent(R.id.btn_featured_trending, getPendingIntent("VIDEO"))
            views.setOnClickPendingIntent(R.id.btn_featured_site, getPendingIntent("SITE"))
            views.setOnClickPendingIntent(R.id.btn_featured_ai, getPendingIntent("AI"))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
