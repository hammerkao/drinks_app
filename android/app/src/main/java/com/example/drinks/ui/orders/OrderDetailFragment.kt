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
import com.example.drinks.data.model.Options
import com.example.drinks.data.model.OrderItemDTO
import com.example.drinks.data.model.SelectedOptions
import com.example.drinks.data.net.Api
import com.example.drinks.net.NetCore
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

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

        // 預設畫面
        renderStoreBlock("—", "—", "—", "—")
        renderOrderBlock("—", "—", "—", "—", "現金", "", 0)
        renderItemsPreview(false)

        fetchFromBackend(orderId)
    }

    private fun fetchFromBackend(orderId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = Api(base = NetCore.BASE_URL, client = NetCore.buildOkHttp(requireContext()))

                // 主資料
                val detail = withContext(Dispatchers.IO) { api.getOrderDetail(orderId) }

                /* ---------- 分店資訊（含 open_hours） ---------- */
                val storeId = detail.storeId
                var storeName: String? = storeId?.let { StoreCatalog.nameOf(it) }
                var storePhone: String? = null
                var storeAddress: String? = null
                var storeHours: String? = null

                if (storeId != null) {
                    withContext(Dispatchers.IO) {
                        runCatching { api.getStores() }.onSuccess { stores ->
                            val hit = stores.firstOrNull { it.id == storeId }
                            if (hit != null) {
                                if (storeName.isNullOrBlank()) storeName = hit.name
                                storePhone   = hit.phone
                                storeAddress = hit.address
                                storeHours   = hit.open_hours
                                runCatching { StoreCatalog.put(storeId, hit.name) }
                            }
                        }
                    }
                }
                renderStoreBlock(storeName ?: "—", storePhone ?: "—", storeAddress ?: "—", storeHours ?: "—")

                /* ---------- 餐點內容 ---------- */
                val apiLines = (detail.items ?: emptyList()).map { it.toCartLine() }
                val snapshot = com.example.drinks.store.CartManager.lastSnapshotLines()

                // 規則：若 API 行項「全部都沒有加料」但快照有加料，則用快照顯示（以補齊加料）
                val apiHasAnyToppings = apiLines.any { it.selected.toppings.isNotEmpty() }
                lines = when {
                    apiLines.isNotEmpty() && apiHasAnyToppings -> apiLines
                    snapshot.isNotEmpty() -> snapshot
                    else -> apiLines
                }

                // 若品名其實是「數字 ID」，補查產品名稱
                withContext(Dispatchers.IO) { resolveProductNamesIfNeeded(api) }

                expanded = lines.size <= 3
                renderItemsPreview(expanded)

                /* ---------- 訂單資訊（後端優先；無則讀 navArgs；付款方式固定顯示「現金」） ---------- */
                val args = requireArguments()
                val pickupMethod = when ((detail.pickupMethod ?: args.getString("pickupMethod"))?.lowercase()) {
                    "pickup", "自取"   -> "自取"
                    "delivery", "外送" -> "外送"
                    else -> "—"
                }
                val pickupTime = formatIsoToTpe(detail.pickupTime)
                    ?: args.getString("pickupTime")
                    ?: formatIsoToTpe(detail.createdAt) ?: "—"

                val buyerName  = detail.buyerName ?: args.getString("buyerName") ?: "—"
                val buyerPhone = detail.buyerPhone ?: args.getString("buyerPhone") ?: "—"

                // 依需求：不論後端回什麼，都顯示「現金」
                val paymentText = "現金"

                val orderNote   = detail.orderNote ?: args.getString("orderNote").orEmpty()

                val totalFromApi = moneyStringToInt(detail.total)
                val displayTotal = if (totalFromApi > 0) totalFromApi else computeTotalFromLines()

                renderOrderBlock(
                    pickupMethod = pickupMethod,
                    pickupTime   = pickupTime,
                    buyerName    = buyerName,
                    buyerPhone   = buyerPhone,
                    paymentMethod= paymentText,
                    orderNote    = orderNote,
                    total        = displayTotal
                )

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "載入失敗：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /* ===== Render ===== */

    private fun renderStoreBlock(name: String, phone: String, address: String, hours: String) {
        tvStoreName.text    = name
        tvStorePhone.text   = "電話：${if (phone.isBlank()) "—" else phone}"
        tvStoreAddress.text = "地址：${if (address.isBlank()) "—" else address}"
        tvStoreHours.text   = "營業時間：${if (hours.isBlank()) "—" else hours}"
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
        tvBuyerPhone.setOrHide("連絡電話：", buyerPhone)
        tvPaymentMethod.setOrHide("付款方式：", paymentMethod)

        if (orderNote.isBlank()) tvOrderNote.visibility = View.GONE
        else { tvOrderNote.visibility = View.VISIBLE; tvOrderNote.text = "備註：$orderNote" }

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
            val parts = mutableListOf<String>()
            l.selected.sweet?.takeIf { it.isNotBlank() }?.let { parts += it }
            l.selected.ice?.takeIf { it.isNotBlank() }?.let { parts += it }
            if (l.selected.toppings.isNotEmpty()) parts += l.selected.toppings.map { "+$it" }  // ← 顯示加料
            l.selected.note?.takeIf { it.isNotBlank() }?.let { parts += "備註：$it" }
            val detail = parts.joinToString("、")

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

    /* 若 name 是產品 ID → 用 products/{id}/ 補真名 */
    private suspend fun resolveProductNamesIfNeeded(api: Api) {
        val ids = lines.mapNotNull { it.name.toIntOrNull()?.takeIf { x -> x > 0 } }.distinct()
        if (ids.isEmpty()) return

        val idToName = mutableMapOf<Int, String>()
        for (pid in ids) {
            runCatching { api.getProduct(pid) }.onSuccess { p -> idToName[pid] = p.name }
        }
        if (idToName.isEmpty()) return

        lines = lines.map { l ->
            val pid = l.name.toIntOrNull()
            if (pid != null && idToName[pid] != null) l.copy(name = idToName[pid]!!) else l
        }
    }

    /* 後端 DTO -> CartLine */
    private fun OrderItemDTO.toCartLine(): CartLine {
        val displayName = this.productName?.takeIf { it.isNotBlank() } ?: "—"

        val lblSweet = this.sweet?.let { Options.label(it) ?: it }
        val lblIce   = this.ice?.let   { Options.label(it) ?: it }
        val lblTops  = (this.toppings ?: emptyList()).map { code -> Options.label(code) ?: code }

        val unit     = moneyStringToInt(this.unitPrice)
        val optPrice = (this.toppings ?: emptyList()).sumOf { id -> Options.toppings[id] ?: 0 }

        val lineKey  = "${lblSweet.orEmpty()}|${lblIce.orEmpty()}|${lblTops.sorted().joinToString(",")}|${this.note.orEmpty()}"

        return CartLine(
            productId    = 0,
            name         = displayName,
            qty          = this.qty,
            unitPrice    = unit,
            optionsPrice = optPrice,
            selected     = SelectedOptions(
                sweet    = lblSweet,
                ice      = lblIce,
                toppings = lblTops.toMutableList(),
                note     = this.note
            ),
            lineKey      = lineKey,
            imageUrl     = null
        )
    }
}
