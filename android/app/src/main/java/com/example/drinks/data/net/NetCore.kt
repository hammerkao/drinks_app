// app/src/main/java/com/example/drinks/net/NetCore.kt
package com.example.drinks.net

import android.content.Context
import com.example.drinks.store.TokenStore
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetCore {
    // 模擬器連本機 Django
    const val BASE_URL: String = "http://10.0.2.2:8000/api/"
    const val BASE_HOST: String = "http://10.0.2.2:8000/"

    /** 將相對路徑轉絕對網址（給圖片等靜態資源用） */
    fun toAbsoluteUrl(path: String?): String? {
        if (path == null || path.isEmpty()) return null
        return if (path.startsWith("http", true)) path
        else BASE_HOST.trimEnd('/') + "/" + path.trimStart('/')
    }


fun buildOkHttp(@Suppress("UNUSED_PARAMETER") context: Context): OkHttpClient {
val authInterceptor = Interceptor { chain ->
val original = chain.request()
val path = original.url.encodedPath // e.g. /api/auth/login/ 或 /api/stores/
val isAuthEndpoint = path.contains("/api/auth/")

val access = TokenStore.accessToken
val req: Request = if (!isAuthEndpoint && (access?.isNotEmpty() == true)) {
original.newBuilder()
.header("Authorization", "Bearer $access")
.build()
} else {
original
}
chain.proceed(req)
}

val logging = HttpLoggingInterceptor().apply {
level = HttpLoggingInterceptor.Level.BODY
}

return OkHttpClient.Builder()
.addInterceptor(authInterceptor)
.authenticator(TokenAuthenticator)   // 401 自動 refresh + retry
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

/* ===================== 401 → refresh → retry ===================== */
private object TokenAuthenticator : Authenticator {
@Synchronized
override fun authenticate(route: Route?, response: Response): Request? {
// 避免無限重試：只處理第一次 401
if (responseCount(response) >= 2) return null

val refresh = TokenStore.refreshToken ?: return null

// 嘗試用 refresh 換新的 access
val newAccess = tryRefreshAccess(refresh) ?: return null

// 存回去
TokenStore.accessToken = newAccess

// 重新附上 Authorization 重送原請求
return response.request.newBuilder()
.header("Authorization", "Bearer $newAccess")
.build()
}

/** 呼叫 /api/auth/jwt/refresh/ 取新 access（用裸 client 避免遞迴） */
private fun tryRefreshAccess(refresh: String): String? {
val url = NetCore.BASE_URL + "auth/jwt/refresh/"
val json = JSONObject().put("refresh", refresh).toString()
val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

val naked = OkHttpClient()
val req = Request.Builder().url(url).post(body).build()
return try {
naked.newCall(req).execute().use { resp ->
if (!resp.isSuccessful) return null
val txt = resp.body?.string().orEmpty()
val obj = JSONObject(txt)
obj.optString("access").takeIf { it.isNotEmpty() }
}
} catch (_: Exception) {
null
}
}

private fun responseCount(response: Response): Int {
var r: Response? = response
var count = 1
while (r?.priorResponse != null) {
count++
r = r.priorResponse
}
return count
}
}
