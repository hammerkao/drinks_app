package com.example.drinks.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.drinks.R
import com.example.drinks.data.json.GsonProvider.gson
import com.example.drinks.data.model.OrderCreateRequest
import com.example.drinks.data.model.OrderItemRequest
import com.example.drinks.data.net.Api as DrinksApi
import com.example.drinks.net.NetCore
import com.example.drinks.store.CartManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CheckoutConfirmFragment : Fragment(R.layout.fragment_checkout_confirm) {

    private lateinit var tvOrderNote: TextView
    private lateinit var toolbar: MaterialToolbar

    // 店家資訊
    private lateinit var tvStoreName: TextView
    private lateinit var tvStorePhone: TextView
    private lateinit var tvStoreAddress: TextView
    private lateinit var tvStoreHours: TextView

    // 餐點內容（可展開）
    private lateinit var previewContainer: LinearLayout
    private lateinit var btnToggleItems: ImageButton
    private var expanded = false

    // 訂單內容
    private lateinit var tvPickupMethod: TextView
    private lateinit var tvPickupTime: TextView
    private lateinit var tvBuyerName: TextView
    private lateinit var tvBuyerPhone: TextView
    private lateinit var tvPaymentMethod: TextView
    private lateinit var tvTotalAmount: TextView

    // 底部操作
    private lateinit var btnModify: MaterialButton
    private lateinit var btnSubmit: MaterialButton

    private fun obtainApi(): DrinksApi =
        DrinksApi(base = NetCore.BASE_URL, client = NetCore.buildOkHttp(requireContext()))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvOrderNote = view.findViewById(R.id.tvOrderNote)

        // Toolbar
        toolbar = view.findViewById(R.id.toolbar)
        view.findViewById<TextView>(R.id.tvToolbarTitle).text = "訂單結算"
        view.findViewById<ImageButton>(R.id.btnNavBack).setOnClickListener { findNavController().popBackStack() }

        // 綁定店家資訊
        tvStoreName    = view.findViewById(R.id.tvStoreName)
        tvStorePhone   = view.findViewById(R.id.tvStorePhone)
        tvStoreAddress = view.findViewById(R.id.tvStoreAddress)
        tvStoreHours   = view.findViewById(R.id.tvStoreHours)

        // 餐點內容
        previewContainer = view.findViewById(R.id.previewContainer)
        btnToggleItems   = view.findViewById(R.id.btnToggleItems)

        // 訂單內容
        tvPickupMethod  = view.findViewById(R.id.tvPickupMethod)
        tvPickupTime    = view.findViewById(R.id.tvPickupTime)
        tvBuyerName     = view.findViewById(R.id.tvBuyerName)
        tvBuyerPhone    = view.findViewById(R.id.tvBuyerPhone)
        tvPaymentMethod = view.findViewById(R.id.tvPaymentMethod)
        tvTotalAmount   = view.findViewById(R.id.tvTotalAmount)

        // 底部操作
        btnModify = view.findViewById(R.id.btnModify)
        btnSubmit = view.findViewById(R.id.btnSubmit)

        // 初始渲染
        renderStoreBlock()
        renderOrderBlock()
        renderItemsPreview(expanded = false)

        // 監聽購物車變更
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                CartManager.changes.collect {
                    renderStoreBlock()
                    renderOrderBlock()
                    renderItemsPreview(expanded)
                }
            }
        }

        btnToggleItems.setOnClickListener {
            expanded = !expanded
            renderItemsPreview(expanded)
        }

        // 修改 -> 回前一頁
        btnModify.setOnClickListener { findNavController().popBackStack() }

        // 送出訂單（方法 B）
        btnSubmit.setOnClickListener {
            if (CartManager.isEmpty()) {
                Toast.makeText(requireContext(), "購物車為空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            MaterialAlertDialogBuilder(requireContext())
                .setMessage("確定送出訂單？")
                .setNegativeButton("取消", null)
                .setPositiveButton("確定") { _, _ -> submitOrder() }
                .show()
        }
    }

    private fun submitOrder() {
        btnSubmit.isEnabled = false
        btnSubmit.text = "送出中..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = obtainApi()

                // 成功頁要用的快照
                val snapshot = CartManager.snapshotForSuccessPage()

                // 分店 ID
                val storeId = arguments?.getInt("store_id")?.takeIf { it > 0 }
                    ?: CartManager.currentStoreId
                    ?: throw IllegalStateException("未取得分店資訊")

                // 組請求並送出
                val req = buildOrderRequest(storeId)
                val created = api.createOrder(req)

                // 清空購物車
                CartManager.clear()

                // 導頁：把快照與資訊帶到成功頁
                val totalInt = moneyStringToInt(created.total)
                val args = Bundle().apply {
                    // 兩個 key 都塞，避免另一頁用不同命名
                    putInt("order_id", created.id)
                    putInt("orderId",  created.id)

                    putInt("total", totalInt)
                    putString("storeName", CartManager.currentStoreName ?: "—")
                    putString("pickupMethod", arguments?.getString("pickupMethod") ?: "自取")
                    putString("paymentMethod", arguments?.getString("paymentMethod") ?: "現金")
                    putString("buyerName", arguments?.getString("buyerName") ?: "—")
                    putString("buyerPhone", arguments?.getString("buyerPhone") ?: "—")
                    putString("orderNote", arguments?.getString("orderNote").orEmpty())
                    putString("createdAt", created.createdAt)      // 成功頁時間以後端為準
                    putString("itemsJson", gson.toJson(snapshot))  // 成功頁顯示用快照
                }
                findNavController().navigate(
                    R.id.action_checkoutConfirm_to_orderSuccess, args
                )
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("送出失敗：${e.message ?: "未知錯誤"}")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            } finally {
                btnSubmit.isEnabled = true
                btnSubmit.text = "送出訂單"
            }
        }
    }

    /* ================= Render ================= */

    private fun renderStoreBlock() {
        tvStoreName.text    = CartManager.currentStoreName ?: "未選擇分店"
        tvStorePhone.text   = "電話：02-12345678"
        tvStoreAddress.text = "地址：新北市板橋區XX路XX號"
        tvStoreHours.text   = "營業時間：09:00 - 22:00"
    }

    private fun renderOrderBlock() {
        val pickupMethod  = arguments?.getString("pickupMethod") ?: "自取"
        val paymentMethod = arguments?.getString("paymentMethod") ?: "現金"
        val buyerName     = arguments?.getString("buyerName") ?: "—"
        val buyerPhone    = arguments?.getString("buyerPhone") ?: "—"
        val orderNote     = arguments?.getString("orderNote").orEmpty()

        tvPickupMethod.text  = "取餐方式：$pickupMethod"
        tvPickupTime.text    = "取餐時間：" + nowTimeDisplay()
        tvBuyerName.text     = "訂購人：$buyerName"
        tvBuyerPhone.text    = "聯絡電話：$buyerPhone"
        tvPaymentMethod.text = "付款方式：$paymentMethod"

        if (orderNote.isBlank()) {
            tvOrderNote.visibility = View.GONE
        } else {
            tvOrderNote.visibility = View.VISIBLE
            tvOrderNote.text = "訂單備註：$orderNote"
        }

        tvTotalAmount.text = "應付金額：" + formatTWD(CartManager.totalAmount())
    }

    private fun renderItemsPreview(expanded: Boolean) {
        previewContainer.removeAllViews()

        val lines = CartManager.getLines()
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
            if (l.selected.toppings.isNotEmpty()) parts += l.selected.toppings.map { "+$it" }
            l.selected.note?.takeIf { it.isNotBlank() }?.let { parts += "備註：$it" }
            val detail = parts.joinToString("、")

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

    private fun nowTimeDisplay(): String {
        val tz = java.util.TimeZone.getTimeZone("Asia/Taipei")
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.TAIWAN)
        sdf.timeZone = tz
        return sdf.format(java.util.Date())
    }

    /* =============== 方法 B：組下單 Request =============== */
    private fun buildOrderRequest(storeId: Int): OrderCreateRequest {
        val uiPickup   = arguments?.getString("pickupMethod") ?: "自取"
        val uiPayment  = arguments?.getString("paymentMethod") ?: "現金"
        val buyerName  = arguments?.getString("buyerName") ?: "—"
        val buyerPhone = arguments?.getString("buyerPhone") ?: "—"
        val orderNote  = arguments?.getString("orderNote").orEmpty()

        val pickupCode = when (uiPickup) { "自取" -> "pickup"; "外送" -> "delivery"; else -> "pickup" }
        val paymentCode = when (uiPayment) {
            "現金" -> "cash"; "信用卡" -> "card";
            "LinePay","LINE Pay" -> "linepay"; "Apple Pay" -> "applepay"; "Google Pay" -> "googlepay";
            else -> "cash"
        }

        fun makeOptionsKey(sweet: String?, ice: String?, toppings: List<String>?, note: String?): String {
            val tops = (toppings ?: emptyList()).sorted().joinToString(",")
            return listOf(sweet ?: "", ice ?: "", tops, note ?: "").joinToString("|")
        }

        val items = CartManager.getLines().map { l ->
            val sweet = l.selected.sweet?.takeIf { it.isNotBlank() }
            val ice   = l.selected.ice?.takeIf { it.isNotBlank() }
            val tops  = l.selected.toppings.takeIf { it.isNotEmpty() }
            val note  = l.selected.note?.takeIf { it.isNotBlank() }
            OrderItemRequest(
                productId    = l.productId,
                qty          = l.qty,
                unitPrice    = l.unitPrice,
                optionsPrice = l.optionsPrice,
                sweet        = sweet,
                ice          = ice,
                toppings     = tops,
                note         = note,
                optionsKey   = makeOptionsKey(sweet, ice, tops, note)
            )
        }

        // 取現在時間（台北時區）為取餐時間
        val tz  = java.util.TimeZone.getTimeZone("Asia/Taipei")
        val iso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.TAIWAN)
            .apply { timeZone = tz }
            .format(java.util.Date())

        return OrderCreateRequest(
            storeId       = storeId,
            pickupMethod  = pickupCode,
            pickupTime    = iso,
            buyerName     = buyerName,
            buyerPhone    = buyerPhone,
            paymentMethod = paymentCode,
            orderNote     = orderNote.ifBlank { null },
            items         = items
        )
    }

    /* ===== 小工具 ===== */
    private fun moneyStringToInt(s: String?): Int {
        if (s.isNullOrBlank()) return 0
        return s.toBigDecimalOrNull()?.setScale(0, java.math.RoundingMode.HALF_UP)?.toInt() ?: 0
    }
}
