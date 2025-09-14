
package com.example.drinks.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.drinks.data.net.NetCore
import com.example.drinks.data.store.TokenStore
import com.example.drinks.databinding.ActivityProductListBinding
import com.example.drinks.ui.ProductAdapter
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager


// import androidx.core.view.isVisible
// import android.util.Log
// 其他 import 保持

class ProductListActivity : AppCompatActivity() {

    private lateinit var b: ActivityProductListBinding
    private val tokenStore by lazy { TokenStore(this) }
    private val api by lazy { NetCore.api(this) }
    private val auth by lazy { NetCore.auth(this) }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityProductListBinding.inflate(layoutInflater)
        setContentView(b.root)

        val ad = ProductAdapter { p -> addToCart(p.id) }
        b.recycler.adapter = ad
        b.recycler.layoutManager = LinearLayoutManager(this)


        // 先把 UI 建好，再嘗試登入；登入失敗也要照樣載產品
        lifecycleScope.launch {
            ensureLoginIfPossible()    // 不成功也不會中斷
            loadProducts(ad)
        }
    }

    private suspend fun ensureLoginIfPossible() {
        if (tokenStore.access != null) return
        try {
            val t = auth.login("admin", "yourpassword") // ← 換成你的帳密
            tokenStore.access = t.access; tokenStore.refresh = t.refresh
            toast("已登入")
        } catch (e: Exception) {
            Log.w("DRINKS", "login failed", e)
            toast("未登入（先載產品清單）")
        }
    }

    private fun loadProducts(ad: ProductAdapter) {



        // 如果你有 empty TextView： b.empty.isVisible = list.isEmpty()
        b.swipe.isRefreshing = true
        lifecycleScope.launch {
            try {
                val list = api.listProducts(ordering = "name")
                ad.submit(list)
                // 顯示空狀態
                b.empty.isVisible = list.isEmpty()
                Toast.makeText(this@ProductListActivity, "取得 ${list.size} 筆商品", Toast.LENGTH_SHORT).show()
                if (list.isEmpty()) toast("目前沒有商品，先到後端新增吧")
            } catch (e: java.io.IOException) {
                toast("連不上後端：${e.message}（確認 runserver 與 BASE_URL）")
            } catch (e: Exception) {
                toast("載入失敗：${e.javaClass.simpleName} ${e.message}")
            } finally {
                b.swipe.isRefreshing = false
            }
        }
    }

    private fun addToCart(productId: Int) {
        lifecycleScope.launch {
            try { api.addItem(productId, 1); toast("已加入購物車") }
            catch (e: Exception) { toast("加入失敗：${e.message}") }
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
