package com.example.drinks.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.net.StoreApi

class SelectStoreActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorView: View
    private lateinit var errorText: TextView
    private lateinit var emptyView: TextView

    private val adapter = StoreAdapter { store ->
        // TODO: 使用者點選後的處理
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_store)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        errorView = findViewById(R.id.errorView)
        errorText = findViewById(R.id.errorText)
        emptyView = findViewById(R.id.emptyView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 只在 onCreate 呼叫一次，避免 onResume 再觸發 loading
        loadStores()
    }

    private fun loadStores() {
        setLoading(true)
        errorView.visibility = View.GONE
        emptyView.visibility = View.GONE

        StoreApi.getStores(
            context = this,
            onSuccess = { list ->
                runOnUiThread {
                    setLoading(false)               // ← 成功一定關閉
                    adapter.submitList(list)
                    emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    errorView.visibility = View.GONE
                }
            },
            onError = { msg ->
                runOnUiThread {
                    setLoading(false)               // ← 失敗也一定關閉
                    errorView.visibility = View.VISIBLE
                    errorText.text = msg
                }
            }
        )
    }

    private fun setLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}
