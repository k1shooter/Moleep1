package com.example.moleep1.ui.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Path
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NotificationsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }
    val text: LiveData<String> = _text


    val isPlacingImage = MutableLiveData<Boolean>(false)
    val pendingImageUri = MutableLiveData<String?>(null)

    val isPlacingGallery = MutableLiveData<Boolean>(false)
    val pendingGallery = MutableLiveData<String?>(null)

    fun setPendingGallery(uri: String?) {
        pendingGallery.value = uri
    }
    fun setIsPlacingGallery(placing: Boolean) {
        isPlacingGallery.value = placing
    }


    val placedGalleries = MutableLiveData<MutableList<PlacedGallery>>(mutableListOf())




    val strokes = MutableLiveData<MutableList<Stroke>>(mutableListOf())
    val placedImages = MutableLiveData<MutableList<PlacedImage>>(mutableListOf())


    fun removeImagesByProfileId(profileId: String) {
        val currentList = placedImages.value.orEmpty()
        val filtered = currentList.filter { it.id != profileId }.toMutableList()
        placedImages.value = filtered
    }

    fun clearAllPlacedImages(){
        placedImages.value= mutableListOf()
    }

    val offsetX = MutableLiveData(0f)
    val offsetY = MutableLiveData(0f)


    var currentColor: Int=0xFF000000.toInt()
    var currentStrokeWidth: Float=8f

    val placedTexts = MutableLiveData<MutableList<PlacedText>>(mutableListOf())


    fun addStroke(stroke: Stroke){
        val updated = strokes.value?: mutableListOf()
        updated.add(stroke)
        strokes.value=updated
    }

    fun clearCanvas() {
        placedImages.value=mutableListOf()
        placedTexts.value=mutableListOf()
        placedGalleries.value=mutableListOf()
        strokes.value = mutableListOf()
    }


    fun uriToBitmap(context: Context, uriString: String): Bitmap? {
        val uri = Uri.parse(uriString)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun updatePlacedImageBitmapById(targetId: String, newUri: String, context: Context) {
        placedImages.value = placedImages.value?.map { placedImage ->
            if (placedImage.id == targetId) {
                val newBitmap = uriToBitmap(context, newUri)
                placedImage.copy(
                    bitmap = newBitmap ?: placedImage.bitmap
                )
            } else placedImage
        } as MutableList<PlacedImage>?
    }


    fun setColor(color: Int){
        currentColor=color
    }

    fun setStrokeWidth(width: Float){
        currentStrokeWidth=width
    }
}