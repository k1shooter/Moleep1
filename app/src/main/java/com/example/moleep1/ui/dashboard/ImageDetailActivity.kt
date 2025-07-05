package com.example.moleep1.ui.dashboard

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.example.moleep1.databinding.ActivityImageDetailBinding
import kotlin.math.abs

class ImageDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageDetailBinding
    private lateinit var gestureDetector: GestureDetector
    private val imageUris = mutableListOf<Uri>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // DashboardFragment에서 보낸 데이터 받기
        val uris = intent.getParcelableArrayListExtra<Uri>("uris") ?: arrayListOf()
        val currentPosition = intent.getIntExtra("position", 0)

        imageUris.addAll(uris)

        // ViewPager2와 어댑터 연결
        binding.imageViewPager.adapter = ImageDetailAdapter(imageUris)
        binding.imageViewPager.setCurrentItem(currentPosition, false)

        // 삭제 버튼 클릭 리스너
        binding.deleteButton.setOnClickListener {
            if (imageUris.isNotEmpty()) {
                val deletedPosition = binding.imageViewPager.currentItem
                imageUris.removeAt(deletedPosition)
                if (imageUris.isEmpty()) {
                    setResultAndFinish()
                }
                else{
                    binding.imageViewPager.adapter?.notifyItemRemoved(deletedPosition)
                    binding.imageViewPager.adapter?.notifyItemRangeChanged(deletedPosition, imageUris.size)
                }
            }
        }

        // 아래로 스와이프하여 닫기 기능
        setupSwipeToClose()
    }

    // 변경된 이미지 리스트를 결과로 설정하고 액티비티를 종료하는 함수
    private fun setResultAndFinish() {
        val resultIntent = Intent().apply {
            putParcelableArrayListExtra("updated_uris", ArrayList(imageUris))
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
        // 닫힐 때 애니메이션 효과 제거
        overridePendingTransition(0, 0)
    }

    // 뒤로 가기 버튼을 눌렀을 때도 변경된 리스트를 전달
    override fun onBackPressed() {
        setResultAndFinish()
        super.onBackPressed()
    }

    // 아래로 스와이프하여 닫는 기능 설정
    @SuppressLint("ClickableViewAccessibility")
    private fun setupSwipeToClose() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 != null) {
                    // 수직 스와이프 거리가 수평보다 크고, 아래 방향일 때
                    if (abs(e1.y - e2.y) > abs(e1.x - e2.x) && e2.y - e1.y > 200 && abs(velocityY) > 200) {
                        setResultAndFinish()
                        return true
                    }
                }
                return false
            }
        })

        binding.imageViewPager.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            // 터치 이벤트를 ViewPager가 계속 처리하도록 false 반환
            return@setOnTouchListener false
        }
    }
}