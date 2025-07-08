package com.example.moleep1.ui.added

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.moleep1.R
import com.example.moleep1.ui.added.event.*
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.*
import java.io.File

class MapPinManager(private val context: Context, private val kakaoMap: KakaoMap) {

    private val labelToEventIdMap = mutableMapOf<String, String>()
    private var isPinAddMode = false

    // ❗ [수정] onPinClickListener가 Label과 String(eventId) 두 파라미터를 받도록 타입을 변경합니다.
    var onMapTappedListener: ((LatLng) -> Unit)? = null
    var onPinClickListener: ((Label, String) -> Unit)? = null

    private val pinStyle = kakaoMap.labelManager!!.addLabelStyles(
        LabelStyles.from(LabelStyle.from(R.drawable.pin_icon_128))
    )

    init {
        setupMapListeners()
    }

    private fun setupMapListeners() {
        kakaoMap.setOnMapClickListener { _, position, _, _ ->
            if (isPinAddMode) {
                onMapTappedListener?.invoke(position)
                isPinAddMode = false
            }
        }

        kakaoMap.setOnPoiClickListener { _, _, _, poiId ->
            labelToEventIdMap[poiId]?.let { eventId ->
                kakaoMap.labelManager?.layer?.getLabel(poiId)?.let { label ->
                    // 변경된 타입에 맞게 Label 객체와 eventId를 함께 전달합니다.
                    onPinClickListener?.invoke(label, eventId)
                }
            }
        }
    }

    fun setPinAddMode(isEnabled: Boolean) {
        isPinAddMode = isEnabled
    }

    fun addPinFromData(event: EventItem) {
        val manager = kakaoMap.labelManager ?: return
        val position = LatLng.from(event.latitude, event.longitude)
        val options = LabelOptions.from(position)
            .setStyles(pinStyle).setRank(1)

        manager.getLayer()?.addLabel(options)?.let { newLabel ->
            labelToEventIdMap[newLabel.labelId] = event.eventId

            // ❗ [수정] 저장된 URI 문자열을 파싱하여 배지 추가
            event.photoUri?.let { pathString ->
                val file = File(pathString)
                if (file.exists()) {
                    addPhotoBadgeToLabel(newLabel, Uri.fromFile(file))
                }
            }
        }
    }

    fun addPhotoBadgeToLabel(label: Label, imageUri: Uri) {
        label.removeAllBadge()
        val sourceBitmap = ImageUtils.uriToBitmap(context, imageUri) ?: return
        val croppedBitmap = ImageUtils.cropToCircle(sourceBitmap)
        val finalBitmap = ImageUtils.resizeBitmap(croppedBitmap, 60, 60)

        val badgeOptions = BadgeOptions.from(finalBitmap)
            .setOffset(0.5f, 0.4f)

        label.addBadge(badgeOptions)[0]?.show()
    }

    fun clearAllPins() {
        kakaoMap.labelManager?.getLayer()?.removeAll()
        labelToEventIdMap.clear()
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