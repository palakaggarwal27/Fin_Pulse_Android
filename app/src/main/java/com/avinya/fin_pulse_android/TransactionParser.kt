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
    // Matches multiple patterns:
    // - "Rs 100", "₹100", "100 rs", "100 Rs", "100rs", "rs100"
    // - "debited by 1.00", "credited by 500.50"
    // - "of Rs 100", "of 50"
    private val amountRegex = Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+\\.?\\d*)|([\\d,]+\\.?\\d*)\\s*(?:rs\\.?|inr|₹)|(?:debited by|credited by|of)\\s+([\\d,]+\\.?\\d*)")
    private val upiRegex = Pattern.compile("([\\w.-]+@[\\w.-]+)")
    
    /**
     * Cleans party names to remove duplications like:
     * - "Dad: Dad" -> "Dad"
     * - "Mom:Mom" -> "Mom"
     * - "John: john" -> "John"
     * - "Starbucks:starbucks" -> "Starbucks"
     */
    private fun cleanDuplicatedPartyName(name: String): String {
        // Check for colon-separated duplication
        if (name.contains(":")) {
            val parts = name.split(":")
            if (parts.size == 2) {
                val first = parts[0].trim()
                val second = parts[1].trim()
                if (first.equals(second, ignoreCase = true)) {
                    return first
                }
            }
        }
        
        // Check for space-separated duplication
        val words = name.trim().split(" ")
        if (words.size == 2 && words[0].equals(words[1], ignoreCase = true)) {
            return words[0]
        }
        
        return name
    }
    
    /**
     * Intelligently extracts person/party name from text using:
     * 1. Capitalization patterns (proper nouns)
     * 2. Position in sentence
     * 3. Context words around the name
     */
    private fun extractNameIntelligently(text: String, isCredit: Boolean): String? {
        val words = text.trim().split(Regex("\\s+"))
        
        // Common words that indicate name position
        val nameIndicators = if (isCredit) {
            listOf("paid", "sent", "transferred", "credited", "gave")
        } else {
            listOf("to", "at", "for")
        }
        
        // Words to skip (not names)
        val skipWords = setOf(
            "you", "your", "rs", "inr", "rupees", "the", "a", "an", "has", "have", 
            "is", "was", "been", "will", "would", "should", "could", "can", "may",
            "upi", "payment", "transaction", "amount", "money", "from", "account"
        )
        
        // For credit transactions, look for name before action words
        if (isCredit) {
            for (i in words.indices) {
                val word = words[i].lowercase()
                if (nameIndicators.any { word.contains(it) }) {
                    // Found indicator word, name should be before this
                    if (i > 0) {
                        var nameBuilder = StringBuilder()
                        var j = i - 1
                        
                        // Collect consecutive capitalized words before the indicator
                        while (j >= 0) {
                            val candidateWord = words[j]
                            val cleanWord = candidateWord.replace(Regex("[^A-Za-z]"), "")
                            
                            // Stop if we hit a skip word or lowercase word
                            if (cleanWord.lowercase() in skipWords || 
                                (cleanWord.isNotEmpty() && cleanWord[0].isLowerCase())) {
                                break
                            }
                            
                            // Check if it's a capitalized word (potential name)
                            if (cleanWord.isNotEmpty() && cleanWord[0].isUpperCase()) {
                                if (nameBuilder.isNotEmpty()) {
                                    nameBuilder.insert(0, " ")
                                }
                                nameBuilder.insert(0, cleanWord)
                            }
                            j--
                        }
                        
                        val extractedName = nameBuilder.toString().trim()
                        if (extractedName.isNotEmpty() && extractedName.length >= 2) {
                            return extractedName
                        }
                    }
                }
            }
            
            // Fallback: Check if first word is capitalized (common in notifications)
            if (words.isNotEmpty()) {
                val firstWord = words[0].replace(Regex("[^A-Za-z]"), "")
                if (firstWord.isNotEmpty() && 
                    firstWord[0].isUpperCase() && 
                    firstWord.lowercase() !in skipWords &&
                    firstWord.length >= 2) {
                    
                    // Check if second word is also capitalized (full name)
                    if (words.size > 1) {
                        val secondWord = words[1].replace(Regex("[^A-Za-z]"), "")
                        if (secondWord.isNotEmpty() && 
                            secondWord[0].isUpperCase() && 
                            secondWord.lowercase() !in skipWords &&
                            !nameIndicators.any { words[1].lowercase().contains(it) }) {
                            return "$firstWord $secondWord"
                        }
                    }
                    
                    return firstWord
                }
            }
        }
        
        return null
    }
    
    fun parse(context: Context, text: String): ParsedTransaction? {
        android.util.Log.d("FinPulse-Parser", "Parsing text: $text")
        
        val amountMatcher = amountRegex.matcher(text)
        if (!amountMatcher.find()) return null
        
        val amountStr = (amountMatcher.group(1) ?: amountMatcher.group(2) ?: amountMatcher.group(3))?.replace(",", "") ?: return null
        val amount = amountStr.toFloatOrNull() ?: return null
        
        var method = "Digital"
        if (text.contains("UPI", ignoreCase = true) || text.contains("VPA", ignoreCase = true)) method = "UPI"
        else if (text.contains("Card", ignoreCase = true) || text.contains("Debit", ignoreCase = true)) method = "Debit Card"
        else if (text.contains("Credit Card", ignoreCase = true)) method = "Credit Card"
        
        val upiMatcher = upiRegex.matcher(text)
        val upiId = if (upiMatcher.find()) upiMatcher.group(1) else null
        
        val lowerText = text.lowercase()
        
        // --- 1. Credit/Debit Detection ---
        val creditKeywords = listOf("received", "credited", "added to", "deposited", "incoming", "refund", "cashback")
        val debitKeywords = listOf("paid", "spent", "debited", "transfer to", "withdrawn", "payment to")
        
        var isCreditGuess = false
        if (lowerText.contains("sent") && (lowerText.contains("to you") || lowerText.contains("you received"))) {
            isCreditGuess = true
        } else if (lowerText.contains("sent you")) {
            isCreditGuess = true
        } else if (debitKeywords.any { lowerText.contains(it) }) {
            isCreditGuess = false
        } else if (lowerText.contains("sent")) {
            isCreditGuess = false
        } else if (creditKeywords.any { lowerText.contains(it) }) {
            isCreditGuess = true
        } else if (lowerText.contains("from") && !lowerText.contains("account")) {
            isCreditGuess = true
        }

        val isCredit = ExpenseManager.isCreditTransaction(context, text, isCreditGuess)

        // --- 2. Party Name Extraction ---
        var party: String? = upiId?.let { ExpenseManager.getLearnedNameForUpi(context, it) }

        if (party == null) {
            // Priority 1: Intelligent name extraction using capitalization and context
            party = extractNameIntelligently(text, isCredit)
            
            // Priority 2: "[Name] sent you Rs..." or "[Name] sent Rs... to you"
            if (party == null && isCredit && lowerText.contains("sent")) {
                // Pattern: "Name sent you Rs"
                if (lowerText.contains("sent you")) {
                    val sentIndex = lowerText.indexOf("sent you")
                    if (sentIndex > 0) {
                        party = text.substring(0, sentIndex).trim()
                    }
                }
                // Pattern: "Name sent Rs... to you"
                else if (lowerText.contains("to you")) {
                    val sentIndex = lowerText.indexOf("sent")
                    if (sentIndex > 0) {
                        party = text.substring(0, sentIndex).trim()
                    }
                }
            }
            
            // Priority 3: "[Name] has sent/transferred Rs..."
            if (party == null && isCredit) {
                val patterns = listOf("has sent", "has transferred", "has paid")
                for (pattern in patterns) {
                    if (lowerText.contains(pattern)) {
                        val index = lowerText.indexOf(pattern)
                        if (index > 0) {
                            party = text.substring(0, index).trim()
                            break
                        }
                    }
                }
            }

            // Priority 4: Keyword extraction
            if (party == null) {
                val partyKeywords = if (isCredit) {
                    listOf("from ", "received from ", "credited by ", "by transfer from ", "payment from ", "transfer from ", "trf from ", "by ", "sent by ")
                } else {
                    listOf("trf to ", "transfer to ", "paid to ", "sent to ", "spent at ", "payment to ", "to ", "at ", "info: upi-")
                }

                for (keyword in partyKeywords) {
                    val index = lowerText.indexOf(keyword)
                    if (index != -1) {
                        val start = index + keyword.length
                        // Capture until punctuation, "ref", "refno", or newline
                        var end = text.length
                        
                        // Look for common terminators in bank SMS
                        val terminators = listOf(" ref", " refno", " if not", " avbl", " avl", " bal", "-")
                        for (terminator in terminators) {
                            val termIndex = lowerText.indexOf(terminator, start)
                            if (termIndex != -1 && termIndex < end) {
                                end = termIndex
                            }
                        }
                        
                        // Also check for punctuation
                        val punctIndex = text.indexOfAny(charArrayOf('.', ',', '\n'), start)
                        if (punctIndex != -1 && punctIndex < end) {
                            end = punctIndex
                        }
                        
                        val extracted = text.substring(start, end).trim()
                        
                        val genericWords = listOf("your", "my", "me", "account", "a/c", "bank", "vpa", "upi")
                        if (extracted.isNotEmpty() && !genericWords.contains(extracted.lowercase()) && extracted.length >= 2) {
                            party = extracted
                            break
                        }
                    }
                }
            }
        }
        
        // --- Remove duplication like "Dad: Dad" or repeated names ---
        party = party?.let { cleanDuplicatedPartyName(it) }

        // --- 3. Final Construction ---
        val finalParty = party?.uppercase() ?: upiId?.uppercase() ?: "UNKNOWN"

        val result = ParsedTransaction(
            amount = amount,
            method = method,
            party = finalParty,
            upiId = upiId,
            isCredit = isCredit,
            description = if (isCredit) "Received from $finalParty" else "Spent at $finalParty"
        )
        
        return result
    }
}
