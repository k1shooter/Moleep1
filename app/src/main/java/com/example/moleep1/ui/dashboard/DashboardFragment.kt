package com.example.moleep1.ui.dashboard

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.moleep1.R
import com.example.moleep1.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: GalleryAdapter
    // ✅ 모든 이미지 경로를 Uri 타입으로 관리할 리스트
    private val imageList = mutableListOf<Uri>()

    // 갤러리에서 이미지를 선택하고 그 결과(Uri)를 받아오는 런처
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                // 갤러리에서 이미지를 성공적으로 선택하면 리스트에 추가하고, 어댑터에 알림
                adapter.addImage(it)
            }
        }

    private val detailActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // ImageDetailActivity에서 보낸 업데이트된 Uri 리스트를 받음
                val updatedUris = result.data?.getParcelableArrayListExtra<Uri>("updated_uris")
                updatedUris?.let {
                    // 기존 리스트를 지우고 새 리스트로 교체
                    imageList.clear()
                    imageList.addAll(it)
                    // 어댑터에 전체 데이터가 변경되었음을 알림
                    adapter.notifyDataSetChanged()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        // ⭐️ 중요: 처음에 보여줄 기본 이미지들을 Uri로 변환하여 리스트에 미리 추가
        if (imageList.isEmpty()) { // 중복 추가 방지
            addInitialDrawablesAsUris()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ 2. 어댑터 생성 시 클릭 이벤트 처리 로직을 전달
        adapter = GalleryAdapter(imageList) { position ->
            // 아이템 클릭 시 ImageDetailActivity 실행
            val intent = Intent(requireContext(), ImageDetailActivity::class.java).apply {
                putParcelableArrayListExtra("uris", ArrayList(imageList))
                putExtra("position", position)
            }
            detailActivityLauncher.launch(intent)
        }

        binding.galleryRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.galleryRecyclerView.adapter = adapter

        binding.addImageButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    /**
     * ⭐️ 기존 Drawable 리소스 ID들을 Uri로 변환하여 imageList에 추가하는 함수
     */
    private fun addInitialDrawablesAsUris() {
        val initialDrawableIds = listOf(
            R.drawable.pic4, R.drawable.pic5, R.drawable.pic6
        )

        initialDrawableIds.forEach { resId ->
            // 리소스 ID로부터 Uri를 생성하는 방법
            val uri = "android.resource://${requireContext().packageName}/$resId".toUri()
            imageList.add(uri)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}