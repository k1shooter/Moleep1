package com.example.moleep1

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 앱이 시작될 때 여기서 딱 한 번만 SDK 초기화
        KakaoMapSdk.init(this, KAKAO_MAP_KEY)
    }
}