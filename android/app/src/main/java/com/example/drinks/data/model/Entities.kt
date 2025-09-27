package com.example.drinks.data.model





data class SelectedOptions(
    var sweet: String? = null,
    var ice: String? = null,
    var toppings: MutableList<String> = mutableListOf()
)

