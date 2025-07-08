// com.example.moleep1.ui.added.ImageUtils.kt

package com.example.moleep1.ui.added

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

object ImageUtils {

    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val sourceBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * [수정] Bitmap 이미지를 중앙 기준으로 정사각형으로 먼저 자른 후, 원형으로 만듭니다.
     */
    fun cropToCircle(bitmap: Bitmap): Bitmap {
        // 1. 이미지를 정사각형으로 먼저 자르기
        val squareBitmap = if (bitmap.width == bitmap.height) {
            bitmap
        } else {
            val size = minOf(bitmap.width, bitmap.height)
            val x = (bitmap.width - size) / 2
            val y = (bitmap.height - size) / 2
            Bitmap.createBitmap(bitmap, x, y, size, size)
        }

        // 2. 정사각형 비트맵을 원형으로 만들기
        val output = Bitmap.createBitmap(squareBitmap.width, squareBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, squareBitmap.width, squareBitmap.height)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(squareBitmap.width / 2f, squareBitmap.height / 2f, squareBitmap.width / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(squareBitmap, rect, rect, paint)
        return output
    }

    fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, false)
    }
}