package com.example.drinks.data.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object GsonProvider { val gson: Gson = GsonBuilder().setLenient().create() }