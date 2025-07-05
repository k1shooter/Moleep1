package com.example.moleep1.ui.added

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.moleep1.databinding.FragmentAddedBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory

class AddedFragment : Fragment() {

    private var _binding: FragmentAddedBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private var kakaoMap: KakaoMap? = null

    // ✅ 위치 정보 클라이언트
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ✅ 권한 요청 런처
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {
            // 권한이 허용되면 현위치 탐색
            fetchCurrentLocation(true)
        } else {
            Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddedBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = binding.mapView
        mapView.start(mapLifeCycleCallback, mapReadyCallback)

        binding.fabCurrentLocation.setOnClickListener {
            fetchCurrentLocation(true)
        }
    }

    private val mapLifeCycleCallback = object : MapLifeCycleCallback() {
        override fun onMapDestroy() { Log.d("KakaoMap", "onMapDestroy") }
        override fun onMapError(error: Exception) { Log.e("KakaoMap", "onMapError: ${error.message}") }
    }

    private val mapReadyCallback = object : KakaoMapReadyCallback() {
        override fun onMapReady(map: KakaoMap) {
            kakaoMap = map
            Log.d("KakaoMap", "Map is ready")
            // ✅ 마지막 위치 복원 시도
            if (!loadLastLocation()) {
                // 저장된 위치가 없으면 현위치 탐색
                fetchCurrentLocation(true)
            }
        }
    }

    // ✅ 현위치 탐색 함수
    private fun fetchCurrentLocation(moveToCamera: Boolean) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            binding.progressBar.visibility = View.GONE
            location?.let {
                if (moveToCamera) {
                    val currentLatLng = LatLng.from(it.latitude, it.longitude)
                    kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(currentLatLng, 15))
                }
            } ?: Toast.makeText(requireContext(), "위치를 찾을 수 없습니다. GPS를 확인해주세요.", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "위치 탐색에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ 마지막 위치 저장 함수
    private fun saveLastLocation() {
        // kakaoMap이 null이 아니면 let 블록 실행
        kakaoMap?.let { map ->
            val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return

            // ★★★ 이 부분 수정 ★★★
            // map.cameraPosition이 null이 아닐 때만 let 블록 실행
            map.cameraPosition?.let { cameraPosition ->
                with(sharedPref.edit()) {
                    putFloat("last_lat", cameraPosition.position.latitude.toFloat())
                    putFloat("last_lng", cameraPosition.position.longitude.toFloat())
                    putFloat("last_zoom", cameraPosition.zoomLevel.toFloat())
                    apply()
                }
            }
        }
    }

    // ✅ 마지막 위치 복원 함수
    private fun loadLastLocation(): Boolean {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return false
        if (!sharedPref.contains("last_lat")) return false

        val lat = sharedPref.getFloat("last_lat", 0f).toDouble()
        val lng = sharedPref.getFloat("last_lng", 0f).toDouble()
        val zoom = sharedPref.getFloat("last_zoom", 15f)

        val lastPosition = LatLng.from(lat, lng)
        kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(lastPosition, zoom.toInt()))
        return true
    }

    override fun onPause() {
        super.onPause()
        saveLastLocation() // ✅ 화면을 벗어날 때 마지막 위치 저장
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