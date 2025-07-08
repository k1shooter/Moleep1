package com.example.moleep1.data.network

import com.example.moleep1.BuildConfig
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface KakaoNaviApiService {
    @POST("v1/origins/directions")
    suspend fun getDirections(
        @Header("Authorization") key: String = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}",
        @Header("Content-Type") contentType: String = "application/json",
        @Body body: DirectionsRequest
    ): Response<DirectionsResponse>
}