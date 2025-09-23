package com.example.drinks.data

import com.example.drinks.data.model.Category
import com.example.drinks.data.model.Product

object Repo {
    val categories = listOf(
        Category("cat_milk",  "奶類"),
        Category("cat_tea",   "茶類"),
        Category("cat_juice", "果汁"),
    )

    // 一定要有 categoryId 欄位（對應 Product.data class）
    val products = listOf(
        Product("prd_milktea_pearl",           "cat_milk", "珍珠奶茶",        65, image = ""),
        Product("prd_milk_cream_green_tea",    "cat_milk", "奶蓋青茶",        60, image = ""),
        Product("prd_roselle_tea",             "cat_tea",  "洛神花茶",        45, image = ""),
        Product("prd_honey_lemon",             "cat_juice","蜂蜜檸檬",        50, image = ""),
        Product("prd_high_mountain_green_tea", "cat_tea",  "高山青茶",        40, image = "")
    )

    // 方便查標題用
    val categoryNameById: Map<String, String> =
        categories.associate { it.id to it.name }

    fun categoryProducts(cid: String) =
        products.filter { it.categoryId == cid }

    fun productById(id: String) =
        products.first { it.id == id }
}
