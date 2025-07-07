package com.example.moleep1.ui.notifications

import android.graphics.Path
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NotificationsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }
    val text: LiveData<String> = _text

    val paths = MutableLiveData<MutableList<Path>>(mutableListOf())

    fun addPath(path: Path) {
        val updated = paths.value ?: mutableListOf()
        updated.add(path)
        paths.value = updated
    }


    val isPlacingImage = MutableLiveData<Boolean>(false)
    val pendingImageUri = MutableLiveData<String?>(null)

    val strokes = MutableLiveData<MutableList<Stroke>>(mutableListOf())
    val placedImages = MutableLiveData<MutableList<PlacedImage>>(mutableListOf())
    var currentColor: Int=0xFF000000.toInt()
    var currentStrokeWidth: Float=8f

    fun addStroke(stroke: Stroke){
        val updated = strokes.value?: mutableListOf()
        updated.add(stroke)
        strokes.value=updated
    }

    fun addPlacedImages(image: PlacedImage){
        val updated = placedImages.value?: mutableListOf()
        updated.add(image)
        placedImages.value=updated
    }

    fun setColor(color: Int){
        currentColor=color
    }

    fun setStrokeWidth(width: Float){
        currentStrokeWidth=width
    }
}