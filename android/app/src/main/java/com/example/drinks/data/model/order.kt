package com.example.drinks.data.model

import com.google.gson.annotations.SerializedName

data class OrderDTO(
    val id: Int,
    val status: String? = null,
    val total: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("items") val items: List<CartItemDTO>? = null
)
