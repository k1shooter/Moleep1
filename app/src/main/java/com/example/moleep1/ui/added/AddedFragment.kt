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
import com.example.moleep1.ui.home.HomeViewModel
import com.kakao.vectormap.*
import com.kakao.vectormap.label.Label

class AddedFragment : Fragment() {

    private var _binding: FragmentAddedBinding? = null
    private val binding get() = _binding!!

    // --- 주요 객체 선언 ---
    private lateinit var mapView: MapView
    private var mapPinManager: MapPinManager? = null
    private lateinit var locationHandler: LocationHandler
    private lateinit var pathPersonAdapter: PathPersonAdapter

    // --- 상태 관리 변수 ---
    private var isPathModeActive = false // 동선 표시 모드 활성화 상태
    private var isInitialLoad = true     // 지도 초기 로딩 확인

    // --- ViewModel 설정 ---
    // Activity의 생명주기를 따르는 ViewModel을 공유하여 다른 UI 컴포넌트와 데이터를 교환
    private val viewModel: EventViewModel by activityViewModels {
        EventViewModelFactory(EventManager(requireContext()))
    }
    private val homeViewModel: HomeViewModel by activityViewModels()

    // --- Fragment 생명주기 ---
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddedBinding.inflate(inflater, container, false)
        locationHandler = LocationHandler(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = binding.mapView
        mapView.start(mapLifeCycleCallback, mapReadyCallback) // 지도 시작
        setupUIListeners()      // UI 버튼 리스너 설정
        setupPathUI()           // 동선 표시용 UI 설정
        setupViewModelObservers() // ViewModel 데이터 변경 감지 시작
    }

    // --- 초기 설정 함수 ---
    private fun setupPathUI() {
        pathPersonAdapter = PathPersonAdapter { person ->
            // 인물 클릭 시, ViewModel에 경로 탐색을 '요청'만 함
            val events = viewModel.getEventsForAttendee(person.id)
            viewModel.findRoutePathForEvents(events)

            binding.cardViewPersonList.visibility = View.GONE
        }
        binding.rvPathPersonList.adapter = pathPersonAdapter
    }

    private fun setupUIListeners() {
        // 현재 위치 버튼
        binding.fabCurrentLocation.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            locationHandler.requestCurrentLocation(
                onSuccess = { latLng ->
                    binding.progressBar.visibility = View.GONE
                    mapPinManager?.moveCamera(latLng)
                },
                onFailure = { binding.progressBar.visibility = View.GONE }
            )
        }
        // 핀 추가 모드 버튼
        binding.btnAddPinMode.setOnClickListener {
            val isEnteringPinMode = (binding.btnAddPinMode.text == "pin")
            mapPinManager?.setPinAddMode(isEnteringPinMode)
            binding.btnAddPinMode.text = if (isEnteringPinMode) {
                Toast.makeText(requireContext(), "핀을 추가할 위치를 지도에서 선택하세요.", Toast.LENGTH_SHORT).show()
                "Cancel"
            } else {
                "pin"
            }
        }
        // 캡처 버튼
        binding.btnCapture.setOnClickListener {
            MapCaptureHelper().captureMapAndSaveToGallery(requireContext(), mapView) { isSuccess, _ ->
                if (isSuccess) Toast.makeText(context, "캡처 완료.", Toast.LENGTH_SHORT).show()
            }
        }
        // 동선 버튼
        binding.btnPath.setOnClickListener {
            isPathModeActive = !isPathModeActive // 모드 전환
            if (isPathModeActive) {
                binding.btnPath.text = "취소"
                // HomeViewModel의 전체 인물 목록을 가져와 어댑터에 설정
                pathPersonAdapter.submitList(homeViewModel.itemList.value ?: emptyList())
                binding.cardViewPersonList.visibility = View.VISIBLE // 인물 리스트 보이기
            } else {
                binding.btnPath.text = "동선"
                binding.cardViewPersonList.visibility = View.GONE // 인물 리스트 숨기기
                mapPinManager?.clearAllPaths() // 모든 동선 지우기
            }
        }
    }

    private fun setupViewModelObservers() {
        // 1. 전체 리스트 Observer (초기 로딩 시에만 전체 핀을 그림)
        viewModel.eventList.observe(viewLifecycleOwner) { events ->
            if (isInitialLoad && events != null) {
                mapPinManager?.clearAllPins()
                events.forEach { event -> mapPinManager?.addPinFromData(event) }
                isInitialLoad = false
            }
        }
        // 2. 새 핀 추가 Observer (핀 하나만 추가)
        viewModel.newPinAdded.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { newEvent -> mapPinManager?.addPinFromData(newEvent) }
        }
        // 3. 기존 핀 업데이트 Observer (핀 하나만 업데이트)
        viewModel.pinUpdated.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { updatedEvent -> mapPinManager?.updatePinDetails(updatedEvent) }
        }
        // 4. 핀 삭제 Observer (핀 하나만 삭제)
        viewModel.pinDeleted.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { eventId -> mapPinManager?.removePinByEventId(eventId) }
        }

        viewModel.routePath.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { path ->
                mapPinManager?.clearAllPaths() // 기존 동선 지우기
                mapPinManager?.drawPath(path)  // 새 동선 그리기
            }
        }
    }

    // --- 지도 관련 콜백 및 Manager 초기화 ---
    private val mapLifeCycleCallback = object : MapLifeCycleCallback() {
        override fun onMapDestroy() {}
        override fun onMapError(error: Exception) { Log.e("KakaoMap", "onMapError", error) }
    }

    private val mapReadyCallback = object : KakaoMapReadyCallback() {
        override fun onMapReady(kakaoMap: KakaoMap) {
            initializeMapManager(kakaoMap)
            if (mapPinManager?.loadLastLocation() == false) {
                binding.fabCurrentLocation.performClick()
            }
        }
    }

    private fun initializeMapManager(kakaoMap: KakaoMap) {
        mapPinManager = MapPinManager(requireContext(), kakaoMap).apply {
            onMapTappedListener = { latLng ->
                binding.btnAddPinMode.text = "pin"
                showEventDetailSheet(latLng)
            }
            onPinClickListener = { clickedLabel, eventId ->
                showEventDetailSheet(clickedLabel.position, eventId)
            }
        }
    }

    private fun showEventDetailSheet(latLng: LatLng, eventId: String? = null) {
        if (childFragmentManager.findFragmentByTag(EventDetailBottomSheet.TAG) != null) return
        EventDetailBottomSheet.newInstance(latLng, eventId)
            .show(childFragmentManager, EventDetailBottomSheet.TAG)
    }

    // --- Fragment 생명주기 마무리 ---
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