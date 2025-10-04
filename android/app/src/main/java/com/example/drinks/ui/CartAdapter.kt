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
    private val onDec: (lineKey: String, currentQty: Int) -> Unit
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

        // ===== 明細：直接顯示使用者選擇（ID 或中文都支援），以及備註 =====
        fun prettyLabel(x: String): String {
            val trimmed = x.trim()
            val mapped = Options.label(trimmed)
            // 若 label() 找不到就回傳原字串（可能本來就已是中文）
            return if (mapped.isBlank()) trimmed else mapped
        }

        val parts = buildList {
            l.selected.sweet?.trim()?.takeIf { it.isNotEmpty() }?.let { add(prettyLabel(it)) }
            l.selected.ice?.trim()?.takeIf { it.isNotEmpty() }?.let { add(prettyLabel(it)) }
            if (l.selected.toppings.isNotEmpty()) {
                // 去重 + 排序，避免順序不同導致顯示亂跳
                addAll(l.selected.toppings.map { it.trim() }.filter { it.isNotEmpty() }.toSet().toList().sorted().map { prettyLabel(it) })
            }
        }

        val note = l.selected.note?.trim()
        val detail = buildString {
            append(parts.joinToString("、").ifEmpty { "—" })
            if (!note.isNullOrEmpty()) {
                append("\n備註：")
                append(note)
            }
        }
        h.desc.text = detail
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

        // 事件
        h.btnInc.setOnClickListener { onInc(l.lineKey) }
        h.btnDec.setOnClickListener { onDec(l.lineKey, l.qty) }
    }

    private fun formatTWD(n: Int): String =
        NumberFormat.getCurrencyInstance(Locale.TAIWAN).format(n)
}
