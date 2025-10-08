// OrdersFragment.kt
package com.example.drinks.ui.orders

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.ui.common.SpacingDecoration
import kotlinx.coroutines.launch




class OrdersFragment : Fragment(R.layout.fragment_orders) {

    private lateinit var adapter: OrdersAdapter
    private lateinit var empty: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        view.findViewById<TextView>(R.id.tvToolbarTitle).text = "我的訂單"

        val rv = view.findViewById<RecyclerView>(R.id.rvOrders)
        empty = view.findViewById(R.id.emptyView)

        adapter = OrdersAdapter { dto ->
            // 只帶 order_id，詳情頁完全以後端為準
            val args = Bundle().apply { putInt("order_id", dto.id) }
            findNavController().navigate(R.id.action_orders_to_orderDetail, args)
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        rv.setHasFixedSize(true)
        rv.addItemDecoration(SpacingDecoration(verticalDp = 8f))

        loadOrders()
    }

    override fun onResume() {
        super.onResume()
        loadOrders()
    }

    private fun loadOrders() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = com.example.drinks.data.net.Api(
                    base = com.example.drinks.net.NetCore.BASE_URL,
                    client = com.example.drinks.net.NetCore.buildOkHttp(requireContext())
                )
                val list = api.listOrders()                 // ← 後端資料
                adapter.submit(list)                        // 你的 Adapter 的 submit(...)
                empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message ?: "載入失敗", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
