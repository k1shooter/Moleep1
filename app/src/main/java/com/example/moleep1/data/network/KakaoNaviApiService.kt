package com.example.moleep1.data.network

import com.example.moleep1.BuildConfig
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface KakaoNaviApiService {
    @POST("v1/origins/directions")
    suspend fun getDirections(
        @Header("Authorization") key: String = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}",
        @Header("Content-Type") contentType: String = "application/json",
        @Body body: DirectionsRequest
    ): Response<DirectionsResponse>

    @GET("v1/directions")
    suspend fun getCarDirections(
        @Header("Authorization") key: String = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}",
        @Query("origin") origin: String, // "x,y" 형태 (경도,위도)
        @Query("destination") destination: String, // "x,y" 형태 (경도,위도)
        @Query("waypoints") waypoints: String = "", // 경유지 (필요시 사용)
        @Query("summary") summary: Boolean = false // 경로 요약 정보만 받을지 여부
    ): Response<CarRouteResponse>
}