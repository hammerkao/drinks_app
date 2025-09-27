package com.example.drinks.net

import android.content.Context
import com.example.drinks.data.json.GsonProvider
import com.example.drinks.data.model.Page
import com.example.drinks.data.model.Product
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

object ProductApi {
    private val gson = GsonProvider.gson

    /**
     * 取得商品清單（支援 DRF 分頁）。預設用 ?store=<id>&page=<n>
     * 如後端用其他參數名，改下面的 query key 即可（例如 branch= 或 shop=）。
     */
    fun listProducts(
        context: Context,
        storeId: Int?,
        categoryId: Int? = null,
        page: Int? = null,
        onSuccess: (Page<Product>) -> Unit,
        onError: (String) -> Unit
    ) {
        val base = NetCore.BASE_URL + "products/"

        val qs = buildString {
            val parts = mutableListOf<String>()
            if (storeId != null) parts += "store=$storeId"
            if (categoryId != null) parts += "category=$categoryId"
            if (page != null) parts += "page=$page"
            if (parts.isNotEmpty()) append("?${parts.joinToString("&")}")
        }

        val req = Request.Builder().url(base + qs).get().build()
        val client = NetCore.buildOkHttp(context)

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.localizedMessage ?: "網路錯誤")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        onError("HTTP ${it.code}")
                        return
                    }
                    val body = it.body?.string()
                    if (body.isNullOrEmpty()) {
                        onError("空回應")
                        return
                    }
                    try {
                        // 優先解析 DRF 分頁
                        val pagedType = object : TypeToken<Page<Product>>() {}.type
                        val pageObj: Page<Product> = gson.fromJson(body, pagedType)
                        onSuccess(pageObj)
                    } catch (_: Exception) {
                        // 後備：若後端沒開分頁，回傳純陣列時也能吃
                        try {
                            val listType = object : TypeToken<List<Product>>() {}.type
                            val items: List<Product> = gson.fromJson(body, listType)
                            onSuccess(Page(items.size, null, null, items))
                        } catch (ex: Exception) {
                            onError("解析失敗：${ex.localizedMessage}")
                        }
                    }
                }
            }
        })
    }
}
