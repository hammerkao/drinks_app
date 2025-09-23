package com.example.drinks.data.model





data class SelectedOptions(
    var sweet: String? = null,
    var ice: String? = null,
    var toppings: MutableList<String> = mutableListOf()
)

data class CartLine(
    val productId: String,
    val name: String,
    var qty: Int,
    val unitPrice: Int,
    val optionsPrice: Int,
    val selected: SelectedOptions,
    val lineKey: String
) {
    val subtotal: Int get() = (unitPrice + optionsPrice) * qty
}