package com.example.moleep1.ui.added

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.LatLng

class LocationHandler(private val fragment: Fragment) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(fragment.requireActivity())

    // 권한 요청 결과 처리
    private val locationPermissionRequest = fragment.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {
            // 권한이 허용되면 콜백 실행
            onPermissionGranted?.invoke()
        } else {
            Toast.makeText(fragment.requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 권한 획득 시 실행할 동작을 저장하는 변수
    private var onPermissionGranted: (() -> Unit)? = null

    /**
     * 현재 위치를 요청합니다. 권한이 없으면 요청하고, 있으면 즉시 위치를 가져옵니다.
     * @param onSuccess 위치 정보를 성공적으로 가져왔을 때 LatLng과 함께 호출됩니다.
     * @param onFailure 실패했을 때 호출됩니다.
     */
    fun requestCurrentLocation(onSuccess: (LatLng) -> Unit, onFailure: () -> Unit) {
        // 권한이 있는지 확인
        if (ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // 권한이 없으면, 권한 획득 후 위치를 가져오도록 콜백을 설정하고 권한을 요청
            onPermissionGranted = { fetchLastLocation(onSuccess, onFailure) }
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            // 권한이 이미 있으면 바로 위치 가져오기
            fetchLastLocation(onSuccess, onFailure)
        }
    }

    private fun fetchLastLocation(onSuccess: (LatLng) -> Unit, onFailure: () -> Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    onSuccess(LatLng.from(it.latitude, it.longitude))
                } ?: run {
                    Toast.makeText(fragment.requireContext(), "위치를 찾을 수 없습니다. GPS를 확인해주세요.", Toast.LENGTH_SHORT).show()
                    onFailure()
                }
            }.addOnFailureListener {
                Toast.makeText(fragment.requireContext(), "위치 탐색에 실패했습니다.", Toast.LENGTH_SHORT).show()
                onFailure()
            }
        } catch (e: SecurityException) {
            Toast.makeText(fragment.requireContext(), "위치 권한이 없어 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            onFailure()
        }
    }
}