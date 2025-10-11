package com.example.drinks.data.cache

import java.util.concurrent.ConcurrentHashMap

object StoreCatalog {
    data class StoreInfo(
        val id: Int,
        val name: String?,
        val phone: String?,
        val address: String?,
        val hours: String?,
    )
    private val map = mutableMapOf<Int, StoreInfo>()

    fun put(id: Int, name: String?, phone: String? = null, address: String? = null, hours: String? = null) {
        val old = map[id]
        map[id] = StoreInfo(
            id = id,
            name = name ?: old?.name,
            phone = phone ?: old?.phone,
            address = address ?: old?.address,
            hours = hours ?: old?.hours
        )
    }
    fun putAll(stores: List<StoreInfo>) = stores.forEach { put(it.id, it.name, it.phone, it.address, it.hours) }

    fun infoOf(id: Int): StoreInfo? = map[id]
    fun nameOf(id: Int): String? = map[id]?.name
}