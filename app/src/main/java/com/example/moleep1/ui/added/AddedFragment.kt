// AddedFragment.kt

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
import com.kakao.vectormap.label.Label // Label import 추가

class AddedFragment : Fragment() {

    private var _binding: FragmentAddedBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private var mapPinManager: MapPinManager? = null
    private lateinit var locationHandler: LocationHandler

    // ❗ [추가] 사진을 추가할 핀(라벨)을 임시 저장하는 변수
    private var tappedLabelForPhoto: Label? = null

    // ❗ [수정] 이미지 선택 후, MapPinManager에 배지 추가를 요청하는 로직 추가
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && tappedLabelForPhoto != null) {
            mapPinManager?.addPhotoBadgeToLabel(tappedLabelForPhoto!!, uri)
            Toast.makeText(requireContext(), "사진이 추가되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "사진 추가에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
        tappedLabelForPhoto = null // 작업 후 초기화
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
            // ❗ [수정] 초기화 로직을 별도 함수로 분리하여 가독성 향상
            initializeMapManager(kakaoMap)

            if (mapPinManager?.loadLastLocation() == false) {
                binding.fabCurrentLocation.performClick()
            }
        }
    }

    /**
     * [추가] MapPinManager를 생성하고 콜백 리스너를 설정하는 함수
     */
    private fun initializeMapManager(kakaoMap: KakaoMap) {
        mapPinManager = MapPinManager(requireContext(), kakaoMap).apply {
            // 1. 핀이 클릭되었을 때 실행될 동작 정의
            onPinClickListener = { clickedLabel ->
                // ❗ 클릭된 라벨을 저장하고, 이미지 선택기 실행
                tappedLabelForPhoto = clickedLabel
                pickImageLauncher.launch("image/*")
            }
            // 2. 핀이 지도에 새로 추가되었을 때 실행될 동작 정의
            onPinAddedListener = {
                binding.btnAddPinMode.text = "Pin"
                Toast.makeText(requireContext(), "핀이 추가되었습니다.", Toast.LENGTH_SHORT).show()
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