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
import androidx.lifecycle.ViewModelProvider
import com.example.moleep1.HomeEdit
import com.example.moleep1.ListViewAdapter
import com.example.moleep1.R
import com.example.moleep1.databinding.FragmentHomeBinding
import com.example.moleep1.list_item

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val dataList=ArrayList<list_item>()


    lateinit var adapter: ListViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val listView= binding.listView
        adapter = ListViewAdapter(requireContext(), dataList)
        listView.setAdapter(adapter)

        homeViewModel.itemList.observe(viewLifecycleOwner) { list ->
            adapter.updateData(ArrayList(list)) // 직접 리스트를 바꿔주는 메서드를 어댑터에 만들어야 함
        }

        val uriString = "android.resource://${requireContext().packageName}/${R.drawable.profile}"
        if (homeViewModel.itemList.value.isNullOrEmpty()) {
            homeViewModel.addItem(list_item("ttt", "desc", uriString))
        }

        val dataList = mutableListOf(
            list_item("a", "b", uriString),
            list_item("c", "d", uriString)
        )

        homeViewModel.setList(ArrayList(dataList))

        homeViewModel.itemList.observe(viewLifecycleOwner) {
            adapter.setItems(ArrayList(it))
            adapter.notifyDataSetChanged()
        }

        listView.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = homeViewModel.itemList.value?.get(position)
            if (selectedItem != null) {
                HomeEdit(selectedItem, position)
                    .show(parentFragmentManager, "edit_dialog")
            }
        }

        return root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

