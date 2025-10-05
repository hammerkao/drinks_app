package com.example.drinks.data.model

import com.google.gson.annotations.SerializedName

data class OrderDTO(
    val id: Int,
    val status: String? = null,
    val total: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("items") val items: List<CartItemDTO>? = null
)

/* ===== 下單請求用 DTO ===== */

// 單一品項
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

// 整張訂單
data class OrderCreateRequest(
    @SerializedName("pickup_method")  val pickupMethod: String,
    @SerializedName("pickup_time")    val pickupTime: String, // ISO8601
    @SerializedName("buyer_name")     val buyerName: String,
    @SerializedName("buyer_phone")    val buyerPhone: String,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("order_note")     val orderNote: String? = null,
    val items: List<OrderItemRequest>,

    // ★ 新增：分店 ID（可為 null；後端用 request.data["store_id"] 取）
    @SerializedName("store_id")       val storeId: Int? = null
)
