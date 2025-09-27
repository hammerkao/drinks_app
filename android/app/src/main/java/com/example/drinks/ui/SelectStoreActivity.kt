package com.example.drinks.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.data.model.Store
import com.example.drinks.data.net.Api
import com.example.drinks.net.NetCore
import kotlinx.coroutines.launch

class SelectStoreActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorView: View
    private lateinit var errorText: TextView
    private lateinit var emptyView: TextView

    private val adapter = StoreAdapter { store: Store ->
        // TODO: 點選後的處理（帶到商品頁等）
        // val it = Intent(this, MainActivity::class.java)
        // startActivity(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_store)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar  = findViewById(R.id.progressBar)
        errorView    = findViewById(R.id.errorView)
        errorText    = findViewById(R.id.errorText)
        emptyView    = findViewById(R.id.emptyView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadStores()
    }

    private fun loadStores() {
        setLoading(true)
        errorView.visibility = View.GONE
        emptyView.visibility = View.GONE

        // ✅ 建立 Api 實例（你的 Api.kt 是 class，不是 object）
        val api = Api(NetCore.BASE_URL, NetCore.buildOkHttp(this))

        lifecycleScope.launch {
            try {
                val list = api.getStores()   // ✅ 呼叫 suspend 函式
                setLoading(false)
                adapter.submitList(list)
                emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                errorView.visibility = View.GONE
            } catch (e: Exception) {
                setLoading(false)
                errorView.visibility = View.VISIBLE
                errorText.text = e.message ?: "載入失敗"
            }
        }
    }

    private fun setLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}
