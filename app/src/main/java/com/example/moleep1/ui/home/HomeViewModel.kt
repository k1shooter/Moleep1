package com.example.moleep1.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.moleep1.list_item
import com.example.moleep1.ui.PrefsManager

class HomeViewModel(private val prefsManager: PrefsManager) : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text


    private val _itemList = MutableLiveData<MutableList<list_item>>().apply {
        value = prefsManager.loadItemList()
    }

    val itemList: LiveData<MutableList<list_item>> = _itemList

    val selectedItem = MutableLiveData<list_item>()

    fun selectItem(item: list_item) {
        selectedItem.value = item
    }

    fun addItem(item: list_item) {
        val newList = _itemList.value.orEmpty().toMutableList()
        newList.add(item)
        _itemList.value = newList
        prefsManager.saveItemList(newList)
    }

    fun updateItem(id: String, newItem: list_item) {
        val currentList = _itemList.value.orEmpty().toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            currentList[index] = newItem
            _itemList.value = currentList
            prefsManager.saveItemList(currentList)
        }
    }

    fun removeItem(id: String){
        val currentList = _itemList.value.orEmpty().toMutableList()
        val newList = currentList.filter { it.id != id }
        _itemList.value = newList.toMutableList()
        prefsManager.saveItemList(newList)
    }



    fun setList(newList: List<list_item>) {
        _itemList.value = newList.toMutableList()
        prefsManager.saveItemList(_itemList.value?:mutableListOf())
    }

    fun clearAllProfiles() {
        _itemList.value = mutableListOf() // 리스트 초기화
        prefsManager.saveItemList(emptyList()) // SharedPreferences 초기화
    }
}