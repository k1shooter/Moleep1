package com.example.moleep1.ui.added

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.moleep1.databinding.FragmentAddedBinding
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView

class AddedFragment : Fragment() {

    private var _binding: FragmentAddedBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private var mapPinManager: MapPinManager? = null
    private lateinit var locationHandler: LocationHandler

    // Fragment의 핀 추가 모드 상태 변수 제거

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            Toast.makeText(requireContext(), "사진이 선택되었습니다.", Toast.LENGTH_SHORT).show()
        }
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
        // 현재 위치 버튼 클릭
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

        // [수정] 핀 추가 모드 버튼 클릭
        binding.btnAddPinMode.setOnClickListener {
            // 현재 버튼 텍스트를 기준으로 모드 토글
            val isEnteringPinMode = (binding.btnAddPinMode.text == "Pin")

            // MapPinManager에 모드 변경 알림
            mapPinManager?.setPinAddMode(isEnteringPinMode)

            if (isEnteringPinMode) {
                Toast.makeText(requireContext(), "핀을 추가할 위치를 지도에서 선택하세요.", Toast.LENGTH_SHORT).show()
                binding.btnAddPinMode.text = "Cancel"
            } else {
                binding.btnAddPinMode.text = "Pin"
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

            // [수정] MapPinManager 초기화 및 콜백 리스너 설정
            mapPinManager = MapPinManager(requireContext(), kakaoMap).apply {
                // 1. 핀이 클릭되었을 때 실행될 동작 정의
                onPinClickListener = {
                    pickImageLauncher.launch("image/*")
                }
                // 2. 핀이 지도에 새로 추가되었을 때 실행될 동작 정의
                onPinAddedListener = {
                    binding.btnAddPinMode.text = "Pin"
                    Toast.makeText(requireContext(), "핀이 추가되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            // [삭제] Fragment에 있던 지도 클릭 리스너 로직 제거
            // kakaoMap.setOnMapClickListener { ... }

            // 마지막 위치 불러오기 시도, 실패하면 현재 위치 요청
            if (mapPinManager?.loadLastLocation() == false) {
                binding.fabCurrentLocation.performClick()
            }
        }
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