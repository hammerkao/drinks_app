package com.example.drinks.data.net

import android.util.Log
import com.example.drinks.data.json.GsonProvider.gson
import com.example.drinks.data.model.*
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class Api(
    private val base: String,
    private val client: OkHttpClient
) {

    /** 建議將 base 轉成 HttpUrl，後續都用 resolve 更安全 */
    private val baseUrl: HttpUrl = (if (base.endsWith("/")) base else "$base/").toHttpUrlOrNull()
        ?: throw IllegalArgumentException("Bad base url: $base")

    private val JSON = "application/json; charset=utf-8".toMediaType()

    /* -------------------- 共用方法 -------------------- */

    private inline fun <reified T> fromJson(text: String): T =
        gson.fromJson(text, object : TypeToken<T>() {}.type)

    private fun throwHttpError(resp: Response, body: String?): Nothing {
        throw IOException("${resp.request.method} ${resp.request.url} -> ${resp.code} ${resp.message}. body=${body.orEmpty()}")
    }

    private suspend inline fun <reified T> parse(resp: Response): T {
        val bodyStr = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) throwHttpError(resp, bodyStr)
        return fromJson(bodyStr)
    }

    private suspend inline fun <reified T> get(
        path: String,
        qs: Map<String, Any?> = emptyMap()
    ): T = withContext(Dispatchers.IO) {
        val url = (baseUrl.resolve(path) ?: error("Bad path: $path"))
            .newBuilder().apply {
                qs.forEach { (k, v) -> if (v != null) addQueryParameter(k, v.toString()) }
            }.build()

        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { parse<T>(it) }
    }

    private suspend inline fun <reified T> send(
        method: String,
        path: String,
        bodyObj: Any? = null
    ): T = withContext(Dispatchers.IO) {
        val url = baseUrl.resolve(path) ?: error("Bad path: $path")

        Log.d("Api", "REQUEST $method $url body=${bodyObj?.let { gson.toJson(it) }}")

        val body = when (bodyObj) {
            null -> ByteArray(0).toRequestBody(null)
            else -> gson.toJson(bodyObj).toRequestBody(JSON)
        }

        val req = Request.Builder().url(url).method(method, body).build()
        client.newCall(req).execute().use { parse<T>(it) }
    }

    /* -------------------- Products -------------------- */

    suspend fun listProducts(
        search: String? = null,
        ordering: String? = "id",
        minPrice: String? = null,
        maxPrice: String? = null,
        category: Int? = null
    ): List<Product> = withContext(Dispatchers.IO) {
        val url = (baseUrl.resolve("products/") ?: error("Bad path"))
            .newBuilder().apply {
                if (search != null) addQueryParameter("search", search)
                if (ordering != null) addQueryParameter("ordering", ordering)
                if (minPrice != null) addQueryParameter("min_price", minPrice)
                if (maxPrice != null) addQueryParameter("max_price", maxPrice)
                if (category != null) addQueryParameter("category", category.toString())
            }.build()

        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throwHttpError(resp, resp.body?.string())
            val text = resp.body?.string().orEmpty()

            val element = JsonParser.parseString(text)
            val listType = object : TypeToken<List<Product>>() {}.type
            when {
                element.isJsonArray  -> gson.fromJson<List<Product>>(text, listType)
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    val results = when {
                        obj.has("results") -> obj["results"]
                        obj.has("data")    -> obj["data"]
                        else               -> null
                    }
                    if (results != null && results.isJsonArray) gson.fromJson(results, listType)
                    else obj.entrySet().firstOrNull { it.value.isJsonArray }?.value
                        ?.let { gson.fromJson<List<Product>>(it, listType) } ?: emptyList()
                }
                else -> emptyList()
            }
        }
    }

    suspend fun getProduct(id: Int): Product = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(baseUrl.resolve("products/$id/")!!)
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throwHttpError(resp, resp.body?.string())
            gson.fromJson(resp.body!!.charStream(), Product::class.java)
        }
    }

    /* -------------------- Stores -------------------- */

    suspend fun getStores(): List<Store> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(baseUrl.resolve("stores/")!!).get().build()
        client.newCall(req).execute().use { resp ->
            val bodyStr = resp.body?.string()
            if (!resp.isSuccessful) throwHttpError(resp, bodyStr)

            val element = JsonParser.parseString(bodyStr ?: "[]")
            val listType = object : TypeToken<List<Store>>() {}.type
            when {
                element.isJsonArray  -> gson.fromJson<List<Store>>(element, listType)
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    val results = when {
                        obj.has("results") -> obj["results"]
                        obj.has("data")    -> obj["data"]
                        else               -> null
                    }
                    if (results != null && results.isJsonArray) gson.fromJson(results, listType)
                    else obj.entrySet().firstOrNull { it.value.isJsonArray }?.value
                        ?.let { gson.fromJson<List<Store>>(it, listType) } ?: emptyList()
                }
                else -> emptyList()
            }
        }
    }

    /* -------------------- Cart（方法 A 會用到） -------------------- */

    suspend fun myCart(): CartDTO = get("carts/me/")

    suspend fun addItem(
        productId: Int,
        qty: Int = 1,
        sweet: String? = null,
        ice: String? = null,
        toppings: List<String>? = null,
        note: String? = null,
        optionsPrice: Int = 0
    ): Map<String, Any> = send(
        method = "POST",
        path = "carts/add_item/",
        bodyObj = mapOf(
            "product_id" to productId,
            "qty" to qty,
            "sweet" to sweet,
            "ice" to ice,
            "toppings" to (toppings ?: emptyList<String>()),
            "note" to note,
            "options_price" to optionsPrice
        )
    )

    suspend fun updateItem(productId: Int, qty: Int): Map<String, Any> =
        send("PATCH", "carts/update_item/", mapOf("product_id" to productId, "qty" to qty))

    suspend fun removeItem(productId: Int): Map<String, Any> =
        send("POST", "carts/remove_item/", mapOf("product_id" to productId))

    suspend fun clearCart() = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(baseUrl.resolve("carts/clear/")!!)
            .method("DELETE", null)
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throwHttpError(resp, resp.body?.string())
        }
    }

    /** 從「伺服器端購物車」轉訂單；可帶 store_id（方法 A 用） */
    suspend fun createOrderFromCart(storeId: Int?): OrderDTO = send(
        method = "POST",
        path   = "orders/",
        bodyObj = storeId?.let { mapOf("store_id" to it) } ?: emptyMap<String, Any>()
    )

    /* -------------------- Orders -------------------- */

    /** 方法 B：直接送完整訂單 payload 建單 */
    suspend fun createOrder(reqObj: OrderCreateRequest): OrderDTO =
        send("POST", "orders/", reqObj)

    /** 訂單列表（相容 DRF 預設分頁 results） */
    suspend fun listOrders(): List<OrderDTO> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(baseUrl.resolve("orders/")!!).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throwHttpError(resp, resp.body?.string())
            val text = resp.body?.string().orEmpty()

            val element = JsonParser.parseString(text)
            val listType = object : TypeToken<List<OrderDTO>>() {}.type
            when {
                element.isJsonArray  -> gson.fromJson<List<OrderDTO>>(text, listType)
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    val results = when {
                        obj.has("results") -> obj["results"]
                        obj.has("data")    -> obj["data"]
                        else               -> null
                    }
                    if (results != null && results.isJsonArray) gson.fromJson(results, listType)
                    else emptyList()
                }
                else -> emptyList()
            }
        }
    }

    /** 單一訂單詳情 */
    suspend fun getOrder(id: Int): OrderDTO = get("orders/$id/")
}
