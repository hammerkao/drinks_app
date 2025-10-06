package com.example.drinks.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.drinks.R
import com.example.drinks.store.CartManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import android.util.Log

class CheckoutFragment : Fragment(R.layout.fragment_checkout) {

    private lateinit var toolbar: MaterialToolbar

    private lateinit var tvStoreName: TextView
    private lateinit var tvSubtotal: TextView
    private lateinit var rv: androidx.recyclerview.widget.RecyclerView

    private lateinit var btnPickup: MaterialButton
    private lateinit var btnDelivery: MaterialButton

    private lateinit var etBuyerName: TextInputEditText
    private lateinit var etBuyerPhone: TextInputEditText

    private lateinit var rgPayment: RadioGroup
    private lateinit var rbCash: RadioButton

    private lateinit var tilOrderNote: TextInputLayout
    private lateinit var etOrderNote: TextInputEditText

    private lateinit var btnNext: MaterialButton

    // ✅ 結帳頁僅顯示，不允許調整數量
    private val adapter by lazy {
        CartAdapter(
            items = CartManager.getLines(),
            onInc = { /* no-op */ },
            onDec = { _, _ -> /* no-op */ },
            readOnly = true
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar + 返回鍵（白色）
        toolbar = view.findViewById(R.id.toolbar)
        val btnBack = view.findViewById<ImageButton>(R.id.btnNavBack)
        val tvTitle = view.findViewById<TextView>(R.id.tvToolbarTitle)
        tvTitle.text = "訂單結算"
        btnBack.setOnClickListener { findNavController().popBackStack() }

        // 綁定視圖
        tvStoreName = view.findViewById(R.id.tvStoreNameCheckout)
        tvSubtotal  = view.findViewById(R.id.tvCheckoutSubtotal)
        rv          = view.findViewById(R.id.rvCheckoutItems)

        btnPickup   = view.findViewById(R.id.btnPickup)
        btnDelivery = view.findViewById(R.id.btnDelivery)

        etBuyerName  = view.findViewById(R.id.etBuyerName)
        etBuyerPhone = view.findViewById(R.id.etBuyerPhone)

        rgPayment = view.findViewById(R.id.rgPayment)
        rbCash    = view.findViewById(R.id.rbCash)

        tilOrderNote = view.findViewById(R.id.tilOrderNote)
        etOrderNote  = view.findViewById(R.id.etOrderNote)

        btnNext = view.findViewById(R.id.btnCheckoutNext)

        // ===== 取得分店資訊（安全讀取）=====
        val storeIdFromArgs = arguments?.getInt("store_id", 0) ?: 0
        val storeNameFromArgs = arguments?.getString("store_name")

        val storeId = when {
            storeIdFromArgs > 0 -> storeIdFromArgs
            CartManager.currentStoreId != null -> CartManager.currentStoreId!!
            else -> 0
        }
        val storeName = storeNameFromArgs ?: CartManager.currentStoreName ?: "未選擇分店"

        Log.d("Checkout", "resolved storeId=$storeId, storeName=$storeName")

        if (storeId <= 0) {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage("未取得分店資訊，請先選擇分店。")
                .setPositiveButton(android.R.string.ok) { _, _ -> findNavController().popBackStack() }
                .show()
            return
        }

        // 顯示分店名稱
        tvStoreName.text = storeName

        // RecyclerView
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        adapter.submit(CartManager.getLines())

        // 顯示小計
        renderSubtotal()

        // 監聽購物車變更，自動刷新
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                CartManager.changes.collect {
                    adapter.submit(CartManager.getLines())
                    renderSubtotal()
                }
            }
        }

        // 取餐方式（外送已在 XML disabled）
        btnPickup.setOnClickListener {
            Toast.makeText(requireContext(), "已選擇：自取", Toast.LENGTH_SHORT).show()
        }

        // 手機格式即時提示（可選）
        etBuyerPhone.addTextChangedListener(simpleWatcher {
            if (!isValidPhone(etBuyerPhone.text?.toString())) {
                etBuyerPhone.error = "請輸入有效手機號碼"
            } else {
                etBuyerPhone.error = null
            }
        })

        // 「下一步」
        btnNext.setOnClickListener {
            // 先做表單檢查
            if (!ensureFormValid()) return@setOnClickListener

            // 訂單備註長度限制（50）
            val note = etOrderNote.text?.toString()?.trim().orEmpty()
            if (note.length > 50) {
                tilOrderNote.error = "最多 50 字"
                return@setOnClickListener
            } else {
                tilOrderNote.error = null
            }

            if (CartManager.isEmpty()) {
                Toast.makeText(requireContext(), "購物車為空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 取值（目前只開放自取）
            val pickupMethod  = "自取"
            val paymentMethod = view.findViewById<RadioButton>(rgPayment.checkedRadioButtonId)
                ?.text?.toString() ?: "現金"
            val buyerName  = etBuyerName.text?.toString()?.trim().orEmpty()
            val buyerPhone = etBuyerPhone.text?.toString()?.trim().orEmpty()
            val orderNote  = note

            // 導頁 + 帶參數
            val args = Bundle().apply {
                putInt("store_id", storeId)
                putString("store_name", storeName)
                putString("pickupMethod", pickupMethod)
                putString("paymentMethod", paymentMethod)
                putString("buyerName", buyerName)
                putString("buyerPhone", buyerPhone)
                putString("orderNote", orderNote)
            }
            findNavController().navigate(R.id.checkoutConfirmFragment, args)
        }
    }

    private fun renderSubtotal() {
        tvSubtotal.text = formatTWD(CartManager.totalAmount())
    }

    private fun ensureFormValid(): Boolean {
        val name = etBuyerName.text?.toString()?.trim().orEmpty()
        val phone = etBuyerPhone.text?.toString()?.trim().orEmpty()

        if (name.isEmpty()) {
            etBuyerName.error = "請填寫姓名"
            etBuyerName.requestFocus()
            return false
        }
        if (!isValidPhone(phone)) {
            etBuyerPhone.error = "請輸入有效手機號碼"
            etBuyerPhone.requestFocus()
            return false
        }
        if (rgPayment.checkedRadioButtonId == -1) {
            Toast.makeText(requireContext(), "請選擇付款方式", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun isValidPhone(p: String?): Boolean =
        p != null && Regex("^09\\d{8}$").matches(p) // 台灣手機簡易檢核

    private fun simpleWatcher(onChange: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChange()
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun formatTWD(n: Int): String =
        NumberFormat.getCurrencyInstance(Locale.TAIWAN).format(n)
}
