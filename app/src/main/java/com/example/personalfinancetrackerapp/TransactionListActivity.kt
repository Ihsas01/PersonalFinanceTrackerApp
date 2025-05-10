package com.example.personalfinancetrackerapp

import android.content.Context
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.personalfinancetrackerapp.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TransactionListActivity : AppCompatActivity() {

    private lateinit var transactionRecycler: RecyclerView
    private lateinit var transactionList: MutableList<Transaction>
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_list)

        // Back button functionality
        val backButton = findViewById<ImageButton>(R.id.btnBack)
        backButton.setOnClickListener {
            finish() // This finishes the activity and returns to the previous one
        }

        transactionRecycler = findViewById(R.id.recyclerTransactions)
        transactionRecycler.layoutManager = LinearLayoutManager(this)

        transactionList = loadTransactions()

        adapter = TransactionAdapter(transactionList)
        transactionRecycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        reloadTransactions()
    }

    private fun reloadTransactions() {
        val sharedPref = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
        val json = sharedPref.getString("transactions", null)
        val type = object : TypeToken<MutableList<Transaction>>() {}.type

        val updatedList: MutableList<Transaction> = if (json != null) {
            Gson().fromJson(json, type)
        } else {
            mutableListOf()
        }

        transactionList.clear()
        transactionList.addAll(updatedList)
        adapter.notifyDataSetChanged()
    }

    private fun loadTransactions(): MutableList<Transaction> {
        val sharedPref = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
        val json = sharedPref.getString("transactions", null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<Transaction>>() {}.type
            Gson().fromJson(json, type)
        } else {
            mutableListOf()
        }
    }
}
