package com.example.drinks.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.data.model.Store
import com.example.drinks.data.net.Api
import com.example.drinks.net.NetCore
import com.example.drinks.store.CartManager   // ← 新增：存分店
import kotlinx.coroutines.launch

class SelectStoreFragment : Fragment(R.layout.fragment_select_store) {

    private var recyclerView: RecyclerView? = null
    private var progressBar: ProgressBar? = null
    private var errorView: View? = null
    private var errorText: TextView? = null
    private var emptyView: TextView? = null

    // 點擊分店 → 先存入 CartManager，再導向商品清單
    private val adapter = StoreAdapter { store: Store ->
        // 1) 記住分店（之後購物車/結帳/確認頁都可直接讀）
        CartManager.setStore(store.id, store.name)

        // 2) 同時把分店帶去下一頁（保險起見，頁面也能從 args 取得）
        val args = bundleOf(
            "store_id" to store.id,
            "store_name" to store.name
        )

        // 3) 導頁（依你的 nav_graph 使用對應的 action 或 destination id）
        // 若 nav_main.xml 有 <action android:id="@+id/action_selectStore_to_productList" … />
        findNavController().navigate(R.id.action_selectStore_to_productList, args)

        // 如果你是直接指到目的地（沒有 action），改用：
        // findNavController().navigate(R.id.dest_product_list, args)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        errorView = view.findViewById(R.id.errorView)
        errorText = view.findViewById(R.id.errorText)
        emptyView = view.findViewById(R.id.emptyView)

        recyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SelectStoreFragment.adapter
        }

        loadStores()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView = null
        progressBar = null
        errorView = null
        errorText = null
        emptyView = null
    }

    private fun loadStores() {
        setLoading(true)
        errorView?.visibility = View.GONE
        emptyView?.visibility = View.GONE

        val api = Api(NetCore.BASE_URL, NetCore.buildOkHttp(requireContext()))
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val list = api.getStores()
                setLoading(false)
                adapter.submitList(list)
                emptyView?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                errorView?.visibility = View.GONE
            } catch (e: Exception) {
                setLoading(false)
                errorView?.visibility = View.VISIBLE
                errorText?.text = e.message ?: "載入失敗"
            }
        }
    }

    private fun setLoading(show: Boolean) {
        progressBar?.visibility = if (show) View.VISIBLE else View.GONE
    }
}
