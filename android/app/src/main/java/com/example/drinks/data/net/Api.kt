package com.example.drinks.data.net


import com.example.drinks.data.json.GsonProvider.gson
import com.example.drinks.data.model.*
import com.google.gson.reflect.TypeToken
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException


class Api(private val base: String, private val client: OkHttpClient) {

    private suspend inline fun <reified T> parse(r: Response): T {
        if (!r.isSuccessful) throw IOException("${r.request.method} ${r.request.url} -> ${r.code} ${r.message}")
        r.body!!.charStream().use { reader ->
            return gson.fromJson(reader, object : TypeToken<T>() {}.type)
        }
    }

    private suspend inline fun <reified T> get(path: String, qs: Map<String, Any?> = emptyMap()): T =
        withContext(Dispatchers.IO) {
            val baseUrl = (base + path).toHttpUrlOrNull()!!
            val url = baseUrl.newBuilder().apply {
                qs.forEach { (k, v) -> if (v != null) addQueryParameter(k, v.toString()) }
            }.build()
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { parse<T>(it) }
        }

    private suspend inline fun <reified T> send(method: String, path: String, bodyObj: Any? = null): T =
        withContext(Dispatchers.IO) {
            val body = bodyObj?.let {
                gson.toJson(it).toRequestBody("application/json; charset=utf-8".toMediaType())
            } ?: ByteArray(0).toRequestBody(null)
            val req = Request.Builder().url(base + path).method(method, body).build()
            client.newCall(req).execute().use { parse<T>(it) }
        }

    // ---- Endpoints ----
    suspend fun listProducts(
        search: String? = null,
        ordering: String? = "id",
        minPrice: String? = null,
        maxPrice: String? = null,
        category: Int? = null
    ): List<Product> = withContext(Dispatchers.IO) {
        val baseUrl = (base + "products/").toHttpUrlOrNull()!!
        val url = baseUrl.newBuilder().apply {
            if (search != null) addQueryParameter("search", search)
            if (ordering != null) addQueryParameter("ordering", ordering)
            if (minPrice != null) addQueryParameter("min_price", minPrice)
            if (maxPrice != null) addQueryParameter("max_price", maxPrice)
            if (category != null) addQueryParameter("category", category.toString())
        }.build()

        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("GET products failed: ${resp.code}")
            val json = resp.body!!.string()
            val element = JsonParser.parseString(json)

            val listType = object : TypeToken<List<Product>>() {}.type
            when {
                element.isJsonArray -> {
                    gson.fromJson<List<Product>>(json, listType)
                }
                element.isJsonObject -> {
                    val obj = element.asJsonObject

                    // 常見的分頁 keys
                    val results = when {
                        obj.has("results") -> obj["results"]
                        obj.has("data")    -> obj["data"]
                        else               -> null
                    }

                    if (results != null && results.isJsonArray) {
                        gson.fromJson(results, listType)
                    } else {
                        // Fallback：抓「第一個 value 是 JsonArray」的欄位
                        val firstArray = obj.entrySet().firstOrNull { it.value.isJsonArray }?.value
                        if (firstArray != null) gson.fromJson(firstArray, listType) else emptyList()
                    }
                }
                else -> emptyList()
            }
        }
    }

    // Cart
    suspend fun myCart(): CartDTO = get("carts/me/")
    suspend fun addItem(productId: Int, qty: Int = 1): Map<String, Any> =
        send("POST", "carts/add_item/", mapOf("product_id" to productId, "qty" to qty))
    suspend fun updateItem(productId: Int, qty: Int): Map<String, Any> =
        send("PATCH", "carts/update_item/", mapOf("product_id" to productId, "qty" to qty))
    suspend fun removeItem(productId: Int): Map<String, Any> =
        send("POST", "carts/remove_item/", mapOf("product_id" to productId))

    suspend fun clearCart() = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(base + "carts/clear/").delete().build()
        client.newCall(req).execute().use { if (!it.isSuccessful) throw IOException("clear failed: ${it.code}") }
    }

    // Orders
    suspend fun listOrders(): List<OrderDTO> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(base + "orders/").get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("GET orders failed: ${resp.code}")
            val json = resp.body!!.string()
            val element = JsonParser.parseString(json)
            val listType = object : TypeToken<List<OrderDTO>>() {}.type
            when {
                element.isJsonArray -> gson.fromJson<List<OrderDTO>>(json, listType)
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    val results = when {
                        obj.has("results") -> obj["results"]
                        obj.has("data") -> obj["data"]
                        else -> null
                    }
                    if (results != null && results.isJsonArray) gson.fromJson(results, listType) else emptyList()
                }
                else -> emptyList()
            }
        }
    }



    suspend fun getBranches(): List<BranchDto> = get("stores/")
}
