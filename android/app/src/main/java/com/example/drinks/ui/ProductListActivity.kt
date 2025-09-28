package com.example.drinks.ui

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

        // ✅ 不帶 lambda 的簡單版 Adapter：點擊行為已在 ProductAdapter 內部直接啟動詳情頁
        adapter = ProductAdapter()
        binding.rvProducts.adapter = adapter

        loadProducts()
    }

    private fun loadProducts() {
        val api = Api(NetCore.BASE_URL, NetCore.buildOkHttp(this))
        val categoryId = intent.getIntExtra("category_id", -1).takeIf { it > 0 }

        lifecycleScope.launch {
            try {
                val list: List<Product> = api.listProducts(category = categoryId)
                adapter.submitList(list)
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProductListActivity,
                    "載入商品失敗：${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
