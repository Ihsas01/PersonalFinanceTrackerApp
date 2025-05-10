package com.example.personalfinancetrackerapp.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPrefHelper(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)

    fun saveBudget(amount: Float) {
        prefs.edit().putFloat("budget", amount).apply()
    }

    fun getBudget(): Float {
        return prefs.getFloat("budget", 0f)
    }

    fun saveCurrency(currency: String) {
        prefs.edit().putString("currency", currency).apply()
    }

    fun getCurrency(): String {
        return prefs.getString("currency", "$") ?: "$"
    }
}