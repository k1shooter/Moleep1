// ui/dashboard/DashboardViewModel.kt
package com.example.moleep1.ui.dashboard

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.moleep1.R

// ❗ Application을 상속받아 Context를 안전하게 사용
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = DashboardPrefsManager(application)

    // ❗ ViewModel 생성 시 SharedPreferences에서 데이터 불러오기
    private val _imageList = MutableLiveData<MutableList<Uri>>().apply {
        value = prefsManager.loadUriList()
    }
    val imageList: LiveData<MutableList<Uri>> = _imageList

    init {
        if (prefsManager.isFirstLaunch()) {
            // 앱 최초 실행 시
            val initialUris = createInitialDrawableUris()
            _imageList.value = initialUris
            prefsManager.saveUriList(initialUris)
            prefsManager.setFirstLaunchCompleted() // 최초 실행 완료 플래그 저장
        } else {
            // 앱 최초 실행이 아닐 경우, 저장소에서 불러오기
            _imageList.value = prefsManager.loadUriList()
        }
    }

    private fun createInitialDrawableUris(): MutableList<Uri> {
        val context = getApplication<Application>().applicationContext
        val initialDrawableIds = listOf(R.drawable.pic1, R.drawable.pic2, R.drawable.pic3)
        val uriList = mutableListOf<Uri>()
        initialDrawableIds.forEach { resId ->
            val uri = "android.resource://${context.packageName}/$resId".toUri()
            uriList.add(uri)
        }
        return uriList
    }

    // 리스트에 이미지 추가
    fun addImage(uri: Uri) {
        // ❗ 영구적인 읽기 권한 획득 (매우 중요)
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        val list = _imageList.value ?: mutableListOf()
        list.add(uri)
        _imageList.value = list

        // ❗ 목록 변경 시 SharedPreferences에 저장
        prefsManager.saveUriList(list)
    }

    // 전체 리스트 교체
    fun setList(newList: List<Uri>) {
        _imageList.value = newList.toMutableList()
        // ❗ 목록 변경 시 SharedPreferences에 저장
        prefsManager.saveUriList(newList)
    }
}