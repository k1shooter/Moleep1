package com.example.moleep1.ui
import android.util.AttributeSet
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.view.View
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.example.moleep1.ui.notifications.Stroke
import com.example.moleep1.ui.notifications.PlacedImage
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import android.graphics.ImageDecoder
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.moleep1.ui.notifications.PlacedText
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import com.example.moleep1.R
import com.example.moleep1.ui.notifications.PlacedGallery

class DrawingView(context: Context, attrs: AttributeSet?) : View(context,attrs) {


    var placedImages = mutableListOf<PlacedImage>()
    var placedTexts = mutableListOf<PlacedText>()
    var placedGalleries = mutableListOf<PlacedGallery>()


    private var pendingImage: Bitmap? = null
    private var pendingPosition: Int = 0
    private var pendingId: String? = null
    private var pendingImageWidth = 200
    private var pendingImageHeight = 200
    private var isPlacingImage = false

    private var isPlacingGal = false
    private var pendingGal: Bitmap? = null
    private var pendingGalWidth = 200
    private var pendingGalHeight = 200

    var pendingText: String? = null
    var isPlacingText = false

    var onPanZoomChanged: ((Float, Float, Float) -> Unit)? = null

    fun clearCanvas() {
        placedImages.clear()
        placedTexts.clear()
        strokes = emptyList()
        pendingImage = null // 선택된 이미지도 해제
        invalidate()
    }

    fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }
    fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        return try {
            val file = File(context.getExternalFilesDir(null), fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Boolean {
        @Suppress("DEPRECATION")
        val savedImageURL = MediaStore.Images.Media.insertImage(
            context.contentResolver,
            bitmap,
            title,
            "그림판에서 저장한 이미지"
        )
        return savedImageURL != null
    }

    // 패닝/줌 값이 바뀔 때 호출
    private fun notifyPanZoomChanged() {
        onPanZoomChanged?.invoke(offsetX, offsetY, scaleFactor)
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

    fun setPlacingImage(flag: Boolean) { isPlacingImage = flag; invalidate() }
    fun setPendingImageUri(uriString: String?) {
        if (uriString.isNullOrEmpty()) {
            pendingImage = null
            invalidate()
            return
        }
        pendingImage = uriToBitmap(context, uriString)
        invalidate()
    }

    fun setPlacingGallery(flag: Boolean) { isPlacingGal = flag; invalidate() }
    fun setPendingGallery(uriString: String?) {
        if (uriString.isNullOrEmpty()) {
            pendingGal = null
            invalidate()
            return
        }
        pendingGal = uriToBitmap(context, uriString)
        invalidate()
    }

    var onImageSelectedListener: ((PlacedImage) -> Unit)? = null
    private var selectedImageIndex: Int? = null

    var onGallerySelectedListener: ((PlacedGallery) -> Unit)? = null
    private var selectedGalleryIndex: Int? = null

    fun deleteSelectedImage() {
        selectedImageIndex?.let {
            placedImages.removeAt(it)
            selectedImageIndex = null
            invalidate()
        }
    }

    fun deleteSelectedGallery() {
        selectedGalleryIndex?.let {
            placedGalleries.removeAt(it)
            selectedGalleryIndex = null
            invalidate()
        }
    }

    fun addProfileImage(imageUri: String?, id : String) {
        if (imageUri.isNullOrEmpty()) return
        Glide.with(this)
            .asBitmap()
            .load(imageUri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    if (!isAttachedToWindow) return
                    pendingImage = Bitmap.createScaledBitmap(resource, pendingImageWidth, pendingImageHeight, true)
                    isPlacingImage = true // 다음 클릭에서 위치 확정
                    pendingId=id
                    invalidate()
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    pendingImage = null
                    pendingId=null
                    isPlacingImage = false
                }
            })
    }

    fun addGalleryImage(imageUri: String?) {
        if (imageUri.isNullOrEmpty()) return
        Glide.with(this)
            .asBitmap()
            .load(imageUri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    if (!isAttachedToWindow) return
                    pendingGal = Bitmap.createScaledBitmap(resource, pendingGalWidth, pendingGalHeight, true)
                    isPlacingGal = true // 다음 클릭에서 위치 확정
                    invalidate()
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    pendingGal = null
                    isPlacingGal = false
                }
            })
    }

    var onStrokeCreated: ((Stroke)->Unit)?=null
    var strokes: List<Stroke> = emptyList()
    private var currentPath: Path? = null
    private var currentPaintColor: Int=0xFF000000.toInt()
    private var currentStrokeWidth: Float=8f

    var scaleFactor = 1.0f
    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener(){
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor*=detector.scaleFactor
            scaleFactor=scaleFactor.coerceIn(0.1f, 5.0f)
            invalidate()
            return true
        }
    })
    //핀치 줌

    var offsetX = 0f
    var offsetY = 0f
    private var lastPanX = 0f
    private var lastPanY = 0f
    private var isPanning = false
    //패닝

    var isPanMode = false



    fun setPaintStyle(color: Int, strokeWidth: Float){
        currentPaintColor=color
        currentStrokeWidth=strokeWidth
    }


    private val textureBitmap = BitmapFactory.decodeResource(resources, R.drawable.boardtex)

    override fun onDraw(canvas: Canvas) {
        // 1. 텍스처로 캔버스 전체 채우기

        // 2. 그 위에 기존의 그림, 이미지, 텍스트 등 원하는 요소를 그리기
        super.onDraw(canvas)

        val centerX=width/2f
        val centerY = height/2f
        canvas.save()
        canvas.translate(offsetX,offsetY)
        canvas.scale(scaleFactor,scaleFactor, centerX-offsetX, centerY-offsetY)




        for ((i, img) in placedGalleries.withIndex()) {
            val w = img.width.coerceAtLeast(10)
            val h = img.height.coerceAtLeast(10)
            val scaledBitmap = Bitmap.createScaledBitmap(img.bitmap, w, h, true)
            canvas.drawBitmap(scaledBitmap, img.x, img.y, null)
            // 선택된 이미지는 테두리 표시
//            if (i == selectedImageIndex) {
//                val paint = Paint().apply {
//                    color = Color.RED
//                    style = Paint.Style.STROKE
//                    strokeWidth = 5f
//                }
//                canvas.drawRect(img.x, img.y, img.x + img.width, img.y + img.height, paint)
//            }
        }

        for ((i, img) in placedImages.withIndex()) {
            val w = img.width.coerceAtLeast(10)
            val h = img.height.coerceAtLeast(10)
            val scaledBitmap = Bitmap.createScaledBitmap(img.bitmap, w, h, true)
            canvas.drawBitmap(scaledBitmap, img.x, img.y, null)
            // 선택된 이미지는 테두리 표시
            if (i == selectedImageIndex) {
                val paint = Paint().apply {
                    color = Color.RED
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                }
                canvas.drawRect(img.x, img.y, img.x + img.width, img.y + img.height, paint)
            }
        }

        for (txt in placedTexts) {
            val paint = Paint().apply {
                color = txt.color
                textSize = txt.textSize
                isAntiAlias = true
            }
            canvas.drawText(txt.text, txt.x, txt.y, paint)
        }

        for(stroke in strokes){
            val paint = Paint().apply{
                this.color = stroke.color
                style = Paint.Style.STROKE
                this.strokeWidth = stroke.strokeWidth
                isAntiAlias = true
            }
            canvas.drawPath(stroke.path,paint)
        }

        currentPath?.let{
            val paint = Paint().apply{
                this.color = currentPaintColor
                style = Paint.Style.STROKE
                this.strokeWidth = currentStrokeWidth
                isAntiAlias = true
            }
            canvas.drawPath(it,paint)
        }



        canvas.restore()


    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {

            if (isPlacingText && pendingText != null && event.action == MotionEvent.ACTION_DOWN) {

                // 1. 임시 Paint로 텍스트 폭, baseline 계산
                val paint = Paint().apply {
                    textSize = 40f // PlacedText에 저장할 크기와 동일하게!
                    // color 등은 필요 없음
                }
                val textWidth = paint.measureText(pendingText!!)
                val baselineOffset = -paint.ascent()
                val textHeight = paint.descent() - paint.ascent()

                // 2. 좌표 변환
                val centerX = width / 2f
                val centerY = height / 2f
                val cx = centerX - offsetX
                val cy = centerY - offsetY
                val canvasX = (event.x - offsetX - cx) / scaleFactor + cx
                val canvasY = (event.y - offsetY - cy) / scaleFactor + cy

                // 3. 중앙 정렬 보정
                val x = canvasX - textWidth / 2f
                val y = canvasY + baselineOffset - textHeight / 2f

                // 4. PlacedText에 저장
                placedTexts.add(
                    PlacedText(
                        text = pendingText!!,
                        x = x,
                        y = y,
                    )
                )
                pendingText = null
                isPlacingText = false
                invalidate()
            }


            if (isPlacingImage && pendingImage != null && event.action == MotionEvent.ACTION_DOWN) {
                // 클릭한 위치에 이미지를 추가
                val centerX = width / 2f
                val centerY = height / 2f
                val cx = centerX - offsetX
                val cy = centerY - offsetY
                val canvasX = (event.x - offsetX - cx) / scaleFactor + cx
                val canvasY = (event.y - offsetY - cy) / scaleFactor + cy

                val x = canvasX - pendingImageWidth / 2f
                val y = canvasY - pendingImageHeight / 2f
                placedImages.add(
                    PlacedImage(
                        bitmap = pendingImage!!,
                        x = x,
                        y = y,
                        width = pendingImageWidth,
                        height = pendingImageHeight,
                        id = pendingId.toString()
                    )
                )
                // 다음 클릭을 위해 초기화
                pendingImage = null
                isPlacingImage = false
                invalidate()


            }

            if (isPlacingGal && pendingGal != null && event.action == MotionEvent.ACTION_DOWN) {
                // 클릭한 위치에 이미지를 추가
                val centerX = width / 2f
                val centerY = height / 2f
                val cx = centerX - offsetX
                val cy = centerY - offsetY
                val canvasX = (event.x - offsetX - cx) / scaleFactor + cx
                val canvasY = (event.y - offsetY - cy) / scaleFactor + cy

                val x = canvasX - pendingGalWidth / 2f
                val y = canvasY - pendingGalHeight / 2f

                placedGalleries.add(
                    PlacedGallery(
                        bitmap = pendingGal!!,
                        x = x,
                        y = y,
                        width = pendingGalWidth,
                        height = pendingGalHeight,
                    )
                )
                // 다음 클릭을 위해 초기화
                pendingGal = null
                isPlacingGal = false

                invalidate()




            }

            scaleGestureDetector.onTouchEvent(event)
            //if(event.pointerCount>1) return true

            if(isPanMode){
                val sensitivity=1.5f

                when(event.actionMasked){
                    MotionEvent.ACTION_DOWN -> {
                        lastPanX = event.x
                        lastPanY = event.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx=event.x-lastPanX
                        val dy = event.y-lastPanY
                        offsetX +=dx*sensitivity
                        offsetY +=dy*sensitivity
                        lastPanX = event.x
                        lastPanY = event.y
                        notifyPanZoomChanged()
                        invalidate()
                        return true
                    }

                }

                if (event.action == MotionEvent.ACTION_DOWN) {
                    // 터치 좌표를 캔버스 좌표계로 변환
                    val centerX = width / 2f
                    val centerY = height / 2f
                    val cx = centerX - offsetX
                    val cy = centerY - offsetY
                    val canvasX = (event.x - offsetX - cx) / scaleFactor + cx
                    val canvasY = (event.y - offsetY - cy) / scaleFactor + cy

                    // 이미지 클릭 여부 판정 (여러 이미지가 겹칠 경우, 마지막에 그린 것 우선)
                    for (i in placedImages.indices.reversed()) {
                        val img = placedImages[i]
                        if (canvasX >= img.x && canvasX <= img.x + img.width &&
                            canvasY >= img.y && canvasY <= img.y + img.height) {
                            selectedImageIndex = i
                            // 리스너나 콜백으로 외부에 알림 (정보 보여주기, 삭제 등)
                            onImageSelectedListener?.invoke(img)
                            invalidate()
                            return true
                        }
                    }
                    for (i in placedGalleries.indices.reversed()) {
                        val img = placedGalleries[i]
                        if (canvasX >= img.x && canvasX <= img.x + img.width &&
                            canvasY >= img.y && canvasY <= img.y + img.height) {
                            selectedGalleryIndex = i
                            // 리스너나 콜백으로 외부에 알림 (정보 보여주기, 삭제 등)
                            onGallerySelectedListener?.invoke(img)
                            invalidate()
                            return true
                        }
                    }
                    selectedImageIndex = null
                    selectedGalleryIndex = null
                    invalidate()
                    return true
                }
                return true
            }

            val centerX=width/2f
            val centerY = height/2f

            val cx = centerX - offsetX
            val cy = centerY - offsetY

            val x = (event.x - offsetX - cx) / scaleFactor + cx
            val y = (event.y - offsetY - cy) / scaleFactor + cy
            when(event.action){
                MotionEvent.ACTION_DOWN->{
                    currentPath=Path().apply{
                        moveTo(x,y)
                    }
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE->{
                    currentPath?.lineTo(x,y)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    currentPath?.let{
                        onStrokeCreated?.invoke(
                            Stroke(
                                path = it,
                                color = currentPaintColor,
                                strokeWidth = currentStrokeWidth
                            )
                        )
                    }
                    currentPath=null
                    invalidate()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)

    }

//    fun setPanMode(enabled: Boolean){
//        isPanMode = enabled
//    }
}