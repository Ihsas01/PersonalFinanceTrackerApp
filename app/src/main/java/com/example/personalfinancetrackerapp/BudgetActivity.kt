package com.example.personalfinancetrackerapp

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.personalfinancetrackerapp.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BudgetActivity : AppCompatActivity() {

    private lateinit var inputBudget: EditText
    private lateinit var btnSave: Button
    private lateinit var btnReset: Button
    private lateinit var tvCurrentBudget: TextView
    private lateinit var tvTotalSpent: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvWarning: TextView

    private var budget: Float = 0f
    private var totalSpent: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        // Back button functionality
        val backButton = findViewById<ImageButton>(R.id.btnBack)
        backButton.setOnClickListener {
            finish() // This finishes the activity and returns to the previous one
        }

        inputBudget = findViewById(R.id.inputBudget)
        btnSave = findViewById(R.id.btnSaveBudget)
        btnReset = findViewById(R.id.btnResetBudget)
        tvCurrentBudget = findViewById(R.id.tvCurrentBudget)
        tvTotalSpent = findViewById(R.id.tvTotalSpent)
        progressBar = findViewById(R.id.progressBar)
        tvWarning = findViewById(R.id.tvWarning)

        progressBar.visibility = ProgressBar.GONE  // 🧹 Remove spinner

        loadBudget()
        calculateTotalSpent()
        updateUI()

        btnSave.setOnClickListener {
            val input = inputBudget.text.toString().toFloatOrNull()
            if (input == null || input <= 0) {
                Toast.makeText(this, "Enter a valid budget", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            budget = input
            getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
                .edit().putFloat("budget", budget).apply()

            updateUI()
            Toast.makeText(this, "Budget Saved ✅", Toast.LENGTH_SHORT).show()
        }

        btnReset.setOnClickListener {
            val prefs = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
            prefs.edit().remove("budget").apply()
            budget = 0f
            updateUI()
            Toast.makeText(this, "Budget has been reset", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadBudget() {
        val prefs = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
        budget = prefs.getFloat("budget", 0f)
    }

    private fun calculateTotalSpent() {
        val prefs = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("transactions", null)
        val type = object : TypeToken<List<Transaction>>() {}.type
        val transactions: List<Transaction> = if (json != null) Gson().fromJson(json, type) else emptyList()
        totalSpent = transactions.sumOf { it.amount }.toFloat()
    }

    private fun updateUI() {
        tvCurrentBudget.text = if (budget == 0f) "No budget set" else "Current Budget: $${"%.2f".format(budget)}"
        tvTotalSpent.text = "Total Spent: $${"%.2f".format(totalSpent)}"

        val percentageUsed = if (budget > 0) (totalSpent / budget) * 100 else 0f
        progressBar.progress = percentageUsed.toInt()

        tvWarning.text = when {
            budget == 0f -> "⚠️ Budget not set!"
            percentageUsed > 100 -> "\uD83D\uDEAB Oops! Budget Exceeded!"
            percentageUsed > 80 -> "\uD83C\uDF15 Heads Up! Approaching Your Limit"
            else -> ""
        }
    }
}
