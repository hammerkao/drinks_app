// order.kt
package com.example.drinks.data.model

import com.google.gson.annotations.SerializedName

/* ========== 後端回傳用 DTO ========== */

data class StoreLite(
    val id: Int,
    val name: String?
)

data class OrderItemDTO(
    @SerializedName("product") val productName: String?,   // 後端 serializer 把 product 轉成名稱
    val qty: Int,
    @SerializedName("unit_price") val unitPrice: String?,  // e.g. "60.00"
    val sweet: String?,
    val ice: String?,
    @SerializedName("toppings_json") val toppings: List<String>?,
    val note: String?
)

/** 這裡一定要把 snake_case 對齊 SerializedName，否則解析不到值 */
data class OrderDTO(
    val id: Int,

    @SerializedName("store") val storeId: Int? = null,
    val status: String? = null,
    val total: String? = null,

    // 新增欄位（對齊 models.py / serializers.py）
    @SerializedName("buyer_name")     val buyerName: String? = null,
    @SerializedName("buyer_phone")    val buyerPhone: String? = null,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("paid")           val paid: Boolean? = null,
    @SerializedName("pickup_method")  val pickupMethod: String? = null,
    @SerializedName("pickup_time")    val pickupTime: String? = null,
    @SerializedName("order_note")     val orderNote: String? = null,

    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("items")     val items: List<OrderItemDTO>? = null
)

/* ========== 建單請求用 DTO（維持既有） ========== */

data class OrderItemRequest(
    @SerializedName("product_id") val productId: Int,
    val qty: Int,
    @SerializedName("unit_price") val unitPrice: Int,
    @SerializedName("options_price") val optionsPrice: Int = 0,
    val sweet: String? = null,
    val ice: String? = null,
    val toppings: List<String>? = null,
    val note: String? = null,
    @SerializedName("options_key") val optionsKey: String? = null
)

data class OrderCreateRequest(
    @SerializedName("pickup_method")  val pickupMethod: String, // "pickup"/"delivery"
    @SerializedName("pickup_time")    val pickupTime: String,   // ISO8601
    @SerializedName("buyer_name")     val buyerName: String,
    @SerializedName("buyer_phone")    val buyerPhone: String,
    @SerializedName("payment_method") val paymentMethod: String, // "cash" / "card" ...
    @SerializedName("order_note")     val orderNote: String? = null,
    val items: List<OrderItemRequest>,
    @SerializedName("store_id")       val storeId: Int? = null
)
