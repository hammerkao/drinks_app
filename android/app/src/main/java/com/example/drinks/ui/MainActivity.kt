package com.example.drinks.ui

import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.drinks.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_main)

        // 1) 透過 NavHostFragment 拿 NavController（最穩）
        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHost) as NavHostFragment
        val nav = navHost.navController

        // 2) 綁定 BottomNavigationView
        val bottom = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottom.setupWithNavController(nav)

        // （可選）如果你的 menu item id 與 nav_graph 目的地 id 不一致，改用手動對應：
        // bottom.setOnItemSelectedListener { item ->
        //     when (item.itemId) {
        //         R.id.menu_store   -> nav.navigate(R.id.dest_select_store)
        //         R.id.menu_home    -> nav.navigate(R.id.dest_home)
        //         R.id.menu_cart    -> nav.navigate(R.id.dest_cart)
        //         R.id.menu_profile -> nav.navigate(R.id.dest_profile)
        //     }
        //     true
        // }

        // （可選）同目的地重點擊不重刷
        // bottom.setOnItemReselectedListener { /* no-op */ }

        // （可選）後退鍵行為：若不能再返回，就結束 Activity
        onBackPressedDispatcher.addCallback(this) {
            if (!nav.popBackStack()) finish()
        }
    }
}
