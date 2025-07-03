package com.example.moleep1.ui.added

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.moleep1.databinding.FragmentAddedBinding

class AddedFragment : Fragment() {

    private var _binding: FragmentAddedBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val addedViewModel =
            ViewModelProvider(this).get(AddedViewModel::class.java)

        _binding = FragmentAddedBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textAdded
        addedViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

