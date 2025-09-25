package com.example.drinks.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.data.model.Store

class StoreAdapter(
    private val onOrderClick: (Store) -> Unit = {}
) : ListAdapter<Store, StoreAdapter.VH>(Diff) {

    // 簡單保存展開狀態（依 id）
    private val expanded = mutableSetOf<Int>()

    object Diff : DiffUtil.ItemCallback<Store>() {
        override fun areItemsTheSame(oldItem: Store, newItem: Store) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Store, newItem: Store) = oldItem == newItem
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.storeName)
        private val tagOpen: TextView = itemView.findViewById(R.id.tagOpen)
        private val tagPickup: TextView = itemView.findViewById(R.id.tagPickup)
        private val toggle: TextView = itemView.findViewById(R.id.toggleMore)
        private val expand: View = itemView.findViewById(R.id.expandArea)
        private val phone: TextView = itemView.findViewById(R.id.storePhone)
        private val address: TextView = itemView.findViewById(R.id.storeAddress)
        private val hours: TextView = itemView.findViewById(R.id.storeHours)
        private val btnOrder: TextView = itemView.findViewById(R.id.btnOrder)

        fun bind(item: Store) {
            name.text = item.name
            phone.text = "電話：${item.phone ?: "-"}"
            address.text = item.address ?: "-"
            hours.text = "營業時間：${item.open_hours ?: "-"}"

            // 狀態 pill（你可以改為依 status 控制）
            tagOpen.visibility = View.VISIBLE
            tagPickup.visibility = View.VISIBLE

            val isExpanded = expanded.contains(item.id)
            expand.visibility = if (isExpanded) View.VISIBLE else View.GONE
            toggle.text = if (isExpanded) "收起 ∧" else "看更多 ▾"

            toggle.setOnClickListener {
                if (isExpanded) expanded.remove(item.id) else expanded.add(item.id)
                notifyItemChanged(bindingAdapterPosition)
            }

            btnOrder.setOnClickListener { onOrderClick(item) }
            // 點整卡也可展開/收起（可選）
            itemView.setOnClickListener { toggle.performClick() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_store, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))


}
