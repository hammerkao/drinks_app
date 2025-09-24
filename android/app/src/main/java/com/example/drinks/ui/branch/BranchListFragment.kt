package com.example.drinks.ui.branch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.drinks.R
import com.example.drinks.data.model.BranchDto
import com.example.drinks.data.net.Api
import com.example.drinks.net.NetCore
import kotlinx.coroutines.launch

class BranchListFragment : Fragment() {

    // 這裡宣告 adapter
    private lateinit var adapter: BranchAdapter

    private val api by lazy {
        Api(
            base = NetCore.BASE_URL,
            client = NetCore.buildOkHttp(requireContext())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_branch_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化 RecyclerView
        val rv = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvBranches)
        adapter = BranchAdapter(onClickOrder = { branch ->
            // 點擊事件：之後導到商品頁
            Toast.makeText(requireContext(), "下單：${branch.name}", Toast.LENGTH_SHORT).show()
        })
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // 進入頁面就載入資料
        loadData { /* loading 完成後做額外處理 */ }
    }

    private fun loadData(done: () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val list = api.getBranches() // <-- 這裡已經存在
                adapter.submit(list)         // <-- 這裡不會再報錯
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "載入失敗：${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                done()
            }
        }
    }
}
