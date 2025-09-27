package com.example.drinks.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.drinks.R
import com.example.drinks.data.model.Product
import com.example.drinks.data.model.SelectedOptions
import com.example.drinks.data.net.Api
import com.example.drinks.net.NetCore
import com.example.drinks.store.CartManager
import kotlinx.coroutines.launch

class ProductDetailActivity : AppCompatActivity() {

    private var product: Product? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_product_detail)

        // 只處理 Int 型別的 pid（相容舊字串）
        val pid: Int = intent.getIntExtra("pid", -1).let { v ->
            if (v != -1) v else intent.getStringExtra("pid")?.toIntOrNull() ?: -1
        }
        if (pid <= 0) {
            Toast.makeText(this, "缺少商品 ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val tvTitle = findViewById<TextView>(R.id.tvDetailTitle)
//        val ivImage: ImageView? = findViewById(R.id.ivDetailImage) // 若版面沒有這個 id，也不會壞

        val rgSweet   = findViewById<RadioGroup>(R.id.rgSweet)
        val rgIce     = findViewById<RadioGroup>(R.id.rgIce)
        val cbPearl   = findViewById<CheckBox>(R.id.cbPearl)
        val cbCoconut = findViewById<CheckBox>(R.id.cbCoconut)
        val cbPudding = findViewById<CheckBox>(R.id.cbPudding)
        val btnAdd    = findViewById<Button>(R.id.btnAddToCart)

        val sel = SelectedOptions()

        // 從後端載入單一商品
        val api = Api(NetCore.BASE_URL, NetCore.buildOkHttp(this))
        lifecycleScope.launch {
            try {
                val p = api.getProduct(pid)
                product = p
                tvTitle.text = p.name
                // 有圖就載，沒有就略過
//                if (ivImage != null && !p.imageUrl.isNullOrBlank()) {
//                    Glide.with(ivImage)
//                        .load(p.imageUrl)
//                        .placeholder(android.R.drawable.ic_menu_report_image)
//                        .error(android.R.drawable.ic_menu_report_image)
//                        .centerCrop()
//                        .into(ivImage)
//                }
            } catch (e: Exception) {
                Toast.makeText(this@ProductDetailActivity, "載入商品失敗：${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // 甜度/冰塊
        rgSweet.setOnCheckedChangeListener { _, id ->
            sel.sweet = when (id) {
                R.id.rbSweet0  -> "sweet_0"
                R.id.rbSweet3  -> "sweet_3"
                R.id.rbSweet5  -> "sweet_5"
                R.id.rbSweet7  -> "sweet_7"
                R.id.rbSweet10 -> "sweet_10"
                else -> null
            }
        }
        rgIce.setOnCheckedChangeListener { _, id ->
            sel.ice = when (id) {
                R.id.rbIce0      -> "ice_0"
                R.id.rbIceLess   -> "ice_less"
                R.id.rbIceNormal -> "ice_normal"
                R.id.rbIceHot    -> "ice_hot"
                else -> null
            }
        }

        // 加料
        fun refreshToppings() {
            sel.toppings.clear()
            if (cbPearl.isChecked)   sel.toppings += "top_pearl"
            if (cbCoconut.isChecked) sel.toppings += "top_coconut"
            if (cbPudding.isChecked) sel.toppings += "top_pudding"
        }
        val watcher = { _: CompoundButton, _: Boolean -> refreshToppings() }
        cbPearl.setOnCheckedChangeListener(watcher)
        cbCoconut.setOnCheckedChangeListener(watcher)
        cbPudding.setOnCheckedChangeListener(watcher)

        // 加入購物車
        btnAdd.setOnClickListener {
            val p = product
            if (p == null) {
                Toast.makeText(this, "商品尚未載入完成", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (sel.sweet == null || sel.ice == null) {
                Toast.makeText(this, "請選擇甜度與冰塊", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (sel.toppings.size > 3) {
                Toast.makeText(this, "加料最多選 3 項", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CartManager.add(p, sel)
            startActivity(Intent(this, CartActivity::class.java))
        }
    }
}
