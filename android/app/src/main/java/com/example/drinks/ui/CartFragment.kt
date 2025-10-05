package com.example.drinks.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.store.CartManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CartFragment : Fragment(R.layout.fragment_cart) {

    private lateinit var adapter: CartAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvCart)
        val tvSubtotal = view.findViewById<TextView>(R.id.tvSubtotal)
        val btnCheckout = view.findViewById<Button>(R.id.btnCheckout)
        val tvStoreName = view.findViewById<TextView>(R.id.tvCartStoreName)

        // 初始顯示分店
        tvStoreName?.text = CartManager.currentStoreName ?: "未選擇分店"

        adapter = CartAdapter(
            items = CartManager.getLines(),
            onInc = { key ->
                CartManager.inc(key)
                adapter.submit(CartManager.getLines())
                tvSubtotal.text = formatTWD(CartManager.totalAmount())
            },
            onDec = { key, qty ->
                if (qty <= 1) {
                    val lineName = CartManager.getLines()
                        .firstOrNull { it.lineKey == key }?.name ?: "品項"
                    MaterialAlertDialogBuilder(requireContext())
                        .setMessage("確定移除 $lineName ？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("移除") { _, _ ->
                            CartManager.remove(key)
                            adapter.submit(CartManager.getLines())
                            tvSubtotal.text = formatTWD(CartManager.totalAmount())
                        }
                        .show()
                } else {
                    CartManager.dec(key)
                    adapter.submit(CartManager.getLines())
                    tvSubtotal.text = formatTWD(CartManager.totalAmount())
                }
            }
        )
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(requireContext())

        // 監聽購物車 / 分店 變更
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                CartManager.changes.collect {
                    val lines = CartManager.getLines()
                    adapter.submit(lines)
                    tvSubtotal.text = formatTWD(CartManager.totalAmount())
                    btnCheckout.isEnabled = lines.isNotEmpty()
                    tvStoreName?.text = CartManager.currentStoreName ?: "未選擇分店"
                }
            }
        }

        // 初次渲染
        adapter.submit(CartManager.getLines())
        tvSubtotal.text = formatTWD(CartManager.totalAmount())
        btnCheckout.isEnabled = CartManager.getLines().isNotEmpty()

        btnCheckout.setOnClickListener {
            // 1) 檢查購物車
            if (CartManager.isEmpty()) {
                Toast.makeText(requireContext(), "購物車為空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 2) 檢查分店
            val storeId = CartManager.currentStoreId
            val storeName = CartManager.currentStoreName
            if (storeId == null || storeId <= 0 || storeName.isNullOrBlank()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("未取得分店資訊，請先選擇分店。")
                    .setPositiveButton("去選分店") { _, _ ->
                        findNavController().navigate(R.id.nav_branch_list)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                return@setOnClickListener
            }
            // 3) 導到結帳頁，帶上分店資訊（雙保險）
            val args = Bundle().apply {
                putInt("store_id", storeId)
                putString("store_name", storeName)
            }
            findNavController().navigate(R.id.nav_checkout, args)
        }
    }

    private fun formatTWD(n: Int): String =
        NumberFormat.getCurrencyInstance(Locale.TAIWAN).format(n)
}
