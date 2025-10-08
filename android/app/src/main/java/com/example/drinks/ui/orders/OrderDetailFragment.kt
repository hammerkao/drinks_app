// OrderDetailFragment.kt
package com.example.drinks.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.drinks.R
import com.example.drinks.data.cache.StoreCatalog
import com.example.drinks.data.model.CartLine
import com.example.drinks.data.model.SelectedOptions
import com.example.drinks.data.model.OrderItemDTO
import com.example.drinks.data.model.Options
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import com.example.drinks.data.json.GsonProvider.gson


class OrderDetailFragment : Fragment(R.layout.fragment_order_detail) {

    private lateinit var toolbar: MaterialToolbar

    // 店家資訊
    private lateinit var tvStoreName: TextView
    private lateinit var tvStorePhone: TextView
    private lateinit var tvStoreAddress: TextView
    private lateinit var tvStoreHours: TextView

    // 餐點內容
    private lateinit var previewContainer: LinearLayout
    private lateinit var btnToggleItems: ImageButton
    private var expanded = false

    // 訂單內容
    private lateinit var tvPickupMethod: TextView
    private lateinit var tvPickupTime: TextView
    private lateinit var tvBuyerName: TextView
    private lateinit var tvBuyerPhone: TextView
    private lateinit var tvPaymentMethod: TextView
    private lateinit var tvOrderNote: TextView
    private lateinit var tvTotalAmount: TextView

    // 資料
    private var lines: List<CartLine> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        toolbar = view.findViewById(R.id.toolbar)
        view.findViewById<TextView>(R.id.tvToolbarTitle).text = "訂單明細"
        view.findViewById<ImageButton>(R.id.btnNavBack).setOnClickListener { findNavController().popBackStack() }

        // 綁 View
        tvStoreName    = view.findViewById(R.id.tvStoreName)
        tvStorePhone   = view.findViewById(R.id.tvStorePhone)
        tvStoreAddress = view.findViewById(R.id.tvStoreAddress)
        tvStoreHours   = view.findViewById(R.id.tvStoreHours)

        previewContainer = view.findViewById(R.id.previewContainer)
        btnToggleItems   = view.findViewById(R.id.btnToggleItems)

        tvPickupMethod  = view.findViewById(R.id.tvPickupMethod)
        tvPickupTime    = view.findViewById(R.id.tvPickupTime)
        tvBuyerName     = view.findViewById(R.id.tvBuyerName)
        tvBuyerPhone    = view.findViewById(R.id.tvBuyerPhone)
        tvPaymentMethod = view.findViewById(R.id.tvPaymentMethod)
        tvOrderNote     = view.findViewById(R.id.tvOrderNote)
        tvTotalAmount   = view.findViewById(R.id.tvTotalAmount)

        val orderId = requireArguments().getInt("order_id", -1)
        if (orderId <= 0) {
            Toast.makeText(requireContext(), "缺少訂單編號", Toast.LENGTH_SHORT).show()
            return
        }

        // 初始畫面
        renderStoreBlock("—")
        renderOrderBlock("自取", "—", "—", "—", "—", "", 0)
        renderItemsPreview(false)

