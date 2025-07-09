// ui/added/event/DurationLabelInfo.kt
package com.example.moleep1.ui.added.event

import com.kakao.vectormap.LatLng

data class DurationLabelInfo(
    val position: LatLng, // 텍스트가 표시될 위치 (경로의 중간 지점)
    val text: String,      // 표시될 텍스트 (예: "5분")
    val pathColor: Int,
    val isAnomalous: Boolean
)