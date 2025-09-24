package com.example.drinks.ui.branch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.data.model.BranchDto
import com.google.android.material.button.MaterialButton

class BranchAdapter(
    private var items: List<BranchDto> = emptyList(),
    private val onClickOrder: (BranchDto) -> Unit
) : RecyclerView.Adapter<BranchAdapter.VH>() {

    class VH(v: View): RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvStatus: TextView = v.findViewById(R.id.tvStatus)
        val tvAddress: TextView = v.findViewById(R.id.tvAddress)
        val tvHours: TextView = v.findViewById(R.id.tvHours)
        val btnOrder: MaterialButton = v.findViewById(R.id.btnOrder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_branch, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, i: Int) {
        val d = items[i]
        h.tvName.text = d.name
        h.tvStatus.text = d.status ?: ""
        h.tvAddress.text = d.address ?: ""
        h.tvHours.text = d.open_hours ?: ""
        h.btnOrder.setOnClickListener { onClickOrder(d) }
    }

    fun submit(list: List<BranchDto>) {
        items = list
        notifyDataSetChanged()
    }
}
