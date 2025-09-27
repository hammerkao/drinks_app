// com/example/drinks/data/model/CartLine.kt
package com.example.drinks.data.model

data class CartLine(
    val productId: Int,          // ✅ Int
    val name: String,
    var qty: Int,
    val unitPrice: Int,          // ✅ Int（單價，元）
    val optionsPrice: Int,       // ✅ Int（加料價，元）
    val selected: SelectedOptions,
    val lineKey: String
) {
    val subtotal: Int get() = (unitPrice + optionsPrice) * qty
}
