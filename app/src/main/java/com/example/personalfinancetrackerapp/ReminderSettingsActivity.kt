package com.example.personalfinancetrackerapp

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.personalfinancetrackerapp.utils.NotificationHelper
import java.util.*

class ReminderSettingsActivity : AppCompatActivity() {

    private lateinit var tvTime: TextView

    // Permission request launcher for notifications on Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, proceed with scheduling
            val prefs = getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE)
            val hour = prefs.getInt("hour", -1)
            val minute = prefs.getInt("minute", -1)
            
            if (hour != -1 && minute != -1) {
                scheduleReminder(hour, minute)
                Toast.makeText(this, "Reminder scheduled", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Notification permission denied. Reminders won't work.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder_settings)

        // Back button functionality
        val backButton = findViewById<ImageButton>(R.id.btnBack)
        backButton.setOnClickListener {
            finish() // This finishes the activity and returns to the previous one
        }

        // Create notification channel early
        NotificationHelper.createNotificationChannel(this)

        val btnSetTime = findViewById<Button>(R.id.btnSetTime)
        tvTime = findViewById(R.id.tvCurrentTime)

        // Check for notification permission
        checkNotificationPermission()
        
        loadSavedTime()

        btnSetTime.setOnClickListener {
            val now = Calendar.getInstance()
            val hour = now.get(Calendar.HOUR_OF_DAY)
            val minute = now.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                saveReminderTime(selectedHour, selectedMinute)
                tvTime.text = "Reminder set for: %02d:%02d".format(selectedHour, selectedMinute)
                
                // Check permission before scheduling
                if (hasNotificationPermission()) {
                    scheduleReminder(selectedHour, selectedMinute)
                    Toast.makeText(this, "Daily reminder set", Toast.LENGTH_SHORT).show()
                } else {
                    requestNotificationPermission()
                }
            }, hour, minute, true).show()
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // For versions below Android 13, no runtime permission needed
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                requestNotificationPermission()
            }
        }
    }

    private fun saveReminderTime(hour: Int, minute: Int) {
        val prefs = getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("hour", hour).putInt("minute", minute).apply()
    }

    private fun loadSavedTime() {
        val prefs = getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE)
        val hour = prefs.getInt("hour", -1)
        val minute = prefs.getInt("minute", -1)

        if (hour != -1) {
            tvTime.text = "Reminder set for: %02d:%02d".format(hour, minute)
            
            if (hasNotificationPermission()) {
                scheduleReminder(hour, minute)
            }
        }
    }

    private fun scheduleReminder(hour: Int, minute: Int) {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, ReminderReceiver::class.java)
            
            // Use unique request code based on time to avoid conflicts
            val requestCode = (hour * 60 + minute)
            
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Cancel any existing alarms with the same ID
            alarmManager.cancel(pendingIntent)

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            // Use EXACT scheduling where available
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP, 
                    calendar.timeInMillis, 
                    pendingIntent
                )
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting reminder: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
