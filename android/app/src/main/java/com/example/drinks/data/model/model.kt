// model.kt（檔頭維持 package 不變）
package com.example.drinks.data.model

import com.google.gson.annotations.SerializedName

data class Page<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)

data class Store(
    val id: Int,
    val name: String,
    val phone: String?,
    val address: String?,
    @SerializedName("open_hours") val open_hours: String?, // 對齊後端欄位
    val status: String?
)


data class SelectedOptions(
    var sweet: String? = null,                 // e.g. "sweet_3"
    var ice: String? = null,                   // e.g. "ice_less"
    val toppings: MutableList<String> = mutableListOf(), // e.g. ["top_pearl", ...]
    var note: String? = null                   // 備註
)
