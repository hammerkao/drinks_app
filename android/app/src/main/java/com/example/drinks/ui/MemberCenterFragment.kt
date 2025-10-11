package com.example.drinks.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.drinks.R
import com.google.android.material.appbar.MaterialToolbar

class MemberCenterFragment : Fragment(R.layout.fragment_member_center) {

    private lateinit var toolbar: MaterialToolbar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        view.findViewById<TextView>(R.id.tvToolbarTitle).text = "會員中心"
        view.findViewById<ImageButton>(R.id.btnNavBack)?.setOnClickListener {
            findNavController().popBackStack()
        }
        // 所有文字都在 XML，這裡不做任何資料/功能。
    }
}
