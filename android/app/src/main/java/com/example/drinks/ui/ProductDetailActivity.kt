package com.example.drinks.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.drinks.R
import com.example.drinks.data.json.GsonProvider
import com.example.drinks.data.model.Product
import com.example.drinks.data.model.SelectedOptions
import com.example.drinks.store.CartManager

class ProductDetailActivity : AppCompatActivity() {

    private val TAG = "ProductDetail"

    // 可能不存在的元件一律用 nullable，避免 NPE
    private var imgCover: ImageView? = null
    private var tvTitle: TextView? = null
    private var tvPrice: TextView? = null
    private var etNote: EditText? = null

    private var tvQty: TextView? = null
    private var btnQtyPlus: Button? = null
    private var btnQtyMinus: Button? = null

    private var rgSweetRow1: RadioGroup? = null
    private var rgSweetRow2: RadioGroup? = null
    private var rgSweetRow3: RadioGroup? = null
    private var rgIceRow1: RadioGroup? = null
    private var rgIceRow2: RadioGroup? = null
    private var rgIceRow3: RadioGroup? = null

    private var cbPearl: CheckBox? = null
    private var cbCoconut: CheckBox? = null
    private var cbPudding: CheckBox? = null

    private var btnAddToCart: Button? = null

    private var qty = 1
    private val sel = SelectedOptions()
    private lateinit var product: Product

