package com.example.personalfinancetrackerapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.personalfinancetrackerapp.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TransactionAdapter(
    private val transactions: MutableList<Transaction>
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvCategoryDate: TextView = itemView.findViewById(R.id.tvCategoryDate)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]

        holder.tvTitle.text = transaction.title
        holder.tvCategoryDate.text = "${transaction.category} - ${transaction.date}"
        holder.tvAmount.text = "Amount: $${transaction.amount}"

        holder.btnEdit.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, TransactionActivity::class.java).apply {
                putExtra("editTransaction", Gson().toJson(transaction))
                putExtra("position", position)
            }
            context.startActivity(intent)
        }

        holder.btnDelete.setOnClickListener {
            val context = holder.itemView.context
            val prefs = context.getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
            val gson = Gson()

            transactions.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, transactions.size)

            val updatedJson = gson.toJson(transactions)
            prefs.edit().putString("transactions", updatedJson).apply()

            Toast.makeText(context, "Transaction Deleted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = transactions.size
}
