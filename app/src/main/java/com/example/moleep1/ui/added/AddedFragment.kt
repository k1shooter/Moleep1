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
            // ❗ [수정] 새 리스너 설정: 지도를 탭하면 BottomSheet를 연다.
            onMapTappedListener = { latLng ->
                binding.btnAddPinMode.text = "Pin"
                showEventDetailSheet(latLng) // eventId 없이 호출 (새 핀)
            }

            onPinClickListener = { clickedLabel, eventId ->
                showEventDetailSheet(clickedLabel.position, eventId) // eventId와 함께 호출 (기존 핀)
            }
        }
    }

    private fun showEventDetailSheet(latLng: LatLng, eventId: String? = null) {
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