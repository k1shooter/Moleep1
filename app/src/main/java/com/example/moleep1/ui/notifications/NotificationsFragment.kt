package com.example.moleep1.ui.notifications

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.example.moleep1.ListViewAdapter
import com.example.moleep1.databinding.FragmentNotificationsBinding
import com.example.moleep1.R
import com.example.moleep1.ui.DrawingView
import com.example.moleep1.ui.home.HomeViewModel
import android.net.Uri
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.moleep1.ui.PrefsManager
import com.example.moleep1.ui.home.HomeViewModelFactory
import android.os.Build
import androidx.annotation.RequiresApi

class NotificationsFragment : Fragment() {

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null // 새로 선택된 이미지를 저장할 변수

    private var _binding: FragmentNotificationsBinding? = null


    private val viewModel: NotificationsViewModel by activityViewModels()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val drawerLayout=view.findViewById<DrawerLayout>(R.id.drawerLayout)
        val btnOpenSidebar=view.findViewById<ImageButton>(R.id.btnOpenSidebar)
        val btnClear=view.findViewById<ImageButton>(R.id.btnClear)
        val btnAddText=view.findViewById<ImageButton>(R.id.btnAddText)
        val listView=view.findViewById<ListView>(R.id.listView)
        val drawingView=view.findViewById<DrawingView>(R.id.drawingView)

        val factory = HomeViewModelFactory(PrefsManager(requireContext()))
        val homeViewModel = ViewModelProvider(requireActivity(), factory).get(HomeViewModel::class.java)

        val adapter = ListViewAdapter(requireContext(), ArrayList(homeViewModel.itemList.value?: emptyList()))
        listView.adapter=adapter

        homeViewModel.itemList.observe(viewLifecycleOwner) { items ->
            // 어댑터에 데이터 업데이트 (Adapter 내부에 데이터 교체 메소드가 있다고 가정)
            // ex) adapter.updateData(items) 또는 adapter.submitList(items)
            adapter.setItems(ArrayList(items)) // setItems가 데이터를 교체하는 메소드라고 가정
            adapter.notifyDataSetChanged() // ListView는 notifyDataSetChanged()가 필요
        }


        val btnSave = view.findViewById<ImageButton>(R.id.btnSave)
        val btnGallery =view.findViewById<ImageButton>(R.id.btnAddGallery)

