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
import com.kakao.vectormap.label.Label

class AddedFragment : Fragment() {

    private var _binding: FragmentAddedBinding? = null
    private val binding get() = _binding!!

    // 카카오맵 관련 주요 객체들
    private lateinit var mapView: MapView
    private var mapPinManager: MapPinManager? = null
    private lateinit var locationHandler: LocationHandler

    // Activity의 생명주기를 따르는 ViewModel을 공유하여 BottomSheet와 데이터를 교환
    private val viewModel: EventViewModel by activityViewModels {
        EventViewModelFactory(EventManager(requireContext()))
    }

    // 지도에 핀을 처음 로드하는지 여부를 확인하는 플래그
    private var isInitialLoad = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddedBinding.inflate(inflater, container, false)
        locationHandler = LocationHandler(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = binding.mapView
        // MapLifeCycleCallback과 KakaoMapReadyCallback을 전달하여 MapView 시작
        mapView.start(mapLifeCycleCallback, mapReadyCallback)
        setupUIListeners()
    }

    // 지도 외의 UI 요소들에 대한 이벤트 리스너 설정
    private fun setupUIListeners() {
        // 현재 위치로 이동하는 FAB(플로팅 액션 버튼) 리스너
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

        // 핀 추가 모드를 토글하는 버튼 리스너
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

        // 지도 화면을 캡처하는 버튼 리스너
        binding.btnCapture.setOnClickListener {
            val captureHelper = MapCaptureHelper()
            captureHelper.captureMapAndSaveToGallery(requireContext(), mapView) { isSuccess, uri ->
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

    // 지도의 생명주기(소멸, 에러) 관련 콜백
    private val mapLifeCycleCallback = object : MapLifeCycleCallback() {
        override fun onMapDestroy() { Log.d("KakaoMap", "onMapDestroy") }
        override fun onMapError(error: Exception) { Log.e("KakaoMap", "onMapError: ${error.message}") }
    }

    // 지도 로딩이 완료되었을 때 호출되는 콜백
    private val mapReadyCallback = object : KakaoMapReadyCallback() {
        override fun onMapReady(kakaoMap: KakaoMap) {
            Log.d("KakaoMap", "Map is ready")
            initializeMapManager(kakaoMap)
            setupViewModelObservers() // ViewModel 데이터 변경 감지 시작

            // 저장된 마지막 위치가 없으면 현재 위치로 이동
            if (mapPinManager?.loadLastLocation() == false) {
                binding.fabCurrentLocation.performClick()
            }
        }
    }

    // ViewModel의 LiveData를 관찰하여 UI를 업데이트하는 로직
    private fun setupViewModelObservers() {
        // 1. 전체 리스트 Observer: 초기 실행 시 한 번만 모든 핀을 그림 (성능 최적화)
        viewModel.eventList.observe(viewLifecycleOwner) { events ->
            if (isInitialLoad) {
                mapPinManager?.clearAllPins()
                events.forEach { event ->
                    mapPinManager?.addPinFromData(event)
                }
                isInitialLoad = false // 초기 로드 완료 후 플래그 변경
            }
        }

        // 2. 새 핀 추가 Observer: '추가' 이벤트가 발생했을 때만 핀 하나를 지도에 추가
        viewModel.newPinAdded.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { newEvent ->
                mapPinManager?.addPinFromData(newEvent)
            }
        }

        // 3. 기존 핀 업데이트 Observer: '수정' 이벤트가 발생했을 때만 핀 하나의 정보를 업데이트
        viewModel.pinUpdated.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { updatedEvent ->
                mapPinManager?.updatePinDetails(updatedEvent)
            }
        }
        // ❗ [추가] 4. 핀 삭제 Observer -> ID에 해당하는 핀 하나만 지도에서 제거
        viewModel.pinDeleted.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { eventId ->
                mapPinManager?.removePinByEventId(eventId)
            }
        }
    }

    // MapPinManager를 초기화하고 리스너를 설정
    private fun initializeMapManager(kakaoMap: KakaoMap) {
        mapPinManager = MapPinManager(requireContext(), kakaoMap).apply {
            // 지도를 탭했을 때 (새 핀 추가)
            onMapTappedListener = { latLng ->
                binding.btnAddPinMode.text = "Pin"
                showEventDetailSheet(latLng) // eventId 없이 호출
            }
            // 기존 핀을 클릭했을 때
            onPinClickListener = { clickedLabel, eventId ->
                showEventDetailSheet(clickedLabel.position, eventId) // eventId와 함께 호출
            }
        }
    }

    // 상세 정보 BottomSheet를 띄우는 함수
    private fun showEventDetailSheet(latLng: LatLng, eventId: String? = null) {
        // 중복 실행 방지
        if (childFragmentManager.findFragmentByTag(EventDetailBottomSheet.TAG) != null) return

        val bottomSheet = EventDetailBottomSheet.newInstance(latLng, eventId)
        bottomSheet.show(childFragmentManager, EventDetailBottomSheet.TAG)
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