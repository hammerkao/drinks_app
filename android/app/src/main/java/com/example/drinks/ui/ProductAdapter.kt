package com.example.drinks.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.drinks.R
import com.example.drinks.data.model.Product

class ProductAdapter(
    private val onPick: (Product) -> Unit = {}  // ← 改為必填回呼，由外部（Fragment）決定怎麼導頁
) : ListAdapter<Product, ProductAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(a: Product, b: Product) = a.id == b.id
        override fun areContentsTheSame(a: Product, b: Product) = a == b
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val img: ImageView = v.findViewById(R.id.img)
        private val name: TextView = v.findViewById(R.id.name)
        private val price: TextView = v.findViewById(R.id.price)
        private val btn: TextView = v.findViewById(R.id.btnPick)

        fun bind(item: Product) {
            name.text = item.name
            price.text = "NT$ ${item.price.substringBefore('.')}"
            if (!item.imageUrl.isNullOrBlank()) {
                Glide.with(img).load(item.imageUrl).into(img)
            } else {
                img.setImageResource(android.R.drawable.ic_menu_report_image)
            }

            // 點擊事件全部交給外部回呼（Fragment 負責 navigate）
            btn.setOnClickListener {
                Toast.makeText(it.context, "go", Toast.LENGTH_SHORT).show() // 你的 debug，可移除
                onPick(item)
            }
            itemView.setOnClickListener { onPick(item) }
        }
    }

    override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH {
        val v = LayoutInflater.from(p.context).inflate(R.layout.item_product, p, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
}