        btnSave.setOnClickListener {
            // 1. 권한 체크


            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    1002
                )
                return@setOnClickListener
            }

            // 3. 권한이 있을 때만 저장 코드 실행
            val bitmap = drawingView.getBitmapFromView(drawingView)
            val success = drawingView.saveBitmapToGallery(requireContext(), bitmap, "drawing_${System.currentTimeMillis()}")
            if (success) {
                Toast.makeText(requireContext(), "갤러리에 저장 완료!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "저장 실패", Toast.LENGTH_SHORT).show()
            }
        }
        btnOpenSidebar.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        btnClear.setOnClickListener {
            drawingView.clearCanvas()
            viewModel.clearCanvas()
        }

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                // 이미지를 선택하면 selectedImageUri에 저장하고 ImageView에 바로 보여줌
                selectedImageUri = result.data!!.data
                viewModel.setPendingGallery(selectedImageUri.toString())
                viewModel.setIsPlacingGallery(true)
            }
        }

        btnGallery.setOnClickListener {
            // 최신 방식의 ActivityResultLauncher 초기화


            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            imagePickerLauncher.launch(intent)


        }

        viewModel.isPlacingGallery.observe(viewLifecycleOwner) { placing ->
            drawingView.setPlacingGallery(placing)
        }
        viewModel.pendingGallery.observe(viewLifecycleOwner) { uri ->
            drawingView.addGalleryImage(uri)
            drawingView.setPendingGallery(uri)
            if(uri!=null) {
                viewModel.pendingGallery.value = null
            }
        }

        btnAddText.setOnClickListener {
            val editText= EditText(requireContext())
            AlertDialog.Builder(requireContext())
                .setTitle("텍스트 추가")
                .setView(editText)
                .setPositiveButton("추가") { _, _ ->
                    drawingView.pendingText = editText.text.toString()
                    drawingView.isPlacingText = true
                }
                .setNegativeButton("취소", null)
                .show()
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val item = adapter.getItem(position)
            if (item != null) {
                homeViewModel.selectItem(item)
                drawerLayout.closeDrawer(GravityCompat.END)
            }
        }
        homeViewModel.selectedItem.observe(viewLifecycleOwner) { item ->
            if (item != null) {
                drawingView.addProfileImage(item.imageUri, item.id)
                homeViewModel.selectedItem.value = null
            } else {
                drawingView.addProfileImage(null, "")
            }
        }





        viewModel.isPlacingImage.observe(viewLifecycleOwner) { placing ->
            drawingView.setPlacingImage(placing)
        }
        viewModel.pendingImageUri.observe(viewLifecycleOwner) { uri ->
            drawingView.setPendingImageUri(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val binding = _binding!!
        //val root=inflater.inflate(R.layout.fragment_notifications, container, false)
        val drawingView=binding.drawingView
        val factory = HomeViewModelFactory(PrefsManager(requireContext()))
        val homeViewModel = ViewModelProvider(requireActivity(), factory).get(HomeViewModel::class.java)

        drawingView.onImageSelectedListener = { img ->
            val profile = homeViewModel.itemList.value?.find { it.id == img.id }

            val dialogview= LayoutInflater.from(requireContext()).inflate(R.layout.image_selected_layout,null,false)
            val tvProfileInfo=dialogview.findViewById<TextView>(R.id.tvProfileInfo)
            val seekBarSize=dialogview.findViewById<SeekBar>(R.id.seekBarSize)

            tvProfileInfo.text="이름: ${profile?.name}\n특이사항: ${profile?.desc}"
            seekBarSize.progress=img.width

            seekBarSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val newSize = progress.coerceAtLeast(50) // 최소 크기 제한
                    img.width = newSize
                    img.height = newSize
                    drawingView.invalidate()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }

            })

            AlertDialog.Builder(requireContext(), R.style.CustomDialog)
                .setTitle("프로필 정보")
                .setView(dialogview)
                .setPositiveButton("삭제") { _, _ ->
                    drawingView.deleteSelectedImage()
                }
                .setNegativeButton("닫기", null)
                .show()
        }//이미지 눌렀을때 띄워버리기

        drawingView.onGallerySelectedListener = { img ->

            val dialogview= LayoutInflater.from(requireContext()).inflate(R.layout.gallery_selected_layout,null,false)
            val seekBarSizew=dialogview.findViewById<SeekBar>(R.id.seekBarSizew)
            val seekBarSizeh=dialogview.findViewById<SeekBar>(R.id.seekBarSizeh)

            seekBarSizew.progress=img.width
            seekBarSizeh.progress=img.height

            seekBarSizew.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val newSize = progress.coerceAtLeast(50) // 최소 크기 제한
                    img.width = newSize
                    drawingView.invalidate()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }

            })

            seekBarSizeh.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val newSize = progress.coerceAtLeast(50) // 최소 크기 제한
                    img.height = newSize
                    drawingView.invalidate()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }

            })

            AlertDialog.Builder(requireContext(), R.style.CustomDialog)
                .setTitle("사진 정보")
                .setView(dialogview)
                .setPositiveButton("삭제") { _, _ ->
                    drawingView.deleteSelectedGallery()
                }
                .setNegativeButton("닫기", null)
                .show()
        }//이미지 눌렀을때 띄워버리기

        var colorRedbutton = binding.btnColorRed
        var colorBluebutton=binding.btnColorBlue
        var colorBlackbutton=binding.btnColorBlack
        var Eraserbutton=binding.btnEraser

        val panModeButton = binding.btnPanMode
        panModeButton.setOnClickListener {
            drawingView.isPanMode=!drawingView.isPanMode
            panModeButton.text = if (drawingView.isPanMode) "M" else "D"
        }

        colorRedbutton.setOnClickListener{
            viewModel.setColor(Color.RED)
            viewModel.setStrokeWidth(8f)
            if(drawingView.isPanMode){
                drawingView.isPanMode=!drawingView.isPanMode
                panModeButton.text = if (drawingView.isPanMode) "M" else "D"
            }
            drawingView.setPaintStyle(Color.RED,viewModel.currentStrokeWidth)
        }
        colorBluebutton.setOnClickListener{
            viewModel.setColor(Color.BLUE)
            viewModel.setStrokeWidth(8f)
            if(drawingView.isPanMode){
                drawingView.isPanMode=!drawingView.isPanMode
                panModeButton.text = if (drawingView.isPanMode) "M" else "D"
            }
            drawingView.setPaintStyle(Color.BLUE,viewModel.currentStrokeWidth)
        }
        colorBlackbutton.setOnClickListener{
            viewModel.setColor(Color.BLACK)
            viewModel.setStrokeWidth(8f)
            if(drawingView.isPanMode){
                drawingView.isPanMode=!drawingView.isPanMode
                panModeButton.text = if (drawingView.isPanMode) "M" else "D"
            }
            drawingView.setPaintStyle(Color.BLACK,viewModel.currentStrokeWidth)
        }
        Eraserbutton.setOnClickListener{
            viewModel.setColor(Color.WHITE)
            viewModel.setStrokeWidth(40f)
            if(drawingView.isPanMode){
                drawingView.isPanMode=!drawingView.isPanMode
                panModeButton.text = if (drawingView.isPanMode) "M" else "D"
            }
            drawingView.setPaintStyle(Color.WHITE,viewModel.currentStrokeWidth)
        }

        drawingView.setPaintStyle(viewModel.currentColor, viewModel.currentStrokeWidth)
        drawingView.onStrokeCreated={stroke -> viewModel.addStroke(stroke)}
        viewModel.placedGalleries.observe(viewLifecycleOwner) { galleries ->
            drawingView.placedGalleries = galleries ?: mutableListOf()
            drawingView.invalidate()
        }

        viewModel.isPlacingGallery.observe(viewLifecycleOwner) { placing ->
            drawingView.setPlacingGallery(placing)
        }
        viewModel.pendingGallery.observe(viewLifecycleOwner) { uri ->
            drawingView.setPendingGallery(uri)
        }
        viewModel.placedGalleries.observe(viewLifecycleOwner) { galleries ->
            drawingView.placedGalleries = galleries ?: mutableListOf()
            drawingView.invalidate()
        }

        viewModel.strokes.observe(viewLifecycleOwner){strokes ->
            drawingView.strokes=strokes
            drawingView.invalidate()
        }
        viewModel.placedImages.observe(viewLifecycleOwner){placedImages ->
            drawingView.placedImages=placedImages
            drawingView.invalidate()
        }
        viewModel.placedTexts.observe(viewLifecycleOwner){placedTexts ->
            drawingView.placedTexts=placedTexts
            drawingView.invalidate()
        }

        viewModel.offsetX.observe(viewLifecycleOwner){offsetX ->
            drawingView.offsetX=offsetX
            drawingView.invalidate()
        }
        viewModel.offsetY.observe(viewLifecycleOwner){offsetY ->
            drawingView.offsetY=offsetY
            drawingView.invalidate()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}