package com.example.drinks.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.data.Repo

class HomeFragment : Fragment() {
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        return i.inflate(R.layout.fragment_home, c, false)
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        val rv = v.findViewById<RecyclerView>(R.id.rvCategories)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = CategoryAdapter(Repo.categories) { cid ->
            // TODO: 導到商品列表的 Fragment（之後你把 ProductListActivity 也改成 Fragment）
        }
    }
}
