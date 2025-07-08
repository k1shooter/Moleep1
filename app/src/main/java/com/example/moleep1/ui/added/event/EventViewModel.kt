package com.example.moleep1.ui.added.event

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moleep1.data.network.ApiClient
import com.example.moleep1.data.network.DirectionsRequest
import com.example.moleep1.data.network.DirectionsResponse
import com.kakao.vectormap.LatLng
import kotlinx.coroutines.launch

class EventViewModel(private val eventManager: EventManager) : ViewModel() {

    private val _eventList = MutableLiveData<MutableList<EventItem>>()
    val eventList: LiveData<MutableList<EventItem>> = _eventList

    // 단일 이벤트를 위한 LiveData
    private val _newPinAdded = MutableLiveData<Event<EventItem>>()
    val newPinAdded: LiveData<Event<EventItem>> = _newPinAdded

    private val _pinUpdated = MutableLiveData<Event<EventItem>>()
    val pinUpdated: LiveData<Event<EventItem>> = _pinUpdated

    private val _pinDeleted = MutableLiveData<Event<String>>()
    val pinDeleted: LiveData<Event<String>> = _pinDeleted

    private val _directionsResult = MutableLiveData<Event<DirectionsResponse>>()
    val directionsResult: LiveData<Event<DirectionsResponse>> = _directionsResult


    init {
        // ViewModel 생성 시 SharedPreferences에서 데이터 로드
        _eventList.value = eventManager.loadEventList()
    }

    fun findEventById(eventId: String): EventItem? {
        return _eventList.value?.find { it.eventId == eventId }
    }

    fun getEventsForAttendee(personId: String): List<EventItem> {
        return _eventList.value
            ?.filter { it.attendeeIds.contains(personId) && it.eventTime != null } // 해당 인물이 포함되고 시간이 있는 사건만 필터링
            ?.sortedBy { it.eventTime } // 시간순으로 정렬
            ?: emptyList()
    }

    fun addOrUpdateEvent(
        eventId: String?,
        name: String,
        desc: String,
        latLng: LatLng,
        photoPath: String?, // 이제 URI가 아닌 파일 경로
        eventTime: Long?,
        attendeeIds: List<String>
    ): EventItem {
        val currentList = _eventList.value ?: mutableListOf()
        val existingEvent = if (eventId != null) findEventById(eventId) else null

        val savedEvent: EventItem

        if (existingEvent != null) {
            // --- 기존 이벤트 수정 ---
            existingEvent.eventName = name
            existingEvent.description = desc
            existingEvent.eventTime = eventTime
            existingEvent.photoUri = photoPath
            if (existingEvent.attendeeIds == null) {
                existingEvent.attendeeIds = mutableListOf()
            }
            existingEvent.attendeeIds.clear()
            existingEvent.attendeeIds.addAll(attendeeIds)

            savedEvent = existingEvent

            // "수정" 신호를 LiveData로 보냄
            _pinUpdated.value = Event(savedEvent)

        } else {
            // --- 새 이벤트 추가 ---
            val newEvent = EventItem(
                eventName = name, description = desc,
                latitude = latLng.latitude, longitude = latLng.longitude,
                photoUri = photoPath, eventTime = eventTime,
                attendeeIds = attendeeIds.toMutableList()
            )
            currentList.add(newEvent)

            savedEvent = newEvent

            // "추가" 신호를 LiveData로 보냄
            _newPinAdded.value = Event(savedEvent)
        }

        // 전체 리스트 상태를 최신으로 업데이트하고 SharedPreferences에 저장
        _eventList.value = currentList.toMutableList()
        eventManager.saveEventList(currentList)

        // 수정 또는 추가된 EventItem 객체를 반환
        return savedEvent
    }

    fun deleteEvent(eventId: String) {
        val currentList = _eventList.value ?: return

        // 리스트에서 해당 아이템 제거
        currentList.removeAll { it.eventId == eventId }

        // SharedPreferences에 변경된 리스트 저장
        eventManager.saveEventList(currentList)
        // 전체 리스트 LiveData 업데이트
        _eventList.value = currentList

        // '삭제' 신호 발생
        _pinDeleted.value = Event(eventId)
    }

    fun getMultiOriginDirections(request: DirectionsRequest) {
        viewModelScope.launch {
            try {
                val response = ApiClient.service.getDirections(body = request)
                if (response.isSuccessful) {
                    response.body()?.let {
                        _directionsResult.postValue(Event(it))
                    }
                } else {
                    // TODO: API 호출 실패 처리 (예: Toast 메시지)
                    Log.e("EventViewModel", "API Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                // TODO: 네트워크 에러 등 예외 처리
                Log.e("EventViewModel", "API Exception", e)
            }
        }
    }
}