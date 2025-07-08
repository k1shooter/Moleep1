// EventItem.kt
package com.example.moleep1.ui.added.event

import java.util.UUID

data class EventItem(
    val eventId: String = UUID.randomUUID().toString(),
    var eventName: String,
    var description: String,
    var photoUri: String? = null,
    var eventTime: Long? = null,
    var attendeeIds: MutableList<String> = mutableListOf(),
    val latitude: Double,
    val longitude: Double
)