        // 以後端為準載入詳情
        fetchFromBackend(orderId)
    }
    private suspend fun resolveStoreName(
        api: com.example.drinks.data.net.Api,
        storeId: Int?
    ): String {
        if (storeId == null || storeId <= 0) return "—"

        // 1) 先查快取
        com.example.drinks.data.cache.StoreCatalog.nameOf(storeId)?.let { return it }

        // 2) 快取沒有 → 打 stores API，補回快取
        return try {
            val stores = api.getStores()
            val hit = stores.firstOrNull { it.id == storeId }
            val name = hit?.name?.takeIf { it.isNotBlank() } ?: "門市 #$storeId"
            com.example.drinks.data.cache.StoreCatalog.put(storeId, hit?.name)
            name
        } catch (_: Exception) {
            "門市 #$storeId"
        }
    }

    /* === 以後端為準載入 === */
    private fun fetchFromBackend(orderId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = com.example.drinks.data.net.Api(
                    base = com.example.drinks.net.NetCore.BASE_URL,
                    client = com.example.drinks.net.NetCore.buildOkHttp(requireContext())
                )

                // 優先用 /orders/{id}；沒有就退用 list + find
                val detail = runCatching { api.getOrderDetail(orderId) }.getOrNull()
                    ?: api.listOrders().firstOrNull { it.id == orderId }
                    ?: run {
                        Toast.makeText(requireContext(), "找不到訂單", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                // 店家名稱：快取優先，否則退為「門市 #id」或 "—"
                val storeName = resolveStoreName(api, detail.storeId)

                // 餐點：後端 items -> CartLine
                lines = (detail.items ?: emptyList()).map { it.toCartLine() }

                // 總額：以後端 total 優先，沒有就以餐點重算
                val totalFromApi = moneyStringToInt(detail.total)
                val displayTotal = if (totalFromApi > 0) totalFromApi else computeTotalFromLines()

                // 時間
                val displayTime = formatIsoToTpe(detail.createdAt) ?: (detail.createdAt ?: "—")

                // 付款/買家（後端若沒有，就保持「—」並自動隱藏）
                val buyer = "—"
                val phone = "—"
                val pay   = "—"
                val note  = ""

                // Render
                renderStoreBlock(storeName)
                renderOrderBlock(
                    pickupMethod = "自取",
                    pickupTime   = displayTime,
                    buyerName    = buyer,
                    buyerPhone   = phone,
                    paymentMethod= pay,
                    orderNote    = note,
                    total        = displayTotal
                )
                expanded = lines.size <= 3
                renderItemsPreview(expanded)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "載入失敗：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /* === Render 區 === */

    private fun renderStoreBlock(storeName: String) {
        tvStoreName.text    = storeName
        tvStorePhone.text   = "電話：02-12345678"
        tvStoreAddress.text = "地址：新北市板橋區XX路XX號"
        tvStoreHours.text   = "營業時間：09:00 - 22:00"
    }

    private fun TextView.setOrHide(prefix: String, value: String) {
        if (value == "—" || value.isBlank()) visibility = View.GONE
        else { visibility = View.VISIBLE; text = "$prefix$value" }
    }

    private fun renderOrderBlock(
        pickupMethod: String,
        pickupTime: String,
        buyerName: String,
        buyerPhone: String,
        paymentMethod: String,
        orderNote: String,
        total: Int
    ) {
        tvPickupMethod.text = "取餐方式：$pickupMethod"
        tvPickupTime.text   = "取餐時間：$pickupTime"
        tvBuyerName.setOrHide("訂購人：", buyerName)
        tvBuyerPhone.setOrHide("聯絡電話：", buyerPhone)
        tvPaymentMethod.setOrHide("付款方式：", paymentMethod)

        if (orderNote.isBlank()) tvOrderNote.visibility = View.GONE
        else { tvOrderNote.visibility = View.VISIBLE; tvOrderNote.text = "訂單備註：$orderNote" }

        tvTotalAmount.text = "應付金額：" + formatTWD(total)
    }

    private fun renderItemsPreview(expanded: Boolean) {
        previewContainer.removeAllViews()
        if (lines.isEmpty()) {
            addRow("（無餐點）", "")
            btnToggleItems.visibility = View.GONE
            return
        }
        btnToggleItems.visibility = View.VISIBLE
        btnToggleItems.setImageResource(
            if (expanded) R.drawable.ic_arrow_up_24 else R.drawable.ic_arrow_down_24
        )

        val toShow = if (expanded) lines else listOf(lines.first())
        toShow.forEach { l ->
            val detail = buildList {
                l.selected.sweet?.takeIf { it.isNotBlank() }?.let { add(it) }
                l.selected.ice?.takeIf { it.isNotBlank() }?.let { add(it) }
                if (l.selected.toppings.isNotEmpty()) addAll(l.selected.toppings)
                l.selected.note?.takeIf { it.isNotBlank() }?.let { add("備註：$it") }
            }.joinToString("、")

            val left  = "${l.qty}  ${l.name}" + if (detail.isNotBlank()) "\n$detail" else ""
            val right = formatTWD(l.subtotal)
            addRow(left, right)
        }

        btnToggleItems.setOnClickListener {
            this.expanded = !this.expanded
            renderItemsPreview(this.expanded)
        }
    }

    private fun addRow(leftText: String, rightText: String) {
        val row = LayoutInflater.from(requireContext())
            .inflate(R.layout.row_confirm_item_text, previewContainer, false)
        row.findViewById<TextView>(R.id.tvLeft).text = leftText
        row.findViewById<TextView>(R.id.tvRight).text = rightText
        previewContainer.addView(row)
    }

    private fun formatTWD(n: Int): String =
        NumberFormat.getCurrencyInstance(Locale.TAIWAN).format(n)

    private fun computeTotalFromLines(): Int = lines.sumOf { it.subtotal }

    private fun moneyStringToInt(s: String?): Int {
        if (s.isNullOrBlank()) return 0
        return try { BigDecimal(s).setScale(0, RoundingMode.HALF_UP).toInt() }
        catch (_: Exception) { 0 }
    }

    /** ISO8601 → 台北時區 yyyy/MM/dd HH:mm */
    private fun formatIsoToTpe(iso: String?): String? {
        if (iso.isNullOrBlank()) return null
        val pattern = java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
        return runCatching {
            java.time.OffsetDateTime.parse(iso)
                .atZoneSameInstant(java.time.ZoneId.of("Asia/Taipei"))
                .format(pattern)
        }.recoverCatching {
            java.time.Instant.parse(iso)
                .atZone(java.time.ZoneId.of("Asia/Taipei"))
                .format(pattern)
        }.getOrNull()
    }

    /* 後端 DTO -> 畫面用 CartLine（單一版本，請保留這個就好） */
    private fun OrderItemDTO.toCartLine(): CartLine {
        // 後端 product 可能是「名稱」或「ID 字串」
        val parsedId = this.productName?.toIntOrNull()
        val productId = parsedId ?: 0

        // 名稱：先用後端字串；若是純數字找不到名稱，暫用「品項 #id」
        val displayName = when {
            !this.productName.isNullOrBlank() && parsedId == null -> this.productName!!
            parsedId != null -> "品項 #$parsedId"   // 之後若接 ProductCatalog 可換真名
            else -> "—"
        }

        val lblSweet = this.sweet?.let { Options.label(it) }
        val lblIce   = this.ice?.let { Options.label(it) }
        val lblTops  = (this.toppings ?: emptyList()).map { Options.label(it) }.toMutableList()

        val unit     = moneyStringToInt(this.unitPrice)
        val optPrice = (this.toppings ?: emptyList()).sumOf { id -> Options.toppings[id] ?: 0 }

        val lineKey  = "${lblSweet.orEmpty()}|${lblIce.orEmpty()}|${lblTops.sorted().joinToString(",")}|${this.note.orEmpty()}"

        return CartLine(
            productId   = productId,
            name        = displayName,
            qty         = this.qty,
            unitPrice   = unit,
            optionsPrice= optPrice,
            selected    = SelectedOptions(
                sweet    = lblSweet,
                ice      = lblIce,
                toppings = lblTops,
                note     = this.note
            ),
            lineKey     = lineKey,
            imageUrl    = null
        )
    }
}
