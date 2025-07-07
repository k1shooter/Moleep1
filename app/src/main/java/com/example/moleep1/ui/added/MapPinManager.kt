package com.example.moleep1.ui.added

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.moleep1.R
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.BadgeOptions
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

class MapPinManager(private val context: Context, private val kakaoMap: KakaoMap) {

    // ❗ [수정] Label의 ID와 객체를 함께 저장하기 위해 Map으로 변경
    private val pins = mutableMapOf<String, Label>()
    private var isPinAddMode = false

    var onPinClickListener: ((Label) -> Unit)? = null
    var onPinAddedListener: ((LatLng) -> Unit)? = null

    private val pinStyle = kakaoMap.labelManager!!.addLabelStyles(
        LabelStyles.from(LabelStyle.from(R.drawable.pin_icon_128))
    )

    init {
        setupMapListeners()
    }

    private fun setupMapListeners() {
        kakaoMap.setOnMapClickListener { _, position, _, _ ->
            if (isPinAddMode) {
                addPin(position)
                isPinAddMode = false
                onPinAddedListener?.invoke(position)
            }
        }

        kakaoMap.setOnPoiClickListener { _, _, _, poiId ->
            // ❗ [수정] 저장해둔 pins Map에서 Label 객체를 직접 찾음
            val clickedLabel = pins[poiId]
            if (clickedLabel != null) {
                onPinClickListener?.invoke(clickedLabel)
            }
        }
    }

    fun setPinAddMode(isEnabled: Boolean) {
        isPinAddMode = isEnabled
    }

    private fun addPin(position: LatLng) {
        val manager = kakaoMap.labelManager ?: return
        val layer = manager.getLayer()
        val options = LabelOptions.from(position)
            .setStyles(pinStyle)
            .setRank(1)

        layer?.addLabel(options)?.let { label ->
            // ❗ [수정] ID와 Label 객체를 pins Map에 저장
            pins[label.labelId] = label
            Log.d("MapPinManager", "✅ 아이콘 핀 추가 성공! ID: ${label.labelId}")
        } ?: Log.e("MapPinManager", "❌ 아이콘 핀 추가 실패!")
    }

    fun addPhotoBadgeToLabel(label: Label, imageUri: Uri) {
        val sourceBitmap = ImageUtils.uriToBitmap(context, imageUri) ?: return
        val croppedBitmap = ImageUtils.cropToCircle(sourceBitmap)
        val finalBitmap = ImageUtils.resizeBitmap(croppedBitmap, 60, 60)

        // ❗ [수정] setOffset을 추가하여 배지 위치를 핀 중앙으로 조정
        val badgeOptions = BadgeOptions.from(finalBitmap)
            .setOffset(0.5f, 0.4f) // x축 중앙(0.5), y축 살짝 위(0.4)

        val badgeList = label.addBadge(badgeOptions)
        badgeList[0].show()
    }

    fun moveCamera(position: LatLng, zoomLevel: Int = 15) {
        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(position, zoomLevel))
    }

    fun saveLastLocation() {
        val sharedPref = context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE)
            ?: return
        kakaoMap.cameraPosition?.let {
            with(sharedPref.edit()) {
                putFloat("last_lat", it.position.latitude.toFloat())
                putFloat("last_lng", it.position.longitude.toFloat())
                putFloat("last_zoom", it.zoomLevel.toFloat())
                apply()
            }
        }
    }

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