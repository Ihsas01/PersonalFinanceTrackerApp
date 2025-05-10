package com.example.personalfinancetrackerapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LaunchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        // Find the Continue to App button
        val btnContinueToApp = findViewById<Button>(R.id.btnContinueToApp)
        
        // Set click listener to navigate to MainActivity
        btnContinueToApp.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close the launch activity so user can't go back to it
        }
    }
}