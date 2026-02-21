package com.avinya.fin_pulse_android

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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

    val defaultCategories = listOf(
        "Food & Dining", "Transport", "Shopping", "Entertainment",
        "Bills & Utilities", "Stationery", "Health & Wellness", "Gifts", "Income", "Miscellaneous"
    )

    private val baseKnowledge = mapOf(
        "Food & Dining" to listOf("starbucks", "domino", "swiggy", "zomato", "eat", "restaurant", "cafe", "dinner", "lunch", "breakfast", "burger", "pizza"),
        "Transport" to listOf("uber", "ola", "taxi", "cab", "metro", "petrol", "fuel", "train", "flight", "rapido"),
        "Shopping" to listOf("amazon", "flipkart", "shop", "mall", "grocery", "mart", "clothes", "zara", "h&m"),
        "Entertainment" to listOf("netflix", "hotstar", "movie", "ticket", "game", "pvr", "concert"),
        "Bills & Utilities" to listOf("rent", "bill", "electricity", "water", "recharge", "wifi", "gas"),
        "Stationery" to listOf("notebook", "pen", "pencil", "paper", "books", "office", "print"),
        "Income" to listOf("salary", "bonus", "transfer", "refund", "credit", "cashback")
    )

    private fun getPreferences(context: Context) = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

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

    fun predictCategory(context: Context, description: String, isCredit: Boolean): String {
        if (isCredit) return "Income"
        val desc = description.lowercase().trim()
        if (desc.isEmpty()) return "Miscellaneous"

        val learnedData = getLearnedData(context)
        for ((keyword, category) in learnedData) {
            if (desc.contains(keyword)) return category
        }

        for ((category, keywords) in baseKnowledge) {
            if (keywords.any { desc.contains(it) }) return category
        }

        return "Miscellaneous"
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

    fun deleteExpense(context: Context, expense: Expense, updateBalance: Boolean) {
        val expenses = getExpenses(context).toMutableList()
        expenses.removeAll { it.id == expense.id }
        saveExpenses(context, expenses)

        if (updateBalance) {
            val currentBank = PreferenceManager.getBankBalance(context)
            val currentCash = PreferenceManager.getCashOnHand(context)
            val multiplier = if (expense.isCredit) -1 else 1 // Restore means subtract credit or add debit
            
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

    fun clearAll(context: Context) {
        getPreferences(context).edit().clear().apply()
    }
}
