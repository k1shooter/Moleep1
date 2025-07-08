// ui/dashboard/DashboardPrefsManager.kt
package com.example.moleep1.ui.dashboard

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DashboardPrefsManager(context: Context) {
    private val prefs = context.getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val KEY_FIRST_LAUNCH = "is_first_launch"

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun saveUriList(uriList: List<Uri>) {
        val stringList = uriList.map { it.toString() }
        val json = gson.toJson(stringList)
        prefs.edit().putString("image_uri_list", json).apply()
    }

    // JSON 문자열을 불러와 URI 리스트로 변환
    fun loadUriList(): MutableList<Uri> {
        val json = prefs.getString("image_uri_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<String>>() {}.type
            val stringList: List<String> = gson.fromJson(json, type)
            stringList.map { Uri.parse(it) }.toMutableList()
        } else {
            mutableListOf()
        }
    }
}