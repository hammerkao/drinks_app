package com.example.drinks.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.drinks.R
import com.example.drinks.data.model.CartLine
import com.example.drinks.data.model.Options
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private var items: List<CartLine>,
    private val onInc: (lineKey: String) -> Unit,
    private val onDec: (lineKey: String, currentQty: Int) -> Unit,
    private val readOnly: Boolean = false // 只顯示、不提供 +/-
) : RecyclerView.Adapter<CartAdapter.VH>() {

    fun submit(list: List<CartLine>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView? = v.findViewById(R.id.imgThumb)
        val title: TextView = v.findViewById(R.id.tvLineTitle)
        val desc: TextView = v.findViewById(R.id.tvLineDesc)
        val unit: TextView? = v.findViewById(R.id.tvUnitPrice)
        val qty: TextView = v.findViewById(R.id.tvQty)
        val btnInc: View = v.findViewById(R.id.btnInc)
        val btnDec: View = v.findViewById(R.id.btnDec)
        val lineSubtotal: TextView? = v.findViewById(R.id.tvSubtotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart_line, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val l = items[pos]
        val unitPrice = l.unitPrice + l.optionsPrice

        // 標題
        h.title.text = l.name

        // ===== 明細（ID -> 中文；不存在就回原字）+ 備註 =====
        fun prettyLabel(x: String): String {
            val t = x.trim()
            val mapped = Options.label(t)
            return if (mapped.isBlank()) t else mapped
        }

        val parts = buildList {
            l.selected.sweet?.trim()?.takeIf { it.isNotEmpty() }?.let { add(prettyLabel(it)) }
            l.selected.ice?.trim()?.takeIf { it.isNotEmpty() }?.let { add(prettyLabel(it)) }
            if (l.selected.toppings.isNotEmpty()) {
                // 去重、過濾空字串、排序後再轉 label
                addAll(
                    l.selected.toppings
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .toSet()
                        .toList()
                        .sorted()
                        .map { prettyLabel(it) }
                )
            }
        }

        val note = l.selected.note?.trim()
        h.desc.text = buildString {
            append(parts.joinToString("、").ifEmpty { "—" })
            if (!note.isNullOrEmpty()) {
                append("\n備註：").append(note)
            }
        }
        // =====================================

        // 價格/數量/小計
        h.unit?.text = formatTWD(unitPrice)
        h.qty.text = l.qty.toString()
        h.lineSubtotal?.text = formatTWD(l.subtotal)

        // 圖片
        h.img?.let { iv ->
            val url = l.imageUrl
            if (!url.isNullOrBlank()) {
                Glide.with(iv.context)
                    .load(url)
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(iv)
            } else {
                iv.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        }

        // + / - 事件與顯示：依 readOnly 控制
        if (readOnly) {
            h.btnInc.visibility = View.GONE
            h.btnDec.visibility = View.GONE
            // 避免舊的 listener 殘留
            h.btnInc.setOnClickListener(null)
            h.btnDec.setOnClickListener(null)
        } else {
            h.btnInc.visibility = View.VISIBLE
            h.btnDec.visibility = View.VISIBLE
            h.btnInc.setOnClickListener { onInc(l.lineKey) }
            h.btnDec.setOnClickListener { onDec(l.lineKey, l.qty) }
        }
    }

    private fun formatTWD(n: Int): String =
        NumberFormat.getCurrencyInstance(Locale.TAIWAN).format(n)
}
