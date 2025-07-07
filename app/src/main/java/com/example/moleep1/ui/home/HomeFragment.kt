package com.example.moleep1.ui.home


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.moleep1.HomeEdit
import com.example.moleep1.ListViewAdapter
import com.example.moleep1.R
import com.example.moleep1.databinding.FragmentHomeBinding
import com.example.moleep1.list_item
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ViewModel은 프래그먼트 생명주기 내내 유지되므로 여기서 초기화
    private val homeViewModel: HomeViewModel by activityViewModels()
    private lateinit var adapter: ListViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 1. 어댑터 설정
        adapter = ListViewAdapter(requireContext(), ArrayList()) // 처음엔 빈 리스트로 시작
        binding.listView.adapter = adapter

        // 2. ViewModel의 데이터 변경 관찰 (★ observe는 한번만!)
        homeViewModel.itemList.observe(viewLifecycleOwner) { items ->
            // 어댑터에 데이터 업데이트 (Adapter 내부에 데이터 교체 메소드가 있다고 가정)
            // ex) adapter.updateData(items) 또는 adapter.submitList(items)
            adapter.setItems(ArrayList(items)) // setItems가 데이터를 교체하는 메소드라고 가정
            adapter.notifyDataSetChanged() // ListView는 notifyDataSetChanged()가 필요
        }

        // 3. 아이템 클릭 리스너 설정
        binding.listView.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = homeViewModel.itemList.value?.get(position)
            if (selectedItem != null) {
                // HomeEdit 다이얼로그 호출
                HomeEdit(selectedItem, position)
                    .show(parentFragmentManager, "edit_dialog")
            }
        }

// 1. 21개 이미지 리소스 ID 리스트
        val profileImages = listOf(
            R.drawable.pf1, R.drawable.pf2, R.drawable.pf3, R.drawable.pf4, R.drawable.pf5,
            R.drawable.pf6, R.drawable.pf7, R.drawable.pf8, R.drawable.pf9, R.drawable.pf10,
            R.drawable.pf11, R.drawable.pf12, R.drawable.pf13, R.drawable.pf14, R.drawable.pf15,
            R.drawable.pf16, R.drawable.pf17, R.drawable.pf18, R.drawable.pf19, R.drawable.pf20,
            R.drawable.pf21
        )

        val firstNames = listOf("현우", "예린", "도윤", "하은", "윤호", "지아", "두팔", "철용", "갑산", "석대", "용대", "빛나라", "숙", "철구", "기영", "철민", "병관", "요셉", "다윗", "종민", "다니엘", "마르코")
        val lastNames = listOf("김", "이", "박", "최", "정", "강", "조", "윤", "임", "한", "민", "독고", "남궁", "백")
        val descriptions1 = listOf(
            "운동을", "독서를", "여행을", "커피를",
            "음악 듣기를", "강아지를", "요리를", "사진 찍기를",
            "영화를", "게임을", "과제를", "클럽을"
        )
        val descriptions2 = listOf(
            "좋아함.", "즐김.", "자주 감.", "잘 던짐.", "키움.", "잘함.",
            "자주 봄.", "싫어함.", "무서워함.", "포기함."
        )

// 2. 초기 데이터 세팅 (앱 첫 실행 시)
        if (homeViewModel.itemList.value.isNullOrEmpty()) {
            val randomFirstName = firstNames.random()
            val randomLastName = lastNames.random()
            val randomDesc1 =descriptions1.random()
            val randomDesc2 =descriptions2.random()
            val randomDesc = "$randomDesc1 $randomDesc2"
            val randomResId = profileImages.random()
            val randomName = "$randomLastName$randomFirstName"

            val uriString = "android.resource://${requireContext().packageName}/$randomResId"
            val initialData = listOf(
                list_item(randomName, randomDesc, uriString)
            )
            homeViewModel.setList(initialData)
        }

// 3. 프로필 추가 버튼 클릭 시 랜덤 이미지 적용
        binding.addprofilebutton.setOnClickListener {
            val randomFirstName = firstNames.random()
            val randomLastName = lastNames.random()
            val randomDesc1 =descriptions1.random()
            val randomDesc2 =descriptions2.random()
            val randomDesc = "$randomDesc1 $randomDesc2"
            val randomResId = profileImages.random()
            val randomName = "$randomLastName$randomFirstName"
            val uriString = "android.resource://${requireContext().packageName}/$randomResId"
            homeViewModel.addItem(list_item(randomName, randomDesc, uriString))
            binding.listView.smoothScrollToPosition(adapter.count - 1)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}