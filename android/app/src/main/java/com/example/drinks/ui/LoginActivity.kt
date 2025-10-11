package com.example.drinks.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

class LoginActivity : AppCompatActivity() {

    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var progress: ProgressBar
    private val authApi: AuthApi by lazy { NetCore.getRetrofit(this).create(AuthApi::class.java) }

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
        // 乾淨起見：先清掉舊 token（避免帶舊憑證）
        TokenStore.clear()

        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                // 依你的後端：使用 phone/password 登入
                val resp = authApi.login(mapOf("phone" to phone, "password" to password))

                // 兼容：access/refresh 或舊版 token 欄位
                val access = resp.access ?: resp.legacyToken
                val refresh = resp.refresh
                if (access.isNullOrEmpty()) {
                    Toast.makeText(this@LoginActivity, "登入回應缺少 access token", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 存到 TokenStore（object 版本）
                TokenStore.set(access, refresh)

                // （可選）若要自動登入，可另外持久化到 SharedPreferences，再在 App 啟動時讀回 TokenStore

                // 進入主畫面
                val it = Intent(this@LoginActivity, MainActivity::class.java)
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(it)
                finish()

            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                Log.e("Login", "HTTP ${e.code()} body=$body", e)
                val msg = when (e.code()) {
                    400, 401 -> "帳號或密碼錯誤"
                    else -> "登入失敗：HTTP ${e.code()}"
                }
                Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Log.e("Login", "network error", e)
                Toast.makeText(this@LoginActivity, "網路錯誤，請檢查連線", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("Login", "unknown error", e)
                Toast.makeText(this@LoginActivity, "發生錯誤：${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }
}
