package com.example.drinks.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.data.cache.StoreCatalog
import com.example.drinks.data.model.OrderDTO
import com.google.android.material.button.MaterialButton
import java.math.BigDecimal
import java.math.RoundingMode

class OrdersAdapter(
    private val onClick: (OrderDTO) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.VH>() {

    private val items = mutableListOf<OrderDTO>()

    fun submit(list: List<OrderDTO>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvStoreName: TextView   = v.findViewById(R.id.tvStoreName)
        val tvAmount: TextView      = v.findViewById(R.id.tvAmount)
        val btnContent: MaterialButton = v.findViewById(R.id.btnContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_order_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val o = items[position]

        // 分店名稱：快取優先，否則退為「門市 #id」或 "—"
        val storeName = StoreCatalog.nameOf(o.storeId)
            ?: o.storeId?.let { if (it > 0) "門市 #$it" else "—" } ?: "—"
        h.tvStoreName.text = storeName

        // 金額：後端 total 多為字串，安全轉整數後顯示
        val totalInt = moneyStringToInt(o.total)
        h.tvAmount.text = "金額：$totalInt"

        h.btnContent.setOnClickListener { onClick(o) }
        h.itemView.setOnClickListener { onClick(o) }
    }

    override fun getItemCount(): Int = items.size
}

/* Helpers */
private fun moneyStringToInt(s: String?): Int = try {
    if (s.isNullOrBlank()) 0
    else BigDecimal(s).setScale(0, RoundingMode.HALF_UP).toInt()
} catch (_: Exception) { 0 }
