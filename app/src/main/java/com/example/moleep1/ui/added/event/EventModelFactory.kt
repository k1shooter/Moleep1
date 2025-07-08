package com.example.moleep1.ui.added.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class EventViewModelFactory(private val eventManager: EventManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // 생성하려는 ViewModel이 EventViewModel 클래스와 호환되는지 확인
        if (modelClass.isAssignableFrom(EventViewModel::class.java)) {
            // EventViewModel을 생성하면서 생성자에 eventManager를 넘겨줌
            @Suppress("UNCHECKED_CAST")
            return EventViewModel(eventManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}