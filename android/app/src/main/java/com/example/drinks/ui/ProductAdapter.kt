package com.example.drinks.ui

import android.content.Intent
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
import com.example.drinks.data.json.GsonProvider
import com.example.drinks.data.model.Product

class ProductAdapter(
    // 仍保留 onPick 勾點（若你另外還想做 tracking 用），但不再依賴它完成跳轉
    private val onPick: ((Product) -> Unit)? = null
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

        private fun goDetail(item: Product) {
            // 直接在這裡啟動詳情頁
            val ctx = itemView.context
            val it = Intent(ctx, ProductDetailActivity::class.java).apply {
                putExtra("product_id", item.id)
                putExtra("pid", item.id) // 相容舊 key
                putExtra("product_json", GsonProvider.gson.toJson(item))
            }
            try {
                ctx.startActivity(it)
            } catch (e: Exception) {
                Toast.makeText(ctx, "開啟詳情失敗：${e.message}", Toast.LENGTH_LONG).show()
            }
            onPick?.invoke(item) // 可選：若你還想做記錄
        }

        fun bind(item: Product) {
            name.text = item.name
            price.text = "NT$ ${item.price.substringBefore('.')}"
            if (!item.imageUrl.isNullOrBlank()) {
                Glide.with(img).load(item.imageUrl).into(img)
            } else {
                img.setImageResource(android.R.drawable.ic_menu_report_image)
            }

            // 按鈕
            btn.setOnClickListener {
                Toast.makeText(it.context, "go", Toast.LENGTH_SHORT).show() // 你的 debug
                goDetail(item)
            }
            // 整列
            itemView.setOnClickListener { goDetail(item) }
        }
    }

    override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH {
        val v = LayoutInflater.from(p.context).inflate(R.layout.item_product, p, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
}
