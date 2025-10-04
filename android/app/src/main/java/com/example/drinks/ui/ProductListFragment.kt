package com.example.drinks.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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

    // ▼ 檢視購物車列
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

        storeId = arguments?.getInt("store_id")
        storeName = arguments?.getString("store_name")
        tvStoreName?.text = storeName ?: "店名"
        com.example.drinks.store.CartManager.currentStoreName = storeName

        // ▼ 底部檢視購物車列
        cartBar = v.findViewById(R.id.cartBar)
        tvCartSummary = v.findViewById(R.id.tvCartSummary)

        cartBar?.setOnClickListener {
            val bottom = requireActivity()
                .findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
            bottom?.selectedItemId = R.id.nav_cart
        }

        rv?.layoutManager = LinearLayoutManager(requireContext())
        rv?.adapter = adapter

        // 參數
        storeId = arguments?.getInt("store_id")
        storeName = arguments?.getString("store_name")
        tvStoreName?.text = storeName ?: "店名"

        // ▲ 回分店：統一用確認流程（按鈕）
        tvBack?.setOnClickListener { confirmBackToBranch() }

        // ▲ 回分店：統一用確認流程（系統返回鍵）
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = confirmBackToBranch()
            }
        )

        loadFirstPage()

        // 監聽購物車變更 → 刷新 CartBar
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                CartManager.changes.collect { refreshCartBar() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCartBar()
    }

    private fun refreshCartBar() {
        val count = CartManager.count()
        val total = CartManager.totalAmount()
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
                    refreshCartBar()
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

    /** 統一的「回分店」流程：有商品先確認並清空，然後明確退回分店頁 */
    private fun confirmBackToBranch() {
        val nav = findNavController()
        if (!CartManager.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage("更換門市將會清空購物車")
                .setNegativeButton("取消", null)
                .setPositiveButton("確定") { _, _ ->
                    CartManager.clear()
                    // 明確指定退回分店頁，避免只在巢狀圖內 pop
                    nav.popBackStack(R.id.nav_branch_list, false)
                }
                .show()
        } else {
            nav.popBackStack(R.id.nav_branch_list, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rv = null; progress = null; errorView = null; errorText = null; emptyView = null
        tvBack = null; tvStoreName = null; cartBar = null; tvCartSummary = null
    }
}
