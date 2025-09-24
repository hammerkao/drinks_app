package com.example.drinks.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.drinks.R
import com.google.android.material.button.MaterialButton

class RegisterSuccessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_success)

        findViewById<MaterialButton>(R.id.btnOk).setOnClickListener {
            // 回登入或主頁，暫時回上一頁
            finish()
        }
    }
}
