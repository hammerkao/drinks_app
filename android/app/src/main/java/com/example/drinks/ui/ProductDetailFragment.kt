package com.example.drinks.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.drinks.R
import com.example.drinks.data.json.GsonProvider
import com.example.drinks.data.model.Product
import com.example.drinks.data.model.SelectedOptions
import com.example.drinks.store.CartManager

class ProductDetailFragment : Fragment(R.layout.fragment_product_detail) {

    // Top + 內容
    private lateinit var imgCover: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvPrice: TextView
    private lateinit var etNote: EditText

    // 甜度（3排）
    private lateinit var rgSweetRow1: RadioGroup
    private lateinit var rgSweetRow2: RadioGroup
    private lateinit var rgSweetRow3: RadioGroup

    // 冰量（3排）
    private lateinit var rgIceRow1: RadioGroup
    private lateinit var rgIceRow2: RadioGroup
    private lateinit var rgIceRow3: RadioGroup

    // 加料
    private lateinit var cbPearl: CheckBox
    private lateinit var cbCoconut: CheckBox
    private lateinit var cbPudding: CheckBox

    // 底部 Footer
    private lateinit var tvQty: TextView
    private lateinit var btnQtyPlus: Button
    private lateinit var btnQtyMinus: Button
    private lateinit var btnAddToCart: Button
    private var qty: Int = 1

    // 互斥監聽參考
    private var sweetListener: RadioGroup.OnCheckedChangeListener? = null
    private var iceListener: RadioGroup.OnCheckedChangeListener? = null

    private val sel = SelectedOptions()
    private lateinit var product: Product

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 綁定 View（一定要先綁再呼叫 bind*）
        imgCover = view.findViewById(R.id.imgCover)
        tvTitle  = view.findViewById(R.id.tvDetailTitle)
        tvPrice  = view.findViewById(R.id.tvPrice)
        etNote   = view.findViewById(R.id.etNote)

        rgSweetRow1 = view.findViewById(R.id.rgSweetRow1)
        rgSweetRow2 = view.findViewById(R.id.rgSweetRow2)
        rgSweetRow3 = view.findViewById(R.id.rgSweetRow3)
        rgIceRow1   = view.findViewById(R.id.rgIceRow1)
        rgIceRow2   = view.findViewById(R.id.rgIceRow2)
        rgIceRow3   = view.findViewById(R.id.rgIceRow3)

        cbPearl   = view.findViewById(R.id.cbPearl)
        cbCoconut = view.findViewById(R.id.cbCoconut)
        cbPudding = view.findViewById(R.id.cbPudding)

        tvQty      = view.findViewById(R.id.tvQty)
        btnQtyPlus = view.findViewById(R.id.btnQtyPlus)
        btnQtyMinus= view.findViewById(R.id.btnQtyMinus)
        btnAddToCart = view.findViewById(R.id.btnAddToCart)

        // 讀取商品資料並渲染
        product = obtainProductFromArgs()
        renderProduct(product)

        // Toolbar 標題 + 返回
        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.title = product.name
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        // 邏輯
        bindQuantity()
        bindNoteLimit()
        bindToppings()

        sweetListener = bindMutualExclusive(listOf(rgSweetRow1, rgSweetRow2, rgSweetRow3)) { text ->
            sel.sweet = text
        }
        iceListener = bindMutualExclusive(listOf(rgIceRow1, rgIceRow2, rgIceRow3)) { text ->
            sel.ice = text
        }

        btnAddToCart.setOnClickListener {
            if (sel.sweet == null || sel.ice == null) {
                Toast.makeText(requireContext(), "請先選擇甜度與冰塊", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (sel.toppings.size > 3) {
                Toast.makeText(requireContext(), "加料最多選 3 項", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            repeat(qty) { CartManager.add(product, sel) }
            Toast.makeText(requireContext(), "已加入購物車（$qty 杯）", Toast.LENGTH_SHORT).show()
            // 視需求導向購物車：
            // findNavController().navigate(R.id.nav_cart)
        }
    }

    private fun bindQuantity() {
        fun render() { tvQty.text = qty.toString() }
        btnQtyPlus.setOnClickListener { qty += 1; render() }
        btnQtyMinus.setOnClickListener { if (qty > 1) qty -= 1; render() }
        render()
    }

    private fun bindNoteLimit() {
        etNote.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                etNote.error = if ((s?.length ?: 0) > 10) "最多 10 個字" else null
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun bindToppings() {
        val boxes = listOf(cbPearl, cbCoconut, cbPudding)
        boxes.forEach { cb ->
            cb.setOnCheckedChangeListener { _, _ ->
                val selected = buildList {
                    if (cbPearl.isChecked) add("珍珠")
                    if (cbCoconut.isChecked) add("椰果")
                    if (cbPudding.isChecked) add("布丁")
                }
                if (selected.size > 3) {
                    cb.isChecked = false
                    Toast.makeText(requireContext(), "加料最多選 3 項", Toast.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }
                sel.toppings.clear()
                sel.toppings.addAll(selected)
            }
        }
    }

    private fun obtainProductFromArgs(): Product {
        arguments?.getString("productJson")?.let { json ->
            try { return GsonProvider.gson.fromJson(json, Product::class.java) } catch (_: Exception) {}
        }
        val id = arguments?.getInt("productId", 0) ?: 0
        val name = arguments?.getString("product_name") ?: "未命名商品"
        val price = arguments?.getString("product_price") ?: "0.00"
        val img = arguments?.getString("product_image")
        return Product(id, name, price, img, true, null)
    }

    private fun renderProduct(p: Product) {
        tvTitle.text = p.name
        val priceDisplay = p.price.removeSuffix(".00")
        tvPrice.text = "NT$$priceDisplay"
        if (!p.imageUrl.isNullOrBlank()) {
            Glide.with(this).load(p.imageUrl).into(imgCover)
        } else {
            imgCover.setImageResource(android.R.color.darker_gray)
        }
    }

    /** 多個 RadioGroup 互斥單選 */
    private fun bindMutualExclusive(
        groups: List<RadioGroup>,
        onChange: (String?) -> Unit
    ): RadioGroup.OnCheckedChangeListener {
        fun currentText(): String? {
            val btn = groups.firstNotNullOfOrNull { g ->
                g.checkedRadioButtonId.takeIf { it != -1 }?.let { id -> g.findViewById<RadioButton>(id) }
            }
            return btn?.text?.toString()
        }
        var listener: RadioGroup.OnCheckedChangeListener? = null
        listener = RadioGroup.OnCheckedChangeListener { changed, checkedId ->
            if (checkedId != -1) {
                groups.filter { it != changed }.forEach { g -> g.setOnCheckedChangeListener(null) }
                groups.filter { it != changed }.forEach { g -> g.clearCheck() }
                groups.filter { it != changed }.forEach { g -> g.setOnCheckedChangeListener(listener!!) }
            }
            onChange(currentText())
        }
        groups.forEach { g -> g.setOnCheckedChangeListener(listener!!) }
        return listener!!
    }
}
