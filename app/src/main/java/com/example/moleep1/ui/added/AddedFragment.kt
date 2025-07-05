package com.example.moleep1.ui.added

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.moleep1.KAKAO_MAP_KEY
import com.example.moleep1.databinding.FragmentAddedBinding
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.MapView


class AddedFragment : Fragment() {

    private var _binding: FragmentAddedBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.

    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private var kakaoMap: KakaoMap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val addedViewModel =
            ViewModelProvider(this).get(AddedViewModel::class.java)

        _binding = FragmentAddedBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showMapView()
    }

    override fun onPause() {
        super.onPause()
        if (this::mapView.isInitialized) {
            mapView.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (this::mapView.isInitialized) {
            mapView.resume()
        }
    }

    private fun showMapView() {
        mapView = binding.mapView
        KakaoMapSdk.init(requireContext(), KAKAO_MAP_KEY)

        mapView.start(object : MapLifeCycleCallback() {

            override fun onMapDestroy() {
                // 지도 API가 정상적으로 종료될 때 호출
                Log.d("KakaoMap", "onMapDestroy")
            }

            override fun onMapError(p0: Exception?) {
                // 인증 실패 및 지도 사용 중 에러가 발생할 때 호출
                Log.e("KakaoMap", "onMapError")
            }
        }, object : KakaoMapReadyCallback(){
            override fun onMapReady(kakaomap: KakaoMap) {
                // 정상적으로 인증이 완료되었을 때 호출
                kakaoMap = kakaomap
            }
        })
    }

    override fun onDestroyView() {
        if (this::mapView.isInitialized) {
            mapView.finish() // 뷰가 파괴될 때 finish() 호출
        }
        super.onDestroyView()
        _binding = null
    }
}

