package com.avinya.fin_pulse_android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            android.util.Log.d("FinPulse-SMS", "SMS broadcast received")
            
            if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                
                for (message in messages) {
                    val body = message.messageBody
                    
                    val parsed = TransactionParser.parse(context, body)
                    if (parsed != null) {
                        val serviceIntent = Intent(context, BubbleService::class.java).apply {
                            putExtra("amount", parsed.amount)
                            putExtra("party", parsed.party)
                            putExtra("method", parsed.method)
                            putExtra("upiId", parsed.upiId)
                            putExtra("isCredit", parsed.isCredit)
                            putExtra("rawText", body)
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FinPulse-SMS", "Error processing SMS", e)
        }
    }
}
