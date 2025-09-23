package com.example.drinks.data.model
data class Product(
    val id: String,
    val categoryId: String,
    val name: String,
    val price: Int,
    val image: String = ""
)
