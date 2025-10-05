package com.example.drinks.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.drinks.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.util.Locale

class OrderSuccessFragment : Fragment(R.layout.fragment_order_success) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val orderId    = requireArguments().getInt("orderId")
        val total      = requireArguments().getInt("total")
        val storeName  = requireArguments().getString("storeName") ?: "—"
        val pickupTime = requireArguments().getString("pickupTime") ?: "—"

        view.findViewById<TextView>(R.id.tvOrderId).text   = "訂單編號：$orderId"
        view.findViewById<TextView>(R.id.tvStore).text     = "門市：$storeName"
        view.findViewById<TextView>(R.id.tvPickupTime).text= "取餐時間：$pickupTime"
        view.findViewById<TextView>(R.id.tvTotal).text     = "金額：" + NumberFormat.getCurrencyInstance(Locale.TAIWAN).format(total)

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnViewOrders)
            .setOnClickListener {
                // 切到底部「訂單」頁（若你有 Orders 目的地）
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)
                    ?.selectedItemId = R.id.nav_orders
                findNavController().popBackStack()
            }

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBackHome)
            .setOnClickListener {
                // 回到點餐 tab
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)
                    ?.selectedItemId = R.id.nav_order
                findNavController().popBackStack()
            }
    }
}
