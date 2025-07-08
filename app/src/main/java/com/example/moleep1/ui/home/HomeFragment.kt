package com.example.moleep1.ui.home


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.example.moleep1.HomeEdit
import com.example.moleep1.ListViewAdapter
import com.example.moleep1.R
import com.example.moleep1.databinding.FragmentHomeBinding
import com.example.moleep1.list_item
import com.example.moleep1.ui.PrefsManager
import com.example.moleep1.ui.notifications.NotificationsViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ListViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val factory = HomeViewModelFactory(PrefsManager(requireContext()))
        val homeViewModel = ViewModelProvider(requireActivity(), factory).get(HomeViewModel::class.java)

        adapter = ListViewAdapter(requireContext(), ArrayList())//커스텀 어댑터
        binding.listView.adapter = adapter

        homeViewModel.itemList.observe(viewLifecycleOwner) { items ->
            adapter.setItems(ArrayList(items))
            adapter.notifyDataSetChanged() // ListView는 notifyDataSetChanged()가 필요
        }//viewmodel에 저장된 itemlist 변경 관찰(변경시 즉시 fragment 내 view에 반영)


        binding.listView.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = homeViewModel.itemList.value?.get(position)
            if (selectedItem != null) {
                HomeEdit(selectedItem, selectedItem.id)
                    .show(parentFragmentManager, "edit_dialog")
            }
        }//listview 아이템 클릭 시

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
        }//프로필이 비어 있다면 뷰 생성할 때 랜덤프로필 한개 넣어놓고 시작

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
        }//랜덤 프로필 적용(프로필 추가)

        binding.clearAllButton.setOnClickListener {
            homeViewModel.clearAllProfiles()
            val notiviewModel: NotificationsViewModel by activityViewModels()
            notiviewModel.clearAllPlacedImages()
        }//homeviewmodel의 profiles data, nofificationviewmodel의 placedimages data 다 날리기

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}