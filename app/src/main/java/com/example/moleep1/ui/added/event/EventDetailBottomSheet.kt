// EventDetailBottomSheet.kt
package com.example.moleep1.ui.added.event

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager

import com.example.moleep1.databinding.BottomSheetEventDetailBinding
import com.example.moleep1.ui.added.AddedFragment
import com.example.moleep1.ui.added.ImageUtils
import com.example.moleep1.ui.added.PersonAdapter
import com.example.moleep1.ui.home.HomeViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.kakao.vectormap.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EventDetailBottomSheet : BottomSheetDialogFragment() {


    private var _binding: BottomSheetEventDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventViewModel by activityViewModels {
        EventViewModelFactory(EventManager(requireContext()))
    }

    private var eventId: String? = null
    private var pinLatLng: LatLng? = null // 핀의 좌표

    private var tempSelectedUri: Uri? = null
    private var currentPhotoPath: String? = null

    private var selectedTime: Long? = null

    private val homeViewModel: HomeViewModel by activityViewModels() // HomeViewModel 가져오기
    private lateinit var personAdapter: PersonAdapter
    private val selectedAttendeeIds = mutableSetOf<String>()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            tempSelectedUri = it
            binding.ivEventPhoto.setImageURI(it)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            eventId = it.getString(ARG_EVENT_ID)
            pinLatLng = LatLng.from(it.getDouble(ARG_LAT), it.getDouble(ARG_LNG))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadData()
        setupListeners()
    }

    private fun setupRecyclerView() {
        personAdapter = PersonAdapter { personId, isSelected ->
            if (isSelected) {
                selectedAttendeeIds.add(personId)
            } else {
                selectedAttendeeIds.remove(personId)
            }
        }
        binding.rvPersonList.apply {
            adapter = personAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadData() {
        binding.btnDelete.visibility = if (eventId != null) View.VISIBLE else View.GONE
        eventId?.let { id ->
            viewModel.findEventById(id)?.let { event ->
                binding.etEventName.setText(event.eventName)
                binding.etEventDescription.setText(event.description)

                selectedTime = event.eventTime
                binding.tvEventTime.text = formatTimestamp(selectedTime) ?: "시간 설정"

                selectedAttendeeIds.clear()
                event.attendeeIds?.let { ids -> // null이 아닐 때만 이 블록이 실행됨
                    selectedAttendeeIds.addAll(ids)
                }

                currentPhotoPath = event.photoUri
                currentPhotoPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        binding.ivEventPhoto.setImageURI(Uri.fromFile(file))
                    }
                }
            }
        }
        homeViewModel.itemList.observe(viewLifecycleOwner) { allPeople ->
            if (allPeople != null) { // allPeople도 null일 수 있으므로 확인
                personAdapter.submitList(allPeople, selectedAttendeeIds)
            }
        }
    }

    private fun setupListeners() {
        binding.ivEventPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.tvEventTime.setOnClickListener {
            showTimePicker()
        }
        binding.btnSave.setOnClickListener {
            val eventName = binding.etEventName.text.toString()
            val description = binding.etEventDescription.text.toString()

            if (eventName.isBlank()) {
                Toast.makeText(requireContext(), "사건 이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pinLatLng == null) return@setOnClickListener

            lifecycleScope.launch {
                // 1. 새로 선택한 이미지가 있는지 확인
                val newPath = tempSelectedUri?.let { uri ->
                    // 백그라운드에서 파일 복사
                    withContext(Dispatchers.IO) {
                        ImageUtils.copyImageToInternalStorage(requireContext(), uri)
                    }
                }

                // 2. ViewModel 업데이트 (새 사진이 있으면 새 경로, 없으면 기존 경로 사용)
                viewModel.addOrUpdateEvent(
                    eventId,
                    eventName,
                    binding.etEventDescription.text.toString(),
                    pinLatLng!!,
                    newPath ?: currentPhotoPath,
                    selectedTime,
                    selectedAttendeeIds.toList()
                )

                dismiss()
            }

        }
        binding.btnDelete.setOnClickListener {
            // eventId가 있는 기존 이벤트만 삭제 가능
            eventId?.let { id ->
                showDeleteConfirmationDialog(id)
            }
        }
    }

    private fun showDeleteConfirmationDialog(eventId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("사건 삭제")
            .setMessage("이 사건에 대한 모든 정보를 정말로 삭제하시겠습니까?")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ ->
                // '삭제'를 누르면 ViewModel에 삭제 요청
                viewModel.deleteEvent(eventId)
                dismiss() // BottomSheet 닫기
            }
            .show()
    }

    private fun showTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(12)
            .setMinute(0)
            .setTitleText("시간 선택")
            .build()

        picker.addOnPositiveButtonClickListener {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, picker.hour)
            calendar.set(Calendar.MINUTE, picker.minute)

            // "오후 5:30" 형식으로 포맷
            val format = SimpleDateFormat("a h:mm", Locale.KOREA)
            selectedTime = calendar.timeInMillis
            binding.tvEventTime.text = formatTimestamp(selectedTime)
        }
        picker.show(childFragmentManager, "time_picker")
    }

    private fun formatTimestamp(timestamp: Long?): String? {
        if (timestamp == null) return null
        val format = SimpleDateFormat("a h:mm", Locale.KOREA)
        return format.format(Date(timestamp))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "EventDetailBottomSheet"
        private const val ARG_EVENT_ID = "event_id"

        private const val ARG_LAT = "latitude"
        private const val ARG_LNG = "longitude"

        fun newInstance(latLng: LatLng, eventId: String? = null): EventDetailBottomSheet {
            return EventDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_EVENT_ID, eventId)
                    putDouble(ARG_LAT, latLng.latitude)
                    putDouble(ARG_LNG, latLng.longitude)
                }
            }
        }
    }
}