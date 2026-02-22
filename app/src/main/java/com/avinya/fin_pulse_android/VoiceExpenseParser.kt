package com.avinya.fin_pulse_android

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

data class VoiceExpense(
    val amount: Float,
    val description: String,
    val category: String,
    val merchant: String?
)

object VoiceExpenseParser {
    private const val TAG = "VoiceExpenseParser"
    private const val KEY_VOICE_PATTERNS = "voice_expense_patterns"
    
    // Common phrases that indicate spending
    private val spendingPhrases = listOf(
        "spent", "paid", "bought", "purchased", "got", "ordered",
        "spend", "pay", "buy", "purchase", "get", "order",
        "for", "on", "at"
    )
    
    // Category keywords mapping
    private val categoryKeywords = mapOf(
        "Food & Dining" to listOf(
            "food", "lunch", "dinner", "breakfast", "meal", "coffee", "tea",
            "restaurant", "cafe", "pizza", "burger", "sandwich", "snack",
            "ice cream", "icecream", "dessert", "sweet", "cake", "pastry",
            "starbucks", "dominos", "mcdonalds", "kfc", "subway",
            "swiggy", "zomato", "uber eats", "food delivery"
        ),
        "Transport" to listOf(
            "uber", "ola", "taxi", "cab", "auto", "rickshaw", "metro",
            "bus", "train", "flight", "travel", "petrol", "diesel", "fuel",
            "parking", "toll", "transport", "rapido", "bike"
        ),
        "Shopping" to listOf(
            "shopping", "clothes", "shirt", "jeans", "shoes", "dress",
            "amazon", "flipkart", "myntra", "ajio", "shop", "mall",
            "gadget", "phone", "laptop", "electronics", "accessories"
        ),
        "Entertainment" to listOf(
            "movie", "cinema", "theatre", "show", "concert", "game",
            "netflix", "spotify", "prime", "hotstar", "entertainment",
            "fun", "party", "club", "bar", "pub"
        ),
        "Bills & Utilities" to listOf(
            "bill", "electricity", "water", "gas", "internet", "mobile",
            "phone bill", "recharge", "broadband", "wifi", "utility"
        ),
        "Health & Wellness" to listOf(
            "medicine", "pharmacy", "doctor", "hospital", "clinic",
            "health", "medical", "gym", "fitness", "yoga", "wellness"
        ),
        "Groceries" to listOf(
            "grocery", "groceries", "vegetables", "fruits", "milk",
            "bread", "eggs", "supermarket", "big bazaar", "dmart",
            "reliance fresh", "more"
        ),
        "Gifts" to listOf(
            "gift", "present", "birthday", "anniversary", "celebration"
        ),
        "Stationery" to listOf(
            "pen", "pencil", "notebook", "paper", "stationery", "book"
        )
    )
    
    // Number word mapping for parsing spoken numbers
    private val numberWords = mapOf(
        "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
        "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
        "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
        "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
        "eighteen" to 18, "nineteen" to 19, "twenty" to 20, "thirty" to 30,
        "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70,
        "eighty" to 80, "ninety" to 90, "hundred" to 100, "thousand" to 1000
    )
    
