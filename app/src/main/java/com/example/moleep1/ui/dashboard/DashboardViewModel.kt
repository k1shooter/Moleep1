package com.example.moleep1.ui.dashboard

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {

    // 데이터를 ViewModel에서 관리. LiveData를 사용해 데이터 변경을 감지할 수 있게 함
    private val _imageList = MutableLiveData<MutableList<Uri>>(mutableListOf())
    val imageList: LiveData<MutableList<Uri>> = _imageList

    // 리스트에 이미지 추가
    fun addImage(uri: Uri) {
        val list = _imageList.value ?: mutableListOf()
        list.add(uri)
        _imageList.value = list
    }

    // 전체 리스트 교체
    fun setList(newList: List<Uri>) {
        _imageList.value = newList.toMutableList()
    }
}