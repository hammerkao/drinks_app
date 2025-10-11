package com.example.drinks.net

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

/* ===================== 回應模型 ===================== */

/** 建議後端回傳 access / refresh；若仍是 token，就當作 access 用 */
data class LoginResp(
    val access: String? = null,
    val refresh: String? = null,
    /** 舊版相容：若後端只回 token，就把它當 access 用 */
    @SerializedName("token") val legacyToken: String? = null
)

data class RegisterResp(
    val access: String? = null,
    val refresh: String? = null,
    @SerializedName("token") val legacyToken: String? = null
)

/** SimpleJWT 的 refresh 端點常見格式 */
data class RefreshReq(val refresh: String)
data class RefreshResp(val access: String)

/* ===================== Retrofit 服務 ===================== */
/** 注意：路徑末端「一定要有斜線 /」對齊 DRF */
interface AuthApi {
    @POST("auth/login/")
    suspend fun login(@Body body: Map<String, String>): LoginResp

    @POST("auth/register/")
    suspend fun register(@Body body: Map<String, String>): RegisterResp

    /** 若你用的是 SimpleJWT，通常會有這支 */
    @POST("auth/jwt/refresh/")
    suspend fun refresh(@Body body: RefreshReq): RefreshResp
}

/* ===================== 小工具 ===================== */
/** 從登入/註冊回應取出 access token（相容舊版 token 欄位） */
fun LoginResp.accessOrLegacy(): String? = access ?: legacyToken
fun RegisterResp.accessOrLegacy(): String? = access ?: legacyToken
