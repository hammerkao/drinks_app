package com.example.drinks.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.drinks.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController
        val bottom = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottom.setupWithNavController(navController)

        // 在「選擇分店」頁：只改高亮，不觸發導航
        navController.addOnDestinationChangedListener { _, dest, _ ->
            val atBranch = dest.id == R.id.nav_branch_list
            if (atBranch) {
                bottom.menu.findItem(R.id.nav_order)?.isChecked = true
                // 視覺弱化（不禁用，才能收到點擊）：
                bottom.alpha = 0.6f
            } else {
                bottom.alpha = 1f
            }
        }

        // 攔截點擊：在分店頁，任何 BottomNav 項目都提示並阻止導航
        bottom.setOnItemSelectedListener { item ->
            val atBranch = navController.currentDestination?.id == R.id.nav_branch_list

            if (atBranch) {
                // 在分店頁：任何按鈕都不允許切換，也不允許變成高亮
                Toast.makeText(this, "請先選擇分店", Toast.LENGTH_SHORT).show()

                // 強制維持高亮在「飲品」tab（僅改勾選，不觸發導航）
                bottom.menu.findItem(R.id.nav_order)?.isChecked = true

                // 回傳 false 代表「不要選中剛點的 item」，所以不會變高亮也不會導航
                return@setOnItemSelectedListener false
            }

            // 其它頁面：交給 Navigation 處理並回傳 true（允許選中/高亮與導航）
            NavigationUI.onNavDestinationSelected(item, navController)
            true
        }

        bottom.setOnItemReselectedListener { item ->
            val graph = navController.graph.findNode(item.itemId) as? NavGraph ?: return@setOnItemReselectedListener
            navController.popBackStack(graph.startDestinationId, false)
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        val bottom = findViewById<BottomNavigationView>(R.id.bottomNav)
        when (i?.getStringExtra("open_tab")) {
            "cart"  -> bottom.selectedItemId = R.id.nav_cart
            "order" -> bottom.selectedItemId = R.id.nav_order
            "orders" -> bottom.selectedItemId = R.id.nav_orders   // ← 新增
        }
    }
}

