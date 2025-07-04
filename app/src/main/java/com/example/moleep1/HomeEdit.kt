package com.example.moleep1

import android.app.Activity
import android.app.ComponentCaller
import android.app.Dialog
import android.content.Intent
import android.icu.lang.UProperty.DEPRECATED
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.moleep1.ui.home.HomeViewModel
import androidx.core.net.toUri

class HomeEdit(
    private val item: list_item,
    private val position : Int
) : DialogFragment() {

    private val viewModel: HomeViewModel by activityViewModels()
    private var selectedImageUri: Uri?=null
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog{
        val view=LayoutInflater.from(requireContext()).inflate(R.layout.home_edit, null)

        val imageView = view.findViewById<ImageView>(R.id.editImage)
        val titleEdit = view.findViewById<EditText>(R.id.editName)
        val descEdit = view.findViewById<EditText>(R.id.editDesc)

        titleEdit.setText(item.name)
        descEdit.setText(item.desc)
        imageView.setImageURI(item.imageUri.toUri())

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                selectedImageUri = result.data!!.data
                imageView.setImageURI(selectedImageUri)
            }
        }
        imageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            imagePickerLauncher.launch(intent)
        }

        return AlertDialog.Builder(requireContext()).setTitle("Edit item").setView(view).setPositiveButton("Save") { _, _ ->
            val updatedItem = list_item(
                titleEdit.text.toString(),
                descEdit.text.toString(),
                item.imageUri
            )
            viewModel.updateItem(position, updatedItem)
        }
            .setNegativeButton("Cancel", null)
            .create()
    }



//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (requestCode == 101 && resultCode == Activity.RESULT_OK && data != null) {
//            selectedImageUri = data.data
//            view?.findViewById<ImageView>(R.id.editImage)?.setImageURI(selectedImageUri)
//        }
//    }
}