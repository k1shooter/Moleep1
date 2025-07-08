package com.example.moleep1.ui.notifications

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.moleep1.ListViewAdapter
import com.example.moleep1.databinding.FragmentNotificationsBinding
import com.example.moleep1.R
import com.example.moleep1.ui.DrawingView
import com.example.moleep1.ui.home.HomeViewModel
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import com.example.moleep1.ui.PrefsManager
import com.example.moleep1.ui.home.HomeViewModelFactory


class NotificationsFragment : Fragment() {




    private lateinit var adapter: ListViewAdapter


    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: NotificationsViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val drawerLayout=view.findViewById<DrawerLayout>(R.id.drawerLayout)
        val btnOpenSidebar=view.findViewById<ImageButton>(R.id.btnOpenSidebar)
        val btnClear=view.findViewById<ImageButton>(R.id.btnClear)
        val btnAddText=view.findViewById<ImageButton>(R.id.btnAddText)
        val listView=view.findViewById<ListView>(R.id.listView)
        val drawingView=view.findViewById<DrawingView>(R.id.drawingView)

        val prefsManager = PrefsManager(requireContext())
        val factory = HomeViewModelFactory(prefsManager)
        val homeViewModel = ViewModelProvider(this, factory).get(HomeViewModel::class.java)

        val adapter = ListViewAdapter(requireContext(), ArrayList(homeViewModel.itemList.value?: emptyList()))
        listView.adapter=adapter

        homeViewModel.itemList.observe(viewLifecycleOwner) { items ->
            // 어댑터에 데이터 업데이트 (Adapter 내부에 데이터 교체 메소드가 있다고 가정)
            // ex) adapter.updateData(items) 또는 adapter.submitList(items)
            adapter.setItems(ArrayList(items)) // setItems가 데이터를 교체하는 메소드라고 가정
            adapter.notifyDataSetChanged() // ListView는 notifyDataSetChanged()가 필요
        }


        val btnSave = view.findViewById<ImageButton>(R.id.btnSave)

        btnSave.setOnClickListener {
            // 1. 권한 체크
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                // 2. 권한이 없으면 요청
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1001
                )
                return@setOnClickListener // 권한 허용 후 다시 시도하도록 종료
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
        }

        btnAddText.setOnClickListener {
            val editText= EditText(requireContext())
            AlertDialog.Builder(requireContext())
                .setTitle("텍스트 추가")
                .setView(editText)
                .setPositiveButton("추가") { _, _ ->
                    // 다음 터치 위치에 텍스트박스 추가 대기
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
                drawingView.addProfileImage(item.imageUri, adapter.getPosition(item))
                homeViewModel.selectedItem.value = null
            } else {
                drawingView.addProfileImage(null, 0)
                //homeViewModel.selectedItem.value = null
            }
        }

        viewModel.isPlacingImage.observe(viewLifecycleOwner) { placing ->
            drawingView.setPlacingImage(placing)
        }
        viewModel.pendingImageUri.observe(viewLifecycleOwner) { uri ->
            drawingView.setPendingImageUri(uri)
        }

        drawingView.setPlacingImage(false)
        drawingView.setPendingImageUri(null)
        //선택된 아이템이 바뀐다면?
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
        val prefsManager = PrefsManager(requireContext())
        val factory = HomeViewModelFactory(prefsManager)
        val homeViewModel = ViewModelProvider(this, factory).get(HomeViewModel::class.java)

        drawingView.onImageSelectedListener = { img ->
            homeViewModel.selectItemByPosition(img.position)

            val dialogview= LayoutInflater.from(requireContext()).inflate(R.layout.image_selected_layout,null,false)
            val tvProfileInfo=dialogview.findViewById<TextView>(R.id.tvProfileInfo)
            val seekBarSize=dialogview.findViewById<SeekBar>(R.id.seekBarSize)

            tvProfileInfo.text="이름: ${homeViewModel.viewedItem.value.name}\n특이사항: ${homeViewModel.viewedItem.value.desc}"
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

            AlertDialog.Builder(requireContext())
                .setTitle("프로필 정보")
                .setView(dialogview)
                .setPositiveButton("삭제") { _, _ ->
                    drawingView.deleteSelectedImage()
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