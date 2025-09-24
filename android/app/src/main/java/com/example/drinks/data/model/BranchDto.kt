package com.example.drinks.data.model

data class BranchDto(
    val id: Long,
    val name: String,
    val phone: String?,
    val address: String?,
    val open_hours: String?,  // "09:00 - 22:00"
    val status: String?       // "營業中" / "休息中"
)