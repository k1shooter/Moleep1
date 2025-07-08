package com.example.moleep1.ui.added.event

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EventManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("event_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveEventList(list: List<EventItem>) {
        val json = gson.toJson(list)
        prefs.edit().putString("event_list", json).apply()
    }

    fun loadEventList(): MutableList<EventItem> {
        val json = prefs.getString("event_list", null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<EventItem>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }
}