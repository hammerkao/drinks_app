package com.example.drinks.net

import android.content.Context
import com.example.drinks.store.TokenStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
object NetCore {
    // 開發用：模擬器連本機 Django
    const val BASE_URL: String = "http://10.0.2.2:8000/api/"
    @Volatile var accessToken: String? = null
    /** 共用 OkHttpClient（自動帶 Bearer；略過 /api/auth/） */
    fun buildOkHttp(context: Context): OkHttpClient {
        val appCtx = context.applicationContext
        val tokenStore = TokenStore(appCtx)

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val path = original.url.encodedPath // e.g. /api/auth/login/ or /api/stores/
            val isAuthEndpoint = path.contains("/api/auth/")


            val token = tokenStore.get()
            val req = if (!isAuthEndpoint && !token.isNullOrBlank()) {
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
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /** 取得 Retrofit 實例（給 AuthApi / 其他 Retrofit 服務用） */
    fun getRetrofit(context: Context): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(buildOkHttp(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}