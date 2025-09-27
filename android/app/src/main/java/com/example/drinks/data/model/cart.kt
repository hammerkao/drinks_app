package com.example.drinks.data.model

import com.google.gson.annotations.SerializedName

data class CartItemDTO(
    val id: Int? = null,
    @SerializedName("product") val productId: Int? = null,
    @SerializedName("product_name") val productName: String? = null,
    val price: String? = null,          // 後端多半是 "100.00"
    val quantity: Int = 1,
    val subtotal: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class CartDTO(
    val id: Int? = null,
    val items: List<CartItemDTO> = emptyList(),
    val total: String? = null,          // 如 "300.00"
    @SerializedName("count") val count: Int? = null
)

/** 若 Api.kt 會送新增購物車請求，可用這個 */
data class AddToCartRequest(
    @SerializedName("product") val productId: Int,
    val quantity: Int = 1,
    @SerializedName("variant") val variantId: Int? = null
)
