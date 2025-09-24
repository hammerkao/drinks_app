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

    // 依你的 DRF 服務調整
    private const val BASE_URL = "http://10.0.2.2:8000/api/"  // 模擬器連本機Django

    fun getRetrofit(context: Context): Retrofit {
        val tokenStore = TokenStore(context.applicationContext)

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = tokenStore.get()
            val req = if (!token.isNullOrBlank()) {
                original.newBuilder()
                    .addHeader("Authorization", "Bearer $token") // 若用 DRF TokenAuth 改成 "Token $token"
                    .build()
            } else original
            chain.proceed(req)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
