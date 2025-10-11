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
import retrofit2.HttpException
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private lateinit var etPhone: EditText
    private lateinit var etRegPassword: EditText
    private lateinit var progress: ProgressBar
    private val authApi: AuthApi by lazy { NetCore.getRetrofit(this).create(AuthApi::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etPhone = findViewById(R.id.etPhone)
        etRegPassword = findViewById(R.id.etRegPassword)
        progress = findViewById(R.id.progress)

        findViewById<View>(R.id.btnClear).setOnClickListener {
            etPhone.text?.clear()
            etRegPassword.text?.clear()
        }

        findViewById<View>(R.id.btnRegister).setOnClickListener {
            val phone = etPhone.text.toString().trim()
            val pwd = etRegPassword.text.toString()
            if (phone.isEmpty() || pwd.isEmpty()) {
                Toast.makeText(this, "請輸入手機與密碼", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            doRegister(phone, pwd)
        }
    }

    private fun doRegister(phone: String, pwd: String) {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val body = mapOf("phone" to phone, "password" to pwd)
                val resp = authApi.register(body)

                // 兼容：access/refresh 或舊版 token 欄位
                val access = resp.access ?: resp.legacyToken
                val refresh = resp.refresh

                // 註冊成功後直接帶登入狀態（可選）
                if (!access.isNullOrEmpty()) {
                    TokenStore.set(access, refresh)
                }

                // 依你原本流程前往成功頁（或直接進主畫面也可）
                startActivity(Intent(this@RegisterActivity, RegisterSuccessActivity::class.java))
                finish()

            } catch (e: HttpException) {
                val msg = "註冊失敗：HTTP ${e.code()}"
                Toast.makeText(this@RegisterActivity, msg, Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(this@RegisterActivity, "網路錯誤，請檢查連線", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, "發生錯誤：${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }
}
