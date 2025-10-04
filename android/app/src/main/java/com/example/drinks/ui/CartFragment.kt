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
        tvStoreName?.text = com.example.drinks.store.CartManager.currentStoreName ?: "未選擇分店"

        adapter = CartAdapter(
            items = CartManager.getLines(),
            onInc = { key ->
                CartManager.inc(key)
                // 樂觀更新（立即反映）
                adapter.submit(CartManager.getLines())
                tvSubtotal.text = formatTWD(CartManager.totalAmount())
            },
            onDec = { key, qty ->
                if (qty <= 1) {
                    // 依 lineKey 找出當前品名
                    val lineName = com.example.drinks.store.CartManager
                        .getLines()
                        .firstOrNull { it.lineKey == key }
                        ?.name ?: "品項"

                    com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setMessage("確定移除 $lineName ？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("移除") { _, _ ->
                            com.example.drinks.store.CartManager.remove(key)
                            adapter.submit(com.example.drinks.store.CartManager.getLines())
                            tvSubtotal.text = formatTWD(com.example.drinks.store.CartManager.totalAmount())
                        }
                        .show()
                } else {
                    com.example.drinks.store.CartManager.dec(key)
                    adapter.submit(com.example.drinks.store.CartManager.getLines())
                    tvSubtotal.text = formatTWD(com.example.drinks.store.CartManager.totalAmount())
                }
            }
        )
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(requireContext())

        // 收聽變更：最終以 CartManager 為準，再次同步（防競態）
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                CartManager.changes.collect {
                    val lines = CartManager.getLines()
                    adapter.submit(lines)
                    tvSubtotal.text = formatTWD(CartManager.totalAmount())
                    btnCheckout.isEnabled = lines.isNotEmpty()
                }
            }
        }

        // 初次渲染
        adapter.submit(CartManager.getLines())
        tvSubtotal.text = formatTWD(CartManager.totalAmount())
        btnCheckout.isEnabled = CartManager.getLines().isNotEmpty()

        btnCheckout.setOnClickListener {
            Toast.makeText(requireContext(), "稍後接上結帳流程", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatTWD(n: Int): String = NumberFormat.getCurrencyInstance(Locale.TAIWAN).format(n)
}
