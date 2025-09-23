package com.example.drinks.store

import com.example.drinks.data.model.CartLine
import com.example.drinks.data.model.Product
import com.example.drinks.data.model.SelectedOptions

object CartManager {
    val lines: MutableList<CartLine> = mutableListOf()

    private fun lineKey(pid: String, sel: SelectedOptions): String {
        val tops = sel.toppings.joinToString("+")
        return "$pid|${sel.sweet ?: ""}|${sel.ice ?: ""}|$tops"
    }

    private fun optionsPrice(sel: SelectedOptions): Int {
        // 簡易：加料價（你也可以改成從 Repo 取）
        val priceMap = mapOf("top_pearl" to 10, "top_coconut" to 10, "top_pudding" to 15)
        return sel.toppings.sumOf { priceMap[it] ?: 0 }
    }

    fun add(product: Product, sel: SelectedOptions) {
        require(sel.sweet != null && sel.ice != null) { "請選擇甜度與冰塊" }
        val key = lineKey(product.id, sel)
        val idx = lines.indexOfFirst { line -> line.lineKey == key }
        if (idx >= 0) {
            lines[idx].qty += 1
        } else {
            lines += CartLine(
                productId = product.id,
                name = product.name,
                qty = 1,
                unitPrice = product.price,
                optionsPrice = optionsPrice(sel),
                selected = sel.copy(toppings = sel.toppings.toMutableList()),
                lineKey = key
            )
        }
    }

    fun total(): Int = lines.sumOf { it.subtotal }
    fun clear() { lines.clear() }
}
