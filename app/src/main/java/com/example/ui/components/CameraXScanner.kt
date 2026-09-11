package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.backend.OcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

import androidx.compose.foundation.clickable

/**
 * [CameraXLiveScanner]
 * - 앱 내부에서 직접 동작하는 초경량 실시간 카메라 스캐너 컴포저블.
 * - 시스템 기본 카메라 앱 전환 없이 화면 내에서 즉시 미리보기를 제공하며,
 *   화면 터치 시 현재 비디오 프레임을 캡처하여 고해상도 비트맵으로 변환합니다.
 */
@Composable
fun CameraXLiveScanner(
    onBitmapCaptured: (Bitmap) -> Unit, // 캡처된 비트맵을 OCR 엔진으로 전달하는 콜백
    onClose: () -> Unit                 // 카메라 뷰 닫기 콜백
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // [스레드 안전 캡처 플래그] 멀티스레드 레이스 컨디션 및 중복 캡처 방지를 위한 원자적 불리언
    val captureNextFrame = remember { AtomicBoolean(false) }

    // [카메라 분석 스레드풀] UI 프레임 드랍을 막기 위한 백그라운드 단일 실행기
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // [자원 해제] 컴포저블 생명주기 종료 시 백그라운드 스레드풀 자동 셧다운
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { captureNextFrame.set(true) } // 화면 터치 시 다음 프레임 캡처 트리거
    ) {
        // AndroidView를 통해 CameraX PreviewView 연동
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    // 실시간 카메라 미리보기 UseCase
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // 프레임 분석기 UseCase: 최신 프레임만 유지하여 메모리 버퍼 오버플로우 방지
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                // 터치 이벤트 발생 시 원자적으로 1회 캡처 수행
                                if (captureNextFrame.compareAndSet(true, false)) {
                                    val bitmap = imageProxy.toBitmapSafely()
                                    if (bitmap != null) {
                                        coroutineScope.launch(Dispatchers.Main) {
                                            onBitmapCaptured(bitmap)
                                        }
                                    }
                                }
                                imageProxy.close() // 버퍼 반환 필수
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalyzer
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 스캔 영역 가이드 UI (중앙 타겟 박스)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(300.dp, 150.dp)
                .border(2.dp, Color.Green, RoundedCornerShape(12.dp))
                .background(Color.Green.copy(alpha = 0.1f))
        ) {
            Text(
                "화면을 터치하면 다중 고장 코드를 캡처합니다",
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }

        // 닫기 버튼
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
        ) {
            Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
        }
    }
}

/**
 * [ImageProxy.toBitmapSafely]
 * - CameraX의 기본 YUV_420_888 프레임을 NV21 바이트 배열로 조립 후 JPEG 압축을 거쳐 Bitmap으로 변환.
 * - 센서 회전 각도(rotationDegrees)를 적용하여 정방향 비트맵을 반환합니다.
 */
fun ImageProxy.toBitmapSafely(): Bitmap? {
    val image = this.image ?: return null
    if (image.format != ImageFormat.YUV_420_888) return null
    
    try {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        val matrix = Matrix()
        matrix.postRotate(this.imageInfo.rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) {
        return null
    }
}
