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

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var adapter: GalleryAdapter

    private val detailActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val updatedUris = result.data?.getParcelableArrayListExtra<Uri>("updated_uris")
                updatedUris?.let {
                    viewModel.setList(it)
                }
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                viewModel.addImage(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = GalleryAdapter(mutableListOf()) { position ->
            val intent = Intent(requireContext(), ImageDetailActivity::class.java).apply {
                putParcelableArrayListExtra("uris", ArrayList(viewModel.imageList.value ?: listOf()))
                putExtra("position", position)
            }
            detailActivityLauncher.launch(intent)
        }

        binding.galleryRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.galleryRecyclerView.adapter = adapter

        viewModel.imageList.observe(viewLifecycleOwner) { list ->
            adapter.updateList(list)
        }

        binding.addImageButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}