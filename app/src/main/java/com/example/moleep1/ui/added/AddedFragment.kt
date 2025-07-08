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

    private lateinit var mapView: MapView
    private var mapPinManager: MapPinManager? = null
    private lateinit var locationHandler: LocationHandler
    private lateinit var pathPersonAdapter: PathPersonAdapter
    private var isPathModeActive = false

    private val viewModel: EventViewModel by activityViewModels {
        EventViewModelFactory(EventManager(requireContext()))
    }
    private val homeViewModel: HomeViewModel by activityViewModels()

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
        setupPathUI()
    }

    private fun setupPathUI() {
        pathPersonAdapter = PathPersonAdapter { person ->
            val events = viewModel.getEventsForAttendee(person.id)
            if (events.size < 2) {
                Toast.makeText(requireContext(), "경로를 만들기에 사건 수가 부족합니다.", Toast.LENGTH_SHORT).show()
                return@PathPersonAdapter
            }
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

    // ViewModel의 LiveData를 관찰하여 UI를 업데이트하는 로직
    private fun setupViewModelObservers() {
        // ❗ [수정] "데이터 준비 완료" 신호를 받으면, 단 한 번만 전체 핀을 그립니다.
        viewModel.isDataReady.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { isReady ->
                if (isReady) {
                    val events = viewModel.eventList.value
                    Log.d("InitialLoad", "데이터 준비 완료, ${events?.size ?: 0}개의 핀을 그립니다.")
                    mapPinManager?.clearAllPins()
                    events?.forEach { mapPinManager?.addPinFromData(it) }
                }
            }
        }

        // --- 개별 업데이트/추가/삭제 Observer는 그대로 유지 ---
        viewModel.newPinAdded.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { newEvent ->
                mapPinManager?.addPinFromData(newEvent)
            }
        }
        viewModel.pinUpdated.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { updatedEvent ->
                mapPinManager?.updatePinDetails(updatedEvent)
            }
        }
        viewModel.pinDeleted.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { eventId ->
                mapPinManager?.removePinByEventId(eventId)
            }
        }
        viewModel.routePath.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { path ->
                mapPinManager?.clearAllPaths()
                mapPinManager?.drawPath(path)
            }
        }
    }

    // 지도 관련 콜백
    private val mapLifeCycleCallback = object : MapLifeCycleCallback() {
        override fun onMapDestroy() {}
        override fun onMapError(error: Exception) { Log.e("KakaoMap", "onMapError", error) }
    }

    private val mapReadyCallback = object : KakaoMapReadyCallback() {
        override fun onMapReady(kakaoMap: KakaoMap) {
            initializeMapManager(kakaoMap)
            // ❗ [수정] 지도가 준비된 후에 Observer 설정을 호출합니다.
            setupViewModelObservers()

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