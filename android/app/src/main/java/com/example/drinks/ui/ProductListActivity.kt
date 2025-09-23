package com.example.drinks.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.drinks.data.Repo
import com.example.drinks.databinding.ActivityProductListBinding

class ProductListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 分類 ID：優先用 Intent，否則用第一個分類
        val cid = intent.getStringExtra("cid") ?: Repo.categories.first().id

        // 標題
        binding.tvCategoryTitle.text = Repo.categoryNameById[cid] ?: "商品"

        // RecyclerView
        binding.rvProducts.layoutManager = LinearLayoutManager(this)
        val adapter = ProductAdapter { product ->
            startActivity(
                Intent(this, ProductDetailActivity::class.java)
                    .putExtra("pid", product.id)
            )
        }
        binding.rvProducts.adapter = adapter
        adapter.submit(Repo.categoryProducts(cid))
    }
}
