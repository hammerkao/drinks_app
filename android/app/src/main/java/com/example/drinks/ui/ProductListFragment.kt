// ProductListFragment.kt
package com.example.drinks.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.data.model.Product
import com.example.drinks.net.ProductApi

class ProductListFragment : Fragment(R.layout.fragment_product_list) {

    private var rv: RecyclerView? = null
    private var progress: ProgressBar? = null
    private var errorView: View? = null
    private var errorText: TextView? = null
    private var emptyView: TextView? = null
    private var tvBack: TextView? = null
    private var tvStoreName: TextView? = null

    private val adapter = ProductAdapter { product ->
        // TODO: 選取後的行為（加入購物車或進入客製頁）
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

        rv?.layoutManager = LinearLayoutManager(requireContext())
        rv?.adapter = adapter

        // 參數
        storeId = arguments?.getInt("store_id")
        storeName = arguments?.getString("store_name")
        tvStoreName?.text = storeName ?: "店名"

        tvBack?.setOnClickListener { findNavController().navigateUp() }

        loadFirstPage()
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
        rv = null; progress = null; errorView = null; errorText = null; emptyView = null; tvBack = null; tvStoreName = null
    }
}
