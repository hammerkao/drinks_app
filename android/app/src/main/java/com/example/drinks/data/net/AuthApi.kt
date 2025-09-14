package com.example.drinks.data.net

import com.example.drinks.data.json.GsonProvider.gson
import com.example.drinks.data.model.LoginReq
import com.example.drinks.data.model.TokenResp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class AuthApi(private val base: String, private val client: OkHttpClient) {
    suspend fun login(username: String, password: String): TokenResp = withContext(Dispatchers.IO) {
        val body = gson.toJson(LoginReq(username, password))
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder().url(base + "auth/token/").post(body).build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw IOException("Login failed: ${r.code} ${r.message}")
            gson.fromJson(r.body!!.charStream(), TokenResp::class.java)
        }
    }
}
