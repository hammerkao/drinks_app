package com.example.drinks.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.data.model.OrderDTO
import com.google.android.material.button.MaterialButton

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
        val tvStore: TextView = v.findViewById(R.id.tvStore)      // 標題列：顯示「訂單 #編號」
        val tvMeta: TextView = v.findViewById(R.id.tvMeta)        // 次行：金額 + 時間
        val btn: MaterialButton = v.findViewById(R.id.btnContent) // 「內容」按鈕
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_order_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val o = items[position]

        // 沒有 store 物件就用訂單編號當標題
        h.tvStore.text = "訂單 #${o.id}"

        val totalText = o.total?.let { if (it.endsWith(".00")) it.dropLast(3) else it } ?: "—"
        val timeText  = o.createdAt?.let { formatToLocal(it) } ?: "—"

        h.tvMeta.text = "金額：$totalText   時間：$timeText"

        h.btn.setOnClickListener { onClick(o) }
        h.itemView.setOnClickListener { onClick(o) }
    }

    override fun getItemCount(): Int = items.size
}

private fun formatToLocal(iso: String): String = try {
    val odt = java.time.OffsetDateTime.parse(iso)
    odt.atZoneSameInstant(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
} catch (_: Exception) {
    iso
}
