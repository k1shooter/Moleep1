package com.example.moleep1.ui.added // 사용자의 패키지명

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.example.moleep1.ui.added.MapCapture
import com.kakao.vectormap.MapView
import com.kakao.vectormap.graphics.gl.GLSurfaceView
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MapCaptureHelper {

    companion object {
        private const val TAG = "MapCaptureHelper"
    }

    fun captureMapAndSaveToGallery(context: Context, mapView: MapView?, onCaptureComplete: (Boolean, Uri?) -> Unit) {
        if (mapView == null) {
            Toast.makeText(context, "지도가 준비되지 않았습니다.", Toast.LENGTH_SHORT).show()
            onCaptureComplete(false, null)
            return
        }

        val surfaceView = mapView.surfaceView as? GLSurfaceView // KakaoMap v2 기준
        // 만약 mapView.surfaceView가 GLSurfaceView를 직접 반환하지 않는 구버전이거나 다른 구조라면,
        // mapView.getChildAt(0) as? GLSurfaceView 등으로 실제 GLSurfaceView를 가져와야 합니다.
        // 또는 MapView 자체를 전달해야 할 수도 있습니다 (MapCapture.java의 파라미터 타입 확인 필요).
        // 제공된 MapCapture.java는 GLSurfaceView를 파라미터로 받습니다.

        if (surfaceView == null) {
            Toast.makeText(context, "캡처할 수 있는 지도 뷰를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Failed to get GLSurfaceView from MapView. MapView class: ${mapView::class.java.name}")
            // 예: if (mapView.getChildAt(0) is GLSurfaceView) { surfaceView = mapView.getChildAt(0) as GLSurfaceView }
            onCaptureComplete(false, null)
            return
        }

        // MapCapture.java의 capture 메소드는 Activity를 첫 번째 인자로 받습니다.
        // Context가 Activity 타입인지 확인하고 캐스팅하거나, Activity 인스턴스를 직접 전달해야 합니다.
        val activity = context as? android.app.Activity
        if (activity == null) {
            Toast.makeText(context, "캡처를 위해서는 Activity 컨텍스트가 필요합니다.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Context is not an Activity instance.")
            onCaptureComplete(false, null)
            return
        }

        // com.kakao.maps.open.android.MapCapture.capture 호출
        MapCapture.capture(activity, surfaceView, object : MapCapture.OnCaptureListener {
            override fun onCaptured(isSucceed: Boolean, filePath: String?) { // filePath는 MapCapture.java 내부에서 생성된 파일명
                if (isSucceed && filePath != null) {
                    // MapCapture.java는 이미지를 /DCIM/MapCaptureDemo/ 에 저장합니다.
                    // filePath는 전체 경로가 아니라 파일 이름("MapCapture_xxx.png")만 반환합니다.
                    // 따라서, 실제 저장된 전체 경로를 구성해야 합니다.
                    val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    val captureDir = File(dcimDir, "MapCaptureDemo")
                    val imageFile = File(captureDir, filePath) // filePath가 파일명만 반환한다고 가정

                    Log.d(TAG, "Map captured by MapCapture.java. File: ${imageFile.absolutePath}")

                    if (imageFile.exists()) {
                        // MapCapture.java가 이미 DCIM에 저장했으므로,
                        // 추가로 MediaStore에 등록하여 갤러리에 바로 보이도록 할 수 있습니다.
                        // 또는, 이 파일을 앱 내부 저장소로 복사 후 MediaStore에 저장할 수도 있습니다.
                        // 여기서는 이미 저장된 파일을 기준으로 MediaStore에 정보만 추가하거나,
                        // BitmapFactory로 읽어 다시 저장하는 방식을 사용할 수 있습니다.

                        // 간단하게 하기 위해, 이미 저장된 파일을 대상으로 MediaStore에 등록하는 과정을 흉내내거나,
                        // BitmapFactory로 읽어서 MediaStore에 다시 저장합니다.
                        // 더 나은 방법은 MapCapture.java가 반환하는 Bitmap을 직접 받아 MediaStore에 저장하는 것입니다.
                        // 현재 MapCapture.java는 파일 경로만 반환하고, 저장 로직까지 포함하고 있습니다.

                        val savedUri = saveImageToGalleryFromExistingFile(context, imageFile)
                        if (savedUri != null) {
                            Toast.makeText(context, "캡처 성공 및 갤러리 반영 완료", Toast.LENGTH_SHORT).show()
                            onCaptureComplete(true, savedUri)
                        } else {
                            Toast.makeText(context, "캡처 성공, 갤러리 반영 실패", Toast.LENGTH_SHORT).show()
                            onCaptureComplete(true, null) // 캡처는 성공했으나 저장은 실패
                        }
                    } else {
                        Log.e(TAG, "Captured file (from MapCapture.java) does not exist at path: ${imageFile.absolutePath}")
                        Toast.makeText(context, "캡처된 파일을 찾을 수 없습니다 (MapCapture.java).", Toast.LENGTH_SHORT).show()
                        onCaptureComplete(false, null)
                    }
                } else {
                    Log.e(TAG, "MapCapture.java failed. isSucceed: $isSucceed, filePath: $filePath")
                    Toast.makeText(context, "지도 캡처에 실패하였습니다 (MapCapture.java).", Toast.LENGTH_SHORT).show()
                    onCaptureComplete(false, null)
                }
            }
        })
    }

    // MapCapture.java가 이미 파일을 저장한 경우, 해당 파일을 MediaStore에 등록하거나
    // BitmapFactory로 읽어 다시 저장하여 갤러리에 표시되도록 하는 함수
    private fun saveImageToGalleryFromExistingFile(context: Context, imageFile: File): Uri? {
        // 이미 파일이 DCIM에 있으므로, MediaScanner를 통해 갤러리에 보이게 할 수 있습니다.
        // 하지만 MediaStore.Images.Media.insertImage() 를 사용하는 것이 좀 더 명시적일 수 있습니다.
        // 여기서는 BitmapFactory로 읽어 MediaStore에 새로 저장하는 방식을 사용합니다.
        // (MapCapture.java가 반환하는 fileName이 전체 경로가 아닌 파일명이라는 점에 유의)

        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        if (bitmap == null) {
            Log.e(TAG, "Failed to decode bitmap from MapCapture.java's output: ${imageFile.absolutePath}")
            return null
        }

        val displayName = "MapCapture_Gallery_${System.currentTimeMillis()}.png"
        val mimeType = "image/png"
        var imageUri: Uri? = null
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "YourAppNameMapCaptures") // 갤러리 내 앱별 폴더
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator + "YourAppNameMapCaptures"
                val dir = File(imagesDir)
                if (!dir.exists()) dir.mkdirs()
                val newGalleryFile = File(imagesDir, displayName)
                // API < 29 에서는 직접 파일 경로를 사용하지만, MediaStore.Images.Media.insertImage가 더 간단할 수 있음
            }
        }

        var fos: OutputStream? = null
        try {
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            imageUri?.let { uri ->
                fos = resolver.openOutputStream(uri)
                fos?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                Log.d(TAG, "Image from MapCapture.java saved to gallery: $uri")

                // MapCapture.java가 생성한 원본 파일을 삭제할지 여부 (선택 사항)
                // imageFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save image from MapCapture.java to gallery", e)
            imageUri?.let { resolver.delete(it, null, null) }
            imageUri = null
        } finally {
            fos?.close()
            bitmap.recycle() // 비트맵 사용 후 메모리 해제
        }
        return imageUri
    }
}
