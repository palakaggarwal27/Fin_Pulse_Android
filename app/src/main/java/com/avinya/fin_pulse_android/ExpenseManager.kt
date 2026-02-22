package com.avinya.fin_pulse_android

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

data class Expense(
    val id: Long = System.currentTimeMillis(),
    val amount: Float,
    val description: String,
    var category: String,
    val type: String, // "Digital" or "Cash"
    val isCredit: Boolean = false, // true for money received
    val timestamp: Long = System.currentTimeMillis()
)

object ExpenseManager {
    private const val PREF_NAME = "FinPulseExpenses"
    private const val KEY_EXPENSES = "expenses_list"
    private const val KEY_LEARNED_AI = "learned_ai_data"
    private const val KEY_CUSTOM_CATEGORIES = "custom_categories"
    private const val KEY_UPI_MAPPINGS = "upi_mappings"
    private const val KEY_NON_TRANSACTIONS = "non_transactions_patterns"
    private const val KEY_CONFIRMED_TRANSACTIONS = "confirmed_transactions_patterns"
    private const val KEY_CREDIT_PATTERNS = "credit_patterns"
    private const val KEY_DEBIT_PATTERNS = "debit_patterns"

    val defaultCategories = listOf(
        "Food & Dining", "Transport", "Shopping", "Entertainment",
        "Bills & Utilities", "Stationery", "Health & Wellness", "Gifts", 
        "Salary", "Bonus", "Refund", "Rent Income", "Miscellaneous"
    )

    private val baseKnowledge = mapOf(
        "Food & Dining" to listOf("starbucks", "domino", "swiggy", "zomato", "eat", "restaurant", "cafe", "dinner", "lunch", "breakfast", "burger", "pizza"),
        "Transport" to listOf("uber", "ola", "taxi", "cab", "metro", "petrol", "fuel", "train", "flight", "rapido"),
        "Shopping" to listOf("amazon", "flipkart", "shop", "mall", "grocery", "mart", "clothes", "zara", "h&m"),
        "Entertainment" to listOf("netflix", "hotstar", "movie", "ticket", "game", "pvr", "concert"),
        "Bills & Utilities" to listOf("rent", "bill", "electricity", "water", "recharge", "wifi", "gas"),
        "Stationery" to listOf("notebook", "pen", "pencil", "paper", "books", "office", "print"),
        "Salary" to listOf("salary", "stipend", "payroll"),
        "Bonus" to listOf("bonus", "incentive"),
        "Refund" to listOf("refund", "cashback", "reversal")
    )

    private val transactionKeywords = listOf(
        "paid", "received", "spent", "debited", "credited", "sent", "transfer", 
        "deposited", "withdrawn", "payment", "transaction", "vpa", "upi"
    )

    private fun getPreferences(context: Context) = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isLikelyTransaction(context: Context, text: String): Boolean {
        val lowerText = text.lowercase()
        
        val pretrainedNon = getPretrainedNonTransactions(context)
        if (pretrainedNon.any { lowerText.contains(it) }) return false
        
        val pretrainedConf = getPretrainedConfirmedTransactions(context)
        if (pretrainedConf.any { lowerText.contains(it) }) return true

        val nonTransactions = getNonTransactionPatterns(context)
        if (nonTransactions.any { lowerText.contains(it) }) return false

        val confirmedTransactions = getConfirmedTransactionPatterns(context)
        if (confirmedTransactions.any { lowerText.contains(it) }) return true

        return transactionKeywords.any { lowerText.contains(it) }
    }

    fun isCreditTransaction(context: Context, text: String, defaultGuess: Boolean): Boolean {
        val lowerText = text.lowercase()
        val pattern = extractPattern(text)

        // Check local learned patterns
        val creditPatterns = getLearnedPatterns(context, KEY_CREDIT_PATTERNS)
        if (creditPatterns.contains(pattern)) return true
        
        val debitPatterns = getLearnedPatterns(context, KEY_DEBIT_PATTERNS)
        if (debitPatterns.contains(pattern)) return false

        // Check pre-trained patterns from assets (if you decide to add them there)
        val pretrainedCredit = loadFromAssets(context, "ai_credit_patterns.json")
        if (pretrainedCredit.any { lowerText.contains(it) }) return true
        
        val pretrainedDebit = loadFromAssets(context, "ai_debit_patterns.json")
        if (pretrainedDebit.any { lowerText.contains(it) }) return false

        return defaultGuess
    }

