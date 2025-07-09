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
import com.example.moleep1.R

class AddedFragment : Fragment() {

    private var _binding: FragmentAddedBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private var mapPinManager: MapPinManager? = null
    private lateinit var locationHandler: LocationHandler
    private lateinit var pathPersonAdapter: PathPersonAdapter
    private var isPathModeActive = false
    private var isPinAddModeActive = false

    private val viewModel: EventViewModel by activityViewModels {
        EventViewModelFactory(EventManager(requireContext()))
    }
    private val homeViewModel: HomeViewModel by activityViewModels()

    private val selectedPeopleForPath = mutableSetOf<String>()

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
        pathPersonAdapter = PathPersonAdapter { personId, isSelected ->
            if (isSelected) {
                selectedPeopleForPath.add(personId)
                val events = viewModel.getEventsForAttendee(personId)
                if (events.size < 2) {
                    Toast.makeText(requireContext(), "경로를 만들기에 사건 수가 부족합니다.", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.findRoutePathForEvents(events, personId)
                }
            } else {
                selectedPeopleForPath.remove(personId)
                mapPinManager?.removePathForPerson(personId)
            }
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
                    mapPinManager?.moveCamera(latLng, 15)
                },
                onFailure = { binding.progressBar.visibility = View.GONE }
            )
        }
        // 핀 추가 모드 버튼
        binding.btnAddPinMode.setOnClickListener {
            // 현재 상태의 반대 상태로 변경하고 UI를 업데이트합니다.
            updatePinAddModeUI(!isPinAddModeActive)

            if (isPinAddModeActive) {
                Toast.makeText(requireContext(), "핀을 추가할 위치를 지도에서 선택하세요.", Toast.LENGTH_SHORT).show()
            }
        }
        // 캡처 버튼
        binding.btnCapture.setOnClickListener {
            MapCaptureHelper().captureMapAndSaveToGallery(requireContext(), mapView) { isSuccess, _ ->
                if (isSuccess) Toast.makeText(context, "캡처 완료.", Toast.LENGTH_SHORT).show()
            }
        }
        // 동선 버튼
        binding.btnPath.setOnClickListener { togglePathMode() }
        // 시간 계산 버튼
        binding.btnCalculateTime.setOnClickListener {
            val activeIds = mapPinManager?.getActivePathPersonIds()
            if (activeIds.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "먼저 동선을 표시해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.calculateDurationsForActivePaths(activeIds)
            }
        }
    }

    // ViewModel의 LiveData를 관찰하여 UI를 업데이트하는 로직
    private fun setupViewModelObservers() {
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
            event.getContentIfNotHandled()?.let { (personId, path) ->
                mapPinManager?.drawPathForPerson(personId, path)
                mapPinManager?.animatePathForPerson(personId)
            }
        }
        viewModel.durationLabels.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { labelsInfo ->
                mapPinManager?.drawDurationLabels(labelsInfo)
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
            setupViewModelObservers()
            drawInitialPins()
            if (mapPinManager?.loadLastLocation() == false) {
                binding.fabCurrentLocation.performClick()
            }
        }
    }

    private fun drawInitialPins() {
        val currentEvents = viewModel.eventList.value
        if (!currentEvents.isNullOrEmpty()) {
            Log.d("InitialDraw", "초기 핀 ${currentEvents.size}개를 그립니다.")
            mapPinManager?.clearAllPins()
            currentEvents.forEach { event ->
                mapPinManager?.addPinFromData(event)
            }
        }
    }

    private fun initializeMapManager(kakaoMap: KakaoMap) {
        mapPinManager = MapPinManager(requireContext(), kakaoMap).apply {
            onMapTappedListener = { latLng ->
                updatePinAddModeUI(false)
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

    private fun updatePinAddModeUI(isActive: Boolean) {
        isPinAddModeActive = isActive
        mapPinManager?.setPinAddMode(isActive)

        if (isActive) {
            // 활성화 상태: 'X' 아이콘
            binding.btnAddPinMode.setIconResource(android.R.drawable.ic_menu_close_clear_cancel)
        } else {
            // 비활성화 상태: 원래 '핀' 아이콘
            binding.btnAddPinMode.setIconResource(R.drawable.outline_add_location_24) // 실제 사용하는 핀 아이콘으로 변경하세요.
        }
    }

    private fun togglePathMode() {
        isPathModeActive = !isPathModeActive
        if (isPathModeActive) {
            binding.btnPath.setIconResource(android.R.drawable.ic_menu_close_clear_cancel)
            pathPersonAdapter.submitList(homeViewModel.itemList.value ?: emptyList(), selectedPeopleForPath)
            binding.cardViewPersonList.visibility = View.VISIBLE
            binding.btnCalculateTime.visibility = View.VISIBLE
        } else {
            binding.btnPath.setIconResource(R.drawable.route)
            binding.cardViewPersonList.visibility = View.GONE
            mapPinManager?.clearAllPaths() // 모든 동선 지우기
            selectedPeopleForPath.clear() // 선택 상태 초기화
            binding.btnCalculateTime.visibility = View.GONE
        }
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