package com.example.drinks.store

object TokenStore {
    // 你現有應該已經有這些欄位/方法，如名稱不同，對齊一下即可
    var accessToken: String? = null
    var refreshToken: String? = null

    fun set(access: String?, refresh: String?) {
        accessToken = access
        refreshToken = refresh
    }
    fun clear() { accessToken = null; refreshToken = null }
    fun hasTokens(): Boolean = !accessToken.isNullOrBlank() || !refreshToken.isNullOrBlank()


}
