// com/example/drinks/data/model/CartLine.kt
package com.example.drinks.data.model

data class CartLine(
    val productId: Int,
    val name: String,
    var qty: Int,
    val unitPrice: Int,
    val optionsPrice: Int,
    val selected: SelectedOptions,
    val lineKey: String,
    val imageUrl: String? = null,          // ← ➕ 新增欄位（預設 null）
) {
    val subtotal: Int
        get() = (unitPrice + optionsPrice) * qty
}

