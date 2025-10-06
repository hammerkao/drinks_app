package com.example.drinks.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.drinks.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.util.Locale

// 你的 Gson 單例與資料結構
import com.example.drinks.data.json.GsonProvider.gson
import com.example.drinks.data.model.CartLine

class OrderSuccessFragment : Fragment(R.layout.fragment_order_success) {

    private lateinit var toolbar: MaterialToolbar

    // 店家資訊
    private lateinit var tvStoreName: TextView
    private lateinit var tvStorePhone: TextView
    private lateinit var tvStoreAddress: TextView
    private lateinit var tvStoreHours: TextView

    // 餐點內容（可展開/收合）
    private lateinit var previewContainer: LinearLayout
    private lateinit var btnToggleItems: ImageButton
    private var expanded: Boolean = false

    // 訂單內容
    private lateinit var tvPickupMethod: TextView
    private lateinit var tvPickupTime: TextView
    private lateinit var tvBuyerName: TextView
    private lateinit var tvBuyerPhone: TextView
    private lateinit var tvPaymentMethod: TextView
    private lateinit var tvOrderNote: TextView
    private lateinit var tvTotalAmount: TextView

    // 底部：查看訂單
    private lateinit var btnViewOrder: MaterialButton

    // 本頁要畫的餐點快照（由上一頁傳進來）
    private var lines: List<CartLine> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
//        toolbar = view.findViewById(R.id.toolbar)
//        view.findViewById<ImageButton>(R.id.btnNavBack).setOnClickListener {
//            findNavController().popBackStack()
//        }
        view.findViewById<TextView>(R.id.tvToolbarTitle).text = "下單成功"

        // 綁定店家區
        tvStoreName    = view.findViewById(R.id.tvStoreName)
        tvStorePhone   = view.findViewById(R.id.tvStorePhone)
        tvStoreAddress = view.findViewById(R.id.tvStoreAddress)
        tvStoreHours   = view.findViewById(R.id.tvStoreHours)

        // 綁定餐點區
        previewContainer = view.findViewById(R.id.previewContainer)
        btnToggleItems   = view.findViewById(R.id.btnToggleItems)

        // 綁定訂單區
        tvPickupMethod  = view.findViewById(R.id.tvPickupMethod)
        tvPickupTime    = view.findViewById(R.id.tvPickupTime)
        tvBuyerName     = view.findViewById(R.id.tvBuyerName)
        tvBuyerPhone    = view.findViewById(R.id.tvBuyerPhone)
        tvPaymentMethod = view.findViewById(R.id.tvPaymentMethod)
        tvOrderNote     = view.findViewById(R.id.tvOrderNote)
        tvTotalAmount   = view.findViewById(R.id.tvTotalAmount)

        // 底部按鈕
        btnViewOrder = view.findViewById(R.id.btnViewOrder)

        // ===== 讀取上一頁傳來的參數 =====
        val storeName     = arguments?.getString("storeName") ?: "—"
        val pickupMethod  = arguments?.getString("pickupMethod") ?: "自取"

        // 後端建立時間（ISO8601，含偏移 / Z），優先用這個
        val createdAtIso  = arguments?.getString("createdAt")

        // 若沒有 createdAt，就退回上一頁傳來的字串（或用現在時間）
        val fallbackPickupTime = arguments?.getString("pickupTime") ?: nowTimeDisplay()

        val buyerName     = arguments?.getString("buyerName") ?: "—"
        val buyerPhone    = arguments?.getString("buyerPhone") ?: "—"
        val paymentMethod = arguments?.getString("paymentMethod") ?: "—"
        val orderNote     = arguments?.getString("orderNote").orEmpty()
        val total         = arguments?.getInt("total", 0) ?: 0

        // 餐點快照（CheckoutConfirmFragment 傳來的 JSON）
        val itemsJson = arguments?.getString("itemsJson")
        lines = try {
            if (itemsJson.isNullOrBlank()) emptyList()
            else gson.fromJson(itemsJson, Array<CartLine>::class.java).toList()
        } catch (_: Exception) {
            emptyList()
        }

        // ✅ 單一來源決定顯示的時間：優先把 createdAt 轉台北時區；沒有就用 fallback
        val displayTime = formatIsoToTpe(createdAtIso) ?: fallbackPickupTime

        // ===== 渲染畫面 =====
        renderStoreBlock(storeName)
        renderOrderBlock(
            pickupMethod = pickupMethod,
            pickupTime = displayTime,
            buyerName = buyerName,
            buyerPhone = buyerPhone,
            paymentMethod = paymentMethod,
            orderNote = orderNote,
            total = total
        )
        renderItemsPreview(expanded = false)

        // 展開/收合
        btnToggleItems.setOnClickListener {
            expanded = !expanded
            renderItemsPreview(expanded)
        }

        // 查看訂單 → 先切到底部「訂單」Tab（你的明細頁之後補上）
        btnViewOrder.setOnClickListener {
            val bottom = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)
            bottom?.selectedItemId = R.id.nav_orders
        }
    }

    // ======= Render 區 =======

    private fun renderStoreBlock(storeName: String) {
        tvStoreName.text    = storeName
        tvStorePhone.text   = "電話：02-12345678"
        tvStoreAddress.text = "地址：新北市板橋區XX路XX號"
        tvStoreHours.text   = "營業時間：09:00 - 22:00"
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
        tvPickupMethod.text  = "取餐方式：$pickupMethod"
        tvPickupTime.text    = "取餐時間：$pickupTime"
        tvBuyerName.text     = "訂購人：$buyerName"
        tvBuyerPhone.text    = "聯絡電話：$buyerPhone"
        tvPaymentMethod.text = "付款方式：$paymentMethod"

        if (orderNote.isBlank()) {
            tvOrderNote.visibility = View.GONE
        } else {
            tvOrderNote.visibility = View.VISIBLE
            tvOrderNote.text = "訂單備註：$orderNote"
        }
        tvTotalAmount.text   = "應付金額：" + formatTWD(total)
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

            val left = "${l.qty}  ${l.name}" + if (detail.isNotBlank()) "\n$detail" else ""
            val right = formatTWD((l.unitPrice + l.optionsPrice) * l.qty)
            addRow(left, right)
        }
    }

    private fun addRow(leftText: String, rightText: String) {
        val row = LayoutInflater.from(requireContext())
            .inflate(R.layout.row_confirm_item_text, previewContainer, false)
        row.findViewById<TextView>(R.id.tvLeft).text = leftText
        row.findViewById<TextView>(R.id.tvRight).text = rightText
        previewContainer.addView(row)
    }

    // ======= Utils =======

    private fun formatTWD(n: Int): String =
        NumberFormat.getCurrencyInstance(Locale.TAIWAN).format(n)

    /** 現在時間（台北時區） */
    private fun nowTimeDisplay(): String {
        val tz = java.util.TimeZone.getTimeZone("Asia/Taipei")
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.TAIWAN)
        sdf.timeZone = tz
        return sdf.format(java.util.Date())
    }

    /** 把後端 ISO8601（含偏移或 Z）轉「台北時區」字串：yyyy/MM/dd HH:mm */
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
}
