package com.example.drinks.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drinks.R
import com.example.drinks.data.Repo

class HomeActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_home)
        val rv = findViewById<RecyclerView>(R.id.rvCategories)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = CategoryAdapter(Repo.categories) { cid ->
            startActivity(Intent(this, ProductListActivity::class.java).putExtra("cid", cid))
        }
    }
}