package com.example.drinks.data.model


data class SelectedOptions(
    var sweet: String? = null,                 // e.g. "sweet_3"
    var ice: String? = null,                   // e.g. "ice_less"
    val toppings: MutableList<String> = mutableListOf(), // e.g. ["top_pearl", ...]
    var note: String? = null                   // ← 新增：備註
)

