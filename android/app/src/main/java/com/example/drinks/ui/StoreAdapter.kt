package com.example.drinks.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Space
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.data.model.Store

class StoreAdapter(
    private val onOrderClick: (Store) -> Unit
) : ListAdapter<Store, StoreAdapter.VH>(Diff) {

    /** 記住哪些 store 被展開，避免回收後狀態亂跳 */
    private val expandedIds = mutableSetOf<Int>()

    companion object Diff : DiffUtil.ItemCallback<Store>() {
        override fun areItemsTheSame(a: Store, b: Store) = a.id == b.id
        override fun areContentsTheSame(a: Store, b: Store) = a == b
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val storeName: TextView = v.findViewById(R.id.storeName)
        private val btnOrder: TextView  = v.findViewById(R.id.btnOrder)

        private val tagOpen: TextView   = v.findViewById(R.id.tagOpen)
        private val tagPickup: TextView = v.findViewById(R.id.tagPickup)

        private val toggleMore: TextView = v.findViewById(R.id.toggleMore)
        private val expandArea: View     = v.findViewById(R.id.expandArea)

        private val tvPhone: TextView   = v.findViewById(R.id.storePhone)
        private val tvAddress: TextView = v.findViewById(R.id.storeAddress)
        private val tvHours: TextView   = v.findViewById(R.id.storeHours)

        fun bind(item: Store) {
            // 基本文字
            storeName.text = item.name
            tvPhone.text   = if (!item.phone.isNullOrBlank()) "電話：${item.phone}" else "電話：—"
            tvAddress.text = if (!item.address.isNullOrBlank()) item.address!! else "—"
            tvHours.text   = if (!item.open_hours.isNullOrBlank()) "營業時間：${item.open_hours}" else "營業時間：—"

            // 標籤（可依你的後端 status 做顯示/文案）
            // 假設 status == "open" 表示營業中；否則隱藏或顯示其它文案
            if (item.status?.equals("open", ignoreCase = true) == true) {
                tagOpen.visibility = View.VISIBLE
                tagOpen.text = "營業中"
            } else {
                tagOpen.visibility = View.GONE
            }
            // 先固定顯示「自取」，若之後後端有欄位可再調整
            tagPickup.visibility = View.VISIBLE
            tagPickup.text = "自取"

            // 只讓「下單去」可點
            itemView.isClickable = false
            itemView.isFocusable = false
            itemView.setOnClickListener(null)

            btnOrder.setOnClickListener {
                btnOrder.isEnabled = false
                onOrderClick(item)
                btnOrder.postDelayed({ btnOrder.isEnabled = true }, 600)
            }

            // 展開/收起狀態
            val expanded = expandedIds.contains(item.id)
            applyExpandState(expanded)

            toggleMore.setOnClickListener {
                val now = expandedIds.contains(item.id)
                if (now) expandedIds.remove(item.id) else expandedIds.add(item.id)
                applyExpandState(!now)
            }
        }

        private fun applyExpandState(expanded: Boolean) {
            expandArea.visibility = if (expanded) View.VISIBLE else View.GONE
            toggleMore.text = if (expanded) "收起 ∧" else "看更多 ▾"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_store, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}
