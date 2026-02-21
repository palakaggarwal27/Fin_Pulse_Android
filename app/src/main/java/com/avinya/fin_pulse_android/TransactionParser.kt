package com.avinya.fin_pulse_android

import java.util.regex.Pattern

data class ParsedTransaction(
    val amount: Float,
    val method: String,
    val party: String,
    val upiId: String? = null,
    val description: String,
    val isCredit: Boolean = false
)

object TransactionParser {
    private val amountRegex = Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+\\.?\\d*)")
    private val upiRegex = Pattern.compile("([\\w.-]+@[\\w.-]+)")
    
    fun parse(text: String): ParsedTransaction? {
        android.util.Log.d("FinPulse-Parser", "Parsing text: $text")
        
        val amountMatcher = amountRegex.matcher(text)
        if (!amountMatcher.find()) {
            android.util.Log.d("FinPulse-Parser", "No amount found in text")
            return null
        }
        
        val amountStr = amountMatcher.group(1)?.replace(",", "") ?: return null
        val amount = amountStr.toFloatOrNull()
        if (amount == null) {
            android.util.Log.w("FinPulse-Parser", "Failed to parse amount: $amountStr")
            return null
        }
        android.util.Log.d("FinPulse-Parser", "Amount found: $amount")
        
        var method = "UPI"
        if (text.contains("UPI", ignoreCase = true) || text.contains("VPA", ignoreCase = true)) method = "UPI"
        else if (text.contains("Card", ignoreCase = true) || text.contains("Debit", ignoreCase = true)) method = "Debit Card"
        else if (text.contains("Credit Card", ignoreCase = true)) method = "Credit Card"
        android.util.Log.d("FinPulse-Parser", "Payment method: $method")
        
        val upiMatcher = upiRegex.matcher(text)
        val upiId = if (upiMatcher.find()) upiMatcher.group(1) else null
        if (upiId != null) {
            android.util.Log.d("FinPulse-Parser", "UPI ID found: $upiId")
        }
        
        // Advanced detection for Debit/Spent vs Credit/Received
        val lowerText = text.lowercase()
        val isCredit = lowerText.contains("received") || 
                       lowerText.contains("credited") || 
                       (lowerText.contains("from") && !lowerText.contains("paid"))
        android.util.Log.d("FinPulse-Parser", "Transaction type: ${if (isCredit) "Credit" else "Debit"}")

        // Improved party extraction for various bank formats
        var party = "Unknown"
        val keywords = listOf("paid to ", "sent to ", "spent at ", "at ", "to ", "from ", "transfer to ", "info: upi-")
        for (keyword in keywords) {
            val index = lowerText.indexOf(keyword)
            if (index != -1) {
                val start = index + keyword.length
                val end = text.indexOfAny(charArrayOf('.', ',', ' ', '-', '\n'), start).let { if (it == -1) text.length else it }
                party = text.substring(start, end).trim()
                if (party.isNotEmpty() && party != "me" && party.length > 2) {
                    android.util.Log.d("FinPulse-Parser", "Party extracted using keyword '$keyword': $party")
                    break
                }
            }
        }

        // Truecaller specific cleanup (often contains "Spent on...")
        if (lowerText.contains("spent on")) {
            val start = lowerText.indexOf("spent on") + 9
            val end = text.indexOfAny(charArrayOf('.', ',', ' '), start).let { if (it == -1) text.length else it }
            party = text.substring(start, end).trim()
            android.util.Log.d("FinPulse-Parser", "Party extracted from Truecaller format: $party")
        }

        val result = ParsedTransaction(
            amount = amount,
            method = method,
            party = party.uppercase(),
            upiId = upiId,
            isCredit = isCredit,
            description = if (isCredit) "Received ₹$amount from $party" else "Paid ₹$amount to $party"
        )
        
        android.util.Log.i("FinPulse-Parser", "Successfully parsed transaction: Amount=${result.amount}, Party=${result.party}, Method=${result.method}, IsCredit=${result.isCredit}")
        return result
    }
}
