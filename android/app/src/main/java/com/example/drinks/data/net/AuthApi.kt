// app/src/main/java/com/example/drinks/net/AuthApi.kt
package com.example.drinks.net

import retrofit2.http.Body
import retrofit2.http.POST

// 後端回傳：登入成功只需要 token
data class LoginResp(val token: String)

// 註冊成功可回 token 或簡單訊息，看你後端實作；這裡先用 token
data class RegisterResp(val token: String)

// 注意：路徑末端「一定要有斜線 /」對齊 DRF
interface AuthApi {
    @POST("auth/login/")
    suspend fun login(@Body body: Map<String, String>): LoginResp

    @POST("auth/register/")
    suspend fun register(@Body body: Map<String, String>): RegisterResp
}
