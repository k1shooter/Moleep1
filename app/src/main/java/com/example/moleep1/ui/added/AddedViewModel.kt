// com.example.moleep1.ui.added.AddedViewModel.kt

package com.example.moleep1.ui.added

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AddedViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Added Fragment"
    }
    val text: LiveData<String> = _text
}