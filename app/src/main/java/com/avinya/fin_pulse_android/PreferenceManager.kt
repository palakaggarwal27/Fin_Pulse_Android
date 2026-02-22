package com.avinya.fin_pulse_android

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PreferenceManager {
    private const val PREF_NAME = "FinPulsePrefs"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_BANK_BALANCE = "bank_balance"
    private const val KEY_CASH_ON_HAND = "cash_on_hand"
    private const val KEY_PROFILE_IMAGE_URI = "profile_image_uri"
    private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    private const val KEY_ALLOWED_PACKAGES = "allowed_packages"

    private val defaultAllowedPackages = listOf(
        "com.phonepe.app", 
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "net.one97.paytm",
        "com.freecharge.android",
        "com.mobikwik_new",
        "com.upi.address.book", // BHIM
        "com.whatsapp", // Added by default, but user can remove
        "com.truecaller"
    )

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveUserData(context: Context, name: String, bank: Float, cash: Float, imageUri: String?) {
        getPreferences(context).edit().apply {
            putString(KEY_USER_NAME, name)
            putFloat(KEY_BANK_BALANCE, bank)
            putFloat(KEY_CASH_ON_HAND, cash)
            putString(KEY_PROFILE_IMAGE_URI, imageUri)
            apply()
        }
    }

    fun getUserName(context: Context): String {
        return getPreferences(context).getString(KEY_USER_NAME, "") ?: ""
    }

    fun getBankBalance(context: Context): Float {
        return getPreferences(context).getFloat(KEY_BANK_BALANCE, 0f)
    }

    fun getCashOnHand(context: Context): Float {
        return getPreferences(context).getFloat(KEY_CASH_ON_HAND, 0f)
    }

    fun getProfileImageUri(context: Context): String? {
        return getPreferences(context).getString(KEY_PROFILE_IMAGE_URI, null)
    }

    fun setOnboardingComplete(context: Context, isComplete: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_ONBOARDING_COMPLETE, isComplete).apply()
    }

    fun isOnboardingComplete(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    fun getAllowedPackages(context: Context): List<String> {
        val json = getPreferences(context).getString(KEY_ALLOWED_PACKAGES, null)
        return if (json != null) {
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
        } else {
            defaultAllowedPackages
        }
    }

    fun saveAllowedPackages(context: Context, packages: List<String>) {
        val json = Gson().toJson(packages)
        getPreferences(context).edit().putString(KEY_ALLOWED_PACKAGES, json).apply()
    }

    fun isPackageAllowed(context: Context, packageName: String): Boolean {
        val allowed = getAllowedPackages(context)
        return allowed.contains(packageName)
    }

    fun clearAll(context: Context) {
        getPreferences(context).edit().clear().apply()
    }
}
