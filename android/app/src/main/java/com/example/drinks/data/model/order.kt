package com.example.drinks.data.model

import com.google.gson.annotations.SerializedName

/* ========== 後端回傳用 DTO ========== */

data class StoreLite(
    val id: Int,
    val name: String?
)

data class OrderItemDTO(
    @SerializedName("product") val productName: String?,   // 後端 serializer 通常把 product 轉成名稱
    val qty: Int,
    @SerializedName("unit_price") val unitPrice: String?,  // e.g. "60.00"
    val sweet: String?,
    val ice: String?,
    @SerializedName("toppings_json") val toppings: List<String>?,
    val note: String?
)

data class OrderDTO(
    val id: Int,

    // 後端回傳的是 store 的整數 ID，不是整個物件
    @SerializedName("store") val storeId: Int? = null,

    val status: String? = null,
    val total: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("items") val items: List<CartItemDTO>? = null
)

/* ========== 建單請求用 DTO ========== */

// 單一品項（用於「直接送訂單 payload」方案）
data class OrderItemRequest(
    @SerializedName("product_id") val productId: Int,
    val qty: Int,
    @SerializedName("unit_price") val unitPrice: Int,       // 單價（元，整數）
    @SerializedName("options_price") val optionsPrice: Int = 0,
    val sweet: String? = null,
    val ice: String? = null,
    val toppings: List<String>? = null,
    val note: String? = null,
    @SerializedName("options_key") val optionsKey: String? = null
)

// 整張訂單（方法 B：直接送完整訂單）
data class OrderCreateRequest(
    @SerializedName("pickup_method")  val pickupMethod: String, // "pickup" / "delivery"
    @SerializedName("pickup_time")    val pickupTime: String,   // ISO8601
    @SerializedName("buyer_name")     val buyerName: String,
    @SerializedName("buyer_phone")    val buyerPhone: String,
    @SerializedName("payment_method") val paymentMethod: String, // "cash" / "card" ...
    @SerializedName("order_note")     val orderNote: String? = null,
    val items: List<OrderItemRequest>,

    // 分店 ID（可為 null；後端用 request.data["store_id"] 取用）
    @SerializedName("store_id")       val storeId: Int? = null
)
