package com.example.drinks.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.drinks.R
import com.example.drinks.data.json.GsonProvider.gson
import com.example.drinks.data.model.CartLine
import com.google.android.material.appbar.MaterialToolbar
import java.text.NumberFormat
import java.util.Locale

class OrderDetailFragment : Fragment(R.layout.fragment_order_detail) {

    private lateinit var toolbar: MaterialToolbar

    // 店家資訊
    private lateinit var tvStoreName: TextView
    private lateinit var tvStorePhone: TextView
    private lateinit var tvStoreAddress: TextView
    private lateinit var tvStoreHours: TextView

    // 餐點內容（可展開/收合）
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
        view.findViewById<ImageButton>(R.id.btnNavBack).setOnClickListener {
            findNavController().popBackStack()
        }

        // 綁定 View
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

        // 以 order_id 為主（從成功頁或列表傳入）
        val orderId = requireArguments().getInt("order_id", -1)
        if (orderId <= 0) {
            Toast.makeText(requireContext(), "缺少訂單編號", Toast.LENGTH_SHORT).show()
            // 仍然嘗試用快照渲染
        }

        // 快照資料（有就用）
        val storeName     = arguments?.getString("storeName") ?: "—"
        val pickupMethod  = arguments?.getString("pickupMethod") ?: "自取"
        val pickupTime    = arguments?.getString("pickupTime") ?: "—"
        val buyerName     = arguments?.getString("buyerName") ?: "—"
        val buyerPhone    = arguments?.getString("buyerPhone") ?: "—"
        val paymentMethod = arguments?.getString("paymentMethod") ?: "—"
        val orderNote     = arguments?.getString("orderNote").orEmpty()
        val totalFromArgs = arguments?.getInt("total", 0) ?: 0

        // itemsJson -> List<CartLine>
        val itemsJson = arguments?.getString("itemsJson")
        lines = try {
            if (itemsJson.isNullOrBlank()) emptyList()
            else gson.fromJson(itemsJson, Array<CartLine>::class.java).toList()
        } catch (_: Exception) {
            emptyList()
        }

        // ✅ 以快照重算金額；沒有快照才用參數 total
        val displayTotal = if (lines.isNotEmpty()) computeTotalFromLines() else totalFromArgs

        // ===== Render（只呼叫一次）=====
        renderStoreBlock(storeName)
        renderOrderBlock(
            pickupMethod = pickupMethod,
            pickupTime = pickupTime,
            buyerName = buyerName,
            buyerPhone = buyerPhone,
            paymentMethod = paymentMethod,
            orderNote = orderNote,
            total = displayTotal
        )

        // 餐點列表：<=3 筆就預設展開
        expanded = lines.size <= 3
        renderItemsPreview(expanded)

        btnToggleItems.setOnClickListener {
            expanded = !expanded
            renderItemsPreview(expanded)
        }
    }

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

            val left  = "${l.qty}  ${l.name}" + if (detail.isNotBlank()) "\n$detail" else ""
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

    private fun formatTWD(n: Int): String =
        NumberFormat.getCurrencyInstance(Locale.TAIWAN).format(n)

    private fun computeTotalFromLines(): Int =
        lines.sumOf { (it.unitPrice + it.optionsPrice) * it.qty }
}
