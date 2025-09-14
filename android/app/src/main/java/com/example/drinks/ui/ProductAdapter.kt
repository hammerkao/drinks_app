package com.example.drinks.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.drinks.data.model.Product
import com.example.drinks.databinding.ItemProductBinding


class ProductAdapter(
    private val onAdd: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.VH>() {

    private val items = mutableListOf<Product>()

    fun submit(list: List<Product>) {
        items.clear(); items.addAll(list); notifyDataSetChanged()
    }

    inner class VH(val b: ItemProductBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemProductBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val product = items[pos]
        h.b.tvName.text = product.name
        h.b.tvPrice.text = "NT$ ${product.price}"
        Glide.with(h.b.img).load(product.image).into(h.b.img)

        h.b.btnAdd.setOnClickListener {
            val p = h.bindingAdapterPosition
            if (p != RecyclerView.NO_POSITION) {
                onAdd(items[p])   // 這裡一定是 Product
            }
        }
    }

    override fun getItemCount() = items.size
}
