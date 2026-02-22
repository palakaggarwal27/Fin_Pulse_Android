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
        const val ACTION_VOICE_INPUT = "com.avinya.fin_pulse_android.ACTION_VOICE_INPUT"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")
        
        // Update each widget instance
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        Log.d(TAG, "Widget enabled")
        // Called when the first widget is created
    }

    override fun onDisabled(context: Context) {
        Log.d(TAG, "Widget disabled")
        // Called when the last widget is removed
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_VOICE_INPUT -> {
                Log.d(TAG, "Voice input action received")
                // Launch voice input activity
                val voiceIntent = Intent(context, VoiceExpenseActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(voiceIntent)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        Log.d(TAG, "Updating widget $appWidgetId")
        
        // Create intent for voice input
        val voiceIntent = Intent(context, VoiceExpenseWidgetProvider::class.java).apply {
            action = ACTION_VOICE_INPUT
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            voiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the widget layout
        val views = RemoteViews(context.packageName, R.layout.widget_voice_expense).apply {
            setOnClickPendingIntent(R.id.widget_mic_button, pendingIntent)
            
            // Update text
            setTextViewText(R.id.widget_title, "Fin-Pulse")
            setTextViewText(R.id.widget_subtitle, "Tap to log expense")
        }
        
        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
