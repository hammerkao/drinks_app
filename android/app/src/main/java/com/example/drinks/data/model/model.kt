package com.example.drinks.data.model

data class Page<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)

data class Store(
    val id: Int,
    val name: String,
    val phone: String?,
    val address: String?,
    val open_hours: String?,   // 對齊後端欄位
    val status: String?
)