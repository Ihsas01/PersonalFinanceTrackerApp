package com.example.personalfinancetrackerapp

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.personalfinancetrackerapp.model.Transaction
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CategoryChartActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_chart)

        // Back button functionality
        val backButton = findViewById<ImageButton>(R.id.btnBack)
        backButton.setOnClickListener {
            finish() // This finishes the activity and returns to the previous one
        }

        pieChart = findViewById(R.id.pieChart)

        val transactions = loadTransactions()
        val categoryTotals = calculateTotals(transactions)

        setupPieChart(categoryTotals)
    }

    private fun loadTransactions(): List<Transaction> {
        val prefs = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("transactions", null)
        val type = object : TypeToken<List<Transaction>>() {}.type
        return if (json != null) Gson().fromJson(json, type) else emptyList()
    }

    private fun calculateTotals(transactions: List<Transaction>): Map<String, Float> {
        val categoryMap = mutableMapOf<String, Float>()

        for (t in transactions) {
            val current = categoryMap[t.category] ?: 0f
            categoryMap[t.category] = current + t.amount.toFloat()
        }

        return categoryMap
    }

    private fun setupPieChart(categoryTotals: Map<String, Float>) {
        val entries = ArrayList<PieEntry>()
        for ((category, amount) in categoryTotals) {
            entries.add(PieEntry(amount, category))
        }

        val dataSet = PieDataSet(entries, "Category-wise Spending")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 14f

        val data = PieData(dataSet)

        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.centerText = "Spending by Category"
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
}
