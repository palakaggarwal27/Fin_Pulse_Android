package com.avinya.fin_pulse_android

import android.content.Context
import java.util.regex.Pattern

data class ParsedTransaction(
    val amount: Float,
    val method: String,
    val party: String,
    val upiId: String? = null,
    val description: String,
    var isCredit: Boolean = false
)

object TransactionParser {
    private val amountRegex = Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+\\.?\\d*)")
    private val upiRegex = Pattern.compile("([\\w.-]+@[\\w.-]+)")
    
    fun parse(context: Context, text: String): ParsedTransaction? {
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
        
        var method = "Digital"
        if (text.contains("UPI", ignoreCase = true) || text.contains("VPA", ignoreCase = true)) method = "UPI"
        else if (text.contains("Card", ignoreCase = true) || text.contains("Debit", ignoreCase = true)) method = "Debit Card"
        else if (text.contains("Credit Card", ignoreCase = true)) method = "Credit Card"
        
        val upiMatcher = upiRegex.matcher(text)
        val upiId = if (upiMatcher.find()) upiMatcher.group(1) else null
        
        val lowerText = text.lowercase()
        
        // Smarter Credit vs Debit detection
        val creditKeywords = listOf("received", "credited", "added to", "deposited", "incoming", "refund", "cashback", "sent you", "sent to you")
        val debitKeywords = listOf("paid", "spent", "debited", "transfer to", "withdrawn", "payment to")
        
        var isCreditGuess = false
        
        // Priority check for "sent you" / "sent to you" which overrides generic "sent"
        if (lowerText.contains("sent you") || lowerText.contains("sent to you")) {
            isCreditGuess = true
        } else if (debitKeywords.any { lowerText.contains(it) }) {
            isCreditGuess = false
        } else if (text.contains("sent", ignoreCase = true)) {
            // Generic "sent" is usually debit
            isCreditGuess = false
        } else if (creditKeywords.any { lowerText.contains(it) }) {
            isCreditGuess = true
        } else if (lowerText.contains("from") && !lowerText.contains("account")) {
            isCreditGuess = true
        }

        // Apply AI Learning for Credit/Debit
        val isCredit = ExpenseManager.isCreditTransaction(context, text, isCreditGuess)

        // 1. Try to get learned name for UPI ID
        var party: String? = upiId?.let { ExpenseManager.getLearnedNameForUpi(context, it) }

        // 2. If no learned name, try extraction
        if (party == null) {
            val partyKeywords = if (isCredit) {
                listOf("from ", "received from ", "credited by ", "by transfer from ", "by ", "sent by ")
            } else {
                listOf("paid to ", "sent to ", "spent at ", "transfer to ", "payment to ", "to ", "at ", "info: upi-")
            }

            for (keyword in partyKeywords) {
                val index = lowerText.indexOf(keyword)
                if (index != -1) {
                    val start = index + keyword.length
                    val end = text.indexOfAny(charArrayOf('.', ',', ' ', '-', '/', '\n'), start).let { if (it == -1) text.length else it }
                    val extracted = text.substring(start, end).trim()
                    
                    val genericWords = listOf("your", "my", "me", "account", "a/c", "bank", "vpa", "upi")
                    if (extracted.isNotEmpty() && !genericWords.contains(extracted.lowercase()) && extracted.length > 2) {
                        party = extracted
                        break
                    }
                }
            }
        }

        // 3. Fallback for common UPI notification formats
        if (party == null) {
            val paidYouIndex = lowerText.indexOf(" paid you")
            if (paidYouIndex != -1) {
                party = text.substring(0, paidYouIndex).trim()
            } else if (lowerText.contains("sent") && lowerText.contains("to you")) {
                // Handle "Someone sent Rs 100 to you"
                val sentIndex = lowerText.indexOf("sent")
                if (sentIndex > 0) {
                    party = text.substring(0, sentIndex).trim()
                }
            }
        }

        // 4. Final fallback: Use UPI ID if available, otherwise "Unknown"
        val finalParty = party?.uppercase() ?: upiId?.uppercase() ?: "UNKNOWN"

        val result = ParsedTransaction(
            amount = amount,
            method = method,
            party = finalParty,
            upiId = upiId,
            isCredit = isCredit,
            description = if (isCredit) "Received from $finalParty" else "Spent at $finalParty"
        )
        
        android.util.Log.i("FinPulse-Parser", "Parsed: Amt=$amount, Party=$finalParty, IsCredit=$isCredit")
        return result
    }
}
