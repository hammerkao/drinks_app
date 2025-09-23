package com.example.drinks.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.data.model.CartLine

class CartAdapter(
    private val items: List<CartLine>,
    private val onQtyChange: (String, Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvLineTitle)
        val desc: TextView = v.findViewById(R.id.tvLineDesc)
        val qty: TextView = v.findViewById(R.id.tvQty)
        val subtotal: TextView = v.findViewById(R.id.tvSubtotal)
        val btnInc: Button = v.findViewById(R.id.btnInc)
        val btnDec: Button = v.findViewById(R.id.btnDec)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart_line, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val l = items[pos]
        h.title.text = "${l.name}  NT$${l.unitPrice + l.optionsPrice}"

        val sweet = l.selected.sweet ?: "—"
        val ice = l.selected.ice ?: "—"
        val tops = if (l.selected.toppings.isNotEmpty())
            l.selected.toppings.joinToString("、")
        else "無加料"

        h.desc.text = "甜度：$sweet、冰塊：$ice、加料：$tops"
        h.qty.text = l.qty.toString()
        h.subtotal.text = "小計：NT$${l.subtotal}"

        h.btnInc.setOnClickListener { onQtyChange(l.lineKey, +1) }
        h.btnDec.setOnClickListener { onQtyChange(l.lineKey, -1) }
    }
}
