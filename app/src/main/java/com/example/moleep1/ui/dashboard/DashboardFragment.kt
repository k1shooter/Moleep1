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
import androidx.fragment.app.viewModels

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // ✅ ViewModel 인스턴스 생성. 이제 데이터는 ViewModel이 관리합니다.
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var adapter: GalleryAdapter

    // ... ActivityResultLauncher들은 그대로 ...
    private val detailActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val updatedUris = result.data?.getParcelableArrayListExtra<Uri>("updated_uris")
                updatedUris?.let {
                    // ✅ ViewModel의 데이터를 업데이트
                    viewModel.setList(it)
                }
            }
        }

    // ... pickImageLauncher도 ViewModel을 사용하도록 수정
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                // ✅ ViewModel의 데이터를 업데이트
                viewModel.addImage(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        // 초기 데이터 추가 로직은 ViewModel로 옮기는 것이 더 좋지만, 우선 이렇게 유지
        if (viewModel.imageList.value.isNullOrEmpty()) {
            addInitialDrawablesAsUris()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ 어댑터는 ViewModel의 데이터를 관찰하여 업데이트
        //    처음엔 빈 리스트로 어댑터를 생성
        adapter = GalleryAdapter(mutableListOf()) { position ->
            val intent = Intent(requireContext(), ImageDetailActivity::class.java).apply {
                // ✅ ViewModel의 현재 데이터로 Intent를 구성
                putParcelableArrayListExtra("uris", ArrayList(viewModel.imageList.value ?: listOf()))
                putExtra("position", position)
            }
            detailActivityLauncher.launch(intent)
        }

        binding.galleryRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.galleryRecyclerView.adapter = adapter

        // ✅ LiveData 관찰 설정
        // ViewModel의 imageList가 변경될 때마다 이 코드가 실행되어 화면을 갱신
        viewModel.imageList.observe(viewLifecycleOwner) { list ->
            // 어댑터에 새 데이터를 전달하고, 뷰를 갱신
            adapter.updateList(list) // 어댑터에 데이터 업데이트용 함수를 만들어야 함
        }

        binding.addImageButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun addInitialDrawablesAsUris() {
        val initialDrawableIds = listOf(R.drawable.pic4, R.drawable.pic5, R.drawable.pic6)
        val uriList = mutableListOf<Uri>()
        initialDrawableIds.forEach { resId ->
            val uri = "android.resource://${requireContext().packageName}/$resId".toUri()
            uriList.add(uri)
        }
        // ✅ ViewModel의 데이터를 업데이트
        viewModel.setList(uriList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}