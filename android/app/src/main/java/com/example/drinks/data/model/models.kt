package com.example.drinks.data.model

import com.google.gson.JsonElement

// models.kt
data class LoginReq(val username: String, val password: String)
data class TokenResp(val access: String, val refresh: String)

data class Product(
    val id: Int,
    val name: String,
    val price: String,     // 金額當字串，顯示時再轉
    val is_active: Boolean,
    val category: JsonElement?,
    val created_at: String,
    val updated_at: String?,
    val image: String?
)

data class ProductMini(val id: Int, val name: String, val price: String)

data class CartItemDTO(val id: Int, val product: Int, val product_detail: ProductMini?, val qty: Int)
data class CartDTO(val id: Int, val user: Int, val updated_at: String, val items: List<CartItemDTO>)

data class OrderItemDTO(val id: Int, val product: Int, val product_detail: ProductMini?, val qty: Int, val unit_price: String)
data class OrderDTO(val id: Int, val user: Int, val status: String, val total: String, val created_at: String, val items: List<OrderItemDTO>)