    /**
     * Parse voice input to extract expense information
     * Example inputs:
     * - "spent 100 on ice cream"
     * - "paid 500 for lunch"
     * - "bought coffee for 150"
     * - "50 rupees for auto"
     */
    fun parse(text: String, context: Context): VoiceExpense? {
        try {
            Log.d(TAG, "Parsing voice input: $text")
            
            val normalizedText = text.lowercase(Locale.getDefault()).trim()
            
            // Extract amount
            val amount = extractAmount(normalizedText)
            if (amount == null || amount <= 0) {
                Log.e(TAG, "Could not extract valid amount from: $text")
                return null
            }
            
            // Extract description and merchant
            val description = extractDescription(normalizedText, amount)
            val merchant = extractMerchant(normalizedText)
            
            // Predict category based on learned patterns and keywords
            val category = predictCategory(description, context)
            
            Log.d(TAG, "Parsed: amount=$amount, desc=$description, category=$category")
            
            return VoiceExpense(
                amount = amount,
                description = description,
                category = category,
                merchant = merchant
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing voice input: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Extract amount from voice text
     * Handles various formats:
     * - "100", "100 rupees", "rupees 100"
     * - "one hundred", "fifty", "twenty five"
     * - "1.5k", "2k"
     */
    private fun extractAmount(text: String): Float? {
        // First try to find numeric amounts
        val numericRegex = Regex("""(\d+\.?\d*)""")
        val numericMatch = numericRegex.find(text)
        if (numericMatch != null) {
            val amount = numericMatch.value.toFloatOrNull()
            if (amount != null && amount > 0) {
                return amount
            }
        }
        
        // Try to parse written numbers (e.g., "fifty", "one hundred")
        val words = text.split(Regex("""\s+"""))
        var result = 0f
        var current = 0f
        
        for (word in words) {
            val cleanWord = word.replace(Regex("""[^\w]"""), "")
            val value = numberWords[cleanWord]
            
            if (value != null) {
                if (value >= 100) {
                    if (current == 0f) current = 1f
                    current *= value
                    result += current
                    current = 0f
                } else {
                    current += value
                }
            }
        }
        
        result += current
        
        return if (result > 0) result else null
    }
    
    /**
     * Extract description from voice text
     * Removes amount and common filler words
     */
    private fun extractDescription(text: String, amount: Float): String {
        var description = text
        
        // Remove the amount
        description = description.replace(amount.toString(), "")
        description = description.replace(amount.toInt().toString(), "")
        
        // Remove common currency words
        description = description.replace(Regex("""rupees?|rs\.?|inr|₹"""), "")
        
        // Remove common spending phrases but keep meaningful prepositions
        val removeWords = listOf("spent", "paid", "bought", "purchased", "got", "ordered")
        for (word in removeWords) {
            description = description.replace(Regex("""\b$word\b"""), "")
        }
        
        // Clean up "on", "for", "at" if they're at the start
        description = description.replace(Regex("""^\s*(on|for|at)\s+"""), "")
        
        // Clean up extra spaces
        description = description.replace(Regex("""\s+"""), " ").trim()
        
        // Capitalize first letter
        description = description.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
        
        return if (description.isNotEmpty()) description else "Cash expense"
    }
    
    /**
     * Extract merchant name from common brand mentions
     */
    private fun extractMerchant(text: String): String? {
        val merchants = listOf(
            "starbucks", "dominos", "mcdonalds", "kfc", "subway",
            "swiggy", "zomato", "uber", "ola", "amazon", "flipkart",
            "dmart", "big bazaar"
        )
        
        for (merchant in merchants) {
            if (text.contains(merchant, ignoreCase = true)) {
                return merchant.replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                }
            }
        }
        
        return null
    }
    
    /**
     * Predict category based on keywords and learned patterns
     */
    private fun predictCategory(description: String, context: Context): String {
        val lowerDesc = description.lowercase(Locale.getDefault())
        
        // First check learned patterns
        val learnedPattern = checkLearnedPatterns(lowerDesc, context)
        if (learnedPattern != null) {
            return learnedPattern
        }
        
        // Check category keywords
        var maxScore = 0
        var predictedCategory = "Miscellaneous"
        
        for ((category, keywords) in categoryKeywords) {
            var score = 0
            for (keyword in keywords) {
                if (lowerDesc.contains(keyword)) {
                    score += if (lowerDesc.split(Regex("""\s+""")).contains(keyword)) 3 else 1
                }
            }
            
            if (score > maxScore) {
                maxScore = score
                predictedCategory = category
            }
        }
        
        return predictedCategory
    }
    
    /**
     * Check learned patterns from user corrections
     */
    private fun checkLearnedPatterns(description: String, context: Context): String? {
        try {
            val prefs = context.getSharedPreferences("FinPulsePrefs", Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_VOICE_PATTERNS, null) ?: return null
            
            val type = object : TypeToken<Map<String, String>>() {}.type
            val patterns: Map<String, String> = Gson().fromJson(json, type)
            
            // Check for exact match
            if (patterns.containsKey(description)) {
                return patterns[description]
            }
            
            // Check for partial match (if description contains learned pattern)
            for ((pattern, category) in patterns) {
                if (description.contains(pattern) || pattern.contains(description)) {
                    return category
                }
            }
            
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking learned patterns: ${e.message}")
            return null
        }
    }
    
    /**
     * Train the AI with user corrections
     */
    fun trainPattern(context: Context, description: String, correctCategory: String) {
        try {
            val prefs = context.getSharedPreferences("FinPulsePrefs", Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_VOICE_PATTERNS, null)
            
            val type = object : TypeToken<MutableMap<String, String>>() {}.type
            val patterns: MutableMap<String, String> = if (json != null) {
                Gson().fromJson(json, type)
            } else {
                mutableMapOf()
            }
            
            val normalizedDesc = description.lowercase(Locale.getDefault()).trim()
            patterns[normalizedDesc] = correctCategory
            
            prefs.edit()
                .putString(KEY_VOICE_PATTERNS, Gson().toJson(patterns))
                .apply()
            
            Log.d(TAG, "Trained pattern: $normalizedDesc -> $correctCategory")
        } catch (e: Exception) {
            Log.e(TAG, "Error training pattern: ${e.message}")
        }
    }
    
    /**
     * Get all supported categories
     */
    fun getSupportedCategories(): List<String> {
        return categoryKeywords.keys.toList() + "Miscellaneous"
    }
}
