package com.example.moleep1.ui.notifications

import android.graphics.Bitmap

data class PlacedImage(
    val bitmap: Bitmap,
    val x: Float,
    val y: Float,
    var width: Int,
    var height: Int,
    val position: Int
)