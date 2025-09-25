package com.example.drinks.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.example.drinks.R
import com.example.drinks.data.model.Store
import com.example.drinks.net.StoreApi

class SelectStoreFragment : Fragment(R.layout.fragment_select_store) {

    private var recyclerView: RecyclerView? = null
    private var progressBar: ProgressBar? = null
    private var errorView: View? = null
    private var errorText: TextView? = null
    private var emptyView: TextView? = null

    private val adapter = StoreAdapter { store: Store ->
        // TODO: 存選到的門市，然後導到首頁或商品清單
        findNavController().navigate(R.id.dest_home)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ① 先綁定 view
        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        errorView = view.findViewById(R.id.errorView)
        errorText = view.findViewById(R.id.errorText)
        emptyView = view.findViewById(R.id.emptyView)

        recyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SelectStoreFragment.adapter
        }

        // ② 再去載資料（避免尚未初始化就 setLoading）
        loadStores()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ③ 清空引用，避免 view 已被銷毀時仍被使用
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

        StoreApi.getStores(
            context = requireContext(),
            onSuccess = { list ->
                activity?.runOnUiThread {
                    setLoading(false)
                    adapter.submitList(list)
                    emptyView?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    errorView?.visibility = View.GONE
                }
            },
            onError = { msg ->
                activity?.runOnUiThread {
                    setLoading(false)
                    errorView?.visibility = View.VISIBLE
                    errorText?.text = msg
                }
            }
        )
    }

    private fun setLoading(show: Boolean) {
        progressBar?.visibility = if (show) View.VISIBLE else View.GONE
    }
}
