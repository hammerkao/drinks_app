package com.example.drinks.net

import android.content.Context
import com.example.drinks.store.TokenStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 全域 Retrofit 單例
 * 使用方式：
 *   val authApi = NetCore.getRetrofit(context).create(AuthApi::class.java)
 */
object NetCore {
    // 改成 public，讓別處取得
    const val BASE_URL = "http://10.0.2.2:8000/api/"

    fun buildOkHttp(context: Context): OkHttpClient {
        val tokenStore = TokenStore(context.applicationContext)

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = tokenStore.get()
            val req = if (!token.isNullOrBlank()) {
                original.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else original
            chain.proceed(req)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    fun getRetrofit(context: Context): Retrofit {
        val client = buildOkHttp(context)
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

