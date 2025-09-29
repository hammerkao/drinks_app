package com.example.drinks.store

import com.example.drinks.data.model.CartLine
import com.example.drinks.data.model.Product
import com.example.drinks.data.model.SelectedOptions
import java.math.BigDecimal
import java.math.RoundingMode

object CartManager {
    /** 目前購物車明細 */
    val lines: MutableList<CartLine> = mutableListOf()

    /** 將 "100.00" -> 100（元） */
    private fun parsePriceToInt(price: String): Int {
        return try {
            BigDecimal(price).setScale(0, RoundingMode.DOWN).toInt()
        } catch (_: Exception) {
            price.substringBefore('.').toIntOrNull() ?: 0
        }
    }

    /** 產生一筆明細的 key（商品 + 客製條件） */
    private fun lineKey(pid: Int, sel: SelectedOptions): String {
        // 若 toppings 次序會影響比對，可改成 .sorted() 再 join
        val tops = sel.toppings.joinToString("+")
        return "${pid}|${sel.sweet ?: ""}|${sel.ice ?: ""}|$tops"
    }

    /** 計算客製加價（支援中文/英文鍵兩種寫法） */
    private fun optionsPrice(sel: SelectedOptions): Int {
        val priceMap = mapOf(
            // 中文
            "珍珠" to 10, "椰果" to 10, "布丁" to 15,
            // 英文字鍵（若你之後改成 id）
            "top_pearl" to 10, "top_coconut" to 10, "top_pudding" to 15
        )
        return sel.toppings.sumOf { priceMap[it] ?: 0 }
    }

    /** 加入 1 杯（相同商品+客製就合併數量） */
    fun add(product: Product, sel: SelectedOptions) {
        add(product, sel, 1)
    }

    /** 加入多杯（可指定 qty） */
    fun add(product: Product, sel: SelectedOptions, qty: Int) {
        require(sel.sweet != null && sel.ice != null) { "請選擇甜度與冰塊" }
        if (qty <= 0) return

        val key = lineKey(product.id, sel)
        val idx = lines.indexOfFirst { it.lineKey == key }

        if (idx >= 0) {
            lines[idx].qty += qty
        } else {
            lines += CartLine(
                productId    = product.id,
                name         = product.name,
                qty          = qty,
                unitPrice    = parsePriceToInt(product.price), // "100.00" -> 100
                optionsPrice = optionsPrice(sel),
                selected     = sel.copy(toppings = sel.toppings.toMutableList()),
                lineKey      = key
            )
        }
    }

    /** 小計總額（如果 CartLine 已有 subtotal 屬性就沿用，否則用式子計） */
    fun total(): Int = lines.sumOf { line ->
        // 若 CartLine 有 subtotal 屬性：
        try {
            line.subtotal
        } catch (_: Throwable) {
            (line.unitPrice + line.optionsPrice) * line.qty
        }
    }

    /** 清空購物車 */
    fun clear() { lines.clear() }

    /* ---------------- 新增的三個方法 ---------------- */

    /** 是否為空 */
    fun isEmpty(): Boolean = lines.isEmpty()

    /** 全部數量（杯數） */
    fun count(): Int = lines.sumOf { it.qty }

    /** 總金額（同 total，取個更語意化的名字給 UI 用） */
    fun totalAmount(): Int = total()
}
