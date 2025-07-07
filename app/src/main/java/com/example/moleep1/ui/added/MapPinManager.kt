package com.example.moleep1.ui.added

import android.content.Context
import android.util.Log
import com.example.moleep1.R
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder

class MapPinManager(private val context: Context, private val kakaoMap: KakaoMap) {

    private val pinLabelIds = mutableSetOf<String>()
    private var isPinAddMode = false // 핀 추가 모드 상태 관리

    // 핀 클릭 시 동작을 외부(Fragment)에서 설정하기 위한 리스너
    var onPinClickListener: (() -> Unit)? = null
    // 핀이 추가된 직후 외부(Fragment)에 알리기 위한 리스너
    var onPinAddedListener: ((LatLng) -> Unit)? = null

    init {
        setupMapListeners()
    }

    private fun setupMapListeners() {
        // [수정] 지도 클릭 리스너를 Manager 내부에서 설정
        kakaoMap.setOnMapClickListener { _, position, _, _ ->
            // 핀 추가 모드일 때만 핀 추가 로직 실행
            if (isPinAddMode) {
                addPin(position)
                // 핀 추가 후, 모드 자동 종료 및 Fragment에 알림
                isPinAddMode = false
                onPinAddedListener?.invoke(position)
            }
        }

        // 기존 POI(핀) 클릭 리스너
        kakaoMap.setOnPoiClickListener { _, _, _, poiId ->
            if (pinLabelIds.contains(poiId)) {
                onPinClickListener?.invoke()
            }
        }
    }

    /**
     * 외부에서 핀 추가 모드를 설정/해제하기 위한 함수
     */
    fun setPinAddMode(isEnabled: Boolean) {
        isPinAddMode = isEnabled
    }

    /**
     * 지도에 핀(Label)을 추가합니다.
     */
    private fun addPin(position: LatLng) {
        // ❗ 디버깅을 위해 함수 내용을 임시로 변경합니다.
        val manager = kakaoMap.labelManager ?: return
        val layer = manager.getLayer()

        // ❗ 아이콘 없이 오직 파란색 텍스트만 있는 스타일로 강제 변경
        val styles = manager.addLabelStyles(LabelStyles.from(
            LabelStyle.from()
                .setTextStyles(35, android.graphics.Color.BLUE, 2, android.graphics.Color.WHITE)
        ))

        // ❗ "테스트" 라는 글자를 라벨로 표시
        val options = LabelOptions.from(position)
            .setStyles(styles)
            .setTexts(LabelTextBuilder().setTexts("test"))
            .setRank(1) // 다른 객체에 가려지지 않도록 Rank를 1로 설정

        val newLabel = layer?.addLabel(options)

        // 라벨이 실제로 추가되었는지 로그로 확인
        if (newLabel != null) {
            Log.d("MapPinManager", "✅ 테스트 라벨 추가 성공! ID: ${newLabel.labelId}")
            pinLabelIds.add(newLabel.labelId)
        } else {
            Log.e("MapPinManager", "❌ 테스트 라벨 추가 실패!")
        }
    }


    /**
     * 지정된 위치와 줌 레벨로 카메라를 이동합니다.
     */
    fun moveCamera(position: LatLng, zoomLevel: Int = 15) {
        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(position, zoomLevel))
    }

    /**
     * 마지막 카메라 위치를 SharedPreferences에 저장합니다.
     */
    fun saveLastLocation() {
        val sharedPref = context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE) ?: return
        kakaoMap.cameraPosition?.let {
            with(sharedPref.edit()) {
                putFloat("last_lat", it.position.latitude.toFloat())
                putFloat("last_lng", it.position.longitude.toFloat())
                putFloat("last_zoom", it.zoomLevel.toFloat())
                apply()
            }
        }
    }

    /**
     * SharedPreferences에서 마지막 위치를 불러와 카메라를 이동합니다.
     * @return 위치 정보가 있었으면 true, 없었으면 false
     */
    fun loadLastLocation(): Boolean {
        val sharedPref = context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE)
        if (!sharedPref.contains("last_lat")) return false

        val lat = sharedPref.getFloat("last_lat", 0f).toDouble()
        val lng = sharedPref.getFloat("last_lng", 0f).toDouble()
        val zoom = sharedPref.getFloat("last_zoom", 15f).toInt()

        moveCamera(LatLng.from(lat, lng), zoom)
        return true
    }
}