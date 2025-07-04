package com.example.moleep1.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.moleep1.R
import com.example.moleep1.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val imageList = listOf(
        R.drawable.pic1, R.drawable.pic2, R.drawable.pic3,
        R.drawable.pic1, R.drawable.pic2, R.drawable.pic3,
        R.drawable.pic1, R.drawable.pic2, R.drawable.pic3,
        R.drawable.pic1, R.drawable.pic2, R.drawable.pic3,
        R.drawable.pic1, R.drawable.pic2, R.drawable.pic3,
        R.drawable.pic1, R.drawable.pic2, R.drawable.pic3,
        R.drawable.pic1, R.drawable.pic2, R.drawable.pic3,
        R.drawable.pic1, R.drawable.pic2, R.drawable.pic3,
        R.drawable.pic1, R.drawable.pic2, R.drawable.pic3,
        R.drawable.pic1, R.drawable.pic2, R.drawable.pic3,
        R.drawable.pic1, R.drawable.pic2, R.drawable.pic3,
        R.drawable.pic1, R.drawable.pic2, R.drawable.pic3
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = binding.galleryRecyclerView
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView.adapter = GalleryAdapter(imageList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
