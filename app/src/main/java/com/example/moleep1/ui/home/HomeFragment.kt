package com.example.moleep1.ui.home


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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

        // 4. 초기 데이터 추가 (테스트용)
        // 이 로직은 보통 ViewModel의 init 블록에 두는 것이 더 좋습니다.
        if (homeViewModel.itemList.value.isNullOrEmpty()) {
            val uriString = "android.resource://${requireContext().packageName}/${R.drawable.profile}"
            val initialData = listOf(
                list_item("a", "b", uriString),
                list_item("c", "d", uriString)
            )
            homeViewModel.setList(initialData)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}