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
            
            // Blacklist for privacy apps - never read notifications from these apps
            val blacklistedApps = setOf(
                "com.whatsapp",                // WhatsApp
                "com.whatsapp.w4b",            // WhatsApp Business
                "com.google.android.gm"        // Gmail
            )
            
            // Skip blacklisted apps
            if (blacklistedApps.contains(packageName)) {
                android.util.Log.d("FinPulse-Notification", "Skipping notification from blacklisted app: $packageName")
                return
            }

            val notification = sbn?.notification ?: return
            val extras = notification.extras
            val title = extras.getString("android.title") ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""

            val fullText = "$title $text $bigText"
            
            // Initial filter: Must look like a transaction
            if (!ExpenseManager.isLikelyTransaction(this, fullText)) {
                android.util.Log.d("FinPulse-Notification", "Filtered out: Not likely a transaction")
                return
            }

            // Try to parse
            val parsed = TransactionParser.parse(this, fullText)

            if (parsed != null) {
                android.util.Log.i("FinPulse-Notification", "Transaction parsed from $packageName! Amount: ${parsed.amount}")
                
                val serviceIntent = Intent(this, BubbleService::class.java).apply {
                    putExtra("amount", parsed.amount)
                    putExtra("party", parsed.party)
                    putExtra("method", parsed.method)
                    putExtra("upiId", parsed.upiId)
                    putExtra("isCredit", parsed.isCredit)
                    putExtra("rawText", fullText)
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FinPulse-Notification", "Error processing notification", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}
