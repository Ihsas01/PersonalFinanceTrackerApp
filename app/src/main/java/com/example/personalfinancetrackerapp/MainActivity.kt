package com.example.personalfinancetrackerapp

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.personalfinancetrackerapp.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.OutputStream

import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {

    private lateinit var tvWarningMain: TextView

    private val importJsonLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                importJsonFromUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val welcomeText = findViewById<TextView>(R.id.tvWelcome)
        tvWarningMain = findViewById(R.id.tvBudgetWarningMain)
        welcomeText.text = "WELCOME TO SMART BUDGET"

        val btnAdd = findViewById<Button>(R.id.btnAddTransaction)
        val btnView = findViewById<Button>(R.id.btnViewTransactions)
        val btnChart = findViewById<Button>(R.id.btnChart)
        val btnBudget = findViewById<Button>(R.id.btnBudget)
        val btnExport = findViewById<Button>(R.id.btnExport)
        val btnImport = findViewById<Button>(R.id.btnImport)

        btnAdd.setOnClickListener {
            startActivity(Intent(this, TransactionActivity::class.java))
        }

        btnView.setOnClickListener {
            startActivity(Intent(this, TransactionListActivity::class.java))
        }

        btnChart.setOnClickListener {
            startActivity(Intent(this, CategoryChartActivity::class.java))
        }

        btnBudget.setOnClickListener {
            startActivity(Intent(this, BudgetActivity::class.java))
        }

        btnExport.setOnClickListener {
            exportTransactions()
        }

        btnImport.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            importJsonLauncher.launch(intent)
        }

        createNotificationChannel() // 📢 Create channel on app start

        // 🔔 Ask for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        val btnReminderSettings = findViewById<Button>(R.id.btnReminderSettings)
        btnReminderSettings.setOnClickListener {
            startActivity(Intent(this, ReminderSettingsActivity::class.java))
        }


    }

    override fun onResume() {
        super.onResume()
        showBudgetWarning(tvWarningMain)
    }

    private fun showBudgetWarning(warningTextView: TextView) {
        val prefs = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
        val budget = prefs.getFloat("budget", 0f)

        val json = prefs.getString("transactions", null)
        val type = object : TypeToken<List<Transaction>>() {}.type
        val transactions: List<Transaction> = if (json != null) Gson().fromJson(json, type) else emptyList()

        val totalSpent = transactions.sumOf { it.amount }.toFloat()
        val percentageUsed = if (budget > 0) (totalSpent / budget) * 100 else 0f

        val warningText = when {
            budget == 0f -> "❓ Oops! No Budget Set."
            percentageUsed > 100 -> {
                sendBudgetNotification("\uD83D\uDEAB Oops! Budget Exceeded!")
                "\uD83D\uDEAB Oops! Budget Exceeded!"
            }
            percentageUsed > 80 -> {
                sendBudgetNotification("\uD83C\uDF15 Heads Up! Approaching Your Limit")
                "\uD83C\uDF15 Heads Up! Approaching Your Limit"
            }
            else -> ""
        }

        warningTextView.text = warningText
    }

    private fun sendBudgetNotification(message: String) {
        val builder = NotificationCompat.Builder(this, "budget_alerts")
            .setSmallIcon(R.mipmap.logo1)
            .setContentTitle("❤ Smart Spending Alert! ❤")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(this)) {
            notify(1001, builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "budget_alerts",
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for approaching or exceeded budgets"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun exportTransactions() {
        val prefs = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("transactions", null)

        if (json == null) {
            Toast.makeText(this, "No transactions to export", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "transactions_backup.json"

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            val outputStream: OutputStream? = contentResolver.openOutputStream(uri)
            outputStream?.use {
                it.write(json.toByteArray())
                it.flush()
                Toast.makeText(this, "✅ Exported to Downloads/$fileName", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "❌ Failed to export", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importJsonFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val json = inputStream?.bufferedReader().use { it?.readText() }

            if (!json.isNullOrEmpty()) {
                getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
                    .edit().putString("transactions", json).apply()

                Toast.makeText(this, "✅ Transactions imported!", Toast.LENGTH_SHORT).show()
                showBudgetWarning(tvWarningMain)
            } else {
                Toast.makeText(this, "⚠️ File is empty", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Failed to import: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun importTransactions() {
        val file = File(filesDir, "transactions_backup.json")
        if (file.exists()) {
            val json = file.readText()
            getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
                .edit().putString("transactions", json).apply()

            Toast.makeText(this, "Transactions imported from backup", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No backup file found", Toast.LENGTH_SHORT).show()
        }
    }
}
