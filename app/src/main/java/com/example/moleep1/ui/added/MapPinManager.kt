package com.example.moleep1.ui.added

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
import com.example.moleep1.R
import com.example.moleep1.ui.added.event.EventItem
import com.kakao.vectormap.*
import com.kakao.vectormap.animation.Interpolation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.*
import com.kakao.vectormap.route.*
import com.kakao.vectormap.route.animation.*
import java.io.File
import androidx.core.graphics.toColorInt
import com.example.moleep1.ui.added.event.DurationLabelInfo
import kotlin.math.abs


class MapPinManager(private val context: Context, private val kakaoMap: KakaoMap) {

    private val labelToEventIdMap = mutableMapOf<String, String>()
    private val eventToLabelIdMap = mutableMapOf<String, String>()
    private var isPinAddMode = false

    var onMapTappedListener: ((LatLng) -> Unit)? = null
    var onPinClickListener: ((Label, String) -> Unit)? = null

    // RouteLine 관련 객체
    private val routeLineManager: RouteLineManager = kakaoMap.routeLineManager!!
    private val routeLineLayer = routeLineManager.layer
    private val activeRouteLines = mutableMapOf<String, RouteLine>()

    private val pinStyle = kakaoMap.labelManager!!.addLabelStyles(
        LabelStyles.from(LabelStyle.from(R.drawable.pin_icon_128))
    )

    private val pathColors = listOf(
        Color.RED, Color.BLUE, Color.GREEN, Color.CYAN, Color.MAGENTA,
        Color.YELLOW, "#FFA500".toColorInt() // Orange
    )

    private val durationLabels = mutableListOf<Label>()

    init {
        setupMapListeners()
    }

    private fun setupMapListeners() {
        kakaoMap.setOnMapClickListener { _, position, _, _ ->
            if (isPinAddMode) {
                onMapTappedListener?.invoke(position)
            }
        }

        kakaoMap.setOnPoiClickListener { _, _, _, poiId ->
            labelToEventIdMap[poiId]?.let { eventId ->
                kakaoMap.labelManager?.layer?.getLabel(poiId)?.let { label ->
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
        val labelId = eventToLabelIdMap[event.eventId] ?: return
        val label = kakaoMap.labelManager?.layer?.getLabel(labelId) ?: return

        event.photoUri?.let { pathString ->
            val file = File(pathString)
            if (file.exists()) {
                addPhotoBadgeToLabel(label, Uri.fromFile(file))
            }
        } ?: label.removeAllBadge()
    }

    private fun addPhotoBadgeToLabel(label: Label, imageUri: Uri) {
        label.removeAllBadge()
        val sourceBitmap = ImageUtils.uriToBitmap(context, imageUri) ?: return
        val croppedBitmap = ImageUtils.cropToCircle(sourceBitmap)
        val finalBitmap = ImageUtils.resizeBitmap(croppedBitmap, 60, 60)
        val badgeOptions = BadgeOptions.from(finalBitmap).setOffset(0.5f, 0.4f)
        label.addBadge(badgeOptions)[0].show()
    }

    fun removePinByEventId(eventId: String) {
        val labelId = eventToLabelIdMap[eventId] ?: return
        kakaoMap.labelManager?.layer?.getLabel(labelId)?.remove()
        eventToLabelIdMap.remove(eventId)
        labelToEventIdMap.remove(labelId)
    }

    fun clearAllPins() {
        kakaoMap.labelManager?.getLayer()?.removeAll()
        labelToEventIdMap.clear()
        eventToLabelIdMap.clear()
    }

    fun drawPathForPerson(personId: String, path: List<LatLng>) {
        if (path.size < 2) return
        removePathForPerson(personId)

        val colorIndex = abs(personId.hashCode()) % pathColors.size
        val randomColor = pathColors[colorIndex]

        val arrowBitmap = ImageUtils.drawableToBitmap(context, R.drawable.route_pattern_arrow)
        if (arrowBitmap == null) {
            Log.e("MapPinManager", "화살표 패턴 비트맵 생성 실패")
            return
        }

        val pattern = RouteLinePattern.from(arrowBitmap, 60f)
        val style = RouteLineStyle.from(10f, randomColor).setPattern(pattern)
        val stylesSet = RouteLineStylesSet.from("path_style_$personId", RouteLineStyles.from(style))
        val segment = RouteLineSegment.from(path).setStyles(stylesSet.getStyles(0))
        val options = RouteLineOptions.from(segment).setStylesSet(stylesSet)

        routeLineLayer.addRouteLine(options)?.let {
            activeRouteLines[personId] = it
        }
    }

    fun animatePathForPerson(personId: String) {
        activeRouteLines[personId]?.let { line ->
            line.setProgress(-1.0f)

            val animation = ProgressAnimation.from(personId, 1500)
            animation.setInterpolation(Interpolation.Linear)
            animation.progressType = ProgressType.ToShow
            animation.progressDirection = ProgressDirection.StartFirst
            animation.isHideAtStop = false
            animation.isResetToInitialState = false

            val animator = routeLineManager.addAnimator(animation)
            animator.addRouteLines(line)
            animator.start(null)
        }
    }

    fun removePathForPerson(personId: String) {
        activeRouteLines[personId]?.remove()
        activeRouteLines.remove(personId)
    }

    fun clearAllPaths() {
        activeRouteLines.values.forEach { it.remove() }
        activeRouteLines.clear()
        clearAllDurationLabels() // 시간 텍스트도 함께 삭제
    }

    // --- 경로 시간 표시 함수 ---

    fun getActivePathPersonIds(): Set<String> {
        return activeRouteLines.keys
    }

    fun drawDurationLabels(labelsInfo: List<DurationLabelInfo>) {
        clearAllDurationLabels()
        labelsInfo.forEach { info ->
            val textColor = if (info.isAnomalous) Color.RED else Color.WHITE
            val strokeColor = info.pathColor

            val textStyle = LabelTextStyle.from(35, textColor, 2, strokeColor)
            val labelStyle = LabelStyle.from(textStyle)
            val styles = LabelStyles.from(labelStyle)

            val options = LabelOptions.from(info.position)
                .setStyles(styles)
                .setTexts(LabelTextBuilder().setTexts(info.text))

            kakaoMap.labelManager?.layer?.addLabel(options)?.let {
                durationLabels.add(it)
            }
        }
    }

    fun clearAllDurationLabels() {
        durationLabels.forEach { it.remove() }
        durationLabels.clear()
    }

    // --- 카메라 및 위치 저장 함수 ---

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