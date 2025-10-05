package com.example.drinks.store

import com.example.drinks.data.model.CartLine
import com.example.drinks.data.model.Product
import com.example.drinks.data.model.SelectedOptions
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CartManager {
    var currentStoreId: Int? = null
    var currentStoreName: String? = null

    fun setStore(id: Int, name: String?) {
        currentStoreId = id
        currentStoreName = name
    }

    // 發佈「內容已變更」事件（UI 只需 collect 這個即可）
    private val _changes = MutableStateFlow(Unit)
    val changes: StateFlow<Unit> = _changes.asStateFlow()

    /** 目前購物車明細（改成 private，避免外部直接改而沒通知） */
    private val lines: MutableList<CartLine> = mutableListOf()
    /** 如果外部需要讀取，提供唯讀快照 */
    fun getLines(): List<CartLine> = lines.toList()

    /** 100.00 -> 100（元） */
    private fun parsePriceToInt(price: String): Int = try {
        BigDecimal(price).setScale(0, RoundingMode.DOWN).toInt()
    } catch (_: Exception) {
        price.substringBefore('.').toIntOrNull() ?: 0
    }

    /** 產生一筆明細的 key（商品 + 客製條件） */
    private fun lineKey(pid: Int, sel: SelectedOptions): String {
        // toppings 排序，避免同選項不同順序導致重複
        val tops = sel.toppings.sorted().joinToString("+")
        return "${pid}|${sel.sweet.orEmpty()}|${sel.ice.orEmpty()}|$tops"
    }

    /** 客製加價 */
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
        add(product, sel, 1)            // ← 這裡不要再發事件，交給下面那支統一發
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
            // CartManager.kt 內
            lines += CartLine(
                productId    = product.id,
                name         = product.name,
                qty          = qty,
                unitPrice    = parsePriceToInt(product.price),
                optionsPrice = optionsPrice(sel),
                // ★ toppings 建議排序一下避免同組合不同順序視為不同 key
                selected     = sel.copy(toppings = sel.toppings.sorted().toMutableList()),
                lineKey      = key,
                imageUrl     = product.imageUrl
            )

        }
        _changes.value = Unit
    }

    /** 提供增減/移除，給 CartAdapter 的 + / − 按鈕用 */
    fun inc(lineKey: String) { changeQty(lineKey, +1) }
    fun dec(lineKey: String) { changeQty(lineKey, -1) }
    fun remove(lineKey: String) {
        val i = lines.indexOfFirst { it.lineKey == lineKey }
        if (i >= 0) {
            lines.removeAt(i)
            _changes.value = Unit
        }
    }

    private fun changeQty(lineKey: String, delta: Int) {
        val i = lines.indexOfFirst { it.lineKey == lineKey }
        if (i < 0) return
        val newQty = (lines[i].qty + delta).coerceAtLeast(0)
        if (newQty == 0) lines.removeAt(i) else lines[i].qty = newQty
        _changes.value = Unit
    }

    /** 小計總額 */
    fun total(): Int = lines.sumOf { (it.unitPrice + it.optionsPrice) * it.qty }

    /** 清空購物車（要記得發事件） */
    fun clear() {
        lines.clear()
        _changes.value = Unit
    }

    /** 是否為空 / 全部數量 / 總金額（語意化 API 給 UI 用） */
    fun isEmpty(): Boolean = lines.isEmpty()
    fun count(): Int = lines.sumOf { it.qty }
    fun totalAmount(): Int = total()
}
