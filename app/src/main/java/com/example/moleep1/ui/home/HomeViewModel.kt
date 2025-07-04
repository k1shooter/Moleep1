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
        // 기존 리스트를 복사하여 새로운 리스트를 만듦
        val newList = _itemList.value.orEmpty().toMutableList()
        newList.add(item)
        _itemList.value = newList // 새 리스트를 할당
    }

    fun updateItem(position: Int, newItem: list_item) {
        val currentList = _itemList.value.orEmpty().toMutableList()
        if (position in currentList.indices) {
            currentList[position] = newItem
            _itemList.value = currentList // 새 리스트를 할당
        }
    }

    fun setList(newList: List<list_item>) {
        _itemList.value = newList.toMutableList()
    }
}