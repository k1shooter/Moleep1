package com.example.moleep1.ui.notifications

import android.graphics.Color

data class PlacedText(
    var text: String,
    var x: Float,
    var y: Float,
    var textSize: Float = 40f,
    var color: Int = Color.BLACK
)