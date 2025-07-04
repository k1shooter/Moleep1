package com.example.moleep1.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.moleep1.list_item

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text


    private val _itemList = MutableLiveData<MutableList<list_item>>().apply {
        value = mutableListOf()
    }
    val itemList: LiveData<MutableList<list_item>> = _itemList

    fun addItem(item: list_item) {
        val currentList = _itemList.value ?: ArrayList()
        currentList.add(item)
        _itemList.value = currentList
    }

    fun updateItem(position: Int, newItem: list_item) {
        val currentList = _itemList.value ?: return
        if (position in currentList.indices) {
            currentList[position] = newItem
            _itemList.value = currentList.toMutableList()
        }
    }

    fun setList(newList: ArrayList<list_item>) {
        _itemList.value = newList.toMutableList()
    }
}