package com.example.drinks.data.model

import com.google.gson.JsonElement

// models.kt
data class LoginReq(val username: String, val password: String)
data class TokenResp(val resp: String, val access: String)


data class ProductMini(val id: Int, val name: String, val price: String)

data class CartItemDTO(val id: Int, val product: Int, val product_detail: ProductMini?, val qty: Int)
data class CartDTO(val id: Int, val user: Int, val updated_at: String, val items: List<CartItemDTO>)

data class OrderItemDTO(val id: Int, val product: Int, val product_detail: ProductMini?, val qty: Int, val unit_price: String)
data class OrderDTO(val id: Int, val user: Int, val status: String, val total: String, val created_at: String, val items: List<OrderItemDTO>)

data class Store(
    val id: Int,
    val name: String,
    val phone: String?,
    val address: String?,
    val open_hours: String?, // 先用 String；若未來要結構化再改
    val status: String?      // e.g. "active" / "inactive"
)


