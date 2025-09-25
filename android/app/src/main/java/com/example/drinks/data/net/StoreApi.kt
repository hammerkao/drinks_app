package com.example.drinks.net

import android.content.Context
import com.example.drinks.data.json.GsonProvider
import com.example.drinks.data.model.Page
import com.example.drinks.data.model.Store
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

object StoreApi {
    private val gson = GsonProvider.gson

    fun getStores(
        context: Context,
        onSuccess: (List<Store>) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = NetCore.BASE_URL + "stores/"
        val req = okhttp3.Request.Builder().url(url).get().build()
        val client = NetCore.buildOkHttp(context)

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onError(e.localizedMessage ?: "網路錯誤")

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        onError("HTTP ${it.code}")
                        return
                    }
                    val body = it.body?.string() ?: run { onError("空回應"); return }
                    try {
                        val type = object : TypeToken<Page<Store>>() {}.type
                        val page: Page<Store> = gson.fromJson(body, type)
                        onSuccess(page.results)   // ← 只把 results 丟給 UI
                    } catch (ex: Exception) {
                        onError("解析失敗：${ex.localizedMessage}")
                    }
                }
            }
        })
    }
}
