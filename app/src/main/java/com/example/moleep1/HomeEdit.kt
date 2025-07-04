package com.example.moleep1

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.moleep1.ui.home.HomeViewModel

class HomeEdit(
    private val item: list_item,
    private val position: Int
) : DialogFragment() {

    // activityViewModels()는 Activity 범위의 ViewModel을 가져오므로 올바른 선택입니다.
    private val viewModel: HomeViewModel by activityViewModels()
    private var selectedImageUri: Uri? = null // 새로 선택된 이미지를 저장할 변수
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.home_edit, null)

        val imageView = view.findViewById<ImageView>(R.id.editImage)
        val titleEdit = view.findViewById<EditText>(R.id.editName)
        val descEdit = view.findViewById<EditText>(R.id.editDesc)

        // 기존 데이터로 UI 초기화
        titleEdit.setText(item.name)
        descEdit.setText(item.desc)
        imageView.setImageURI(item.imageUri.toUri())

        // 최신 방식의 ActivityResultLauncher 초기화
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                // 이미지를 선택하면 selectedImageUri에 저장하고 ImageView에 바로 보여줌
                selectedImageUri = result.data!!.data
                imageView.setImageURI(selectedImageUri)
            }
        }

        // 이미지 클릭 시 갤러리 열기
        imageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            imagePickerLauncher.launch(intent)
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Edit item")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                // ★★★ 이 부분 로직 수정 ★★★
                // 새로 선택된 이미지가 있으면 그 URI를, 없으면 기존 URI를 사용
                val finalImageUriString = selectedImageUri?.toString() ?: item.imageUri

                val updatedItem = list_item(
                    titleEdit.text.toString(),
                    descEdit.text.toString(),
                    finalImageUriString // 수정한 URI로 객체 생성
                )
                viewModel.updateItem(position, updatedItem)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}