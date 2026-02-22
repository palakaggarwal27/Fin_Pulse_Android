package com.avinya.fin_pulse_android

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.util.Log

/**
 * Voice Expense Widget Provider
 * Allows users to log expenses via voice directly from home screen
 */
class VoiceExpenseWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "VoiceExpenseWidget"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Intent to launch VoiceExpenseActivity (Direct Voice Input)
        val voiceIntent = Intent(context, VoiceExpenseActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val voicePendingIntent = PendingIntent.getActivity(
            context,
            1,
            voiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to launch MainActivity (Full App)
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            2,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val views = RemoteViews(context.packageName, R.layout.widget_voice_expense).apply {
            // Clicking the App Name "Fin-Pulse" opens the full app
            setOnClickPendingIntent(R.id.widget_title, mainPendingIntent)
            
            // Clicking ANYWHERE else (Mic, Subtitle, or the empty space) opens Voice Input
            setOnClickPendingIntent(R.id.widget_mic_button, voicePendingIntent)
            setOnClickPendingIntent(R.id.widget_subtitle, voicePendingIntent)
            
            // To make the background area also trigger voice, we'll set it on the root if possible
            // or just ensure the main elements cover enough area. 
            // In Android widgets, individual view IDs must be targeted.
        }
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
