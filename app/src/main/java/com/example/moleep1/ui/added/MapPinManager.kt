package com.example.moleep1.ui.added

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
import com.example.moleep1.R
import com.example.moleep1.ui.added.event.*
import com.kakao.vectormap.*
import com.kakao.vectormap.animation.Interpolation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.*
import com.kakao.vectormap.route.*
import com.kakao.vectormap.route.animation.ProgressAnimation
import com.kakao.vectormap.route.animation.ProgressDirection
import com.kakao.vectormap.route.animation.ProgressType
import com.kakao.vectormap.shape.MapPoints
import com.kakao.vectormap.shape.Polyline
import com.kakao.vectormap.shape.PolylineOptions
import java.io.File


class MapPinManager(private val context: Context, private val kakaoMap: KakaoMap) {

    private val labelToEventIdMap = mutableMapOf<String, String>()
    private val eventToLabelIdMap = mutableMapOf<String, String>()
    private var isPinAddMode = false

    // ❗ [수정] onPinClickListener가 Label과 String(eventId) 두 파라미터를 받도록 타입을 변경합니다.
    var onMapTappedListener: ((LatLng) -> Unit)? = null
    var onPinClickListener: ((Label, String) -> Unit)? = null

    private val polylines = mutableListOf<Polyline>()

    private val routeLineManager: RouteLineManager = kakaoMap.routeLineManager!!
    private val routeLineLayer = routeLineManager.layer
    private var currentRouteLine: RouteLine? = null

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
            eventToLabelIdMap[event.eventId] = newLabel.labelId

            event.photoUri?.let { pathString ->
                val file = File(pathString)
                if (file.exists()) {
                    addPhotoBadgeToLabel(newLabel, Uri.fromFile(file))
                }
            }
        }
    }

    fun updatePinDetails(event: EventItem) {
        // eventId를 사용해 현재 지도에 있는 labelId를 찾음
        val labelId = eventToLabelIdMap[event.eventId] ?: return
        val label = kakaoMap.labelManager?.layer?.getLabel(labelId) ?: return

        Log.d("MapPinManager", "${event.eventName} 핀의 배지 업데이트...")
        // 사진 URI가 있으면 배지를 업데이트, 없으면 모든 배지를 제거
        event.photoUri?.let { pathString ->
            val file = File(pathString)
            if (file.exists()) {
                addPhotoBadgeToLabel(label, Uri.fromFile(file))
            }
        } ?: label.removeAllBadge()
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

    fun removePinByEventId(eventId: String) {
        val labelId = eventToLabelIdMap[eventId] ?: return

        // 1. 지도에서 Label 객체 제거
        kakaoMap.labelManager?.layer?.getLabel(labelId)?.remove()

        // 2. 두 개의 맵에서 ID 정보 제거
        eventToLabelIdMap.remove(eventId)
        labelToEventIdMap.remove(labelId)

        Log.d("MapPinManager", "핀 완전 삭제 완료: $eventId")
    }

    fun clearAllPins() {
        kakaoMap.labelManager?.getLayer()?.removeAll()
        labelToEventIdMap.clear()
        eventToLabelIdMap.clear()
    }

    fun drawPathForEvents(events: List<EventItem>) {
        if (events.size < 2) return

        val latLngList = events.map { LatLng.from(it.latitude, it.longitude) }
        val mapPoints = MapPoints.fromLatLng(latLngList)

        // ❗ [수정] from() 함수에 두께와 색상을 파라미터로 직접 전달합니다.
        val options = PolylineOptions.from(mapPoints, 10f, Color.MAGENTA)

        kakaoMap.shapeManager?.layer?.addPolyline(options)?.let {
            polylines.add(it)
        }
    }

    fun drawPath(path: List<LatLng>) {
        if (path.isEmpty()) return

        val mapPoints = MapPoints.fromLatLng(path)
        val options = PolylineOptions.from(mapPoints, 10f, Color.MAGENTA)

        kakaoMap.shapeManager?.layer?.addPolyline(options)?.let {
            polylines.add(it)
        }
    }

    fun drawRouteLine(path: List<LatLng>) {
        if (path.isEmpty()) return
        clearAllPaths()

        val arrowBitmap = ImageUtils.drawableToBitmap(context, R.drawable.route_pattern_arrow)
        if (arrowBitmap == null) {
            Log.e("MapPinManager", "화살표 패턴 비트맵 생성 실패")
            return
        }

        val pattern = RouteLinePattern.from(arrowBitmap, 60f)
        val style = RouteLineStyle.from(10f, Color.RED).setPattern(pattern) // 두께 10, 빨간색
        val stylesSet = RouteLineStylesSet.from("path_style", RouteLineStyles.from(style))
        val segment = RouteLineSegment.from(path).setStyles(stylesSet.getStyles(0))
        val options = RouteLineOptions.from(segment).setStylesSet(stylesSet)
        currentRouteLine = routeLineLayer.addRouteLine(options)
    }

    fun animatePath() {
        currentRouteLine?.let { line ->
            // 1. 애니메이션 설정값 생성
            val animation = ProgressAnimation.from("path_anim", 1500) // 1.5초
            animation.setInterpolation(Interpolation.CubicInOut) // 선형 보간
            animation.progressType = ProgressType.ToShow // 라인이 나타나도록 설정
            animation.progressDirection = ProgressDirection.StartFirst // 시작점부터 애니메이션
            animation.isHideAtStop = false // 애니메이션이 멈췄을 때 라인을 숨기지 않음 (계속 보임)
            animation.isResetToInitialState = false // 멈췄을 때 처음 상태로 돌아가지 않음

            // 2. RouteLineAnimator 생성
            val animator = routeLineManager.addAnimator(animation)

            // 3. RouteLine을 추가하고 애니메이션 시작
            animator.addRouteLines(line)
            animator.start(null) // 콜백이 필요 없으면 null 전달
        }
    }

    fun clearAllPaths() {
        currentRouteLine?.remove()
        currentRouteLine = null
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