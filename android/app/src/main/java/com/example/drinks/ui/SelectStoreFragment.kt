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
import kotlinx.coroutines.launch

class SelectStoreFragment : Fragment(R.layout.fragment_select_store) {

    private var recyclerView: RecyclerView? = null
    private var progressBar: ProgressBar? = null
    private var errorView: View? = null
    private var errorText: TextView? = null
    private var emptyView: TextView? = null

    // 點擊分店 → 導向商品清單（帶 store_id / store_name）
    private val adapter = StoreAdapter { store: Store ->
        val args = bundleOf(
            "store_id" to store.id,
            "store_name" to store.name
        )
        // ★ 確認 nav_main.xml 裡的 action id 就是 action_selectStore_to_productList
        findNavController().navigate(R.id.action_selectStore_to_productList, args)
        // 若你在 nav_graph 用的是直接目的地 id，改成：
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
