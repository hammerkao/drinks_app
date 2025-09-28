package com.example.drinks.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.drinks.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_main)

        // 這三行是關鍵：用 NavHostFragment 取得 navController
        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHost) as NavHostFragment
        val navController = navHost.navController



        val bottom = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottom.setupWithNavController(navController)

        // menu item 的 id 要和 nav_graph 的 destination id 對得上
        bottom.setupWithNavController(navController)

        // 重新點擊目前 tab 回到根（可選）
        bottom.setOnItemReselectedListener { dest ->
            // 交給 Navigation 自己處理即可，通常會 noop
        }
    }
}
