package com.avinya.fin_pulse_android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            android.util.Log.d("FinPulse-SMS", "SMS broadcast received")
            
            if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                android.util.Log.d("FinPulse-SMS", "Received ${messages.size} SMS message(s)")
                
                for (message in messages) {
                    val body = message.messageBody
                    val sender = message.originatingAddress
                    android.util.Log.d("FinPulse-SMS", "SMS from $sender: $body")
                    
                    val parsed = TransactionParser.parse(body)
                    if (parsed != null) {
                        android.util.Log.i("FinPulse-SMS", "Transaction found in SMS! Starting BubbleService...")
                        
                        // Trigger the Bubble Service with parsed data
                        val serviceIntent = Intent(context, BubbleService::class.java).apply {
                            putExtra("amount", parsed.amount)
                            putExtra("party", parsed.party)
                            putExtra("method", parsed.method)
                            putExtra("upiId", parsed.upiId)
                            putExtra("isCredit", parsed.isCredit)
                        }
                        // Use startForegroundService on Android O+ to comply with requirements
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            android.util.Log.d("FinPulse-SMS", "Starting BubbleService as foreground service")
                            context.startForegroundService(serviceIntent)
                        } else {
                            android.util.Log.d("FinPulse-SMS", "Starting BubbleService")
                            context.startService(serviceIntent)
                        }
                    } else {
                        android.util.Log.d("FinPulse-SMS", "No transaction found in SMS")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FinPulse-SMS", "Error processing SMS", e)
            e.printStackTrace()
            // Don't crash the SMS receiver
        }
    }
}
