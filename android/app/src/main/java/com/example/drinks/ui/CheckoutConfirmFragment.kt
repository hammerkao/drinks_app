package com.example.drinks.ui

import android.view.LayoutInflater
import android.os.Bundle
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
import com.example.drinks.store.CartManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
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
    private var expanded: Boolean = false

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        tvOrderNote = view.findViewById(R.id.tvOrderNote)
        // Toolbar
        toolbar = view.findViewById(R.id.toolbar)
        val btnBack = view.findViewById<ImageButton>(R.id.btnNavBack)
        val tvTitle = view.findViewById<TextView>(R.id.tvToolbarTitle)

        tvTitle.text = "訂單結算" // 需要動態改標題也從這裡改
        btnBack.setOnClickListener { findNavController().popBackStack() }

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

        // 監聽購物車變更（回前頁修改後再回來也會更新）
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                CartManager.changes.collect {
                    renderStoreBlock()
                    renderOrderBlock()
                    renderItemsPreview(expanded)
                }
            }
        }

        // 展開/收合
        btnToggleItems.setOnClickListener {
            expanded = !expanded
            renderItemsPreview(expanded)
        }

        // 修改 -> 回前一頁（訂單結算）
        btnModify.setOnClickListener {
            findNavController().popBackStack()
        }

        // 送出訂單（示範：確認 -> 清空 -> 切到「訂單」tab）
        btnSubmit.setOnClickListener {
            if (CartManager.isEmpty()) {
                Toast.makeText(requireContext(), "購物車為空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            MaterialAlertDialogBuilder(requireContext())
                .setMessage("確認送出訂單？")
                .setNegativeButton("取消", null)
                .setPositiveButton("送出") { _, _ ->
                    // TODO: 呼叫建立訂單 API
                    CartManager.clear()
                    val bottom = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)
                    bottom?.selectedItemId = R.id.nav_orders
                    Toast.makeText(requireContext(), "已送出訂單", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    }

    // ======= Render =======

    private fun renderStoreBlock() {
        tvStoreName.text = CartManager.currentStoreName ?: "未選擇分店"
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

        // ✅ 備註：有字就顯示，空白就隱藏（避免多佔一行）
        if (orderNote.isBlank()) {
            tvOrderNote.visibility = View.GONE
        } else {
            tvOrderNote.visibility = View.VISIBLE
            tvOrderNote.text = "訂單備註：$orderNote"
        }

        tvTotalAmount.text   = "應付金額：" + formatTWD(CartManager.totalAmount())
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
            val qty = l.qty
            val name = l.name
            val detail = buildList {
                l.selected.sweet?.takeIf { it.isNotBlank() }?.let { add(it) }
                l.selected.ice?.takeIf { it.isNotBlank() }?.let { add(it) }
                if (l.selected.toppings.isNotEmpty()) addAll(l.selected.toppings)
                l.selected.note?.takeIf { it.isNotBlank() }?.let { add("備註：${it}") }
            }.joinToString("、")

            // ✅ 只顯示數量，不顯示編號
            val left = "$qty  $name" + if (detail.isNotBlank()) "\n$detail" else ""
            val right = formatTWD((l.unitPrice + l.optionsPrice) * l.qty)

            addRow(leftText = left, rightText = right)
        }
    }

    /** 以共用 row_confirm_item_text.xml 新增一行 */
    private fun addRow(leftText: String, rightText: String) {
        val row = LayoutInflater.from(requireContext())
            .inflate(R.layout.row_confirm_item_text, previewContainer, false)
        row.findViewById<TextView>(R.id.tvLeft).text = leftText
        row.findViewById<TextView>(R.id.tvRight).text = rightText
        previewContainer.addView(row)
    }

    private fun formatTWD(n: Int): String =
        NumberFormat.getCurrencyInstance(Locale.TAIWAN).format(n)

    /** API 24 OK 的時間格式 */
    private fun nowTimeDisplay(): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN)
        return sdf.format(Date())
    }
}
