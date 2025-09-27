package com.example.drinks.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.drinks.R

class HomeFragment : Fragment(R.layout.fragment_home) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 這頁暫時不顯示分類；之後要接後端再補列表邏輯
    }
}