    private var sweetListener: RadioGroup.OnCheckedChangeListener? = null
    private var iceListener: RadioGroup.OnCheckedChangeListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("DetailLife", "onCreate() entered")
        Toast.makeText(this, "進入詳情頁 onCreate", Toast.LENGTH_SHORT).show()
        try {
            setContentView(R.layout.activity_product_detail)
            bindViewsSafely()

            product = obtainProductFromIntent()
            renderProduct(product)

            bindQuantityIfPresent()
            bindNoteLimitIfPresent()
            bindToppingsIfPresent()

            // 綁定互斥（只有在三排都存在時）
            listOf(rgSweetRow1, rgSweetRow2, rgSweetRow3).takeIf { it.allNotNull() }?.let {
                sweetListener = bindMutualExclusive(it.requireAll()) { t -> sel.sweet = t }
            }
            listOf(rgIceRow1, rgIceRow2, rgIceRow3).takeIf { it.allNotNull() }?.let {
                iceListener = bindMutualExclusive(it.requireAll()) { t -> sel.ice = t }
            }

            btnAddToCart?.setOnClickListener {
                if (rgSweetRow1 != null && sel.sweet == null) {
                    toast("請先選擇甜度"); return@setOnClickListener
                }
                if (rgIceRow1 != null && sel.ice == null) {
                    toast("請先選擇冰塊"); return@setOnClickListener
                }
                if (sel.toppings.size > 3) {
                    toast("加料最多選 3 項"); return@setOnClickListener
                }

                repeat(qty) { CartManager.add(product, sel) }
                startActivity(Intent(this, CartActivity::class.java))
            }

        } catch (e: Exception) {
            // 就算哪裡出錯，不讓整頁崩，直接吐在 Logcat + Toast，幫你定位
            Log.e(TAG, "onCreate failed", e)
            toast("詳情頁初始化失敗：${e.message}")
        }
    }

    // --------- 綁定 / 呈現 ---------

    private fun bindViewsSafely() {
        fun <T : View> v(id: Int): T? = findViewById(id)

        imgCover = v(R.id.imgCover)
        tvTitle = v(R.id.tvDetailTitle)
        tvPrice = v(R.id.tvPrice)
        etNote = v(R.id.etNote)

        tvQty = v(R.id.tvQty)
        btnQtyPlus = v(R.id.btnQtyPlus)
        btnQtyMinus = v(R.id.btnQtyMinus)

        rgSweetRow1 = v(R.id.rgSweetRow1)
        rgSweetRow2 = v(R.id.rgSweetRow2)
        rgSweetRow3 = v(R.id.rgSweetRow3)

        rgIceRow1 = v(R.id.rgIceRow1)
        rgIceRow2 = v(R.id.rgIceRow2)
        rgIceRow3 = v(R.id.rgIceRow3)

        cbPearl = v(R.id.cbPearl)
        cbCoconut = v(R.id.cbCoconut)
        cbPudding = v(R.id.cbPudding)

        btnAddToCart = v(R.id.btnAddToCart)

        // 方便你看是不是某些 id 沒貼進 layout
        fun markMissing(name: String, view: View?) {
            if (view == null) Log.w(TAG, "layout 缺少 id @$name")
        }
        markMissing("imgCover", imgCover)
        markMissing("tvDetailTitle", tvTitle)
        markMissing("tvPrice", tvPrice)
        markMissing("btnAddToCart", btnAddToCart)
    }

    private fun renderProduct(p: Product) {
        tvTitle?.text = p.name
        tvPrice?.text = "NT$${p.price.substringBefore('.')}"
        val url = p.imageUrl
        if (!url.isNullOrBlank()) imgCover?.let { Glide.with(this).load(url).into(it) }
        else imgCover?.setImageResource(android.R.color.darker_gray)
    }

    private fun bindQuantityIfPresent() {
        val tv = tvQty ?: return
        val plus = btnQtyPlus ?: return
        val minus = btnQtyMinus ?: return
        fun render() { tv.text = qty.toString() }
        plus.setOnClickListener { qty += 1; render() }
        minus.setOnClickListener { if (qty > 1) qty -= 1; render() }
        render()
    }

    private fun bindNoteLimitIfPresent() {
        val note = etNote ?: return
        note.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                note.error = if ((s?.length ?: 0) > 10) "最多 10 個字" else null
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun bindToppingsIfPresent() {
        val pearl = cbPearl; val coco = cbCoconut; val pud = cbPudding
        if (pearl == null && coco == null && pud == null) return
        val boxes = listOfNotNull(pearl, coco, pud)
        boxes.forEach { cb ->
            cb.setOnCheckedChangeListener { _, _ ->
                val list = buildList {
                    if (pearl?.isChecked == true) add("珍珠")
                    if (coco?.isChecked == true) add("椰果")
                    if (pud?.isChecked == true) add("布丁")
                }
                if (list.size > 3) {
                    cb.isChecked = false
                    toast("加料最多選 3 項")
                    return@setOnCheckedChangeListener
                }
                sel.toppings.clear(); sel.toppings.addAll(list)
            }
        }
    }

    // --------- 取得 Intent 商品 ---------

    private fun obtainProductFromIntent(): Product {
        intent.getStringExtra("product_json")?.let { json ->
            try { return GsonProvider.gson.fromJson(json, Product::class.java) }
            catch (e: Exception) { Log.e(TAG, "parse product_json fail", e) }
        }
        (intent.getSerializableExtra("product") as? Product)?.let { return it }

        val id = intent.getIntExtra("product_id", intent.getIntExtra("pid", 0))
        val name = intent.getStringExtra("product_name") ?: "未命名商品"
        val price = intent.getStringExtra("product_price") ?: "0.00"
        val img = intent.getStringExtra("product_image")
        return Product(id, name, price, img, true, null)
    }

    // --------- 共用 ---------

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    private fun <T> List<T?>.allNotNull(): Boolean = all { it != null }
    private fun <T> List<T?>.requireAll(): List<T> = filterNotNull()

    private fun bindMutualExclusive(
        groups: List<RadioGroup>,
        onChange: (String?) -> Unit
    ): RadioGroup.OnCheckedChangeListener? {
        if (groups.size < 2) return null
        fun currentText(): String? {
            val g = groups.firstOrNull { it.checkedRadioButtonId != -1 } ?: return null
            val btn = g.findViewById<RadioButton>(g.checkedRadioButtonId)
            return btn?.text?.toString()
        }
        var listener: RadioGroup.OnCheckedChangeListener? = null
        listener = RadioGroup.OnCheckedChangeListener { changed, checkedId ->
            if (checkedId != -1) {
                groups.filter { it !== changed }.forEach { g ->
                    g.setOnCheckedChangeListener(null)
                    g.clearCheck()
                    g.setOnCheckedChangeListener(listener!!)
                }
            }
            onChange(currentText())
        }
        groups.forEach { it.setOnCheckedChangeListener(listener!!) }
        return listener
    }

    override fun onStart() {
        super.onStart()
        android.util.Log.d("DetailLife", "onStart()")
    }
}
