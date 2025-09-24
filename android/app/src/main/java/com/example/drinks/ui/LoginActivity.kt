package com.example.drinks.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.drinks.R
import com.example.drinks.net.AuthApi
import com.example.drinks.net.NetCore
import com.example.drinks.store.TokenStore
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var progress: ProgressBar
    private val authApi: AuthApi by lazy { NetCore.getRetrofit(this).create(AuthApi::class.java) }
    private val tokenStore by lazy { TokenStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        progress = findViewById(R.id.progress)

        findViewById<View>(R.id.btnGoRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        findViewById<View>(R.id.btnLogin).setOnClickListener {
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString()
            if (phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "請輸入手機與密碼", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            doLogin(phone, password)
        }
    }

    private fun doLogin(phone: String, password: String) {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = authApi.login(mapOf("phone" to phone, "password" to password))
                tokenStore.save(resp.token)
                Toast.makeText(this@LoginActivity, "登入成功", Toast.LENGTH_SHORT).show()
                // TODO: 導頁
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "登入失敗：${e.message}", Toast.LENGTH_SHORT).show()
            } finally { progress.visibility = View.GONE }
        }
    }
}
