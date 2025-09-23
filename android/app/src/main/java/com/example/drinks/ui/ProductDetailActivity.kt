package com.example.drinks.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.drinks.R
import com.example.drinks.data.Repo
import com.example.drinks.data.model.SelectedOptions
import com.example.drinks.store.CartManager



class ProductDetailActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_product_detail)

        val pid = intent.getStringExtra("pid") ?: return
        val product = Repo.productById(pid)
        findViewById<TextView>(R.id.tvDetailTitle).text = product.name

        val rgSweet = findViewById<RadioGroup>(R.id.rgSweet)
        val rgIce = findViewById<RadioGroup>(R.id.rgIce)
        val cbPearl = findViewById<CheckBox>(R.id.cbPearl)
        val cbCoconut = findViewById<CheckBox>(R.id.cbCoconut)
        val cbPudding = findViewById<CheckBox>(R.id.cbPudding)

        val sel = SelectedOptions()

        rgSweet.setOnCheckedChangeListener { _, id ->
            sel.sweet = when(id){
                R.id.rbSweet0 -> "sweet_0"
                R.id.rbSweet3 -> "sweet_3"
                R.id.rbSweet5 -> "sweet_5"
                R.id.rbSweet7 -> "sweet_7"
                R.id.rbSweet10 -> "sweet_10"
                else -> null
            }
        }
        rgIce.setOnCheckedChangeListener { _, id ->
            sel.ice = when(id){
                R.id.rbIce0 -> "ice_0"
                R.id.rbIceLess -> "ice_less"
                R.id.rbIceNormal -> "ice_normal"
                R.id.rbIceHot -> "ice_hot"
                else -> null
            }
        }

        fun refreshToppings() {
            sel.toppings.clear()
            if (cbPearl.isChecked) sel.toppings += "top_pearl"
            if (cbCoconut.isChecked) sel.toppings += "top_coconut"
            if (cbPudding.isChecked) sel.toppings += "top_pudding"
        }
        val watcher = { _: CompoundButton, _: Boolean -> refreshToppings() }
        cbPearl.setOnCheckedChangeListener(watcher)
        cbCoconut.setOnCheckedChangeListener(watcher)
        cbPudding.setOnCheckedChangeListener(watcher)

        findViewById<Button>(R.id.btnAddToCart).setOnClickListener {
            if (sel.sweet == null || sel.ice == null) {
                Toast.makeText(this, "請選擇甜度與冰塊", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (sel.toppings.size > 3) {
                Toast.makeText(this, "加料最多選 3 項", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CartManager.add(product, sel)
            startActivity(Intent(this, CartActivity::class.java))
        }
    }
}
