package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri

/**
 * 갤러리 이미지 EXIF 회전각 자동 보정 및 OOM 방지 다운샘플링 디코딩 함수 (단일 책임 원칙 적용 및 UI 분리)
 */
fun decodeGalleryUriWithExifCorrection(context: Context, uri: Uri): Bitmap? {
    return try {
        var rotationDegrees = 0
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }

        // 1단계: OOM 방지를 위해 비트맵 메모리 할당 없이 원본 해상도(Bounds)만 먼저 측정
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        val origWidth = boundsOptions.outWidth
        val origHeight = boundsOptions.outHeight
        if (origWidth <= 0 || origHeight <= 0) {
            android.util.Log.w("ScannerOcrDialog", "⚠️ [갤러리] 유효하지 않은 이미지 크기: ${origWidth}x${origHeight}")
            return null
        }

        // 2단계: Google ML Kit OCR에 최적인 최대 1280px 해상도로 inSampleSize(2의 거듭제곱) 계산
        val maxDimension = 1280
        var sampleSize = 1
        while ((origWidth / sampleSize) > maxDimension || (origHeight / sampleSize) > maxDimension) {
            sampleSize *= 2
        }

        // 3단계: 다운샘플링 적용하여 안전하게 디코딩
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        var loadedBitmap: Bitmap? = null
        context.contentResolver.openInputStream(uri)?.use { stream ->
            loadedBitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
        }

        val rawBitmap = loadedBitmap ?: run {
            android.util.Log.e("ScannerOcrDialog", "❌ [갤러리] 다운샘플링 비트맵 디코딩 실패 (null)")
            return null
        }

        // 4단계: EXIF 회전 보정 (회전 시 원본 임시 비트맵 recycle 호출로 메모리 누수 즉각 해제)
        if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotatedBitmap = Bitmap.createBitmap(
                rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
            )
            if (rotatedBitmap != rawBitmap) {
                rawBitmap.recycle()
            }
            rotatedBitmap
        } else {
            rawBitmap
        }
    } catch (e: Throwable) {
        android.util.Log.e("ScannerOcrDialog", "❌ [갤러리] 이미지 로드 중 예외 발생: ${e.message}", e)
        null
    }
}
