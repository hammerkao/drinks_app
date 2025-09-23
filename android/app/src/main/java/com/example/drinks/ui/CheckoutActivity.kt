package com.example.drinks.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.drinks.R
import com.example.drinks.store.CartManager
import kotlin.jvm.java

class CheckoutActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_checkout)

        val tvPayable = findViewById<TextView>(R.id.tvPayable)
        tvPayable.text = "應付金額：NT$${CartManager.total()} (現金，自取)"

        findViewById<Button>(R.id.btnPlaceOrder).setOnClickListener {
            CartManager.clear()
            startActivity(Intent(this, SuccessActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
    }
}
