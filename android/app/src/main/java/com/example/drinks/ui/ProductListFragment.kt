package com.example.drinks.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.data.model.Product
import com.example.drinks.net.ProductApi
import com.example.drinks.store.CartManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ProductListFragment : Fragment(R.layout.fragment_product_list) {

    private var rv: RecyclerView? = null
    private var progress: ProgressBar? = null
    private var errorView: View? = null
    private var errorText: TextView? = null
    private var emptyView: TextView? = null
    private var tvBack: TextView? = null
    private var tvStoreName: TextView? = null

    // ▼ 新增：檢視購物車列
    private var cartBar: View? = null
    private var tvCartSummary: TextView? = null

    private val adapter = ProductAdapter { product ->
        val args = bundleOf(
            "productJson" to com.example.drinks.data.json.GsonProvider.gson.toJson(product),
            "productId" to product.id
        )
        findNavController().navigate(R.id.dest_product_detail, args)
    }

    private var storeId: Int? = null
    private var storeName: String? = null

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        rv = v.findViewById(R.id.recyclerView)
        progress = v.findViewById(R.id.progressBar)
        errorView = v.findViewById(R.id.errorView)
        errorText = v.findViewById(R.id.errorText)
        emptyView = v.findViewById(R.id.emptyView)
        tvBack = v.findViewById(R.id.tvBack)
        tvStoreName = v.findViewById(R.id.tvStoreName)

        // ▼ 新增：底部檢視購物車列
        cartBar = v.findViewById(R.id.cartBar)
        tvCartSummary = v.findViewById(R.id.tvCartSummary)
        cartBar?.setOnClickListener {
            // 依你的導覽圖 id；若有 CartFragment 就跳過去，或先用 Toast
            try {
                findNavController().navigate(R.id.nav_cart)
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "未配置購物車頁，先留在此頁", Toast.LENGTH_SHORT).show()
            }
        }

        rv?.layoutManager = LinearLayoutManager(requireContext())
        rv?.adapter = adapter

        // 參數
        storeId = arguments?.getInt("store_id")
        storeName = arguments?.getString("store_name")
        tvStoreName?.text = storeName ?: "店名"

        // ▲ 回分店：若購物車不為空 → 出現警告
        tvBack?.setOnClickListener {
            if (!CartManager.isEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("更換門市將會清空購物車")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("確定") { _, _ ->
                        CartManager.clear()
                        findNavController().popBackStack() // 回分店列表
                    }
                    .show()
            } else {
                findNavController().popBackStack()
            }
        }

        loadFirstPage()
    }

    override fun onResume() {
        super.onResume()
        refreshCartBar()
    }

    private fun refreshCartBar() {
        val count = CartManager.count()         // 需要 CartManager 有 count()
        val total = CartManager.totalAmount()   // 需要 CartManager 有 totalAmount()，單位 NT$
        if (count > 0) {
            cartBar?.visibility = View.VISIBLE
            tvCartSummary?.text = "檢視購物車（$count） NT$$total"
        } else {
            cartBar?.visibility = View.GONE
        }
    }

    private fun loadFirstPage() {
        val sid = storeId ?: run { showError("缺少 store_id"); return }
        setLoading(true)
        ProductApi.listProducts(
            context = requireContext(),
            storeId = sid,
            page = 1,
            onSuccess = { pg ->
                activity?.runOnUiThread {
                    setLoading(false)
                    val list: List<Product> = pg.results
                    adapter.submitList(list)
                    emptyView?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    refreshCartBar() // 載入完成後也更新一次
                }
            },
            onError = { msg ->
                activity?.runOnUiThread {
                    setLoading(false)
                    showError(msg)
                }
            }
        )
    }

    private fun setLoading(show: Boolean) {
        progress?.visibility = if (show) View.VISIBLE else View.GONE
        errorView?.visibility = View.GONE
        emptyView?.visibility = View.GONE
    }

    private fun showError(msg: String) {
        errorView?.visibility = View.VISIBLE
        errorText?.text = msg
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rv = null; progress = null; errorView = null; errorText = null; emptyView = null
        tvBack = null; tvStoreName = null; cartBar = null; tvCartSummary = null
    }
}
