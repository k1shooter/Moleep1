// AddedFragment.kt

package com.example.moleep1.ui.added

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.moleep1.databinding.FragmentAddedBinding
import com.example.moleep1.ui.added.event.*
import com.kakao.vectormap.*
import com.kakao.vectormap.label.Label // Label import 추가

class AddedFragment : Fragment() {

    private var _binding: FragmentAddedBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private var mapPinManager: MapPinManager? = null
    private lateinit var locationHandler: LocationHandler
    private val viewModel: EventViewModel by activityViewModels {
        EventViewModelFactory(EventManager(requireContext()))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddedBinding.inflate(inflater, container, false)
        locationHandler = LocationHandler(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = binding.mapView
        mapView.start(mapLifeCycleCallback, mapReadyCallback)
        setupUIListeners()
    }

    private fun setupUIListeners() {
        binding.fabCurrentLocation.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            locationHandler.requestCurrentLocation(
                onSuccess = { latLng ->
                    binding.progressBar.visibility = View.GONE
                    mapPinManager?.moveCamera(latLng)
                },
                onFailure = {
                    binding.progressBar.visibility = View.GONE
                }
            )
        }

        binding.btnAddPinMode.setOnClickListener {
            val isEnteringPinMode = (binding.btnAddPinMode.text == "Pin")
            mapPinManager?.setPinAddMode(isEnteringPinMode)
            if (isEnteringPinMode) {
                Toast.makeText(requireContext(), "핀을 추가할 위치를 지도에서 선택하세요.", Toast.LENGTH_SHORT).show()
                binding.btnAddPinMode.text = "Cancel"
            } else {
                binding.btnAddPinMode.text = "Pin"
            }
        }
        binding.btnCapture.setOnClickListener {
            // MapCaptureHelper 인스턴스 생성
            val captureHelper = MapCaptureHelper()
            // 캡처 함수 호출
            captureHelper.captureMapAndSaveToGallery(requireContext(), mapView) { isSuccess, uri ->
                // 캡처 완료 후 결과에 따라 Toast 메시지 표시
                // MapCaptureHelper 내부에서도 Toast를 보여주므로 이 부분은 생략하거나 로그로 대체해도 됩니다.
                activity?.runOnUiThread {
                    if (isSuccess) {
                        Log.d("AddedFragment", "캡처 성공. 이미지 Uri: $uri")
                        Toast.makeText(context, "캡처 완료.", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("AddedFragment", "캡처 실패.")
                    }
                }
            }
        }
    }

    private val mapLifeCycleCallback = object : MapLifeCycleCallback() {
        override fun onMapDestroy() { Log.d("KakaoMap", "onMapDestroy") }
        override fun onMapError(error: Exception) { Log.e("KakaoMap", "onMapError: ${error.message}") }
    }

    private val mapReadyCallback = object : KakaoMapReadyCallback() {
        override fun onMapReady(kakaoMap: KakaoMap) {
            Log.d("KakaoMap", "Map is ready")
            initializeMapManager(kakaoMap)
            setupViewModelObservers() // ❗ [추가] ViewModel 관찰자 설정

            if (mapPinManager?.loadLastLocation() == false) {
                binding.fabCurrentLocation.performClick()
            }
        }
    }

    private fun setupViewModelObservers() {
        viewModel.eventList.observe(viewLifecycleOwner) { events ->
            mapPinManager?.clearAllPins() // 기존 핀 모두 제거
            events.forEach { event ->
                mapPinManager?.addPinFromData(event) // 저장된 데이터로 핀 추가
            }
        }
    }

    private fun initializeMapManager(kakaoMap: KakaoMap) {
        mapPinManager = MapPinManager(requireContext(), kakaoMap).apply {
            // ❗ [수정] 클릭 리스너: label과 함께 영구 eventId를 받음
            onPinClickListener = { clickedLabel, eventId ->
                showEventDetailSheetForExisting(eventId, clickedLabel.position)
            }
            // ❗ [수정] 추가 리스너: 새로 생성된 Label 객체를 받음
            onPinAddedListener = { newLabel ->
                binding.btnAddPinMode.text = "Pin"
                Toast.makeText(requireContext(), "핀이 추가되었습니다.", Toast.LENGTH_SHORT).show()
                showEventDetailSheetForNew(newLabel)
            }
        }
    }

    private fun showEventDetailSheetForNew(label: Label) {
        if (childFragmentManager.findFragmentByTag(EventDetailBottomSheet.TAG) != null) return
        val bottomSheet = EventDetailBottomSheet.newInstanceForNewPin(label.labelId, label.position)

        // ❗ [수정] 리스너를 설정할 때, 익명 객체로 즉석에서 구현하고 할당합니다.
        bottomSheet.setOnDismissListener(object : EventDetailBottomSheet.OnDismissListener {
            override fun onDismissWithoutSaving(tempLabelId: String) {
                // 콜백 로직이 사용되는 곳에 바로 있어 이해하기 쉬움
                mapPinManager?.removePinById(tempLabelId)
            }
        })

        bottomSheet.show(childFragmentManager, EventDetailBottomSheet.TAG)
    }

    private fun showEventDetailSheetForExisting(eventId: String, latLng: LatLng) {
        if (childFragmentManager.findFragmentByTag(EventDetailBottomSheet.TAG) != null) return
        val bottomSheet = EventDetailBottomSheet.newInstanceForExistingPin(eventId, latLng)
        bottomSheet.show(childFragmentManager, EventDetailBottomSheet.TAG)
    }

    fun linkNewPin(labelId: String, eventId: String) {
        mapPinManager?.linkEventToLabel(labelId, eventId)
    }

    override fun onPause() {
        super.onPause()
        mapPinManager?.saveLastLocation()
        mapView.pause()
    }

    override fun onResume() {
        super.onResume()
        mapView.resume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}