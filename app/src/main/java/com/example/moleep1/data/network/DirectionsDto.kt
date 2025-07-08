package com.example.moleep1.data.network

import com.google.gson.annotations.SerializedName

// --- 요청(Request)을 위한 데이터 클래스 ---

data class DirectionsRequest(
    val origins: List<Origin>,
    val destination: Destination,
    val radius: Int = 10000 // 기본값 설정
)

data class Origin(
    val x: String,
    val y: String,
    val key: String
)

data class Destination(
    val x: String,
    val y: String
)

// --- 응답(Response)을 위한 데이터 클래스 ---

data class DirectionsResponse(
    @SerializedName("trans_id")
    val transId: String,
    val routes: List<Route>
)

data class Route(
    @SerializedName("result_code")
    val resultCode: Int,
    @SerializedName("result_msg")
    val resultMsg: String,
    val key: String,
    val summary: Summary
)

data class Summary(
    val distance: Int, // 미터 단위
    val duration: Int  // 초 단위
)

data class CarRouteResponse(
    @SerializedName("trans_id")
    val transId: String,
    val routes: List<CarRoute>
)

data class CarRoute(
    @SerializedName("result_code")
    val resultCode: Int,
    @SerializedName("result_msg")
    val resultMsg: String,
    val summary: Summary,
    val sections: List<Section>
)

data class Section(
    val distance: Int,
    val duration: Int,
    val roads: List<Road>
)

data class Road(
    val distance: Int,
    val duration: Int,
    // 경로를 구성하는 좌표 목록 (가장 중요한 데이터)
    val vertexes: List<Double>
)