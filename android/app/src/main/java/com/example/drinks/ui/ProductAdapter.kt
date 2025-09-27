// com/example/drinks/ui/ProductAdapter.kt
package com.example.drinks.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.data.model.Product
import java.math.BigDecimal
import com.bumptech.glide.Glide

private fun String.priceToDisplay(): String {
    // "100.00" -> "NT$ 100"
    return try {
        val bd = BigDecimal(this)
        val noCents = bd.setScale(0, BigDecimal.ROUND_DOWN).toPlainString()
        "NT$ $noCents"
    } catch (_: Exception) {
        // 萬一解析失敗就直出
        "NT$ $this"
    }
}

class ProductAdapter(
    private val onPick: (Product) -> Unit = {}
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
            price.text = "NT$ ${item.price.substringBefore('.')}" // "100.00" -> "100"

            val url = item.imageUrl
            if (!url.isNullOrBlank()) {
                Glide.with(img)
                    .load(url)                       // 後端已給絕對 URL，直接用
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(img)
            } else {
                img.setImageResource(android.R.drawable.ic_menu_report_image)
            }

            btn.setOnClickListener { onPick(item) }
        }
    }

    override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH {
        val v = LayoutInflater.from(p.context).inflate(R.layout.item_product, p, false)
        return VH(v)
    }
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
}
