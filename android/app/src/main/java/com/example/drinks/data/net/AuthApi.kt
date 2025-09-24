package com.example.drinks.net

import retrofit2.http.Body
import retrofit2.http.POST

data class LoginResponse(val token: String)
data class RegisterResponse(val id: Long, val phone: String)

interface AuthApi {
    @POST("auth/register/")
    suspend fun register(@Body body: Map<String, @JvmSuppressWildcards Any>): RegisterResponse

    @POST("auth/login/")
    suspend fun login(@Body body: Map<String, @JvmSuppressWildcards Any>): LoginResponse
}
