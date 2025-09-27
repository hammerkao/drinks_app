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
import com.example.drinks.data.model.Store     // ← 確保有這行
import androidx.lifecycle.lifecycleScope
import com.example.drinks.data.net.Api
import com.example.drinks.net.NetCore
import kotlinx.coroutines.launch

class SelectStoreFragment : Fragment(R.layout.fragment_select_store) {

    private var recyclerView: RecyclerView? = null
    private var progressBar: ProgressBar? = null
    private var errorView: View? = null
    private var errorText: TextView? = null
    private var emptyView: TextView? = null

    // ✅ 只保留這一個 adapter，點擊後前往商品列表並帶入參數
    private val adapter = StoreAdapter { store: Store ->
        val b = Bundle().apply {
            putInt("store_id", store.id)
            putString("store_name", store.name)
        }
        findNavController().navigate(R.id.action_to_product_list, b)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rv = view.findViewById<RecyclerView>(R.id.recyclerView)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

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
