package com.example.drinks.data.model

object Options {
    val sweet = listOf("sweet_0","sweet_3","sweet_5","sweet_7","sweet_10")
    val ice = listOf("ice_0","ice_less","ice_normal","ice_hot")
    val toppings = mapOf(
        "top_pearl" to 10,
        "top_coconut" to 10,
        "top_pudding" to 15
    )
    fun label(id: String) = when(id){
        "sweet_0"->"無糖"; "sweet_3"->"三分糖"; "sweet_5"->"半糖"; "sweet_7"->"少糖"; "sweet_10"->"全糖";
        "ice_0"->"去冰"; "ice_less"->"微冰"; "ice_normal"->"正常冰"; "ice_hot"->"熱飲";
        "top_pearl"->"珍珠"; "top_coconut"->"椰果"; "top_pudding"->"布丁"; else -> id
    }
}