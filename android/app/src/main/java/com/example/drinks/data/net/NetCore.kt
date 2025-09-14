package com.example.drinks.data.net

import android.content.Context
import com.example.drinks.data.json.GsonProvider.gson
import com.example.drinks.data.store.TokenStore
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

object NetCore {
    // 模擬器連本機
    const val BASE_URL = "http://10.0.2.2:8000/api/"

    private fun authInterceptor(store: TokenStore) = Interceptor { chain ->
        val b = chain.request().newBuilder()
        store.access?.let { b.header("Authorization", "Bearer $it") }
        chain.proceed(b.build())
    }

    // 401 時用 refresh 換 access，成功後自動重送一次
    private fun jwtAuthenticator(store: TokenStore) = Authenticator { _, resp ->
        if (resp.request.header("Authorization") == null || resp.priorResponse != null) return@Authenticator null
        val refresh = store.refresh ?: return@Authenticator null
        val newAccess = try { refreshAccess(refresh) } catch (_: Exception) { null }
        if (newAccess != null) {
            store.access = newAccess
            resp.request.newBuilder().header("Authorization", "Bearer $newAccess").build()
        } else {
            store.clear(); null
        }
    }

    // 同步 refresh（Authenticator 不能用 suspend）
    private fun refreshAccess(refresh: String): String? {
        val client = OkHttpClient()
        val body = gson.toJson(mapOf("refresh" to refresh))
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder()
            .url("${BASE_URL}auth/token/refresh/")
            .post(body)
            .build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return null
            val node = gson.fromJson(r.body!!.charStream(), TokenResp::class.java)
            return node.access
        }
    }

    fun api(ctx: Context): Api {
        val store = TokenStore(ctx)
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor(store))
            .authenticator(jwtAuthenticator(store))
            .addInterceptor(logging)
            .build()
        return Api(BASE_URL, client)
    }

    fun auth(ctx: Context): AuthApi {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        return AuthApi(BASE_URL, client)
    }
}

// 為了 refreshAccess 用到
data class TokenResp(val access: String, val refresh: String? = null)
