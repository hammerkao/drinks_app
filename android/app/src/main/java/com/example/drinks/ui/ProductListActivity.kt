package com.example.drinks.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.drinks.data.model.Product
import com.example.drinks.data.net.Api
import com.example.drinks.databinding.ActivityProductListBinding
import com.example.drinks.net.NetCore
import kotlinx.coroutines.launch

class ProductListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductListBinding
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 標題：優先 store_name，其次 title，再不然顯示「商品」
        binding.tvCategoryTitle.text =
            intent.getStringExtra("store_name")
                ?: intent.getStringExtra("title")
                        ?: "商品"

        // RecyclerView
        binding.rvProducts.layoutManager = LinearLayoutManager(this)
        adapter = ProductAdapter { product ->
            startActivity(
                Intent(this, ProductDetailActivity::class.java)
                    .putExtra("pid", product.id) // Int
            )
        }
        binding.rvProducts.adapter = adapter

        loadProducts()
    }

    private fun loadProducts() {
        val api = Api(NetCore.BASE_URL, NetCore.buildOkHttp(this))

        // 可選：帶入過濾條件（若後端有支援）
        val categoryId = intent.getIntExtra("category_id", -1).takeIf { it > 0 }

        lifecycleScope.launch {
            try {
                val list: List<Product> = api.listProducts(
                    // search = null,
                    // ordering = "id",
                    // minPrice = null, maxPrice = null,
                    category = categoryId
                )
                adapter.submitList(list)
            } catch (e: Exception) {
                Toast.makeText(this@ProductListActivity, "載入商品失敗：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
