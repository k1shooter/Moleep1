// EventDetailBottomSheet.kt
package com.example.moleep1.ui.added.event

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import android.content.DialogInterface // ❗ import 추가

import com.example.moleep1.databinding.BottomSheetEventDetailBinding
import com.example.moleep1.ui.added.AddedFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kakao.vectormap.LatLng

class EventDetailBottomSheet : BottomSheetDialogFragment() {

    interface OnDismissListener {
        fun onDismissWithoutSaving(tempLabelId: String)
    }

    private var _binding: BottomSheetEventDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventViewModel by activityViewModels {
        EventViewModelFactory(EventManager(requireContext()))
    }

    private var dismissListener: OnDismissListener? = null
    private var isSaved = false // 저장 버튼 클릭 여부 추적

    private var eventId: String? = null
    private var tempLabelId: String? = null // 새 핀일 경우 임시 ID
    private var pinLatLng: LatLng? = null // 핀의 좌표

    fun setOnDismissListener(listener: OnDismissListener) {
        this.dismissListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            eventId = it.getString(ARG_EVENT_ID) // 기존 핀
            tempLabelId = it.getString(ARG_TEMP_LABEL_ID) // 새 핀
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
        loadData()
        setupListeners()
    }

    private fun loadData() {
        // ❗ [수정] eventId로 데이터를 찾아 UI에 표시
        eventId?.let { id ->
            viewModel.findEventById(id)?.let { event ->
                binding.etEventName.setText(event.eventName)
                binding.etEventDescription.setText(event.description)
            }
        }
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            val eventName = binding.etEventName.text.toString()
            val description = binding.etEventDescription.text.toString()

            if (eventName.isBlank() || pinLatLng == null) return@setOnClickListener

            isSaved = true

            val savedEvent = viewModel.addOrUpdateEvent(eventId, eventName, description, pinLatLng!!)


            if (eventId == null && tempLabelId != null) {
                (parentFragment as? AddedFragment)?.linkNewPin(tempLabelId!!, savedEvent.eventId)
            }
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!isSaved && eventId == null && tempLabelId != null) {
            dismissListener?.onDismissWithoutSaving(tempLabelId!!)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "EventDetailBottomSheet"
        // ❗ [수정] Argument 키 변경
        private const val ARG_EVENT_ID = "event_id"
        private const val ARG_TEMP_LABEL_ID = "temp_label_id"
        private const val ARG_LAT = "latitude"
        private const val ARG_LNG = "longitude"

        // ❗ [수정] newInstance 함수 재정의
        fun newInstanceForNewPin(labelId: String, latLng: LatLng): EventDetailBottomSheet {
            return EventDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_TEMP_LABEL_ID, labelId)
                    putDouble(ARG_LAT, latLng.latitude)
                    putDouble(ARG_LNG, latLng.longitude)
                }
            }
        }
        fun newInstanceForExistingPin(eventId: String, latLng: LatLng): EventDetailBottomSheet {
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