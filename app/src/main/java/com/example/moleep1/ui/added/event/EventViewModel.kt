// EventViewModel.kt
package com.example.moleep1.ui.added.event

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kakao.vectormap.LatLng

class EventViewModel(private val eventManager: EventManager) : ViewModel() {

    private val _eventList = MutableLiveData<MutableList<EventItem>>()
    val eventList: LiveData<MutableList<EventItem>> = _eventList

    init {
        _eventList.value = eventManager.loadEventList()
    }

    // ❗ [수정] eventId로 이벤트를 찾음
    fun findEventById(eventId: String): EventItem? {
        return _eventList.value?.find { it.eventId == eventId }
    }

    // ❗ [수정] 이벤트 추가/수정 로직 변경
    fun addOrUpdateEvent(eventId: String?, name: String, desc: String, latLng: LatLng): EventItem {
        val currentList = _eventList.value ?: mutableListOf()
        val existingEvent = if (eventId != null) findEventById(eventId) else null

        if (existingEvent != null) {
            // 업데이트
            existingEvent.eventName = name
            existingEvent.description = desc
            _eventList.value = currentList
            eventManager.saveEventList(currentList)
            return existingEvent
        } else {
            // 새로 추가
            val newEvent = EventItem(
                eventName = name,
                description = desc,
                latitude = latLng.latitude,
                longitude = latLng.longitude
            )
            currentList.add(newEvent)
            _eventList.value = currentList
            eventManager.saveEventList(currentList)
            return newEvent
        }
    }
}