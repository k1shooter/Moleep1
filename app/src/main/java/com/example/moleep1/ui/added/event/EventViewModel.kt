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

    fun addOrUpdateEvent(eventId: String?, name: String, desc: String, latLng: LatLng, photoUri: String?, eventTime: String?): EventItem {
        val currentList = _eventList.value ?: mutableListOf()
        val existingEvent = if (eventId != null) findEventById(eventId) else null

        val savedEvent: EventItem
        if (existingEvent != null) {
            existingEvent.eventName = name
            existingEvent.description = desc
            existingEvent.eventTime = eventTime
            // 사진 경로가 null이 아니면 업데이트 (사진을 지우는 경우는 아직 미구현)
            if (photoUri != null) {
                existingEvent.photoUri = photoUri
            }
            savedEvent = existingEvent
        } else {
            val newEvent = EventItem(
                eventName = name, description = desc,
                latitude = latLng.latitude, longitude = latLng.longitude,
                photoUri = photoUri, eventTime = eventTime
            )
            currentList.add(newEvent)
            savedEvent = newEvent
        }

        // ❗ [확인] 이 코드가 있어야 UI가 즉시 갱신됩니다.
        _eventList.value = currentList.toMutableList()

        eventManager.saveEventList(currentList)
        return savedEvent
    }
}