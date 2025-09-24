package com.example.drinks.ui.branch

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.data.model.BranchDto
import com.example.drinks.data.net.Api
import com.example.drinks.net.NetCore
import kotlinx.coroutines.launch
import java.io.IOException

class BranchListFragment : Fragment() {

    private lateinit var emptyView: TextView
    private lateinit var recyclerView: RecyclerView

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

        emptyView = view.findViewById(R.id.emptyView)
        recyclerView = view.findViewById(R.id.rvBranches)

        // 初次載入時隱藏
        emptyView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun updateUI(list: List<BranchDto>) {
        if (list.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.submit(list)  // 更新 RecyclerView 資料
        }
    }

    private fun loadData(done: () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val list = api.getBranches()
                adapter.submit(list)
            } catch (e: IOException) {
                Log.e("BranchList", "network/http error", e)   // 會看到 401/403 內容
                Toast.makeText(requireContext(), "載入失敗：網路/權限 (${e.message})", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("BranchList", "parse error", e)          // 例如 Expected BEGIN_ARRAY…
                Toast.makeText(requireContext(), "載入失敗：資料解析錯誤", Toast.LENGTH_LONG).show()
            } finally { done() }
        }
    }
}