    // --- Pre-trained knowledge from Assets ---
    private var cachedPretrainedNon: List<String>? = null
    private var cachedPretrainedConf: List<String>? = null

    private fun getPretrainedNonTransactions(context: Context): List<String> {
        if (cachedPretrainedNon != null) return cachedPretrainedNon!!
        cachedPretrainedNon = loadFromAssets(context, "ai_non_transactions.json")
        return cachedPretrainedNon!!
    }

    private fun getPretrainedConfirmedTransactions(context: Context): List<String> {
        if (cachedPretrainedConf != null) return cachedPretrainedConf!!
        cachedPretrainedConf = loadFromAssets(context, "ai_confirmed_transactions.json")
        return cachedPretrainedConf!!
    }

    private fun loadFromAssets(context: Context, fileName: String): List<String> {
        return try {
            context.assets.open(fileName).use { inputStream ->
                val type = object : TypeToken<List<String>>() {}.type
                Gson().fromJson(InputStreamReader(inputStream), type) ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun trainNonTransaction(context: Context, text: String) {
        val pattern = extractPattern(text)
        if (pattern.isEmpty()) return
        val current = getNonTransactionPatterns(context).toMutableSet()
        current.add(pattern)
        getPreferences(context).edit().putString(KEY_NON_TRANSACTIONS, Gson().toJson(current.toList())).apply()
    }

    fun trainConfirmedTransaction(context: Context, text: String, isCredit: Boolean) {
        val pattern = extractPattern(text)
        if (pattern.isEmpty()) return
        
        // 1. Confirm it is a transaction
        val currentConf = getConfirmedTransactionPatterns(context).toMutableSet()
        currentConf.add(pattern)
        getPreferences(context).edit().putString(KEY_CONFIRMED_TRANSACTIONS, Gson().toJson(currentConf.toList())).apply()

        // 2. Remove from non-transactions
        val nonTransactions = getNonTransactionPatterns(context).toMutableSet()
        if (nonTransactions.remove(pattern)) {
             getPreferences(context).edit().putString(KEY_NON_TRANSACTIONS, Gson().toJson(nonTransactions.toList())).apply()
        }

        // 3. Train Direction (Credit/Debit)
        val creditPatterns = getLearnedPatterns(context, KEY_CREDIT_PATTERNS).toMutableSet()
        val debitPatterns = getLearnedPatterns(context, KEY_DEBIT_PATTERNS).toMutableSet()

        if (isCredit) {
            creditPatterns.add(pattern)
            debitPatterns.remove(pattern)
        } else {
            debitPatterns.add(pattern)
            creditPatterns.remove(pattern)
        }

        getPreferences(context).edit().apply {
            putString(KEY_CREDIT_PATTERNS, Gson().toJson(creditPatterns.toList()))
            putString(KEY_DEBIT_PATTERNS, Gson().toJson(debitPatterns.toList()))
            apply()
        }
    }

    private fun extractPattern(text: String): String {
        return text.lowercase()
            .replace(Regex("\\d+"), "") 
            .replace(Regex("(rs\\.?|inr|₹)\\s*"), "") 
            .trim()
            .take(50) 
    }

    private fun getNonTransactionPatterns(context: Context): List<String> {
        val json = getPreferences(context).getString(KEY_NON_TRANSACTIONS, null) ?: return emptyList()
        return Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
    }

    private fun getConfirmedTransactionPatterns(context: Context): List<String> {
        val json = getPreferences(context).getString(KEY_CONFIRMED_TRANSACTIONS, null) ?: return emptyList()
        return Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
    }

    private fun getLearnedPatterns(context: Context, key: String): List<String> {
        val json = getPreferences(context).getString(key, null) ?: return emptyList()
        return Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
    }

    fun predictCategory(context: Context, description: String, isCredit: Boolean): String {
        val desc = description.lowercase().trim()
        if (desc.isEmpty()) return "Miscellaneous"
        val learnedData = getLearnedData(context)
        for ((keyword, category) in learnedData) { if (desc.contains(keyword)) return category }
        for ((category, keywords) in baseKnowledge) { if (keywords.any { desc.contains(it) }) return category }
        return if (isCredit) "Salary" else "Miscellaneous"
    }

    fun trainAI(context: Context, description: String, correctCategory: String) {
        val desc = description.lowercase().trim()
        if (desc.isEmpty()) return
        val learnedData = getLearnedData(context).toMutableMap()
        learnedData[desc] = correctCategory
        getPreferences(context).edit().putString(KEY_LEARNED_AI, Gson().toJson(learnedData)).apply()
        if (!defaultCategories.contains(correctCategory)) {
            val currentCustom = getCustomCategories(context).toMutableList()
            if (!currentCustom.contains(correctCategory)) {
                currentCustom.add(correctCategory)
                getPreferences(context).edit().putString(KEY_CUSTOM_CATEGORIES, Gson().toJson(currentCustom)).apply()
            }
        }
    }

    fun trainUpiMapping(context: Context, upiId: String, name: String) {
        if (upiId.isEmpty() || name.isEmpty()) return
        val mappings = getUpiMappings(context).toMutableMap()
        mappings[upiId.lowercase()] = name
        getPreferences(context).edit().putString(KEY_UPI_MAPPINGS, Gson().toJson(mappings)).apply()
    }

    fun getLearnedNameForUpi(context: Context, upiId: String): String? {
        return getUpiMappings(context)[upiId.lowercase()]
    }

    private fun getUpiMappings(context: Context): Map<String, String> {
        val json = getPreferences(context).getString(KEY_UPI_MAPPINGS, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, String>>() {}.type
        return Gson().fromJson(json, type)
    }

    private fun getLearnedData(context: Context): Map<String, String> {
        val json = getPreferences(context).getString(KEY_LEARNED_AI, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, String>>() {}.type
        return Gson().fromJson(json, type)
    }

    private fun getCustomCategories(context: Context): List<String> {
        val json = getPreferences(context).getString(KEY_CUSTOM_CATEGORIES, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun addExpense(context: Context, expense: Expense) {
        val expenses = getExpenses(context).toMutableList()
        expenses.add(0, expense)
        saveExpenses(context, expenses)
        val currentBank = PreferenceManager.getBankBalance(context)
        val currentCash = PreferenceManager.getCashOnHand(context)
        val multiplier = if (expense.isCredit) 1 else -1
        if (expense.type == "Digital") {
            PreferenceManager.saveUserData(context, PreferenceManager.getUserName(context), currentBank + (multiplier * expense.amount), currentCash, PreferenceManager.getProfileImageUri(context))
        } else {
            PreferenceManager.saveUserData(context, PreferenceManager.getUserName(context), currentBank, currentCash + (multiplier * expense.amount), PreferenceManager.getProfileImageUri(context))
        }
    }

    fun updateExpenseCategory(context: Context, expenseId: Long, newCategory: String) {
        val expenses = getExpenses(context).toMutableList()
        val index = expenses.indexOfFirst { it.id == expenseId }
        if (index != -1) {
            expenses[index].category = newCategory
            saveExpenses(context, expenses)
            trainAI(context, expenses[index].description, newCategory)
        }
    }

    fun deleteExpense(context: Context, expense: Expense, updateBalance: Boolean) {
        val expenses = getExpenses(context).toMutableList()
        expenses.removeAll { it.id == expense.id }
        saveExpenses(context, expenses)
        if (updateBalance) {
            val currentBank = PreferenceManager.getBankBalance(context)
            val currentCash = PreferenceManager.getCashOnHand(context)
            val multiplier = if (expense.isCredit) -1 else 1
            if (expense.type == "Digital") {
                PreferenceManager.saveUserData(context, PreferenceManager.getUserName(context), currentBank + (multiplier * expense.amount), currentCash, PreferenceManager.getProfileImageUri(context))
            } else {
                PreferenceManager.saveUserData(context, PreferenceManager.getUserName(context), currentBank, currentCash + (multiplier * expense.amount), PreferenceManager.getProfileImageUri(context))
            }
        }
    }

    private fun saveExpenses(context: Context, expenses: List<Expense>) {
        val json = Gson().toJson(expenses)
        getPreferences(context).edit().putString(KEY_EXPENSES, json).apply()
    }

    fun getExpenses(context: Context): List<Expense> {
        val json = getPreferences(context).getString(KEY_EXPENSES, null) ?: return emptyList()
        val type = object : TypeToken<List<Expense>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun getCategories(context: Context): List<String> {
        val json = getPreferences(context).getString(KEY_CUSTOM_CATEGORIES, null)
        val custom: List<String> = if (json != null) {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
        return (defaultCategories + custom).distinct()
    }

    fun clearAll(context: Context) {
        getPreferences(context).edit().clear().apply()
    }
}
