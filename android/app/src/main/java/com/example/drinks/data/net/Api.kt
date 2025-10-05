package com.example.drinks.data.net


import android.util.Log
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
import com.example.drinks.data.model.CartDTO
import com.example.drinks.data.model.CartItemDTO
import com.example.drinks.data.model.AddToCartRequest
import com.example.drinks.data.model.OrderCreateRequest
import com.example.drinks.data.model.OrderDTO
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.withContext


class Api(private val base: String, private val client: OkHttpClient) {

    // 建議把 base 先轉 HttpUrl，後面都用 resolve
    private val baseUrl: HttpUrl = (if (base.endsWith("/")) base else "$base/").toHttpUrlOrNull()
        ?: throw IllegalArgumentException("Bad base url: $base")

    suspend fun createOrderFromCart(storeId: Int?): com.example.drinks.data.model.OrderDTO =
        send(
            method = "POST",
            path   = "orders/",
            bodyObj = storeId?.let { mapOf("store_id" to it) } ?: emptyMap<String, Any>()
        )

    private suspend inline fun <reified T> parse(r: Response): T {
        val bodyStr = r.body?.string().orEmpty()
        if (!r.isSuccessful) {
            throw IOException("${r.request.method} ${r.request.url} -> ${r.code} ${r.message}. body=$bodyStr")
        }
        return gson.fromJson(bodyStr, object : TypeToken<T>() {}.type)
    }

    private suspend inline fun <reified T> get(path: String, qs: Map<String, Any?> = emptyMap()): T =
        withContext(Dispatchers.IO) {
            val url = (baseUrl.resolve(path) ?: error("Bad path: $path"))
                .newBuilder().apply {
                    qs.forEach { (k, v) -> if (v != null) addQueryParameter(k, v.toString()) }
                }.build()
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { parse<T>(it) }
        }

    private suspend inline fun <reified T> send(method: String, path: String, bodyObj: Any? = null): T =

        withContext(Dispatchers.IO) {

            Log.d("Api", "REQUEST $method ${base + path} body=${bodyObj?.let { gson.toJson(it) }}")

            val url = baseUrl.resolve(path) ?: error("Bad path: $path")
            android.util.Log.d("Api", "REQUEST $method $url body=${bodyObj?.let { gson.toJson(it) }}")

            val body = bodyObj?.let {
                gson.toJson(it).toRequestBody("application/json; charset=utf-8".toMediaType())
            } ?: ByteArray(0).toRequestBody(null)

            val req = Request.Builder().url(url).method(method, body).build()
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
            if (!resp.isSuccessful) throw IOException("GET products failed: ${resp.code}")
            val json = resp.body!!.string()
            val element = JsonParser.parseString(json)
            val listType = object : TypeToken<List<Product>>() {}.type
            when {
                element.isJsonArray  -> gson.fromJson<List<Product>>(json, listType)
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

    // Cart（A 方案用得到）
    suspend fun myCart(): CartDTO = get("carts/me/")

    // ➕ 同步單一品項到伺服器端購物車（支援甜度/冰塊/加料/備註/加價）
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

    /** 清空伺服器端購物車（注意：一定要在 class Api 內，才能用到 base/client） */
    suspend fun clearCart() = withContext(Dispatchers.IO) {
        // 若 .delete() 無法解析，可用 .method("DELETE", null)
        val req: Request = Request.Builder()
            .url(base + "carts/clear/")
            .method("DELETE", null)
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("DELETE carts/clear/ -> ${resp.code} ${resp.message}")
            }
        }
    }

    suspend fun listOrders(): List<OrderDTO> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(baseUrl.resolve("orders/")!!).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("GET orders failed: ${resp.code}")
            val json = resp.body!!.string()
            val element = JsonParser.parseString(json)
            val listType = object : TypeToken<List<OrderDTO>>() {}.type
            when {
                element.isJsonArray  -> gson.fromJson<List<OrderDTO>>(json, listType)
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    val results = when {
                        obj.has("results") -> obj["results"]
                        obj.has("data")    -> obj["data"]
                        else               -> null
                    }
                    if (results != null && results.isJsonArray) gson.fromJson(results, listType) else emptyList()
                }
                else -> emptyList()
            }
        }
    }

    suspend fun getStores(): List<Store> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(baseUrl.resolve("stores/")!!).get().build()
        client.newCall(req).execute().use { resp ->
            val bodyStr = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw IOException("GET /stores/ -> ${resp.code} ${resp.message}. body=$bodyStr")

            val element = JsonParser.parseString(bodyStr)
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

    suspend fun getProduct(id: Int): Product =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url(baseUrl.resolve("products/$id/")!!)
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IOException("GET /products/$id/ -> ${resp.code} ${resp.message}")
                }
                gson.fromJson(resp.body!!.charStream(), Product::class.java)
            }
        }


    suspend fun createOrder(req: com.example.drinks.data.model.OrderCreateRequest)
            : com.example.drinks.data.model.OrderDTO =
        send("POST", "orders/", req)
}

