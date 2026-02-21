package com.avinya.fin_pulse_android

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        android.util.Log.i("FinPulse-Notification", "Notification Listener Service connected!")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        android.util.Log.w("FinPulse-Notification", "Notification Listener Service disconnected!")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            val packageName = sbn?.packageName ?: "unknown"
            android.util.Log.d("FinPulse-Notification", "Notification received from: $packageName")
            
            val notification = sbn?.notification ?: return
            val extras = notification.extras
            val title = extras.getString("android.title") ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""

            val fullText = "$title $text $bigText"
            android.util.Log.d("FinPulse-Notification", "Notification text: $fullText")
            
            val parsed = TransactionParser.parse(fullText)

            if (parsed != null) {
                android.util.Log.i("FinPulse-Notification", "Transaction parsed! Amount: ${parsed.amount}, Party: ${parsed.party}, Method: ${parsed.method}")
                
                val serviceIntent = Intent(this, BubbleService::class.java).apply {
                    putExtra("amount", parsed.amount)
                    putExtra("party", parsed.party)
                    putExtra("method", parsed.method)
                    putExtra("upiId", parsed.upiId)
                    putExtra("isCredit", parsed.isCredit)
                }
                // Use startForegroundService on Android O+ to comply with requirements
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    android.util.Log.d("FinPulse-Notification", "Starting BubbleService as foreground service")
                    startForegroundService(serviceIntent)
                } else {
                    android.util.Log.d("FinPulse-Notification", "Starting BubbleService")
                    startService(serviceIntent)
                }
            } else {
                android.util.Log.d("FinPulse-Notification", "No transaction found in notification text")
            }
        } catch (e: Exception) {
            android.util.Log.e("FinPulse-Notification", "Error processing notification", e)
            e.printStackTrace()
            // Don't crash the notification listener service
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not needed for now
    }
}
