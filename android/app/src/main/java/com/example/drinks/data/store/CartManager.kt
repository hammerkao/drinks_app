package com.example.drinks.store

import com.example.drinks.data.model.CartLine
import com.example.drinks.data.model.Product
import com.example.drinks.data.model.SelectedOptions
import java.math.BigDecimal
import java.math.RoundingMode

object CartManager {
    val lines: MutableList<CartLine> = mutableListOf()

    /** 將 "100.00" -> 100（元） */
    private fun parsePriceToInt(price: String): Int {
        return try {
            BigDecimal(price).setScale(0, RoundingMode.DOWN).toInt()
        } catch (_: Exception) {
            price.substringBefore('.').toIntOrNull() ?: 0
        }
    }

    /** 用 Int pid；key 內再 toString */
    private fun lineKey(pid: Int, sel: SelectedOptions): String {
        val tops = sel.toppings.joinToString("+")
        return "${pid}|${sel.sweet ?: ""}|${sel.ice ?: ""}|$tops"
    }

    private fun optionsPrice(sel: SelectedOptions): Int {
        // 簡易：加料價（之後可改為從後端帶回）
        val priceMap = mapOf("top_pearl" to 10, "top_coconut" to 10, "top_pudding" to 15)
        return sel.toppings.sumOf { priceMap[it] ?: 0 }
    }

    fun add(product: Product, sel: SelectedOptions) {
        require(sel.sweet != null && sel.ice != null) { "請選擇甜度與冰塊" }

        val key = lineKey(product.id, sel)  // ✅ 這裡不再型別不合
        val idx = lines.indexOfFirst { it.lineKey == key }

        if (idx >= 0) {
            lines[idx].qty += 1
        } else {
            lines += CartLine(
                productId   = product.id,
                name        = product.name,
                qty         = 1,
                unitPrice   = parsePriceToInt(product.price), // ✅ "100.00" -> 100
                optionsPrice= optionsPrice(sel),
                selected    = sel.copy(toppings = sel.toppings.toMutableList()),
                lineKey     = key
            )
        }
    }

    fun total(): Int = lines.sumOf { it.subtotal }
    fun clear() { lines.clear() }
}
