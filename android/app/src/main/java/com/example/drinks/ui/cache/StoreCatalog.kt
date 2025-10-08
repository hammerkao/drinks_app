package com.example.drinks.data.cache

import java.util.concurrent.ConcurrentHashMap

object StoreCatalog {
    private val idToName = ConcurrentHashMap<Int, String>()

    fun put(id: Int, name: String?) {
        if (id > 0 && !name.isNullOrBlank()) idToName[id] = name
    }

    fun nameOf(id: Int?): String? = id?.let { idToName[it] }
}
