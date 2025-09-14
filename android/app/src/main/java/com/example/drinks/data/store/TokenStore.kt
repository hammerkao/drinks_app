package com.example.drinks.data.store

import android.content.Context

class TokenStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("auth", Context.MODE_PRIVATE)
    var access: String? get() = sp.getString("access", null); set(v){ sp.edit().putString("access", v).apply() }
    var refresh: String? get() = sp.getString("refresh", null); set(v){ sp.edit().putString("refresh", v).apply() }
    fun clear(){ sp.edit().clear().apply() }
}