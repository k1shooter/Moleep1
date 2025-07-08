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
    private val _viewedItem = MutableLiveData<list_item?>()
    val viewedItem: LiveData<list_item?> get() = _viewedItem

    fun selectItem(item: list_item) {
        selectedItem.value = item
    }

    fun addItem(item: list_item) {
        // 기존 리스트를 복사하여 새로운 리스트를 만듦
        val newList = _itemList.value.orEmpty().toMutableList()
        newList.add(item)
        _itemList.value = newList // 새 리스트를 할당
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

        // 2. 그림판 오브젝트도 해당 id로 삭제 (예: PlacedImage.profileId와 일치)

    }

    fun selectItemByPosition(position: Int) {
        val currentList = _itemList.value.orEmpty().toMutableList()
        _viewedItem.value = currentList[position]
    }

    fun selectItemById(id: String) {
        val item = _itemList.value?.find { it.id == id }
        _viewedItem.value = item // item이 null이어도 OK
    }


    fun setList(newList: List<list_item>) {
        _itemList.value = newList.toMutableList()
        prefsManager.saveItemList(_itemList.value?:mutableListOf())
    }

    fun saveAll() {
        _itemList.value?.let { prefsManager.saveItemList(it) }
    }
}