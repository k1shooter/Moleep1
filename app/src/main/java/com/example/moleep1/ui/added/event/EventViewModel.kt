package com.example.moleep1.ui.added.event

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moleep1.data.network.ApiClient
import com.example.moleep1.data.network.DirectionsResponse
import com.kakao.vectormap.LatLng
import kotlinx.coroutines.launch

class EventViewModel(private val eventManager: EventManager) : ViewModel() {
    private val _eventList = MutableLiveData<MutableList<EventItem>>()
    val eventList: LiveData<MutableList<EventItem>> = _eventList

    // --- 단일 이벤트를 위한 LiveData ---
    private val _newPinAdded = MutableLiveData<Event<EventItem>>()
    val newPinAdded: LiveData<Event<EventItem>> = _newPinAdded

    private val _pinUpdated = MutableLiveData<Event<EventItem>>()
    val pinUpdated: LiveData<Event<EventItem>> = _pinUpdated

    private val _pinDeleted = MutableLiveData<Event<String>>()
    val pinDeleted: LiveData<Event<String>> = _pinDeleted

    // --- 경로 탐색을 위한 LiveData ---
    private val _directionsResult = MutableLiveData<Event<DirectionsResponse>>()
    val directionsResult: LiveData<Event<DirectionsResponse>> = _directionsResult

    private val _routePath = MutableLiveData<Event<Pair<String, List<LatLng>>>>()
    val routePath: LiveData<Event<Pair<String, List<LatLng>>>> = _routePath

    init {
        // ViewModel이 생성될 때 저장소에서 데이터를 불러옴
        val loadedList = eventManager.loadEventList()
        _eventList.value = loadedList
        Log.d("ViewModelInit", "저장소에서 불러온 Event 개수: ${loadedList.size}")

    }
    fun findEventById(eventId: String): EventItem? {
        return _eventList.value?.find { it.eventId == eventId }
    }

    fun getEventsForAttendee(personId: String): List<EventItem> {
        return _eventList.value
            ?.filter { it.attendeeIds.contains(personId) && it.eventTime != null }
            ?.sortedBy { it.eventTime }
            ?: emptyList()
    }

    fun addOrUpdateEvent(
        eventId: String?, name: String, desc: String, latLng: LatLng,
        photoPath: String?, eventTime: Long?, attendeeIds: List<String>
    ) {
        val currentList = _eventList.value ?: mutableListOf()
        val existingEvent = if (eventId != null) findEventById(eventId) else null

        if (existingEvent != null) {
            existingEvent.eventName = name
            existingEvent.description = desc
            existingEvent.eventTime = eventTime
            existingEvent.photoUri = photoPath
            if (existingEvent.attendeeIds == null) {
                existingEvent.attendeeIds = mutableListOf()
            }
            existingEvent.attendeeIds.clear()
            existingEvent.attendeeIds.addAll(attendeeIds)
            _pinUpdated.value = Event(existingEvent)
        } else {
            val newEvent = EventItem(
                eventName = name, description = desc,
                latitude = latLng.latitude, longitude = latLng.longitude,
                photoUri = photoPath, eventTime = eventTime,
                attendeeIds = attendeeIds.toMutableList()
            )
            currentList.add(newEvent)
            _newPinAdded.value = Event(newEvent)
        }
        _eventList.value = currentList.toMutableList()
        eventManager.saveEventList(currentList)
    }

    fun deleteEvent(eventId: String) {
        val currentList = _eventList.value ?: return
        currentList.removeAll { it.eventId == eventId }
        eventManager.saveEventList(currentList)
        _eventList.value = currentList
        _pinDeleted.value = Event(eventId)
    }

    fun findRoutePathForEvents(events: List<EventItem>, personId: String) {
        if (events.size < 2) return

        viewModelScope.launch {
            val totalVertexes = mutableListOf<Double>()
            for (i in 0 until events.size - 1) {
                val originEvent = events[i]
                val destinationEvent = events[i + 1]
                val originStr = "${originEvent.longitude},${originEvent.latitude}"
                val destStr = "${destinationEvent.longitude},${destinationEvent.latitude}"

                try {
                    val response = ApiClient.service.getCarDirections(origin = originStr, destination = destStr)
                    if (response.isSuccessful) {
                        response.body()?.routes?.firstOrNull()?.sections?.forEach { section ->
                            section.roads.forEach { road ->
                                totalVertexes.addAll(road.vertexes)
                            }
                        }
                    } else { Log.e("ViewModel", "API Error: ${response.code()}") }
                } catch (e: Exception) { Log.e("ViewModel", "API Exception", e) }
            }

            val finalPath = mutableListOf<LatLng>()
            for (i in totalVertexes.indices step 2) {
                if (i + 1 < totalVertexes.size) {
                    val lng = totalVertexes[i]
                    val lat = totalVertexes[i + 1]
                    finalPath.add(LatLng.from(lat, lng))
                }
            }
            _routePath.postValue(Event(personId to finalPath))
        }
    }
}