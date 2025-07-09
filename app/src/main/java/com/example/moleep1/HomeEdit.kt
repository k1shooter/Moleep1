package com.example.moleep1

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.example.moleep1.ui.PrefsManager
import com.example.moleep1.ui.home.HomeViewModel
import com.example.moleep1.ui.home.HomeViewModelFactory
import com.example.moleep1.ui.notifications.NotificationsViewModel
import java.io.File

class HomeEdit(
    private val item: list_item,
    private val id : String
) : DialogFragment() {

    // activityViewModels()는 Activity 범위의 ViewModel을 가져오므로 올바른 선택입니다.


    private var selectedImageUri: Uri? = null // 새로 선택된 이미지를 저장할 변수
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.filesDir, fileName)
            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            // FileProvider 사용 권장 (Android 7.0+)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val factory = HomeViewModelFactory(PrefsManager(requireContext()))
        val viewModel = ViewModelProvider(requireActivity(), factory).get(HomeViewModel::class.java)

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
                val originalUri = result.data!!.data!!
                val fileName = "profile_${System.currentTimeMillis()}.jpg"
                val copiedUri = copyUriToInternalStorage(requireContext(), originalUri, fileName)
                if (copiedUri != null) {
                    selectedImageUri = copiedUri
                    imageView.setImageURI(selectedImageUri)
                } else {
                    Toast.makeText(requireContext(), "이미지 복사 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 이미지 클릭 시 갤러리 열기
        imageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            imagePickerLauncher.launch(intent)
        }



        return AlertDialog.Builder(requireContext(), R.style.CustomDialog)
            .setTitle("Profile Edit")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val finalImageUriString = selectedImageUri?.toString() ?: item.imageUri

                item.imageUri = finalImageUriString
                item.desc = descEdit.text.toString()
                item.name = titleEdit.text.toString()
                viewModel.updateItem(id, item)
                val notiviewModel: NotificationsViewModel by activityViewModels()
                notiviewModel.updatePlacedImageBitmapById(id, finalImageUriString, context = requireContext())
            }
            .setNeutralButton("Delete") { _, _ -> // ★★★ 삭제 버튼 추가 ★★★
                viewModel.removeItem(id)
                val notiviewModel: NotificationsViewModel by activityViewModels()
                notiviewModel.removeImagesByProfileId(id)
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}