package com.example.moleep1.ui.added.event

import android.graphics.Color
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moleep1.data.network.ApiClient
import com.example.moleep1.data.network.DirectionsResponse
import com.kakao.vectormap.LatLng
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import kotlin.math.abs

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

    private val _durationLabels = MutableLiveData<Event<List<DurationLabelInfo>>>()
    val durationLabels: LiveData<Event<List<DurationLabelInfo>>> = _durationLabels

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

    private val pathColors = listOf(
        Color.RED, Color.BLUE, Color.GREEN, Color.CYAN, Color.MAGENTA,
        Color.YELLOW, "#FFA500".toColorInt() // Orange
    )

    fun calculateDurationsForActivePaths(personIds: Set<String>) {
        viewModelScope.launch {
            val allLabelsInfo = mutableListOf<DurationLabelInfo>()

            // 활성화된 각 인물에 대해 반복
            personIds.forEach { personId ->
                val events = getEventsForAttendee(personId)
                if (events.size < 2) return@forEach

                val pathColor = pathColors[abs(personId.hashCode()) % pathColors.size]

                // 각 인물의 경로 구간별로 반복
                for (i in 0 until events.size - 1) {
                    val originEvent = events[i]
                    val destinationEvent = events[i + 1]
                    val originStr = "${originEvent.longitude},${originEvent.latitude}"
                    val destStr = "${destinationEvent.longitude},${destinationEvent.latitude}"

                    try {
                        val response = ApiClient.service.getCarDirections(origin = originStr, destination = destStr)
                        if (response.isSuccessful) {
                            response.body()?.routes?.firstOrNull()?.let { route ->
                                val allRoads = route.sections.flatMap { it.roads }
                                if (allRoads.isNotEmpty()) {
                                    // 1. 경로를 구성하는 도로들 중 가장 중간에 있는 도로를 찾음
                                    val midRoad = allRoads[allRoads.size / 2]
                                    // 2. 해당 도로의 좌표들 중 가장 중간 좌표를 찾음
                                    val midVertexIndex = (midRoad.vertexes.size / 2)
                                    // 3. index가 짝수인지 확인하여 (x,y) 쌍을 맞춤
                                    val finalMidIndex = if (midVertexIndex % 2 == 0) midVertexIndex else midVertexIndex - 1

                                    if (finalMidIndex + 1 < midRoad.vertexes.size) {
                                        val midLng = midRoad.vertexes[finalMidIndex]
                                        val midLat = midRoad.vertexes[finalMidIndex + 1]
                                        val midPoint = LatLng.from(midLat, midLng)

                                        val durationText = "${route.summary.duration / 60}분"

                                        val travelDuration = route.summary.duration
                                        val eventTimeDiff = ((destinationEvent.eventTime ?: 0) - (originEvent.eventTime ?: 0)) / 1000
                                        val isAnomalous = eventTimeDiff > 0 && eventTimeDiff < travelDuration

                                        allLabelsInfo.add(DurationLabelInfo(midPoint, durationText, pathColor, isAnomalous))
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) { Log.e("ViewModel", "API Exception", e) }
                }
            }
            // 계산이 완료된 모든 라벨 정보를 LiveData로 전달
            _durationLabels.postValue(Event(allLabelsInfo))
        }
    }
}