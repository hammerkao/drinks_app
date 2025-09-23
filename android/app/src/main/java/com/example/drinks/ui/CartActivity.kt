package com.example.drinks.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.store.CartManager

class CartActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_cart)

        val rv = findViewById<RecyclerView>(R.id.rvCart)
        val tvTotal = findViewById<TextView>(R.id.tvTotal)
        val btnCheckout = findViewById<Button>(R.id.btnCheckout)

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = CartAdapter(CartManager.lines) { key, delta ->
            val idx = CartManager.lines.indexOfFirst { line -> line.lineKey == key } // ← 具名參數 line
            if (idx >= 0) {
                val line = CartManager.lines[idx]
                line.qty = (line.qty + delta).coerceAtLeast(0)
                if (line.qty == 0) CartManager.lines.removeAt(idx)
                rv.adapter?.notifyDataSetChanged()
                tvTotal.text = "總計：NT$${CartManager.total()}"
            }
        }

        tvTotal.text = "總計：NT$${CartManager.total()}"
        btnCheckout.setOnClickListener {
            startActivity(Intent(this, CheckoutActivity::class.java))
        }
    }
}
