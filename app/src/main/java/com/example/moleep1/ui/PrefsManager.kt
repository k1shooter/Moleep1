package com.example.moleep1.ui

import android.content.Context
import android.content.SharedPreferences
import com.example.moleep1.list_item
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveItemList(list: List<list_item>) {
        val json = gson.toJson(list)
        prefs.edit().putString("item_list", json).apply()
    }

    fun loadItemList(): MutableList<list_item> {
        val json = prefs.getString("item_list", null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<list_item>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }
}