package com.example.drinks.store

import android.content.Context
class TokenStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("auth", Context.MODE_PRIVATE)
    fun save(token: String) = sp.edit().putString("token", token).apply()
    fun get(): String? = sp.getString("token", null)
    fun clear() = sp.edit().remove("token").apply()

}
